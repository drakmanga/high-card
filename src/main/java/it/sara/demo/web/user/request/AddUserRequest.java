package it.sara.demo.web.user.request;

import it.sara.demo.service.util.ValidationUtil;
import it.sara.demo.web.request.GenericRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Richiesta di creazione di un nuovo utente.
 * <p>
 * Le annotazioni costituiscono il primo livello di controllo, applicato sul
 * bordo HTTP per scartare subito le richieste malformate. Il controllo
 * autoritativo resta nel layer di servizio, che deve essere sicuro anche
 * quando invocato al di fuori del canale web.
 * <p>
 * Le espressioni regolari sono riusate da {@link ValidationUtil} per impedire
 * che le due definizioni della stessa regola divergano.
 */
@Getter
@Setter
public class AddUserRequest extends GenericRequest {

    @NotBlank(message = "First name is required")
    @Size(max = ValidationUtil.NAME_MAX_LENGTH, message = "First name is too long")
    @Pattern(regexp = ValidationUtil.NAME_REGEX, message = "First name is not valid")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = ValidationUtil.NAME_MAX_LENGTH, message = "Last name is too long")
    @Pattern(regexp = ValidationUtil.NAME_REGEX, message = "Last name is not valid")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Size(max = ValidationUtil.EMAIL_MAX_LENGTH, message = "Email is too long")
    @Pattern(regexp = ValidationUtil.EMAIL_REGEX, message = "Email is not valid")
    private String email;

    /** Verificato qui solo nella forma; la conformità italiana è accertata dal servizio. */
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = ValidationUtil.PHONE_INPUT_REGEX, message = "Phone number is not valid")
    private String phoneNumber;
}
