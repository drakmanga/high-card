package it.sara.demo.service.util;

import org.springframework.stereotype.Component;

/**
 * Utilita' di base sulle stringhe, condivise dal layer di servizio.
 */
@Component
public class StringUtil {

    /**
     * Include le stringhe di soli spazi: un campo obbligatorio valorizzato
     * con {@code "   "} non è realmente valorizzato.
     */
    public boolean isNullOrEmpty(String str) {
        return str == null || str.isBlank();
    }
}
