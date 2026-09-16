package it.sara.demo.web.assembler;

import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.service.user.result.GetUsersResult;
import it.sara.demo.web.user.request.GetUsersRequest;
import it.sara.demo.web.user.response.GetUsersResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Traduce la richiesta di ricerca nel criterio di servizio applicando i default,
 * e il risultato di servizio nella risposta esposta.
 */
@Component
public class GetUsersAssembler {

    public static final int DEFAULT_LIMIT = 20;

    public static final CriteriaGetUsers.OrderType DEFAULT_ORDER = CriteriaGetUsers.OrderType.BY_LASTNAME;

    /**
     * @return il criterio, con i default applicati ai campi assenti
     */
    public CriteriaGetUsers toCriteria(GetUsersRequest request) {
        CriteriaGetUsers returnValue = new CriteriaGetUsers();
        if (request == null) {
            returnValue.setOffset(0);
            returnValue.setLimit(DEFAULT_LIMIT);
            returnValue.setOrder(DEFAULT_ORDER);
            return returnValue;
        }
        returnValue.setQuery(request.getQuery());
        returnValue.setOffset(request.getOffset() != null ? request.getOffset() : 0);
        returnValue.setLimit(request.getLimit() != null ? request.getLimit() : DEFAULT_LIMIT);
        returnValue.setOrder(request.getOrder() != null ? request.getOrder() : DEFAULT_ORDER);
        return returnValue;
    }

    public GetUsersResponse toResponse(GetUsersResult result) {
        GetUsersResponse returnValue = new GetUsersResponse();
        returnValue.setStatus(GetUsersResponse.buildStatus(GetUsersResponse.SUCCESS_CODE, "Users retrieved."));
        if (result == null) {
            returnValue.setUsers(Collections.emptyList());
            returnValue.setTotal(0);
            return returnValue;
        }
        returnValue.setUsers(result.getUsers() != null ? result.getUsers() : Collections.emptyList());
        returnValue.setTotal(result.getTotal());
        return returnValue;
    }
}
