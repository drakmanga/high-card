package it.sara.demo.service.user;

import it.sara.demo.exception.GenericException;
import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.service.user.result.AddUserResult;
import it.sara.demo.service.user.result.GetUsersResult;

/**
 * Servizi di dominio relativi agli utenti.
 * <p>
 * Il contratto è espresso con criteri e risultati applicativi: gli oggetti
 * esposti sul web non attraversano questo confine.
 */
public interface UserService {

    /**
     * Crea un nuovo utente dopo averne validato i dati.
     *
     * @throws GenericException se i dati non sono validi o la persistenza non riesce
     */
    AddUserResult addUser(CriteriaAddUser addUserRequest) throws GenericException;

    /**
     * Ricerca gli utenti applicando filtro testuale, ordinamento e paginazione.
     *
     * @throws GenericException se i parametri di ricerca non sono validi
     */
    GetUsersResult getUsers(CriteriaGetUsers criteriaGetUsers) throws GenericException;
}
