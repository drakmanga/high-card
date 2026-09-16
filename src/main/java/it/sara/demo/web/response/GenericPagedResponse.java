package it.sara.demo.web.response;

import lombok.Getter;
import lombok.Setter;

/**
 * Risposta paginata esposta dal layer web.
 */
@Getter
@Setter
public class GenericPagedResponse extends GenericResponse {

    /** Totale degli elementi che soddisfano i criteri, prima della paginazione. */
    private int total;
}
