package cz.muni.xmichalk.DocumentValidity;

import cz.muni.xmichalk.DTO.TokenDTO.Token;

public interface IValidityVerifier {
    boolean verifyValidity(Token token);
}
