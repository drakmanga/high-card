package it.sara.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test di emissione e validazione dei token JWT.
 * <p>
 * Copre i tre criteri richiesti: issuer, scadenza e policy sui ruoli,
 * oltre all'integrità della firma.
 */
class JwtTokenProviderTest {

    private static final String ISSUER = "https://auth.sara.it";
    private static final String AUDIENCE = "high-card-api";
    private static final String SECRET =
            Base64.getEncoder().encodeToString("test-secret-key-with-at-least-256-bits!!".getBytes());

    private JwtProperties properties;
    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setIssuer(ISSUER);
        properties.setAudience(AUDIENCE);
        properties.setSecret(SECRET);
        properties.setExpirationSeconds(3600L);
        properties.setClockSkewSeconds(0L);

        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtProperties", properties);
    }

    private SecretKey keyOf(String base64Secret) {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Secret));
    }

    @Test
    @DisplayName("Un token senza soggetto viene rifiutato")
    void shouldRejectTokenWithoutSubject() {
        Instant now = Instant.now();
        String token = Jwts.builder()
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .claim(JwtTokenProvider.ROLES_CLAIM, List.of("ADMIN"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(keyOf(SECRET), Jwts.SIG.HS256)
                .compact();

        // Autenticherebbe una richiesta priva di identita': nessuna azione attribuibile.
        assertThrows(JwtException.class, () -> provider.parseAndValidate(token));
    }

    @Test
    @DisplayName("Il token emesso contiene issuer, audience, soggetto e ruoli")
    void shouldGenerateTokenWithExpectedClaims() {
        String token = provider.generateToken("admin", List.of("ADMIN"));

        Claims claims = provider.parseAndValidate(token);

        assertEquals(ISSUER, claims.getIssuer());
        assertTrue(claims.getAudience().contains(AUDIENCE));
        assertEquals("admin", claims.getSubject());
        assertEquals(List.of("ADMIN"), provider.extractRoles(claims));
    }

    @Test
    @DisplayName("Il token emesso ha una scadenza futura coerente con la configurazione")
    void shouldSetFutureExpiration() {
        Claims claims = provider.parseAndValidate(provider.generateToken("user", List.of("USER")));

        assertTrue(claims.getExpiration().after(new Date()));
        assertEquals(3600L, provider.getExpirationSeconds());
    }

    @Test
    @DisplayName("Rifiuta un token scaduto")
    void shouldRejectExpiredToken() {
        Instant past = Instant.now().minusSeconds(7200);
        String expired = Jwts.builder()
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject("admin")
                .claim(JwtTokenProvider.ROLES_CLAIM, List.of("ADMIN"))
                .issuedAt(Date.from(past))
                .expiration(Date.from(past.plusSeconds(60)))
                .signWith(keyOf(SECRET), Jwts.SIG.HS256)
                .compact();

        assertThrows(ExpiredJwtException.class, () -> provider.parseAndValidate(expired));
    }

    @Test
    @DisplayName("Rifiuta un token con issuer diverso da quello atteso")
    void shouldRejectWrongIssuer() {
        String foreign = Jwts.builder()
                .issuer("https://attacker.example.com")
                .audience().add(AUDIENCE).and()
                .subject("admin")
                .claim(JwtTokenProvider.ROLES_CLAIM, List.of("ADMIN"))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(keyOf(SECRET), Jwts.SIG.HS256)
                .compact();

        assertThrows(Exception.class, () -> provider.parseAndValidate(foreign));
    }

    @Test
    @DisplayName("Rifiuta un token destinato a un'altra audience")
    void shouldRejectWrongAudience() {
        String foreign = Jwts.builder()
                .issuer(ISSUER)
                .audience().add("another-service").and()
                .subject("admin")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(keyOf(SECRET), Jwts.SIG.HS256)
                .compact();

        assertThrows(Exception.class, () -> provider.parseAndValidate(foreign));
    }

    @Test
    @DisplayName("Rifiuta un token firmato con una chiave diversa")
    void shouldRejectWrongSignature() {
        String otherSecret = Base64.getEncoder()
                .encodeToString("another-secret-key-with-256-bits-len!!!!".getBytes());
        String forged = Jwts.builder()
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject("admin")
                .claim(JwtTokenProvider.ROLES_CLAIM, List.of("ADMIN"))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(keyOf(otherSecret), Jwts.SIG.HS256)
                .compact();

        assertThrows(SignatureException.class, () -> provider.parseAndValidate(forged));
    }

    @Test
    @DisplayName("Rifiuta un token non firmato")
    void shouldRejectUnsignedToken() {
        String unsigned = Jwts.builder()
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject("admin")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .compact();

        assertThrows(Exception.class, () -> provider.parseAndValidate(unsigned));
    }

    @Test
    @DisplayName("Rifiuta una stringa che non è un token")
    void shouldRejectMalformedToken() {
        assertThrows(MalformedJwtException.class, () -> provider.parseAndValidate("non-un-token"));
    }

    @Test
    @DisplayName("Restituisce ruoli vuoti se il claim è assente o di tipo inatteso")
    void shouldReturnEmptyRolesWhenClaimMissing() {
        String withoutRoles = Jwts.builder()
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject("admin")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(keyOf(SECRET), Jwts.SIG.HS256)
                .compact();

        assertTrue(provider.extractRoles(provider.parseAndValidate(withoutRoles)).isEmpty());
    }
}
