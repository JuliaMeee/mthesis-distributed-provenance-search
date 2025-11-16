package cz.muni.xmichalk.documentLoader.storageDTO;

public record AdditionalData(
        String bundle,
        String hashFunction,
        String trustedPartyUri,
        String trustedPartyCertificate
) {
}