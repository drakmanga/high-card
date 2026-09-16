package it.sara.demo.service.auth;

import it.sara.demo.exception.GenericException;
import it.sara.demo.service.auth.criteria.CriteriaLogin;
import it.sara.demo.service.auth.result.LoginResult;

/**
 * Servizio di autenticazione applicativa.
 */
public interface AuthService {

    /**
     * Verifica le credenziali ed emette un token di accesso.
     *
     * @throws GenericException se le credenziali sono assenti o non valide
     */
    LoginResult login(CriteriaLogin criteria) throws GenericException;
}
