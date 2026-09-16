package it.sara.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Emissione e validazione dei token JWT.
 * <p>
 * La validazione verifica, in quest'ordine:
 * <ol>
 *   <li><b>firma</b>: il token deve essere firmato con la chiave attesa, in
 *       algoritmo HMAC-SHA256; i token privi di firma ({@code alg: none})
 *       sono rifiutati perché il parser accetta solo token firmati;</li>
 *   <li><b>issuer</b>: il claim {@code iss} deve corrispondere all'autorita' configurata;</li>
 *   <li><b>audience</b>: il claim {@code aud} deve indicare questo servizio;</li>
 *   <li><b>expiration</b>: il claim {@code exp} deve essere futuro, con la
 *       tolleranza di sfasamento configurata;</li>
 *   <li><b>policy</b>: i ruoli presenti nel claim {@code roles} determinano le
 *       autorizzazioni applicate alle singole rotte.</li>
 * </ol>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    public static final String ROLES_CLAIM = "roles";

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * Emette un token firmato per il soggetto indicato.
     */
    public String generateToken(String subject, List<String> roles) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(jwtProperties.getExpirationSeconds());
        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .subject(subject)
                .claim(ROLES_CLAIM, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * @throws JwtException se firma, issuer, audience, scadenza o soggetto non sono conformi
     */
    public Claims parseAndValidate(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(signingKey())
                .requireIssuer(jwtProperties.getIssuer())
                .requireAudience(jwtProperties.getAudience())
                .clockSkewSeconds(jwtProperties.getClockSkewSeconds())
                .build()
                .parseSignedClaims(token);

        Claims claims = jws.getPayload();
        if (claims.getSubject() == null || claims.getSubject().isBlank()) {
            // Un token senza soggetto autenticherebbe una richiesta priva di identita':
            // nessuna azione risulterebbe attribuibile a qualcuno.
            throw new JwtException("Token has no subject");
        }
        return claims;
    }

    /**
     * @return i ruoli dichiarati dal token, eventualmente vuoti
     */
    public List<String> extractRoles(Claims claims) {
        Object roles = claims.get(ROLES_CLAIM);
        if (roles instanceof List<?> list) {
            return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
        }
        return Collections.emptyList();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(jwtProperties.getSecret()));
    }

    public long getExpirationSeconds() {
        return jwtProperties.getExpirationSeconds();
    }
}
