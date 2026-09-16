package it.sara.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Punto di avvio dell'applicazione Spring Boot.
 * <p>
 * {@link UserDetailsServiceAutoConfiguration} è esclusa: creerebbe un'utenza in
 * memoria con password generata a ogni avvio, inutilizzabile qui — l'identita'
 * arriva solo dal token JWT — e stampata nei log, dove sembrerebbe una credenziale
 * dell'applicazione.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class HighCardApplication {

    public static void main(String[] args) {
        SpringApplication.run(HighCardApplication.class, args);
    }

}
