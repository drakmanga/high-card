package it.sara.demo.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configurazione della sicurezza applicativa.
 * <p>
 * <b>Policy di autorizzazione.</b> è la regola che il token deve soddisfare
 * oltre a firma, issuer e scadenza:
 * <ul>
 *   <li>{@code POST /user/v1/user} (ricerca utenti): richiede il ruolo
 *       {@code USER} oppure {@code ADMIN};</li>
 *   <li>{@code PUT /user/v1/user} (creazione utente): richiede il ruolo
 *       {@code ADMIN}, trattandosi di un'operazione di scrittura;</li>
 *   <li>{@code POST /auth/v1/login}: rotta pubblica, è il punto di ingresso
 *       che emette il token.</li>
 * </ul>
 * <p>
 * Ogni altro percorso è negato. I matcher sono per path esatto, quindi una
 * variante del percorso (slash finale, differenze di maiuscole) non incontra la
 * regola di ruolo: chiudere con {@code denyAll()} invece di
 * {@code authenticated()} evita che scivoli su una regola che chiede solo di
 * essere autenticati, e che un endpoint aggiunto in futuro risulti raggiungibile
 * da qualunque utente per semplice dimenticanza. Un percorso inesistente riceve
 * quindi una negazione, non un {@code 404}: è la stessa risposta di una risorsa
 * esistente ma non autorizzata, e non consente di enumerare le rotte.
 * <p>
 * La sessione è {@code STATELESS}: l'identita' viaggia esclusivamente nel
 * token, non esiste stato lato server. Per lo stesso motivo la protezione CSRF
 * è disattivata, poichè non vengono usati cookie di sessione.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Sola lettura. */
    public static final String ROLE_USER = "USER";

    /** Lettura e scrittura. */
    public static final String ROLE_ADMIN = "ADMIN";

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private SecurityStatusWriter securityStatusWriter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/v1/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/user/v1/user").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/user/v1/user").hasRole(ROLE_ADMIN)
                        // Gli altri verbi sulla risorsa esistente restano in carico al
                        // layer web, che risponde 405 invece di mascherarlo da 403.
                        .requestMatchers("/user/v1/user").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(securityStatusWriter.authenticationEntryPoint())
                        .accessDeniedHandler(securityStatusWriter.accessDeniedHandler()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Disattiva l'auto-registrazione del filtro JWT presso il servlet container.
     * <p>
     * Spring Boot registra automaticamente ogni bean di tipo {@code Filter} su
     * tutti i percorsi: il filtro girerebbe due volte, una fuori dalla catena di
     * sicurezza e senza le sue regole. Deve intervenire solo dove
     * {@code addFilterBefore} lo colloca.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
