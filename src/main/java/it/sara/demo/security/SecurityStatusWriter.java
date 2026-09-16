package it.sara.demo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.sara.demo.web.response.GenericResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Traduce gli esiti negativi di Spring Security nello standard {@code StatusDTO}.
 * <p>
 * Autenticazione mancante e autorizzazione negata vengono normalmente
 * restituite dal framework come HTTP 401 e 403, al di fuori della catena dei
 * {@code @ExceptionHandler}. Questa classe le intercetta e le riporta al
 * formato unico previsto dal progetto: HTTP 200 con il codice applicativo
 * reale dentro il corpo della risposta.
 */
@Slf4j
@Component
public class SecurityStatusWriter {

    public static final int UNAUTHORIZED = 401;

    public static final int FORBIDDEN = 403;

    @Autowired
    private ObjectMapper objectMapper;

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) ->
                write(response, UNAUTHORIZED, "Authentication is required");
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                write(response, FORBIDDEN, "Access is denied");
    }

    private void write(HttpServletResponse response, int code, String message) throws IOException {
        GenericResponse body = new GenericResponse();
        body.setStatus(GenericResponse.buildStatus(code, message));
        log.warn("Security rejection [traceId={}, code={}]", body.getStatus().getTraceId(), code);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
