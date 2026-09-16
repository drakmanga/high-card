package it.sara.demo.service.auth.criteria;

import it.sara.demo.service.criteria.GenericCriteria;
import lombok.Getter;
import lombok.Setter;

/**
 * Criterio di autenticazione: credenziali presentate dal chiamante.
 */
@Getter
@Setter
public class CriteriaLogin extends GenericCriteria {

    private String username;

    /** In chiaro, confrontata con l'hash memorizzato. */
    private String password;
}
