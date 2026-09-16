package it.sara.demo.service.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Regole di validazione del layer di servizio.
 * <p>
 * <b>Strategia difensiva.</b> Tutte le regole sono <i>whitelist</i>: si definisce
 * l'insieme dei valori ammessi e si rifiuta tutto il resto. L'approccio opposto,
 * la <i>blacklist</i> di sequenze pericolose ({@code '}, {@code --},
 * {@code UNION SELECT}), è sistematicamente aggirabile tramite codifiche
 * alternative e non va usato.
 * <p>
 * <b>Nota sulla SQL Injection.</b> In questo progetto la persistenza è simulata
 * in memoria e non viene composta alcuna query SQL. L'input non fidato del client
 * arrivava però fino allo strato di persistenza senza controlli di forma, che è
 * la precondizione della vulnerabilita': la validazione applicata qui la rimuove.
 * Resta valido il principio che, con un database reale, la difesa primaria deve
 * essere la <b>query parametrizzata</b> ({@code PreparedStatement}, binding
 * JPA/JDBC): la validazione dell'input è difesa aggiuntiva, mai sostitutiva.
 */
@Component
public class ValidationUtil {

    public static final int EMAIL_MAX_LENGTH = 254;

    public static final int NAME_MAX_LENGTH = 50;

    /**
     * Indirizzo email.
     * <p>
     * La parte locale ammette lettere, cifre e i separatori {@code . _ % + -},
     * e deve iniziare e terminare con un carattere alfanumerico. è un
     * sottoinsieme deliberato di quanto consentirebbe RFC 5322, che ammette
     * anche apice singolo, backtick e altri metacaratteri: sono legali ma
     * praticamente inutilizzati, e la loro esclusione riduce la superficie di
     * attacco senza impatto sugli indirizzi reali.
     * <p>
     * Esposto come costante per essere riusato dalle annotazioni di Bean
     * Validation del layer web, evitando due definizioni divergenti.
     */
    public static final String EMAIL_REGEX =
            "^[A-Za-z0-9](?:[A-Za-z0-9._%+-]*[A-Za-z0-9])?"
                    + "@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z]{2,}$";

    /**
     * Forma lasca accettata sul layer web: cifre, prefisso e separatori.
     * La conformita' italiana è verificata dal layer di servizio.
     */
    public static final String PHONE_INPUT_REGEX = "^[+0-9][0-9\\s.()/-]{5,24}$";

    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    /**
     * Numerazione telefonica italiana, valutata dopo la normalizzazione dei separatori:
     * <ul>
     *   <li>prefisso internazionale {@code +39} o {@code 0039}, facoltativo;</li>
     *   <li>numerazione mobile: cifra iniziale {@code 3}, da 9 a 10 cifre totali;</li>
     *   <li>numerazione fissa: cifra iniziale {@code 0}, da 6 a 11 cifre totali.</li>
     * </ul>
     */
    private static final Pattern ITALIAN_PHONE_PATTERN =
            Pattern.compile("^(?:\\+39|0039)?(?:3\\d{8,9}|0\\d{5,10})$");

    /** Separatori ammessi nell'input telefonico e rimossi prima della validazione. */
    private static final Pattern PHONE_SEPARATORS = Pattern.compile("[\\s.\\-()/]");

    /**
     * Nome e cognome: gruppi di lettere Unicode separati da un <b>singolo</b>
     * spazio, apostrofo o trattino.
     * <p>
     * La struttura è vincolata, non solo l'alfabeto: un separatore deve stare
     * <b>fra</b> due lettere, quindi non può aprire o chiudere il valore nè
     * comparire due volte di seguito. così {@code D'Angelo}, {@code Dell'Orto},
     * {@code Anna-Maria} e {@code De Luca} restano validi, mentre sequenze come
     * {@code admin'--} o {@code ' OR '1'='1} sono respinte pur usando soltanto
     * caratteri dell'alfabeto ammesso.
     * <p>
     * La lunghezza massima è verificata separatamente in
     * {@link #isValidName(String)}: includerla nell'espressione regolare la
     * renderebbe illeggibile.
     */
    public static final String NAME_REGEX = "^\\p{L}+(?:[ '\\-]\\p{L}+)*$";

    private static final Pattern NAME_PATTERN = Pattern.compile(NAME_REGEX);

    public boolean isValidEmail(String email) {
        if (email == null || email.isBlank() || email.length() > EMAIL_MAX_LENGTH) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * I separatori di formattazione vengono ignorati.
     */
    public boolean isValidItalianPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return false;
        }
        return ITALIAN_PHONE_PATTERN.matcher(normalizePhoneNumber(phoneNumber)).matches();
    }

    public String normalizePhoneNumber(String phoneNumber) {
        return PHONE_SEPARATORS.matcher(phoneNumber.trim()).replaceAll("");
    }

    public boolean isValidName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String trimmed = name.trim();
        if (trimmed.length() > NAME_MAX_LENGTH) {
            return false;
        }
        return NAME_PATTERN.matcher(trimmed).matches();
    }
}
