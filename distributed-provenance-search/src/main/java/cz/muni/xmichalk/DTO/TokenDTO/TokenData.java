package cz.muni.xmichalk.DTO.TokenDTO;

public class TokenData {
    public String originatorId;
    public String authorityId;
    public long tokenTimestamp;
    public long documentCreationTimestamp;
    public String documentDigest;
    public AdditionalData additionalData;
}
