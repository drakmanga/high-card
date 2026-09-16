package it.sara.demo.web.auth.response;

import it.sara.demo.web.response.GenericResponse;
import lombok.Getter;
import lombok.Setter;

/**
 * Risposta di autenticazione: token emesso e sua validità.
 */
@Getter
@Setter
public class LoginResponse extends GenericResponse {

    /** Da presentare nell'header {@code Authorization} delle chiamate successive. */
    private String accessToken;

    private String tokenType;

    /** Durata di validità del token, in secondi. */
    private long expiresIn;
}
