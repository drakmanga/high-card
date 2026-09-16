package it.sara.demo.service.user.impl;

import it.sara.demo.dto.UserDTO;
import it.sara.demo.exception.GenericException;
import it.sara.demo.service.assembler.UserAssembler;
import it.sara.demo.service.database.UserRepository;
import it.sara.demo.service.database.model.User;
import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.service.user.result.AddUserResult;
import it.sara.demo.service.user.result.GetUsersResult;
import it.sara.demo.service.util.StringUtil;
import it.sara.demo.service.util.ValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test del servizio utenti: validazione in creazione, ricerca, ordinamento e paginazione.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Spy
    private StringUtil stringUtil = new StringUtil();

    @Spy
    private ValidationUtil validationUtil = new ValidationUtil();

    @Spy
    private UserAssembler userAssembler = new UserAssembler();

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private List<User> dataset;

    @BeforeEach
    void setUp() {
        dataset = new ArrayList<>(List.of(
                user("Mario", "Rossi", "mario.rossi@example.com"),
                user("Giulia", "Bianchi", "giulia.bianchi@example.com"),
                user("Luca", "Verdi", "luca.verdi@test.it"),
                user("anna", "Colombo", "anna.colombo@example.com")
        ));
    }

    private User user(String firstName, String lastName, String email) {
        User user = new User();
        user.setGuid(java.util.UUID.randomUUID().toString());
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhoneNumber("+393301234567");
        return user;
    }

    private CriteriaAddUser validCriteria() {
        CriteriaAddUser criteria = new CriteriaAddUser();
        criteria.setFirstName("Mario");
        criteria.setLastName("Rossi");
        criteria.setEmail("mario.rossi@example.com");
        criteria.setPhoneNumber("+39 330 123 4567");
        return criteria;
    }

    private CriteriaGetUsers searchCriteria(String query, int offset, int limit,
                                            CriteriaGetUsers.OrderType order) {
        CriteriaGetUsers criteria = new CriteriaGetUsers();
        criteria.setQuery(query);
        criteria.setOffset(offset);
        criteria.setLimit(limit);
        criteria.setOrder(order);
        return criteria;
    }

    // ------------------------------------------------------------------ addUser

    @Test
    @DisplayName("Crea l'utente e ne restituisce il guid")
    void shouldAddUserAndReturnGuid() throws GenericException {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, User.class).setGuid("generated-guid");
            return true;
        });

        AddUserResult result = userService.addUser(validCriteria());

        assertNotNull(result);
        assertEquals("generated-guid", result.getGuid());
    }

    @Test
    @DisplayName("Normalizza il numero telefonico prima di persistere")
    void shouldNormalizePhoneNumberBeforeSaving() throws GenericException {
        when(userRepository.save(any(User.class))).thenReturn(true);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        userService.addUser(validCriteria());

        verify(userRepository).save(captor.capture());
        assertEquals("+393301234567", captor.getValue().getPhoneNumber());
    }

    @Test
    @DisplayName("Rifiuta un'email malformata senza degradare l'errore a 500")
    void shouldRejectInvalidEmailWithBadRequest() {
        CriteriaAddUser criteria = validCriteria();
        criteria.setEmail("non-una-email");

        GenericException exception = assertThrows(GenericException.class, () -> userService.addUser(criteria));

        // Regressione: il catch(Exception) originale trasformava ogni 400 in 500.
        assertEquals(400, exception.getStatus().getCode());
        assertEquals("Email is not valid", exception.getStatus().getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Rifiuta un numero telefonico non italiano")
    void shouldRejectNonItalianPhoneNumber() {
        CriteriaAddUser criteria = validCriteria();
        criteria.setPhoneNumber("+12125550123");

        GenericException exception = assertThrows(GenericException.class, () -> userService.addUser(criteria));

        assertEquals(400, exception.getStatus().getCode());
        assertTrue(exception.getStatus().getMessage().contains("Italian"));
    }

    @Test
    @DisplayName("Rifiuta un nome contenente un tentativo di iniezione")
    void shouldRejectInjectionAttemptInName() {
        CriteriaAddUser criteria = validCriteria();
        criteria.setFirstName("Robert'); DROP TABLE users;--");

        GenericException exception = assertThrows(GenericException.class, () -> userService.addUser(criteria));

        assertEquals(400, exception.getStatus().getCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Rifiuta i campi obbligatori mancanti")
    void shouldRejectMissingMandatoryFields() {
        CriteriaAddUser criteria = validCriteria();
        criteria.setLastName("  ");

        GenericException exception = assertThrows(GenericException.class, () -> userService.addUser(criteria));

        assertEquals(400, exception.getStatus().getCode());
    }

    @Test
    @DisplayName("Rifiuta un criterio nullo")
    void shouldRejectNullCriteria() {
        GenericException exception = assertThrows(GenericException.class, () -> userService.addUser(null));
        assertEquals(400, exception.getStatus().getCode());
    }

    @Test
    @DisplayName("Segnala un errore 500 se la persistenza non riesce")
    void shouldReportErrorWhenSaveFails() {
        when(userRepository.save(any(User.class))).thenReturn(false);

        GenericException exception = assertThrows(GenericException.class, () -> userService.addUser(validCriteria()));

        assertEquals(500, exception.getStatus().getCode());
    }

    @Test
    @DisplayName("Ogni errore trasporta un traceId per la correlazione con i log")
    void shouldAlwaysCarryTraceId() {
        CriteriaAddUser criteria = validCriteria();
        criteria.setEmail("rotta");

        GenericException exception = assertThrows(GenericException.class, () -> userService.addUser(criteria));

        assertNotNull(exception.getStatus().getTraceId());
    }

    // ----------------------------------------------------------------- getUsers

    @Test
    @DisplayName("Restituisce la pagina richiesta e il totale complessivo")
    void shouldPaginateAndReportTotal() throws GenericException {
        when(userRepository.getAll()).thenReturn(dataset);

        GetUsersResult result = userService.getUsers(
                searchCriteria(null, 1, 2, CriteriaGetUsers.OrderType.BY_LASTNAME));

        assertEquals(4, result.getTotal(), "il totale deve precedere il taglio di pagina");
        assertEquals(2, result.getUsers().size());
        assertEquals("Colombo", result.getUsers().get(0).getLastName());
        assertEquals("Rossi", result.getUsers().get(1).getLastName());
    }

    @Test
    @DisplayName("Ordina per cognome in modo crescente e decrescente")
    void shouldSortByLastName() throws GenericException {
        when(userRepository.getAll()).thenReturn(dataset);

        GetUsersResult ascending = userService.getUsers(
                searchCriteria(null, 0, 10, CriteriaGetUsers.OrderType.BY_LASTNAME));
        GetUsersResult descending = userService.getUsers(
                searchCriteria(null, 0, 10, CriteriaGetUsers.OrderType.BY_LASTNAME_DESC));

        assertEquals(List.of("Bianchi", "Colombo", "Rossi", "Verdi"),
                ascending.getUsers().stream().map(UserDTO::getLastName).toList());
        assertEquals(List.of("Verdi", "Rossi", "Colombo", "Bianchi"),
                descending.getUsers().stream().map(UserDTO::getLastName).toList());
    }

    @Test
    @DisplayName("Ordina per nome ignorando le differenze di maiuscole")
    void shouldSortByFirstNameCaseInsensitively() throws GenericException {
        when(userRepository.getAll()).thenReturn(dataset);

        GetUsersResult result = userService.getUsers(
                searchCriteria(null, 0, 10, CriteriaGetUsers.OrderType.BY_FIRSTNAME));

        assertEquals(List.of("anna", "Giulia", "Luca", "Mario"),
                result.getUsers().stream().map(UserDTO::getFirstName).toList());
    }

    @Test
    @DisplayName("Filtra per nome, cognome o email ignorando le maiuscole")
    void shouldFilterCaseInsensitively() throws GenericException {
        when(userRepository.getAll()).thenReturn(dataset);

        assertEquals(1, userService.getUsers(
                searchCriteria("ROSS", 0, 10, CriteriaGetUsers.OrderType.BY_LASTNAME)).getTotal());
        assertEquals(1, userService.getUsers(
                searchCriteria("giulia", 0, 10, CriteriaGetUsers.OrderType.BY_LASTNAME)).getTotal());
        assertEquals(1, userService.getUsers(
                searchCriteria("@TEST.IT", 0, 10, CriteriaGetUsers.OrderType.BY_LASTNAME)).getTotal());
        assertEquals(3, userService.getUsers(
                searchCriteria("@example.com", 0, 10, CriteriaGetUsers.OrderType.BY_LASTNAME)).getTotal());
    }

    @Test
    @DisplayName("Trova l'utente cercando nome e cognome insieme, in qualsiasi ordine")
    void shouldFilterByFullName() throws GenericException {
        when(userRepository.getAll()).thenReturn(dataset);

        // Regressione: confrontando l'intera stringa con un campo per volta, nessun
        // utente veniva trovato, perché nessun campo contiene nome e cognome insieme.
        assertEquals(1, userService.getUsers(
                searchCriteria("Mario Rossi", 0, 10, CriteriaGetUsers.OrderType.BY_LASTNAME)).getTotal());
        assertEquals(1, userService.getUsers(
                searchCriteria("rossi mario", 0, 10, CriteriaGetUsers.OrderType.BY_LASTNAME)).getTotal());
        assertEquals(1, userService.getUsers(
                searchCriteria("  Mario   Rossi  ", 0, 10, CriteriaGetUsers.OrderType.BY_LASTNAME)).getTotal());
    }

    @Test
    @DisplayName("Ogni parola del filtro deve trovare riscontro: le parole si restringono a vicenda")
    void shouldRequireEveryTokenToMatch() throws GenericException {
        when(userRepository.getAll()).thenReturn(dataset);

        // Mario e Bianchi esistono, ma non nello stesso utente.
        assertEquals(0, userService.getUsers(
                searchCriteria("Mario Bianchi", 0, 10, CriteriaGetUsers.OrderType.BY_LASTNAME)).getTotal());
        // "example.com" restringe i tre di example.com al solo Mario.
        assertEquals(1, userService.getUsers(
                searchCriteria("mario example.com", 0, 10, CriteriaGetUsers.OrderType.BY_LASTNAME)).getTotal());
    }

    @Test
    @DisplayName("Un filtro vuoto non restringe il risultato")
    void shouldIgnoreBlankQuery() throws GenericException {
        when(userRepository.getAll()).thenReturn(dataset);

        assertEquals(4, userService.getUsers(
                searchCriteria("   ", 0, 10, CriteriaGetUsers.OrderType.BY_LASTNAME)).getTotal());
    }

    @Test
    @DisplayName("Un offset oltre il totale restituisce una pagina vuota, non un errore")
    void shouldReturnEmptyPageBeyondTotal() throws GenericException {
        when(userRepository.getAll()).thenReturn(dataset);

        GetUsersResult result = userService.getUsers(
                searchCriteria(null, 100, 10, CriteriaGetUsers.OrderType.BY_LASTNAME));

        assertTrue(result.getUsers().isEmpty());
        assertEquals(4, result.getTotal());
    }

    @Test
    @DisplayName("Rifiuta parametri di paginazione fuori dai limiti")
    void shouldRejectInvalidPagination() {
        assertEquals(400, assertThrows(GenericException.class, () -> userService.getUsers(
                searchCriteria(null, -1, 10, CriteriaGetUsers.OrderType.BY_LASTNAME)))
                .getStatus().getCode());
        assertEquals(400, assertThrows(GenericException.class, () -> userService.getUsers(
                searchCriteria(null, 0, 0, CriteriaGetUsers.OrderType.BY_LASTNAME)))
                .getStatus().getCode());
        assertEquals(400, assertThrows(GenericException.class, () -> userService.getUsers(
                searchCriteria(null, 0, CriteriaGetUsers.MAX_LIMIT + 1, CriteriaGetUsers.OrderType.BY_LASTNAME)))
                .getStatus().getCode());
        assertEquals(400, assertThrows(GenericException.class, () -> userService.getUsers(
                searchCriteria(null, 0, 10, null)))
                .getStatus().getCode());
    }

    @Test
    @DisplayName("Rifiuta un criterio di ricerca nullo")
    void shouldRejectNullSearchCriteria() {
        assertEquals(400, assertThrows(GenericException.class, () -> userService.getUsers(null))
                .getStatus().getCode());
    }
}
