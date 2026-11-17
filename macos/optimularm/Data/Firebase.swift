//
//  Firebase.swift
//  optimularm
//

import Foundation

// Firebase over REST, not the SDK: the SDK keeps its sign-in in the data-protection keychain,
// which an unsigned build (free Apple account, CI) can't open.
private let config = NSDictionary(contentsOf: Bundle.main.url(forResource: "GoogleService-Info", withExtension: "plist")!)!
private let apiKey = config["API_KEY"] as! String
private let documentsURL = URL(string: "https://firestore.googleapis.com/v1/projects/\(config["PROJECT_ID"] as! String)/databases/(default)/documents")!

typealias Fields = [String: Any]

struct FirebaseError: LocalizedError {
    let status: Int
    let errorDescription: String?
}

private func send(_ url: URL, method: String = "GET", json: Any? = nil, token: String? = nil) async throws -> Any {
    var request = URLRequest(url: url, timeoutInterval: 8)
    request.httpMethod = method
    if let token { request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization") }
    if let json {
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: json)
    }
    let (data, response) = try await URLSession.shared.data(for: request)
    let body = (try? JSONSerialization.jsonObject(with: data)) ?? [:]
    let status = (response as? HTTPURLResponse)?.statusCode ?? 0
    guard status == 200 else {
        let message = ((body as? Fields)?["error"] as? Fields)?["message"] as? String
        throw FirebaseError(status: status, errorDescription: message ?? "HTTP \(status)")
    }
    return body
}

/** Anonymous sign-in. shortcut: the refresh token sits in UserDefaults, not the keychain; fine for an anonymous alarm pairing. */
private actor Auth {
    static let shared = Auth()
    private var idToken: String?
    private var expiry = Date.distantPast
    private var signingIn: Task<String, Error>?

    func token() async throws -> String {
        if let idToken, expiry > .now { return idToken }
        if let signingIn { return try await signingIn.value }
        let task = Task { try await fetchToken() }
        signingIn = task
        defer { signingIn = nil }
        return try await task.value
    }

    private func fetchToken() async throws -> String {
        let defaults = UserDefaults.standard
        let json = if let refresh = defaults.string(forKey: "refreshToken") {
            try await send(URL(string: "https://securetoken.googleapis.com/v1/token?key=\(apiKey)")!, method: "POST",
                           json: ["grant_type": "refresh_token", "refresh_token": refresh]) as! Fields
        } else {
            try await send(URL(string: "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=\(apiKey)")!, method: "POST",
                           json: ["returnSecureToken": true]) as! Fields
        }
        // the two endpoints name the same fields differently
        let token = (json["id_token"] ?? json["idToken"]) as! String
        defaults.set(json["refresh_token"] ?? json["refreshToken"], forKey: "refreshToken")
        defaults.set(json["user_id"] ?? json["localId"], forKey: "uid")
        let lifetime = Double((json["expires_in"] ?? json["expiresIn"]) as? String ?? "") ?? 3600
        idToken = token
        expiry = .now + lifetime - 60
        return token
    }
}

/** This Mac's anonymous user id. */
func signIn() async throws -> String {
    _ = try await Auth.shared.token()
    return UserDefaults.standard.string(forKey: "uid")!
}

private func encode(_ value: Any) -> Fields {
    switch value {
    case let bool as Bool: ["booleanValue": bool]
    case let string as String: ["stringValue": string]
    case let date as Date: ["timestampValue": date.ISO8601Format()]
    default: ["nullValue": NSNull()]
    }
}

private func decode(_ document: Fields) -> Fields {
    ((document["fields"] as? [String: Fields]) ?? [:]).compactMapValues { value in
        // timestamps come with micro- or nanoseconds; seconds are enough here
        if let time = value["timestampValue"] as? String { return try? Date(String(time.prefix(19)) + "Z", strategy: .iso8601) }
        return value["stringValue"] ?? value["booleanValue"]
    }
}

/** A document's fields, or nil if it doesn't exist. */
func getDocument(_ path: String) async throws -> Fields? {
    do {
        return decode(try await send(documentsURL.appending(path: path), token: Auth.shared.token()) as! Fields)
    } catch let error as FirebaseError where error.status == 404 {
        return nil
    }
}

/** Writes [fields] into the document, creating it if needed; [replace] drops every field not given, [delete] removes the named ones. */
func setDocument(_ path: String, _ fields: Fields, replace: Bool = false, delete: [String] = []) async throws {
    let mask = replace ? [] : (Array(fields.keys) + delete).map { URLQueryItem(name: "updateMask.fieldPaths", value: $0) }
    _ = try await send(documentsURL.appending(path: path).appending(queryItems: mask), method: "PATCH",
                       json: ["fields": fields.mapValues(encode)], token: Auth.shared.token())
}

/** The first document of [collection] under [parent], newest [orderBy] first. */
func latestDocument(_ parent: String, _ collection: String, orderBy field: String) async throws -> Fields? {
    let query: Fields = ["structuredQuery": [
        "from": [["collectionId": collection]],
        "orderBy": [["field": ["fieldPath": field], "direction": "DESCENDING"]],
        "limit": 1,
    ]]
    let rows = try await send(documentsURL.appending(path: "\(parent):runQuery"), method: "POST", json: query,
                              token: Auth.shared.token()) as! [Fields]
    return (rows.first?["document"] as? Fields).map(decode)
}
