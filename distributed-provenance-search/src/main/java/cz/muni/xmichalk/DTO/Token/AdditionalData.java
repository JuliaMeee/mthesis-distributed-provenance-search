package cz.muni.xmichalk.DTO.Token;

public record AdditionalData(
        String bundle,
        String hashFunction,
        String trustedPartyUri,
        String trustedPartyCertificate
) {
}
