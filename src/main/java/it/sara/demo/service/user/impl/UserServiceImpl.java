package it.sara.demo.service.user.impl;

import it.sara.demo.dto.UserDTO;
import it.sara.demo.exception.GenericException;
import it.sara.demo.service.assembler.UserAssembler;
import it.sara.demo.service.database.UserRepository;
import it.sara.demo.service.database.model.User;
import it.sara.demo.service.user.UserService;
import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.service.user.result.AddUserResult;
import it.sara.demo.service.user.result.GetUsersResult;
import it.sara.demo.service.util.StringUtil;
import it.sara.demo.service.util.ValidationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implementazione dei servizi di dominio relativi agli utenti.
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private StringUtil stringUtil;

    @Autowired
    private ValidationUtil validationUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAssembler userAssembler;

    /**
     * {@inheritDoc}
     */
    @Override
    public AddUserResult addUser(CriteriaAddUser criteria) throws GenericException {

        if (criteria == null) {
            throw new GenericException(400, "Request is required");
        }

        validate(criteria);

        // Il numero è l'unico campo normalizzato: viene accettato con separatori di
        // formattazione, che vanno rimossi prima di persisterlo. Gli altri campi sono
        // già nella forma definitiva, perché la validazione rifiuta spazi di bordo.
        User user = new User();
        user.setFirstName(criteria.getFirstName());
        user.setLastName(criteria.getLastName());
        user.setEmail(criteria.getEmail());
        user.setPhoneNumber(validationUtil.normalizePhoneNumber(criteria.getPhoneNumber()));

        try {
            if (!userRepository.save(user)) {
                throw new GenericException(500, "Error saving user");
            }
        } catch (GenericException e) {
            throw e;
        } catch (Exception e) {
            // Solo gli errori imprevisti vengono normalizzati in errore generico:
            // le GenericException applicative devono raggiungere il chiamante intatte.
            log.error("Unexpected error while saving user", e);
            throw GenericException.generic(e);
        }

        AddUserResult returnValue = new AddUserResult();
        returnValue.setGuid(user.getGuid());
        return returnValue;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Il totale restituito conta gli elementi che soddisfano il filtro
     * <b>prima</b> del taglio di pagina, così che il client possa calcolare il
     * numero di pagine disponibili.
     * <p>
     * Filtro e ordinamento sono applicati all'intera collezione prima della
     * paginazione. è corretto su una sorgente in memoria e rispecchia la
     * clausola {@code WHERE ... ORDER BY ... LIMIT ... OFFSET} in cui questa
     * logica va tradotta. <b>Sostituendo {@code FakeDatabase} con un database
     * reale, le tre operazioni vanno spinte nella query</b>: ordinare l'intera
     * tabella nel servizio per restituirne una pagina non è sostenibile oltre
     * volumi minimi.
     */
    @Override
    public GetUsersResult getUsers(CriteriaGetUsers criteriaGetUsers) throws GenericException {

        if (criteriaGetUsers == null) {
            throw new GenericException(400, "Request is required");
        }

        validate(criteriaGetUsers);

        List<User> matching = userRepository.getAll().stream()
                .filter(Objects::nonNull)
                .filter(user -> matches(user, criteriaGetUsers.getQuery()))
                .sorted(comparatorFor(criteriaGetUsers.getOrder()))
                .toList();

        List<UserDTO> page = matching.stream()
                .skip(criteriaGetUsers.getOffset())
                .limit(criteriaGetUsers.getLimit())
                .map(userAssembler::toDTO)
                .collect(Collectors.toList());

        GetUsersResult returnValue = new GetUsersResult();
        returnValue.setUsers(page);
        returnValue.setTotal(matching.size());
        return returnValue;
    }

    private void validate(CriteriaGetUsers criteria) throws GenericException {
        if (criteria.getOffset() < 0) {
            throw new GenericException(400, "Offset must be greater than or equal to 0");
        }
        if (criteria.getLimit() < 1) {
            throw new GenericException(400, "Limit must be greater than or equal to 1");
        }
        if (criteria.getLimit() > CriteriaGetUsers.MAX_LIMIT) {
            throw new GenericException(400, "Limit must be less than or equal to " + CriteriaGetUsers.MAX_LIMIT);
        }
        if (criteria.getOrder() == null) {
            throw new GenericException(400, "Order is required");
        }
    }

    /**
     * Un filtro assente o vuoto non restringe il risultato.
     * <p>
     * Il filtro è valutato per parole: ognuna deve trovare riscontro in almeno
     * uno fra nome, cognome ed email. Confrontare invece l'intera stringa con un
     * campo per volta renderebbe impossibile cercare per nome completo, perché
     * nessun singolo campo contiene nome e cognome insieme: {@code Mario Rossi}
     * non troverebbe l'utente che {@code Mario} e {@code Rossi} trovano entrambi.
     */
    private boolean matches(User user, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return Arrays.stream(query.trim().toLowerCase(Locale.ROOT).split("\\s+"))
                .allMatch(token -> contains(user.getFirstName(), token)
                        || contains(user.getLastName(), token)
                        || contains(user.getEmail(), token));
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private Comparator<User> comparatorFor(CriteriaGetUsers.OrderType order) {
        Comparator<User> byFirstName = Comparator.comparing(
                User::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        Comparator<User> byLastName = Comparator.comparing(
                User::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

        return switch (order) {
            case BY_FIRSTNAME -> byFirstName;
            case BY_FIRSTNAME_DESC -> byFirstName.reversed();
            case BY_LASTNAME -> byLastName;
            case BY_LASTNAME_DESC -> byLastName.reversed();
        };
    }

    /**
     * La validazione è ripetuta qui anche se il layer web applica già Bean
     * Validation: il servizio deve restare sicuro anche se invocato da un
     * chiamante non HTTP (batch, scheduler, altro servizio).
     */
    private void validate(CriteriaAddUser criteria) throws GenericException {

        if (stringUtil.isNullOrEmpty(criteria.getFirstName())) {
            throw new GenericException(400, "First name is required");
        }
        if (!validationUtil.isValidName(criteria.getFirstName())) {
            throw new GenericException(400, "First name is not valid");
        }

        if (stringUtil.isNullOrEmpty(criteria.getLastName())) {
            throw new GenericException(400, "Last name is required");
        }
        if (!validationUtil.isValidName(criteria.getLastName())) {
            throw new GenericException(400, "Last name is not valid");
        }

        if (stringUtil.isNullOrEmpty(criteria.getEmail())) {
            throw new GenericException(400, "Email is required");
        }
        if (!validationUtil.isValidEmail(criteria.getEmail())) {
            throw new GenericException(400, "Email is not valid");
        }

        if (stringUtil.isNullOrEmpty(criteria.getPhoneNumber())) {
            throw new GenericException(400, "Phone number is required");
        }
        if (!validationUtil.isValidItalianPhoneNumber(criteria.getPhoneNumber())) {
            throw new GenericException(400, "Phone number is not a valid Italian number");
        }
    }
}
