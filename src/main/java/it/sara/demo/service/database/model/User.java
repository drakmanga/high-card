package it.sara.demo.service.database.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {

    private String guid;
    private String firstName;
    private String lastName;
    private String email;

    /** Numero in forma canonica, privo di separatori di formattazione. */
    private String phoneNumber;
}
