package cz.muni.xmichalk.dto.token;

public class AdditionalData {
    public String bundle;
    public String hashFunction;
    public String trustedPartyUri;
    public String trustedPartyCertificate;

    public AdditionalData() {
    }

    public AdditionalData(String bundle, String hashFunction, String trustedPartyUri, String trustedPartyCertificate) {
        this.bundle = bundle;
        this.hashFunction = hashFunction;
        this.trustedPartyUri = trustedPartyUri;
        this.trustedPartyCertificate = trustedPartyCertificate;
    }
}