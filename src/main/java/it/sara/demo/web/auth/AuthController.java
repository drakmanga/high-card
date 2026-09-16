package it.sara.demo.web.auth;

import it.sara.demo.exception.GenericException;
import it.sara.demo.service.auth.AuthService;
import it.sara.demo.service.auth.criteria.CriteriaLogin;
import it.sara.demo.service.auth.result.LoginResult;
import it.sara.demo.web.assembler.LoginAssembler;
import it.sara.demo.web.auth.request.LoginRequest;
import it.sara.demo.web.auth.response.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint di autenticazione che emette il token JWT.
 * <p>
 * Componente <b>dimostrativo</b>, incluso perché senza un punto di emissione
 * il requisito JWT non sarebbe verificabile. In esercizio va rimosso a favore
 * di un Identity Provider esterno, con l'applicazione riconfigurata come puro
 * OAuth2 Resource Server: si veda
 * {@link it.sara.demo.service.auth.impl.AuthServiceImpl} per il dettaglio
 * della migrazione.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private LoginAssembler loginAssembler;

    /**
     * @throws GenericException se le credenziali non sono valide
     */
    @RequestMapping(value = {"/v1/login"}, method = RequestMethod.POST)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) throws GenericException {
        CriteriaLogin criteria = loginAssembler.toCriteria(request);
        LoginResult result = authService.login(criteria);
        return ResponseEntity.ok(loginAssembler.toResponse(result));
    }
}
