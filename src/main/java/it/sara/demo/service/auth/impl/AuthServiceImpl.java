package it.sara.demo.service.auth.impl;

import it.sara.demo.exception.GenericException;
import it.sara.demo.security.JwtTokenProvider;
import it.sara.demo.security.SecurityConfig;
import it.sara.demo.service.auth.AuthService;
import it.sara.demo.service.auth.criteria.CriteriaLogin;
import it.sara.demo.service.auth.result.LoginResult;
import it.sara.demo.service.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Implementazione del servizio di autenticazione.
 *
 * <h2>Natura dimostrativa di questo componente</h2>
 * Questa classe emette token a partire da un archivio di credenziali
 * <b>in memoria</b>, coerente con la persistenza simulata del resto del
 * progetto ({@code FakeDatabase}). Esiste per una ragione precisa: senza un
 * punto di emissione il requisito JWT non sarebbe nè dimostrabile nè
 * testabile end-to-end.
 * <p>
 * <b>Non è il design che si adotterebbe in esercizio.</b> Un servizio di
 * dominio non deve essere anche authorization server: le due responsabilità
 * hanno cicli di vita, superfici di attacco e requisiti di audit diversi.
 * L'evoluzione naturale è delegare l'emissione a un Identity Provider esterno
 * (Keycloak, Auth0, Microsoft Entra ID) e riconfigurare questa applicazione
 * come puro <i>OAuth2 Resource Server</i>, che si limita a validare token
 * altrui verificandone la firma tramite JWKS pubblicato dall'IdP.
 * <p>
 * In quello scenario si rimuovono questa classe, {@code AuthController} e la
 * generazione in {@link JwtTokenProvider}, si sostituisce la firma simmetrica
 * HS256 con la verifica asimmetrica RS256/ES256 sulla chiave pubblica
 * dell'IdP, e si sostituisce la configurazione con
 * {@code spring-boot-starter-oauth2-resource-server} e la proprieta'
 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}.
 * <b>La logica di validazione già presente — issuer, audience, scadenza e
 * policy sui ruoli — resta invariata</b>: cambia solo chi emette il token.
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    /** Messaggio unico, per non rivelare quale dei due campi sia errato. */
    private static final String INVALID_CREDENTIALS = "Invalid credentials";

    @Autowired
    private StringUtil stringUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Utenze dimostrative. Sono memorizzati gli <b>hash</b> BCrypt, non le password:
     * il servizio non deve mai disporre della password in chiaro, nemmeno in memoria.
     * Le credenziali corrispondenti sono documentate in {@code IMPLEMENTATION.md}.
     */
    private static final Map<String, DemoAccount> ACCOUNTS = Map.of(
            "admin", new DemoAccount(
                    "$2a$10$bEJ9BlOVzEbzvZaaHR39v.vCQJVFd2MmuH6N3lh9JFxtsuMx729Yq",
                    List.of(SecurityConfig.ROLE_ADMIN)),
            "user", new DemoAccount(
                    "$2a$10$72pjD6sJ/YM2H06YJqACceHMoy2FkWIhiRxW2sj0npz2d/aNA7tIW",
                    List.of(SecurityConfig.ROLE_USER))
    );

    /**
     * Hash di confronto usato quando lo username non esiste.
     * <p>
     * Serve a far costare un tentativo su utenza inesistente quanto uno su utenza
     * reale: senza, il tempo di risposta rivelerebbe quali username sono censiti.
     */
    private static final String DUMMY_HASH =
            "$2a$10$AGUa7d6zA8JUNcRapnpmZuhv9QE.NFOMHB1kT1ByuJPktaGO9aoT.";

    @Override
    public LoginResult login(CriteriaLogin criteria) throws GenericException {

        if (criteria == null) {
            throw new GenericException(400, "Request is required");
        }
        if (stringUtil.isNullOrEmpty(criteria.getUsername())) {
            throw new GenericException(400, "Username is required");
        }
        if (stringUtil.isNullOrEmpty(criteria.getPassword())) {
            throw new GenericException(400, "Password is required");
        }

        DemoAccount account = ACCOUNTS.get(criteria.getUsername());

        // Il confronto viene eseguito anche per gli username inesistenti, contro un hash
        // fittizio, così che il tempo di risposta non riveli quali username sono censiti.
        String expectedHash = account != null ? account.passwordHash() : DUMMY_HASH;
        boolean passwordMatches = passwordEncoder.matches(criteria.getPassword(), expectedHash);

        if (account == null || !passwordMatches) {
            log.warn("Failed login attempt for username '{}'", sanitizeForLog(criteria.getUsername()));
            throw new GenericException(401, INVALID_CREDENTIALS);
        }

        LoginResult returnValue = new LoginResult();
        returnValue.setAccessToken(jwtTokenProvider.generateToken(criteria.getUsername(), account.roles()));
        returnValue.setTokenType(TOKEN_TYPE);
        returnValue.setExpiresIn(jwtTokenProvider.getExpirationSeconds());
        return returnValue;
    }

    /**
     * Neutralizza i caratteri di controllo prima di scrivere nel log un valore
     * scelto dal chiamante.
     * <p>
     * Senza, uno username contenente un ritorno a capo inserisce righe arbitrarie
     * nel log applicativo: chi legge, o un aggregatore che lo indicizza, riceve
     * eventi che nessun componente ha mai prodotto.
     */
    private String sanitizeForLog(String value) {
        return value.replaceAll("\\p{Cntrl}", "_");
    }

    /** Utenza dimostrativa in memoria: hash della password, mai la password in chiaro. */
    private record DemoAccount(String passwordHash, List<String> roles) {
    }
}
