package it.sara.demo.service.auth.impl;

import it.sara.demo.exception.GenericException;
import it.sara.demo.security.JwtTokenProvider;
import it.sara.demo.service.auth.criteria.CriteriaLogin;
import it.sara.demo.service.auth.result.LoginResult;
import it.sara.demo.service.util.StringUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test del servizio di autenticazione.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Spy
    private StringUtil stringUtil = new StringUtil();

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private CriteriaLogin credentials(String username, String password) {
        CriteriaLogin criteria = new CriteriaLogin();
        criteria.setUsername(username);
        criteria.setPassword(password);
        return criteria;
    }

    @Test
    @DisplayName("Emette un token per credenziali valide, con i ruoli dell'utenza")
    void shouldIssueTokenForValidCredentials() throws GenericException {
        when(jwtTokenProvider.generateToken(anyString(), anyList())).thenReturn("signed-token");
        when(jwtTokenProvider.getExpirationSeconds()).thenReturn(3600L);

        LoginResult result = authService.login(credentials("admin", "admin123"));

        assertNotNull(result);
        assertEquals("signed-token", result.getAccessToken());
        assertEquals("Bearer", result.getTokenType());
        assertEquals(3600L, result.getExpiresIn());
        verify(jwtTokenProvider).generateToken("admin", List.of("ADMIN"));
    }

    @Test
    @DisplayName("Assegna il ruolo di sola lettura all'utenza standard")
    void shouldIssueUserRole() throws GenericException {
        when(jwtTokenProvider.generateToken(anyString(), anyList())).thenReturn("signed-token");

        authService.login(credentials("user", "user123"));

        verify(jwtTokenProvider).generateToken("user", List.of("USER"));
    }

    @Test
    @DisplayName("Rifiuta una password errata senza emettere token")
    void shouldRejectWrongPassword() {
        GenericException exception = assertThrows(GenericException.class,
                () -> authService.login(credentials("admin", "sbagliata")));

        assertEquals(401, exception.getStatus().getCode());
        verify(jwtTokenProvider, never()).generateToken(anyString(), anyList());
    }

    @Test
    @DisplayName("Usa lo stesso messaggio per utente inesistente e password errata")
    void shouldNotRevealWhetherUsernameExists() {
        GenericException unknownUser = assertThrows(GenericException.class,
                () -> authService.login(credentials("inesistente", "qualsiasi")));
        GenericException wrongPassword = assertThrows(GenericException.class,
                () -> authService.login(credentials("admin", "sbagliata")));

        // Messaggi distinti permetterebbero di enumerare le utenze esistenti.
        assertEquals(unknownUser.getStatus().getMessage(), wrongPassword.getStatus().getMessage());
        assertEquals(401, unknownUser.getStatus().getCode());
    }

    @Test
    @DisplayName("Rifiuta credenziali assenti o incomplete")
    void shouldRejectMissingCredentials() {
        assertEquals(400, assertThrows(GenericException.class,
                () -> authService.login(null)).getStatus().getCode());
        assertEquals(400, assertThrows(GenericException.class,
                () -> authService.login(credentials(null, "password"))).getStatus().getCode());
        assertEquals(400, assertThrows(GenericException.class,
                () -> authService.login(credentials("admin", ""))).getStatus().getCode());
    }
}
