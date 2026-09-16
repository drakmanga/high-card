package it.sara.demo.service.database;

import it.sara.demo.service.database.model.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;


@Component
public class UserRepository {

    /**
     * Persiste un nuovo utente assegnandogli un identificativo univoco.
     */
    public boolean save(User user) {
        if (user == null) {
            return false;
        }
        user.setGuid(java.util.UUID.randomUUID().toString());
        return FakeDatabase.TABLE_USER.add(user);
    }

    public List<User> getAll() {
        return Collections.unmodifiableList(FakeDatabase.TABLE_USER);
    }
}
