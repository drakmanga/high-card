package it.sara.demo.web.user.response;

import it.sara.demo.web.response.GenericResponse;
import lombok.Getter;
import lombok.Setter;

/**
 * Risposta della creazione utente.
 */
@Getter
@Setter
public class AddUserResponse extends GenericResponse {

    /** Identificativo assegnato dal server: permette al client di referenziare
     * l'utente appena creato senza doverlo ricercare. */
    private String guid;
}
