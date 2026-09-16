package it.sara.demo.web.user;

import it.sara.demo.exception.GenericException;
import it.sara.demo.service.user.UserService;
import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.service.user.result.AddUserResult;
import it.sara.demo.service.user.result.GetUsersResult;
import it.sara.demo.web.assembler.AddUserAssembler;
import it.sara.demo.web.assembler.GetUsersAssembler;
import it.sara.demo.web.user.request.AddUserRequest;
import it.sara.demo.web.user.request.GetUsersRequest;
import it.sara.demo.web.user.response.AddUserResponse;
import it.sara.demo.web.user.response.GetUsersResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint REST per la gestione degli utenti.
 * <p>
 * Il controller non contiene logica di dominio: converte le richieste esposte in
 * criteri di servizio tramite gli assembler, delega al servizio e converte il
 * risultato nella risposta. Gli errori sono gestiti da
 * {@link it.sara.demo.web.advice.GlobalExceptionHandler}.
 *
 * <h2>Nota sui verbi HTTP</h2>
 * I verbi sono quelli del codice di partenza, mantenuti deliberatamente:
 * la specifica dell'esercizio identifica l'endpoint di creazione come
 * <i>"the PUT endpoint"</i>, e modificarlo renderebbe il riferimento incoerente
 * con il codice.
 * <p>
 * <b>Sono però entrambi in contrasto con RFC 9110</b>, e si segnala qui la
 * scelta corretta per un'API non vincolata da questa specifica:
 * <ul>
 *   <li><b>Creazione: {@code PUT} andrebbe sostituito con {@code POST}.</b>
 *       {@code PUT} richiede semantica <b>idempotente</b> e indirizza una
 *       risorsa a un URI già noto al client. Qui il {@code guid} è generato
 *       dal server e ogni invocazione ripetuta produce un utente distinto: è
 *       esattamente la semantica di {@code POST}. {@code PUT} sarebbe corretto
 *       solo su un URI del tipo {@code /user/{guid}}, con identificativo scelto
 *       dal client.</li>
 *   <li><b>Ricerca: {@code POST} è accettabile ma non ottimale.</b> Una
 *       ricerca è un'operazione sicura e idempotente, quindi la scelta
 *       ortodossa sarebbe {@code GET} con i criteri in query string, che la
 *       renderebbe cacheabile. {@code POST} resta però un compromesso
 *       difendibile: i criteri viaggiano nel corpo e non finiscono in access
 *       log, cronologia del client, header {@code Referer} e cache intermedie —
 *       un vantaggio concreto quando il filtro contiene dati personali degli
 *       utenti censiti, come nome, cognome ed email.</li>
 * </ul>
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AddUserAssembler addUserAssembler;

    @Autowired
    private GetUsersAssembler getUsersAssembler;

    /**
     * Crea un nuovo utente e ne restituisce l'identificativo. Operazione non
     * idempotente: ogni invocazione genera un utente distinto, con
     * identificativo assegnato dal server — ragione per cui {@code PUT} non è
     * il verbo semanticamente corretto.
     *
     * @throws GenericException se i dati non superano la validazione di dominio
     *                          o il salvataggio non riesce
     */
    @RequestMapping(value = {"/v1/user"}, method = RequestMethod.PUT)
    public ResponseEntity<AddUserResponse> addUser(@Valid @RequestBody AddUserRequest request) throws GenericException {
        CriteriaAddUser criteria = addUserAssembler.toCriteria(request);
        AddUserResult result = userService.addUser(criteria);
        return ResponseEntity.ok(addUserAssembler.toResponse(result));
    }

    /**
     * Ricerca gli utenti in modo paginato, filtrato e ordinato.
     *
     * @throws GenericException se i parametri di ricerca non sono validi
     */
    @RequestMapping(value = {"/v1/user"}, method = RequestMethod.POST)
    public ResponseEntity<GetUsersResponse> getUsers(@Valid @RequestBody GetUsersRequest request) throws GenericException {
        CriteriaGetUsers criteria = getUsersAssembler.toCriteria(request);
        GetUsersResult result = userService.getUsers(criteria);
        return ResponseEntity.ok(getUsersAssembler.toResponse(result));
    }
}
