package cz.muni.xmichalk.DTO.Token;

public record Token(
        TokenData data,
        String signature
) {
}
