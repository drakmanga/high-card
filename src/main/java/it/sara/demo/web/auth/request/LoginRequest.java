package it.sara.demo.web.auth.request;

import it.sara.demo.web.request.GenericRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Richiesta di autenticazione.
 */
@Getter
@Setter
public class LoginRequest extends GenericRequest {

    public static final int USERNAME_MAX_LENGTH = 100;

    /** Whitelist: l'unico insieme di caratteri che le utenze possono usare. */
    @NotBlank(message = "Username is required")
    @Size(max = USERNAME_MAX_LENGTH, message = "Username is too long")
    @Pattern(regexp = "^[A-Za-z0-9._@-]+$", message = "Username is not valid")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(max = 100, message = "Password is too long")
    private String password;
}
