package it.sara.demo;

import it.sara.demo.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
class HighCardApplicationTests {

    @Autowired
    private FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration;

    @Test
    void contextLoads() {
    }

    @Test
    @DisplayName("Il filtro JWT non è registrato presso il servlet container")
    void shouldNotAutoRegisterJwtFilter() {
        // Regressione: come bean Filter verrebbe registrato su tutti i percorsi,
        // e girerebbe anche fuori dalla catena di sicurezza.
        assertFalse(jwtFilterRegistration.isEnabled());
    }

}
