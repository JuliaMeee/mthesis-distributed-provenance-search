package cz.muni.xmichalk.documentLoader.storageDTO;

public record TokenData(
        String originatorId,
        String authorityId,
        long tokenTimestamp,
        long documentCreationTimestamp,
        String documentDigest,
        AdditionalData additionalData
) {
}