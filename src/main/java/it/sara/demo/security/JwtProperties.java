package it.sara.demo.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Parametri di configurazione della sicurezza JWT.
 * <p>
 * Valorizzati dalle proprieta' con prefisso {@code security.jwt}. In ambiente
 * di esercizio la chiave di firma deve provenire da una variabile d'ambiente
 * o da un gestore di segreti, mai dal file di properties versionato.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /** Atteso nel claim {@code iss}. */
    private String issuer;

    /** Atteso nel claim {@code aud}. */
    private String audience;

    /** Chiave HMAC in Base64, di almeno 256 bit. */
    private String secret;

    private long expirationSeconds = 3600L;

    /** Tolleranza sullo sfasamento di orologio fra emittente e verificatore. */
    private long clockSkewSeconds = 30L;
}
