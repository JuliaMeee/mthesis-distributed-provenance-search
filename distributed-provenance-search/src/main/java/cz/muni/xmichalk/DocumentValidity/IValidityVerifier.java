package cz.muni.xmichalk.DocumentValidity;

import cz.muni.xmichalk.DTO.Token.Token;

public interface IValidityVerifier {
    boolean verifyValidity(Token token);
}
