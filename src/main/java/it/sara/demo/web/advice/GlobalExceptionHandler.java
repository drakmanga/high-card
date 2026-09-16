package it.sara.demo.web.advice;

import it.sara.demo.dto.StatusDTO;
import it.sara.demo.exception.GenericException;
import it.sara.demo.web.response.GenericResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Gestore centralizzato delle eccezioni del layer web.
 * <p>
 * Uniforma ogni esito, positivo o negativo, allo standard {@link StatusDTO}: il
 * codice HTTP è sempre {@code 200} e l'errore effettivo è descritto da
 * {@link StatusDTO#getCode()}. Il client ha così un unico formato da
 * interpretare, e nessuna eccezione può sfuggire producendo il corpo di errore
 * di default del container.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final int BAD_REQUEST = 400;

    private static final int METHOD_NOT_ALLOWED = 405;

    private static final int UNSUPPORTED_MEDIA_TYPE = 415;

    private static final int INTERNAL_ERROR = 500;

    /**
     * Eccezioni applicative, che trasportano già il proprio stato.
     */
    @ExceptionHandler(GenericException.class)
    public ResponseEntity<GenericResponse> handleGenericException(GenericException exception) {
        StatusDTO status = exception.getStatus();
        log.warn("Application error [traceId={}, code={}]: {}",
                status.getTraceId(), status.getCode(), status.getMessage());
        return toResponse(status);
    }

    /**
     * Errori di Bean Validation sul corpo della richiesta.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(this::describeFieldError)
                .distinct()
                .collect(Collectors.joining("; "));
        return badRequest(message.isEmpty() ? "Request is not valid" : message, exception);
    }

    /**
     * Errori di binding dei parametri di query.
     * <p>
     * {@code MethodArgumentNotValidException} estende {@link BindException}:
     * handler dichiarato sopra ha comunque la precedenza per i corpi JSON.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<GenericResponse> handleBindException(BindException exception) {
        String message = exception.getFieldErrors().stream()
                .map(this::describeFieldError)
                .distinct()
                .collect(Collectors.joining("; "));
        return badRequest(message.isEmpty() ? "Request is not valid" : message, exception);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponse> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .distinct()
                .collect(Collectors.joining("; "));
        return badRequest(message.isEmpty() ? "Request is not valid" : message, exception);
    }

    /**
     * Corpo assente, non deserializzabile, oppure con un valore incompatibile
     * con il campo di destinazione.
     * <p>
     * I due casi vengono distinti: un valore di enum inesistente produce un JSON
     * sintatticamente valido, e segnalarlo come corpo malformato indirizzerebbe
     * male chi sta correggendo il client.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
        // Il dettaglio interno non viene esposto: rivelerebbe la struttura del modello.
        String field = invalidFieldName(exception);
        if (field != null) {
            return badRequest("Parameter '" + field + "' is not valid", exception);
        }
        return badRequest("Request body is missing or malformed", exception);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GenericResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return badRequest("Parameter '" + exception.getName() + "' is not valid", exception);
    }

    /**
     * Verbo non ammesso sulla risorsa.
     * <p>
     * Senza questo handler l'eccezione ricadrebbe nella rete di sicurezza e
     * verrebbe segnalata come errore interno, nascondendo al chiamante che si
     * tratta invece di una richiesta formulata male.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<GenericResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        StatusDTO status = GenericResponse.buildStatus(METHOD_NOT_ALLOWED,
                "Method " + exception.getMethod() + " is not allowed on this resource");
        log.warn("Method not allowed [traceId={}]: {}", status.getTraceId(), exception.getMessage());
        return toResponse(status);
    }

    /**
     * Content-Type assente o non gestito.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<GenericResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException exception) {
        StatusDTO status = GenericResponse.buildStatus(UNSUPPORTED_MEDIA_TYPE,
                "Content type is missing or not supported");
        log.warn("Unsupported media type [traceId={}]: {}", status.getTraceId(), exception.getMessage());
        return toResponse(status);
    }

    /**
     * Rete di sicurezza per ogni eccezione non prevista. Il messaggio originale
     * non raggiunge il client: il dettaglio resta nei log, correlabile per {@code traceId}.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleUnexpectedException(Exception exception) {
        StatusDTO status = GenericResponse.buildStatus(INTERNAL_ERROR, GenericException.GENERIC_ERROR_MESSAGE);
        log.error("Unexpected error [traceId={}]", status.getTraceId(), exception);
        return toResponse(status);
    }

    /**
     * Descrive un errore di campo senza esporre dettagli interni.
     * <p>
     * Il messaggio predefinito di un errore di conversione contiene il nome
     * completo della classe attesa: esporlo rivelerebbe la struttura dei package.
     */
    private String describeFieldError(FieldError fieldError) {
        if (fieldError.isBindingFailure()) {
            return "Parameter '" + fieldError.getField() + "' is not valid";
        }
        return fieldError.getDefaultMessage();
    }

    /**
     * @return il nome del campo che ha causato l'errore di conversione, oppure
     *         {@code null} se il corpo non è interpretabile come JSON valido
     */
    private String invalidFieldName(HttpMessageNotReadableException exception) {
        if (!(exception.getCause() instanceof MismatchedInputException mismatch)) {
            return null;
        }
        return mismatch.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(Objects::nonNull)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    /**
     * Registra l'errore con il messaggio già ripulito, non con quello originale
     * dell'eccezione: quest'ultimo riporta i valori rifiutati, quindi email,
     * telefono e nome dell'utente — dati personali che non devono finire nei log
     * applicativi. Il {@code traceId} correla comunque la risposta alla richiesta.
     */
    private ResponseEntity<GenericResponse> badRequest(String message, Exception exception) {
        StatusDTO status = GenericResponse.buildStatus(BAD_REQUEST, message);
        log.warn("Invalid request [traceId={}, cause={}]: {}",
                status.getTraceId(), exception.getClass().getSimpleName(), message);
        return toResponse(status);
    }

    private ResponseEntity<GenericResponse> toResponse(StatusDTO status) {
        GenericResponse body = new GenericResponse();
        body.setStatus(status);
        return ResponseEntity.ok(body);
    }
}
