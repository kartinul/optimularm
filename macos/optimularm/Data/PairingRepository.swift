//
//  PairingRepository.swift
//  optimularm
//

import FirebaseAuth
import FirebaseFirestore

private func pairingDoc(_ pairId: String) -> DocumentReference {
    Firestore.firestore().collection("pairings").document(pairId)
}

func signIn() async throws -> String {
    if let user = Auth.auth().currentUser { return user.uid }
    return try await Auth.auth().signInAnonymously().user.uid
}

/** Creates a pending pairing plus default settings and returns the new pairId. */
func createPairing() async throws -> String {
    let pairId = UUID().uuidString
    try await pairingDoc(pairId).setData([
        "pairId": pairId,
        "macDeviceId": try await signIn(),
        "status": "pending",
        "createdAt": FieldValue.serverTimestamp(),
    ])
    try await createDefaultSettings(pairId)
    return pairId
}

func listenPairStatus(_ pairId: String, onChange: @escaping (String?) -> Void) -> ListenerRegistration {
    pairingDoc(pairId).addSnapshotListener { snapshot, _ in
        onChange(snapshot?.get("status") as? String)
    }
}
