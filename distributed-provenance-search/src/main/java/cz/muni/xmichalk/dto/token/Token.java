package cz.muni.xmichalk.dto.token;

public class Token {
    public TokenData data;
    public String signature;

    public Token() {
    }

    public Token(TokenData data, String signature) {
        this.data = data;
        this.signature = signature;
    }
}