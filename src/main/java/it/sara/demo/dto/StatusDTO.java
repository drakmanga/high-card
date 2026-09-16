package it.sara.demo.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Stato applicativo restituito in ogni risposta del servizio.
 * <p>
 * Il codice HTTP è sempre {@code 200}: l'esito reale dell'operazione è
 * trasportato da {@link #code}, che segue la semantica dei codici HTTP.
 */
@Getter
@Setter
public class StatusDTO {

    private int code;

    private String message;

    /** Identificativo univoco della richiesta, per la correlazione con i log. */
    private String traceId;
}
