package it.sara.demo.service.result;

import lombok.Getter;
import lombok.Setter;

/**
 * Risultato di servizio paginato.
 */
@Getter
@Setter
public class GenericPagedResult extends GenericResult {

    /** Totale degli elementi che soddisfano i criteri, prima della paginazione. */
    private int total;
}
