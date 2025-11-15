package cz.muni.xmichalk.dto.token;

public class TokenData {
    public String originatorId;
    public String authorityId;
    public long tokenTimestamp;
    public long documentCreationTimestamp;
    public String documentDigest;
    public AdditionalData additionalData;

    public TokenData() {
    }

    public TokenData(String originatorId, String authorityId, long tokenTimestamp, long documentCreationTimestamp, String documentDigest, AdditionalData additionalData) {
        this.originatorId = originatorId;
        this.authorityId = authorityId;
        this.tokenTimestamp = tokenTimestamp;
        this.documentCreationTimestamp = documentCreationTimestamp;
        this.documentDigest = documentDigest;
        this.additionalData = additionalData;
    }
}