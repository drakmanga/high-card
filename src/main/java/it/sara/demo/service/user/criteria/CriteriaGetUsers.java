package it.sara.demo.service.user.criteria;

import it.sara.demo.service.criteria.GenericCriteria;
import lombok.Getter;
import lombok.Setter;

/**
 * Criterio di ricerca utenti: filtro testuale, paginazione e ordinamento.
 */
@Getter
@Setter
public class CriteriaGetUsers extends GenericCriteria {

    /**
     * Numero massimo di elementi restituibili in una pagina.
     */
    public static final int MAX_LIMIT = 100;

    /** Applicato in modo case-insensitive su nome, cognome ed email. */
    private String query;

    /** Indice del primo elemento, a base zero. */
    private int offset;

    private int limit;

    private OrderType order;

    /**
     * Criteri di ordinamento supportati dalla ricerca utenti.
     */
    @Getter
    public enum OrderType {

        BY_FIRSTNAME("by firstName"),
        BY_FIRSTNAME_DESC("by firstName desc"),
        BY_LASTNAME("by lastName"),
        BY_LASTNAME_DESC("by lastName desc");

        private final String displayName;

        OrderType(String displayName) {
            this.displayName = displayName;
        }
    }
}
