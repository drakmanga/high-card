package it.sara.demo.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Rappresentazione di un utente trasportata fra layer di servizio e layer web.
 */
@Getter
@Setter
public class UserDTO {
    private String guid;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
}
