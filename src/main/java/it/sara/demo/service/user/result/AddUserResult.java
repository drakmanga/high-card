package it.sara.demo.service.user.result;

import it.sara.demo.service.result.GenericResult;
import lombok.Getter;
import lombok.Setter;

/**
 * Esito della creazione di un utente.
 */
@Getter
@Setter
public class AddUserResult extends GenericResult {

    /** Identificativo assegnato dal server all'utente creato. */
    private String guid;
}
