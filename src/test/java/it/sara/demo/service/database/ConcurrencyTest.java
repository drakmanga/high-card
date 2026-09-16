package it.sara.demo.service.database;

import it.sara.demo.service.database.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica che la persistenza simulata regga l'accesso concorrente.
 * <p>
 * Un'applicazione web serve ogni richiesta su un thread diverso: una scrittura
 * in corso durante una lettura deve poter avvenire senza che il lettore fallisca.
 */
class ConcurrencyTest {

    private final UserRepository userRepository = new UserRepository();

    private User newUser(int index) {
        User user = new User();
        user.setFirstName("Nome");
        user.setLastName("Cognome");
        user.setEmail("utente" + index + "@example.com");
        user.setPhoneNumber("+393301234567");
        return user;
    }

    @Test
    @DisplayName("Una lettura concorrente a una scrittura non solleva eccezioni")
    void shouldSurviveConcurrentReadAndWrite() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Exception> failure = new AtomicReference<>();

        pool.submit(() -> {
            try {
                start.await();
                for (int i = 0; i < 500; i++) {
                    userRepository.save(newUser(i));
                }
            } catch (Exception e) {
                failure.compareAndSet(null, e);
            }
        });

        pool.submit(() -> {
            try {
                start.await();
                for (int i = 0; i < 500; i++) {
                    List<User> all = userRepository.getAll();
                    // Attraversamento completo: è qui che una lista non thread-safe
                    // solleva ConcurrentModificationException se qualcuno scrive.
                    long count = all.stream().filter(u -> u != null && u.getEmail() != null).count();
                    assertTrue(count >= 0);
                }
            } catch (Exception e) {
                failure.compareAndSet(null, e);
            }
        });

        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "i task devono completare");

        assertNull(failure.get(), () -> "accesso concorrente fallito: " + failure.get());
    }
}
