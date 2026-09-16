package it.sara.demo.service.auth.result;

import it.sara.demo.service.result.GenericResult;
import lombok.Getter;
import lombok.Setter;

/**
 * Esito dell'autenticazione: token emesso e relativa validita'.
 */
@Getter
@Setter
public class LoginResult extends GenericResult {

    private String accessToken;

    private String tokenType;

    /** Durata di validita' del token, in secondi. */
    private long expiresIn;
}
