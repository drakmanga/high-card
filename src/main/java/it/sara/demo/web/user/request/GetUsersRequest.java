package it.sara.demo.web.user.request;

import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.web.request.GenericRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Richiesta di ricerca utenti.
 * <p>
 * Tutti i campi sono facoltativi: in loro assenza
 * {@link it.sara.demo.web.assembler.GetUsersAssembler} applica i valori di default.
 */
@Getter
@Setter
public class GetUsersRequest extends GenericRequest {

    public static final int QUERY_MAX_LENGTH = 100;

    /** Applicato in modo case-insensitive su nome, cognome ed email. */
    @Size(max = QUERY_MAX_LENGTH, message = "Query is too long")
    private String query;

    /** Indice del primo elemento, a base zero. */
    @Min(value = 0, message = "Offset must be greater than or equal to 0")
    private Integer offset;

    @Min(value = 1, message = "Limit must be greater than or equal to 1")
    @Max(value = CriteriaGetUsers.MAX_LIMIT, message = "Limit must be less than or equal to {value}")
    private Integer limit;

    private CriteriaGetUsers.OrderType order;
}
