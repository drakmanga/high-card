package it.sara.demo.service.user.criteria;

import it.sara.demo.service.criteria.GenericCriteria;
import lombok.Getter;
import lombok.Setter;

/**
 * Criterio di creazione di un nuovo utente.
 */
@Getter
@Setter
public class CriteriaAddUser extends GenericCriteria {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
}
