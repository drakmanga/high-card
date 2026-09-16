package it.sara.demo.service.database;

import it.sara.demo.service.database.model.User;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FakeDatabase {

    /**
     * Tabella degli utenti.
     * <p>
     * {@link CopyOnWriteArrayList} e non {@code ArrayList}: un'applicazione web
     * serve ogni richiesta su un thread diverso, e una ricerca che attraversa la
     * collezione mentre una creazione vi scrive solleverebbe
     * {@link java.util.ConcurrentModificationException}. L'iterazione avviene qui
     * su uno snapshot, quindi non fallisce mai.
     * <p>
     * è la struttura adatta a questo profilo d'uso — letture frequenti, scritture
     * rare — perché il costo della copia si paga solo in scrittura. Con un numero
     * elevato di scritture andrebbe sostituita.
     */
    public static final List<User> TABLE_USER = new CopyOnWriteArrayList<>();

    private static final String[] FIRST_NAMES = {
            "Mario", "Giulia", "Luca", "Francesca", "Alessandro",
            "Chiara", "Matteo", "Sara", "Davide", "Elena"
    };

    private static final String[] LAST_NAMES = {
            "Rossi", "Bianchi", "Verdi", "Esposito", "Romano",
            "Colombo", "Ricci", "Marino", "Greco", "Bruno"
    };

    static {
        for (int i = 0; i < FIRST_NAMES.length; i++) {
            User user = new User();
            user.setGuid(java.util.UUID.randomUUID().toString());
            user.setFirstName(FIRST_NAMES[i]);
            user.setLastName(LAST_NAMES[i]);
            user.setEmail(FIRST_NAMES[i].toLowerCase() + "." + LAST_NAMES[i].toLowerCase() + "@example.com");
            user.setPhoneNumber("+39330123450" + i);
            TABLE_USER.add(user);
        }
    }

    private FakeDatabase() {
        // classe di sole costanti: istanziazione inibita
    }
}
