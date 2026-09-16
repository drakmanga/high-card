package it.sara.demo.service.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test delle regole di validazione del layer di servizio.
 */
class ValidationUtilTest {

    private final ValidationUtil validationUtil = new ValidationUtil();

    @ParameterizedTest
    @ValueSource(strings = {
            "mario.rossi@example.com",
            "m@b.it",
            "nome+tag@sotto.dominio.co.uk",
            "utente_1@example-domain.org"
    })
    @DisplayName("Accetta indirizzi email formalmente validi")
    void shouldAcceptValidEmails(String email) {
        assertTrue(validationUtil.isValidEmail(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "senza-chiocciola.it",
            "doppia@@example.com",
            "@example.com",
            "mario@",
            "mario@dominio",
            "mario rossi@example.com",
            "mario@example.c",
            "admin'--@example.com",
            "a'b@example.com",
            "test`@example.com",
            ".mario@example.com",
            "mario.@example.com"
    })
    @DisplayName("Rifiuta indirizzi email malformati")
    void shouldRejectInvalidEmails(String email) {
        assertFalse(validationUtil.isValidEmail(email));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Rifiuta email assenti o vuote")
    void shouldRejectBlankEmail(String email) {
        assertFalse(validationUtil.isValidEmail(email));
    }

    @Test
    @DisplayName("Rifiuta email oltre la lunghezza massima RFC")
    void shouldRejectTooLongEmail() {
        assertFalse(validationUtil.isValidEmail("a".repeat(ValidationUtil.EMAIL_MAX_LENGTH) + "@example.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "+393301234567",
            "3301234567",
            "+39 330 123 4567",
            "0039 330 1234567",
            "330-123-4567",
            "+390612345678",
            "0612345678",
            "331234567"
    })
    @DisplayName("Accetta numerazioni italiane valide, anche con separatori")
    void shouldAcceptValidItalianPhoneNumbers(String phoneNumber) {
        assertTrue(validationUtil.isValidItalianPhoneNumber(phoneNumber), "atteso valido: " + phoneNumber);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "+12125550123",
            "+443301234567",
            "1234567890",
            "+39",
            "+3933012345678",
            "+39330123",
            "abcdefghij"
    })
    @DisplayName("Rifiuta numerazioni non italiane o di lunghezza errata")
    void shouldRejectInvalidPhoneNumbers(String phoneNumber) {
        assertFalse(validationUtil.isValidItalianPhoneNumber(phoneNumber), "atteso non valido: " + phoneNumber);
    }

    @Test
    @DisplayName("Normalizza il numero rimuovendo i separatori di formattazione")
    void shouldNormalizePhoneNumber() {
        assertEquals("+393301234567", validationUtil.normalizePhoneNumber("+39 330 123 4567"));
        assertEquals("+393301234567", validationUtil.normalizePhoneNumber(" +39-330.123/4567 "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Mario", "De Luca", "D'Angelo", "Anna-Maria", "Bjorn"})
    @DisplayName("Accetta nomi con lettere, spazi, apostrofi e trattini")
    void shouldAcceptValidNames(String name) {
        assertTrue(validationUtil.isValidName(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Robert'); DROP TABLE users;--",
            "Mario1",
            "<script>alert(1)</script>",
            "' OR '1'='1",
            "Mario;DELETE",
            "First name 0"
    })
    @DisplayName("Rifiuta nomi con cifre o metacaratteri sfruttabili per iniezione")
    void shouldRejectInjectionAttemptsInNames(String name) {
        assertFalse(validationUtil.isValidName(name), "atteso non valido: " + name);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "admin'--",
            "Anna--Maria",
            "Anna''Maria",
            "'Anna",
            "Anna'",
            "-Anna",
            "Anna-",
            "Anna Maria'",
            "'"
    })
    @DisplayName("Rifiuta separatori consecutivi o in posizione iniziale e finale")
    void shouldRejectMalformedSeparators(String name) {
        // Un separatore deve stare fra due lettere: la regola è strutturale,
        // non un elenco di caratteri vietati. Blocca 'admin'--' senza vietare
        // l'apostrofo, che serve ai cognomi italiani reali.
        assertFalse(validationUtil.isValidName(name), "atteso non valido: " + name);
    }

    @Test
    @DisplayName("Rifiuta nomi oltre la lunghezza massima")
    void shouldRejectTooLongName() {
        assertFalse(validationUtil.isValidName("a".repeat(ValidationUtil.NAME_MAX_LENGTH + 1)));
    }
}
