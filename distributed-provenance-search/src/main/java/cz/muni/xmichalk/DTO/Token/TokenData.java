package cz.muni.xmichalk.DTO.Token;

public record TokenData(
        String originatorId,
        String authorityId,
        long tokenTimestamp,
        long documentCreationTimestamp,
        String documentDigest,
        AdditionalData additionalData
) {
}
