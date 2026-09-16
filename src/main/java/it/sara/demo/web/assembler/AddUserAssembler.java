package it.sara.demo.web.assembler;

import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.service.user.result.AddUserResult;
import it.sara.demo.web.user.request.AddUserRequest;
import it.sara.demo.web.user.response.AddUserResponse;
import it.sara.demo.web.response.GenericResponse;
import org.springframework.stereotype.Component;

/**
 * Traduce la richiesta HTTP di creazione nel criterio di servizio, e il
 * risultato di dominio nella risposta esposta, così che il layer di servizio
 * non riceva nè produca mai oggetti esposti sul web.
 */
@Component
public class AddUserAssembler {

    /**
     * @return il criterio valorizzato, oppure {@code null} se la richiesta è {@code null}
     */
    public CriteriaAddUser toCriteria(AddUserRequest addUserRequest) {
        if (addUserRequest == null) {
            return null;
        }
        CriteriaAddUser returnValue = new CriteriaAddUser();
        returnValue.setFirstName(addUserRequest.getFirstName());
        returnValue.setLastName(addUserRequest.getLastName());
        returnValue.setEmail(addUserRequest.getEmail());
        returnValue.setPhoneNumber(addUserRequest.getPhoneNumber());
        return returnValue;
    }

    /**
     * @return la risposta con l'identificativo assegnato, oppure {@code null} se il risultato è {@code null}
     */
    public AddUserResponse toResponse(AddUserResult result) {
        if (result == null) {
            return null;
        }
        AddUserResponse returnValue = new AddUserResponse();
        returnValue.setStatus(GenericResponse.buildStatus(GenericResponse.SUCCESS_CODE, "User added."));
        returnValue.setGuid(result.getGuid());
        return returnValue;
    }
}
