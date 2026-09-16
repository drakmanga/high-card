package it.sara.demo.exception;

import it.sara.demo.dto.StatusDTO;
import lombok.Getter;

import java.util.UUID;

/**
 * Eccezione applicativa che trasporta lo stato da restituire al chiamante
 * secondo lo standard {@link StatusDTO}.
 * <p>
 * Ogni istanza produce uno stato dedicato con {@code traceId} proprio: non
 * esistono istanze di stato condivise, che sarebbero mutabili e non
 * correlabili alla singola richiesta.
 */
@Getter
public class GenericException extends Exception {

    public static final int GENERIC_ERROR_CODE = 500;

    public static final String GENERIC_ERROR_MESSAGE = "Generic error";

    private final transient StatusDTO status;

    public GenericException(StatusDTO status) {
        super(status != null ? status.getMessage() : GENERIC_ERROR_MESSAGE);
        this.status = status != null ? status : buildStatus(GENERIC_ERROR_CODE, GENERIC_ERROR_MESSAGE);
    }

    public GenericException(int code, String message) {
        super(message);
        this.status = buildStatus(code, message);
    }

    public GenericException(int code, String message, Throwable cause) {
        super(message, cause);
        this.status = buildStatus(code, message);
    }

    public static GenericException generic(Throwable cause) {
        return new GenericException(GENERIC_ERROR_CODE, GENERIC_ERROR_MESSAGE, cause);
    }

    private static StatusDTO buildStatus(int code, String message) {
        StatusDTO status = new StatusDTO();
        status.setCode(code);
        status.setMessage(message);
        status.setTraceId(UUID.randomUUID().toString());
        return status;
    }
}
