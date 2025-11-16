package cz.muni.xmichalk.dto.token;

public record TokenData(
        String originatorId,
        String authorityId,
        long tokenTimestamp,
        long documentCreationTimestamp,
        String documentDigest,
        AdditionalData additionalData
) {
}