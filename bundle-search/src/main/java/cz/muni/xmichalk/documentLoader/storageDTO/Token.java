package cz.muni.xmichalk.documentLoader.storageDTO;

public record Token(
        TokenData data,
        String signature
) {
}