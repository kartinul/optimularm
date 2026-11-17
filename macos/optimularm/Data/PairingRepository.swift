//
//  PairingRepository.swift
//  optimularm
//

import Foundation

struct Pairing {
    var paired = false
    /** The phone left: the pairing went back to pending after having been paired, so its QR must not be reused. */
    var abandoned = false
    /** When the phone's next alarm rings, as it last computed. */
    var ringAt: Date?
}

/** Creates a pending pairing plus default settings and returns the new pairId. */
func createPairing() async throws -> String {
    let pairId = UUID().uuidString
    try await setDocument("pairings/\(pairId)", [
        "pairId": pairId,
        "macDeviceId": try await signIn(),
        "status": "pending",
        "createdAt": Date(),
    ], replace: true)
    try await setDocument("settings/\(pairId)", WakeSettings().fields, replace: true)
    return pairId
}

/** A pairing that's gone, or that this Mac's user can no longer read (e.g. a new anonymous user), counts as abandoned. */
func fetchPairing(_ pairId: String) async throws -> Pairing {
    let document: Fields?
    do {
        document = try await getDocument("pairings/\(pairId)")
    } catch let error as FirebaseError where error.status == 403 {
        document = nil
    }
    guard let fields = document else { return Pairing(abandoned: true) }
    let status = fields["status"] as? String
    return Pairing(
        paired: status == "paired",
        abandoned: status == "pending" && fields["pairedAt"] != nil,
        ringAt: fields["ringAt"] as? Date
    )
}
