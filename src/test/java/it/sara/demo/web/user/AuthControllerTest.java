package it.sara.demo.web.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test di integrazione dell'endpoint di autenticazione.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final String LOGIN_ENDPOINT = "/auth/v1/login";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    @Test
    @DisplayName("Uno username con caratteri di controllo viene respinto prima di raggiungere il log")
    void shouldRejectUsernameWithControlCharacters() throws Exception {
        // Un ritorno a capo nello username inserirebbe righe arbitrarie nel log applicativo.
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "vittima\nINFO riga forgiata", "password", "x"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(400))
                .andExpect(jsonPath("$.status.message").value("Username is not valid"));
    }

    @Test
    @DisplayName("L'endpoint di login è pubblico e restituisce un token per credenziali valide")
    void shouldIssueTokenWithoutAuthentication() throws Exception {
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "admin", "password", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(200))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    @DisplayName("Credenziali errate producono HTTP 200 con codice applicativo 401")
    void shouldRejectInvalidCredentials() throws Exception {
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "admin", "password", "sbagliata"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(401))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    @DisplayName("Credenziali incomplete producono un errore di validazione 400")
    void shouldRejectMissingCredentials() throws Exception {
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(400));
    }
}
