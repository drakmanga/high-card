package it.sara.demo.web.user.response;

import it.sara.demo.dto.UserDTO;
import it.sara.demo.web.response.GenericPagedResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Risposta della ricerca utenti.
 */
@Getter
@Setter
public class GetUsersResponse extends GenericPagedResponse {
    private List<UserDTO> users;
}
