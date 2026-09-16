package it.sara.demo.web.response;

import it.sara.demo.dto.StatusDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Risposta base esposta dal layer web.
 * <p>
 * Ogni risposta, anche di errore, viaggia con HTTP 200 e riporta l'esito
 * applicativo dentro {@link StatusDTO}.
 */
@Getter
@Setter
public class GenericResponse {

    public static final int SUCCESS_CODE = 200;

    private StatusDTO status;

    /**
     * Costruisce lo stato applicativo con identificativo di tracciamento univoco.
     */
    public static StatusDTO buildStatus(int code, String message) {
        StatusDTO status = new StatusDTO();
        status.setCode(code);
        status.setMessage(message);
        status.setTraceId(UUID.randomUUID().toString());
        return status;
    }
}
