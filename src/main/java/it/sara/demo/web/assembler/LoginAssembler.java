package it.sara.demo.web.assembler;

import it.sara.demo.service.auth.criteria.CriteriaLogin;
import it.sara.demo.service.auth.result.LoginResult;
import it.sara.demo.web.auth.request.LoginRequest;
import it.sara.demo.web.auth.response.LoginResponse;
import org.springframework.stereotype.Component;

/**
 * Assembler del layer web per l'autenticazione.
 */
@Component
public class LoginAssembler {

    /**
     * @return il criterio valorizzato, oppure {@code null} se la richiesta è {@code null}
     */
    public CriteriaLogin toCriteria(LoginRequest request) {
        if (request == null) {
            return null;
        }
        CriteriaLogin returnValue = new CriteriaLogin();
        returnValue.setUsername(request.getUsername());
        returnValue.setPassword(request.getPassword());
        return returnValue;
    }

    public LoginResponse toResponse(LoginResult result) {
        LoginResponse returnValue = new LoginResponse();
        returnValue.setStatus(LoginResponse.buildStatus(
                LoginResponse.SUCCESS_CODE, "Authentication successful."));
        returnValue.setAccessToken(result.getAccessToken());
        returnValue.setTokenType(result.getTokenType());
        returnValue.setExpiresIn(result.getExpiresIn());
        return returnValue;
    }
}
