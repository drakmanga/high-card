package it.sara.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro che autentica la richiesta a partire dal token JWT nell'header
 * {@code Authorization: Bearer <token>}.
 * <p>
 * Un token assente o non valido non interrompe la catena: il filtro lascia
 * il contesto di sicurezza vuoto e la decisione viene presa dalle regole di
 * autorizzazione configurate in {@link SecurityConfig}. così le rotte
 * pubbliche restano raggiungibili anche con un token malformato.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    /** Prefisso richiesto da Spring Security per le authority di ruolo. */
    private static final String ROLE_PREFIX = "ROLE_";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtTokenProvider.parseAndValidate(token);
                List<SimpleGrantedAuthority> authorities = jwtTokenProvider.extractRoles(claims).stream()
                        .map(role -> new SimpleGrantedAuthority(
                                role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role))
                        .toList();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (JwtException | IllegalArgumentException e) {
                // Token non valido: il contesto resta vuoto e la richiesta
                // viene respinta dalle regole di autorizzazione, non da qui.
                SecurityContextHolder.clearContext();
                log.warn("Rejected JWT on {} {}: {}",
                        request.getMethod(), request.getRequestURI(), e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
