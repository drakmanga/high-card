package it.sara.demo.service.user.result;

import it.sara.demo.dto.UserDTO;
import it.sara.demo.service.result.GenericPagedResult;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Esito della ricerca utenti: pagina di risultati e totale complessivo.
 */
@Getter
@Setter
public class GetUsersResult extends GenericPagedResult {
    private List<UserDTO> users;
}
