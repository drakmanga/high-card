package it.sara.demo.web.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.sara.demo.security.JwtTokenProvider;
import it.sara.demo.service.database.FakeDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Batteria di payload di iniezione contro l'endpoint di creazione utente (task 2).
 * <p>
 * <b>Cosa dimostra e cosa no.</b> Questi test verificano che la validazione
 * whitelist respinga i payload di iniezione prima che raggiungano lo strato di
 * persistenza. <b>Non dimostrano immunita' alla SQL Injection</b>: nessuna
 * validazione di input può garantirla. In presenza di un database reale
 * l'immunita' deriva dalla <b>query parametrizzata</b>, e la validazione resta
 * una difesa aggiuntiva che riduce la superficie di attacco.
 * <p>
 * In questo progetto non viene composta alcuna query SQL: la persistenza è
 * simulata in memoria. Il difetto corretto è quindi l'assenza di controlli di
 * forma sull'input, che è la precondizione della vulnerabilita'.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SqlInjectionTest {

    private static final String USERS_ENDPOINT = "/user/v1/user";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken() {
        return "Bearer " + jwtTokenProvider.generateToken("admin", List.of("ADMIN"));
    }

    /**
     * Invia il payload nel campo indicato e verifica che sia respinto con codice
     * applicativo 400 e che nulla venga persistito.
     */
    private void expectRejected(String field, String payload) throws Exception {
        Map<String, String> body = new HashMap<>(Map.of(
                "firstName", "Anna",
                "lastName", "Neri",
                "email", "anna.neri@example.com",
                "phoneNumber", "+393301234567"));
        body.put(field, payload);

        int sizeBefore = FakeDatabase.TABLE_USER.size();

        mockMvc.perform(put(USERS_ENDPOINT)
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(400));

        assertEquals(sizeBefore, FakeDatabase.TABLE_USER.size(),
                "nessun utente deve essere persistito a fronte di un payload respinto");
    }

    @ParameterizedTest(name = "firstName: {0}")
    @ValueSource(strings = {
            "Robert'); DROP TABLE users;--",
            "' OR '1'='1",
            "' OR 1=1--",
            "admin'--",
            "admin' #",
            "1; DELETE FROM users",
            "' UNION SELECT * FROM users--",
            "'; EXEC xp_cmdshell('dir');--",
            "\" OR \"\"=\"",
            "' AND SLEEP(5)--",
            "'; WAITFOR DELAY '0:0:5'--",
            "0x27204f5220273127",
            "%27%20OR%20%271%27%3D%271",
            "' || (SELECT password FROM users) || '",
            "namè--",
            "-- comment",
            "'",
            "Anna--Maria",
            "Anna''Maria",
            "'Anna",
            "Anna'"
    })
    @DisplayName("Il nome respinge ogni payload di iniezione")
    void shouldRejectInjectionInFirstName(String payload) throws Exception {
        expectRejected("firstName", payload);
    }

    @ParameterizedTest(name = "lastName: {0}")
    @ValueSource(strings = {
            "'); DROP TABLE users;--",
            "' OR '1'='1",
            "Neri'; DELETE FROM users WHERE 't'='t",
            "' UNION ALL SELECT NULL--",
            "Neri'--"
    })
    @DisplayName("Il cognome respinge ogni payload di iniezione")
    void shouldRejectInjectionInLastName(String payload) throws Exception {
        expectRejected("lastName", payload);
    }

    @ParameterizedTest(name = "email: {0}")
    @ValueSource(strings = {
            "' OR '1'='1@example.com",
            "admin'--@example.com",
            "a'b@example.com",
            "test@example.com'; DROP TABLE users;--",
            "test@example.com' OR '1'='1",
            "\"'\"@example.com",
            "test@exam'ple.com",
            "test`@example.com"
    })
    @DisplayName("L'email respinge ogni payload di iniezione")
    void shouldRejectInjectionInEmail(String payload) throws Exception {
        expectRejected("email", payload);
    }

    @ParameterizedTest(name = "phoneNumber: {0}")
    @ValueSource(strings = {
            "+393301234567'; DROP TABLE users;--",
            "' OR 1=1--",
            "+39330123456' OR '1'='1",
            "3301234567; DELETE FROM users"
    })
    @DisplayName("Il numero telefonico respinge ogni payload di iniezione")
    void shouldRejectInjectionInPhoneNumber(String payload) throws Exception {
        expectRejected("phoneNumber", payload);
    }

    @ParameterizedTest(name = "nome legittimo: {0}")
    @ValueSource(strings = {"D'Angelo", "Dell'Orto", "Anna-Maria", "De Luca", "O'Brien", "Van der Berg"})
    @DisplayName("I nomi legittimi con apostrofo, trattino e spazio restano accettati")
    void shouldStillAcceptLegitimateNames(String name) throws Exception {
        Map<String, String> body = new HashMap<>(Map.of(
                "firstName", "Anna",
                "lastName", name,
                "email", "anna.neri@example.com",
                "phoneNumber", "+393301234567"));

        mockMvc.perform(put(USERS_ENDPOINT)
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(200));
    }
}
