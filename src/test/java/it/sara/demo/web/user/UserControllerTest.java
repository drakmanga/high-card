package it.sara.demo.web.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.sara.demo.security.JwtTokenProvider;
import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test di integrazione degli endpoint utente.
 * <p>
 * Verifica l'invariante centrale del progetto: <b>ogni</b> risposta, anche di
 * errore, viaggia con HTTP 200 e riporta l'esito reale dentro {@code status.code}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

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

    private String userToken() {
        return "Bearer " + jwtTokenProvider.generateToken("user", List.of("USER"));
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    // -------------------------------------------------------------- sicurezza

    @Test
    @DisplayName("Senza token la risposta è HTTP 200 con codice applicativo 401")
    void shouldRejectAnonymousRequestWithStatus200() throws Exception {
        mockMvc.perform(post(USERS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(401))
                .andExpect(jsonPath("$.status.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("Con un token manomesso la risposta è HTTP 200 con codice applicativo 401")
    void shouldRejectTamperedToken() throws Exception {
        String tampered = adminToken().substring(0, adminToken().lastIndexOf('.')) + ".firmaNonValida";

        mockMvc.perform(post(USERS_ENDPOINT)
                        .header("Authorization", tampered)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(401));
    }

    @Test
    @DisplayName("La policy nega la scrittura al ruolo di sola lettura")
    void shouldDenyWriteToReadOnlyRole() throws Exception {
        Map<String, String> body = Map.of(
                "firstName", "Paolo", "lastName", "Conti",
                "email", "paolo.conti@example.com", "phoneNumber", "+393401112233");

        mockMvc.perform(put(USERS_ENDPOINT)
                        .header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(403));
    }

    @Test
    @DisplayName("La policy consente la scrittura al ruolo amministrativo")
    void shouldAllowWriteToAdminRole() throws Exception {
        Map<String, String> body = Map.of(
                "firstName", "Paola", "lastName", "Ferri",
                "email", "paola.ferri@example.com", "phoneNumber", "+393401112244");

        mockMvc.perform(put(USERS_ENDPOINT)
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(200))
                // Il guid generato dal server deve raggiungere il client: senza,
                // non ha modo di referenziare l'utente appena creato.
                .andExpect(jsonPath("$.guid").isNotEmpty());
    }

    // ------------------------------------------------------------ validazione

    @Test
    @DisplayName("Un numero non italiano viene respinto con codice applicativo 400")
    void shouldRejectNonItalianPhoneNumber() throws Exception {
        Map<String, String> body = Map.of(
                "firstName", "Paolo", "lastName", "Conti",
                "email", "paolo.conti@example.com", "phoneNumber", "+12125550123");

        mockMvc.perform(put(USERS_ENDPOINT)
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(400));
    }

    @Test
    @DisplayName("Un'email malformata viene respinta con codice applicativo 400")
    void shouldRejectMalformedEmail() throws Exception {
        Map<String, String> body = Map.of(
                "firstName", "Paolo", "lastName", "Conti",
                "email", "non-una-email", "phoneNumber", "+393401112233");

        mockMvc.perform(put(USERS_ENDPOINT)
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(400));
    }

    @Test
    @DisplayName("Un tentativo di iniezione nel nome viene respinto sul bordo HTTP")
    void shouldRejectInjectionAttempt() throws Exception {
        Map<String, String> body = Map.of(
                "firstName", "Robert'); DROP TABLE users;--", "lastName", "Conti",
                "email", "paolo.conti@example.com", "phoneNumber", "+393401112233");

        mockMvc.perform(put(USERS_ENDPOINT)
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(400));
    }

    @Test
    @DisplayName("Un corpo JSON malformato produce un errore applicativo, non un 500 del container")
    void shouldRejectMalformedJson() throws Exception {
        mockMvc.perform(put(USERS_ENDPOINT)
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(400));
    }

    @Test
    @DisplayName("Un limite di pagina oltre il massimo consentito viene respinto")
    void shouldRejectLimitAboveMaximum() throws Exception {
        mockMvc.perform(post(USERS_ENDPOINT)
                        .header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("limit", 1000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(400));
    }

    @Test
    @DisplayName("Il messaggio sul limite massimo riporta il valore della costante")
    void shouldReportConfiguredMaxLimit() throws Exception {
        // Il messaggio interpola il vincolo: cablarlo lo farebbe divergere da MAX_LIMIT.
        mockMvc.perform(post(USERS_ENDPOINT)
                        .header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("limit", CriteriaGetUsers.MAX_LIMIT + 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(400))
                .andExpect(jsonPath("$.status.message")
                        .value("Limit must be less than or equal to " + CriteriaGetUsers.MAX_LIMIT));
    }

    @Test
    @DisplayName("Un verbo non ammesso sulla risorsa produce il codice applicativo 405")
    void shouldRejectUnsupportedMethod() throws Exception {
        mockMvc.perform(get(USERS_ENDPOINT)
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(405));
    }

    @Test
    @DisplayName("Un Content-Type non gestito produce il codice applicativo 415")
    void shouldRejectUnsupportedMediaType() throws Exception {
        mockMvc.perform(put(USERS_ENDPOINT)
                        .header("Authorization", adminToken())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("non è json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(415));
    }

    @Test
    @DisplayName("Una richiesta senza Content-Type produce il codice applicativo 415")
    void shouldRejectMissingContentType() throws Exception {
        mockMvc.perform(put(USERS_ENDPOINT)
                        .header("Authorization", adminToken())
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(415));
    }

    @Test
    @DisplayName("Una variante del percorso di scrittura resta negata al ruolo di sola lettura")
    void shouldDenyPathVariantToReadOnlyRole() throws Exception {
        // I matcher sono per path esatto: senza denyAll() finale queste varianti
        // ricadrebbero su una regola che chiede soltanto di essere autenticati.
        mockMvc.perform(put(USERS_ENDPOINT + "/")
                        .header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(403));

        mockMvc.perform(put("/USER/v1/user")
                        .header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(403));
    }

    @Test
    @DisplayName("Una rotta inesistente non viene segnalata come errore interno")
    void shouldNotReportUnknownRouteAsServerError() throws Exception {
        mockMvc.perform(post("/user/v1/nope")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(403));
    }

    @Test
    @DisplayName("Un valore di ordinamento inesistente non espone i tipi interni")
    void shouldRejectUnknownOrderWithoutLeakingInternals() throws Exception {
        mockMvc.perform(post(USERS_ENDPOINT)
                        .header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("order", "NON_ESISTE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(400))
                // Il nome completo delle classi interne non deve raggiungere il client.
                .andExpect(jsonPath("$.status.message")
                        .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("it.sara.demo"))))
                // Il corpo è JSON valido: segnalarlo come malformato indirizzerebbe
                // male chi corregge il client, il campo va nominato.
                .andExpect(jsonPath("$.status.message")
                        .value(org.hamcrest.Matchers.containsString("order")));
    }

    // --------------------------------------------------------------- ricerca

    @Test
    @DisplayName("La ricerca restituisce una pagina, il totale e l'email completa")
    void shouldReturnPagedUsers() throws Exception {
        mockMvc.perform(post(USERS_ENDPOINT)
                        .header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("limit", 2, "order", "BY_LASTNAME"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(200))
                .andExpect(jsonPath("$.users.length()").value(2))
                .andExpect(jsonPath("$.total").isNumber())
                // Regressione: l'assembler originale restituiva solo il dominio dell'email.
                .andExpect(jsonPath("$.users[0].email").value(org.hamcrest.Matchers.containsString("@")))
                .andExpect(jsonPath("$.users[0].phoneNumber").isNotEmpty());
    }

    @Test
    @DisplayName("La ricerca per nome completo trova l'utente")
    void shouldFindUserByFullName() throws Exception {
        mockMvc.perform(post(USERS_ENDPOINT)
                        .header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("query", "Mario Rossi"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(200))
                .andExpect(jsonPath("$.users[0].firstName").value("Mario"))
                .andExpect(jsonPath("$.users[0].lastName").value("Rossi"));
    }

    @Test
    @DisplayName("Il filtro testuale è insensibile alle maiuscole")
    void shouldFilterCaseInsensitively() throws Exception {
        mockMvc.perform(post(USERS_ENDPOINT)
                        .header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("query", "ROSSI"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(200))
                .andExpect(jsonPath("$.users[0].lastName").value("Rossi"));
    }
}
