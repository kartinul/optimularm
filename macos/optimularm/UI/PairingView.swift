//
//  PairingView.swift
//  optimularm
//

import CoreImage.CIFilterBuiltins
import SwiftUI

struct PairingView: View {
    let pairId: String

    var body: some View {
        VStack {
            Image(nsImage: qrImage("optimularm:\(pairId)"))
                .interpolation(.none)
                .resizable()
                .frame(width: 200, height: 200)
            Text("Scan with the optimularm app on your phone")
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

private func qrImage(_ text: String) -> NSImage {
    let filter = CIFilter.qrCodeGenerator()
    filter.message = Data(text.utf8)
    let output = filter.outputImage?.transformed(by: CGAffineTransform(scaleX: 10, y: 10)) ?? CIImage()
    let rep = NSCIImageRep(ciImage: output)
    let image = NSImage(size: rep.size)
    image.addRepresentation(rep)
    return image
}
