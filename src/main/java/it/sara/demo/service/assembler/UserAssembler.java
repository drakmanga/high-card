package it.sara.demo.service.assembler;

import it.sara.demo.dto.UserDTO;
import it.sara.demo.service.database.model.User;
import org.springframework.stereotype.Component;

/**
 * Converte il modello di persistenza nel DTO trasportato verso il layer web.
 */
@Component
public class UserAssembler {

    /**
     * @return il DTO valorizzato, oppure {@code null} se l'entita' è {@code null}
     */
    public UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        UserDTO returnValue = new UserDTO();
        returnValue.setGuid(user.getGuid());
        returnValue.setFirstName(user.getFirstName());
        returnValue.setLastName(user.getLastName());
        returnValue.setEmail(user.getEmail());
        returnValue.setPhoneNumber(user.getPhoneNumber());
        return returnValue;
    }
}
