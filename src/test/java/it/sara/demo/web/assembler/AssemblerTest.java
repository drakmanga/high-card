package it.sara.demo.web.assembler;

import it.sara.demo.dto.UserDTO;
import it.sara.demo.service.assembler.UserAssembler;
import it.sara.demo.service.database.model.User;
import it.sara.demo.service.user.criteria.CriteriaAddUser;
import it.sara.demo.service.user.criteria.CriteriaGetUsers;
import it.sara.demo.service.user.result.GetUsersResult;
import it.sara.demo.service.user.result.AddUserResult;
import it.sara.demo.web.response.GenericResponse;
import it.sara.demo.web.user.request.AddUserRequest;
import it.sara.demo.web.user.response.AddUserResponse;
import it.sara.demo.web.user.request.GetUsersRequest;
import it.sara.demo.web.user.response.GetUsersResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test degli assembler, con copertura di regressione sui difetti di mappatura
 * presenti nel codice di partenza.
 */
class AssemblerTest {

    @Nested
    @DisplayName("AddUserAssembler")
    class AddUserAssemblerTest {

        private final AddUserAssembler assembler = new AddUserAssembler();

        @Test
        @DisplayName("Mappa il cognome dal campo corretto della richiesta")
        void shouldMapLastNameFromLastName() {
            AddUserRequest request = new AddUserRequest();
            request.setFirstName("Mario");
            request.setLastName("Rossi");
            request.setEmail("mario.rossi@example.com");
            request.setPhoneNumber("+393301234567");

            CriteriaAddUser criteria = assembler.toCriteria(request);

            // Regressione: l'assembler originale assegnava getFirstName() al cognome.
            assertEquals("Mario", criteria.getFirstName());
            assertEquals("Rossi", criteria.getLastName());
            assertEquals("mario.rossi@example.com", criteria.getEmail());
            assertEquals("+393301234567", criteria.getPhoneNumber());
        }

        @Test
        @DisplayName("Gestisce una richiesta nulla senza sollevare eccezioni")
        void shouldHandleNullRequest() {
            assertNull(assembler.toCriteria(null));
        }

        @Test
        @DisplayName("Riporta nella risposta il guid assegnato dal servizio")
        void shouldMapGuidToResponse() {
            AddUserResult result = new AddUserResult();
            result.setGuid("guid-1");

            AddUserResponse response = assembler.toResponse(result);

            assertEquals("guid-1", response.getGuid());
            assertEquals(GenericResponse.SUCCESS_CODE, response.getStatus().getCode());
        }

        @Test
        @DisplayName("Gestisce un risultato nullo senza sollevare eccezioni")
        void shouldHandleNullResult() {
            assertNull(assembler.toResponse(null));
        }
    }

    @Nested
    @DisplayName("UserAssembler")
    class UserAssemblerTest {

        private final UserAssembler assembler = new UserAssembler();

        @Test
        @DisplayName("Mappa l'email per intero e valorizza il numero telefonico")
        void shouldMapAllFields() {
            User user = new User();
            user.setGuid("guid-1");
            user.setFirstName("Mario");
            user.setLastName("Rossi");
            user.setEmail("mario.rossi@example.com");
            user.setPhoneNumber("+393301234567");

            UserDTO dto = assembler.toDTO(user);

            // Regressione: l'assembler originale troncava l'email al solo dominio
            // e non mappava affatto il numero telefonico.
            assertEquals("mario.rossi@example.com", dto.getEmail());
            assertEquals("+393301234567", dto.getPhoneNumber());
            assertEquals("guid-1", dto.getGuid());
            assertEquals("Mario", dto.getFirstName());
            assertEquals("Rossi", dto.getLastName());
        }

        @Test
        @DisplayName("Gestisce un'entita' nulla e campi nulli senza NullPointerException")
        void shouldHandleNullSafely() {
            assertNull(assembler.toDTO(null));

            UserDTO dto = assembler.toDTO(new User());
            assertNotNull(dto);
            assertNull(dto.getEmail());
        }
    }

    @Nested
    @DisplayName("GetUsersAssembler")
    class GetUsersAssemblerTest {

        private final GetUsersAssembler assembler = new GetUsersAssembler();

        @Test
        @DisplayName("Applica i valori di default ai campi non specificati")
        void shouldApplyDefaults() {
            CriteriaGetUsers criteria = assembler.toCriteria(new GetUsersRequest());

            assertEquals(0, criteria.getOffset());
            assertEquals(GetUsersAssembler.DEFAULT_LIMIT, criteria.getLimit());
            assertEquals(GetUsersAssembler.DEFAULT_ORDER, criteria.getOrder());
            assertNull(criteria.getQuery());
        }

        @Test
        @DisplayName("Conserva i valori esplicitamente richiesti")
        void shouldKeepExplicitValues() {
            GetUsersRequest request = new GetUsersRequest();
            request.setQuery("rossi");
            request.setOffset(5);
            request.setLimit(3);
            request.setOrder(CriteriaGetUsers.OrderType.BY_FIRSTNAME_DESC);

            CriteriaGetUsers criteria = assembler.toCriteria(request);

            assertEquals("rossi", criteria.getQuery());
            assertEquals(5, criteria.getOffset());
            assertEquals(3, criteria.getLimit());
            assertEquals(CriteriaGetUsers.OrderType.BY_FIRSTNAME_DESC, criteria.getOrder());
        }

        @Test
        @DisplayName("Applica i default anche a una richiesta nulla")
        void shouldHandleNullRequest() {
            CriteriaGetUsers criteria = assembler.toCriteria(null);
            assertEquals(GetUsersAssembler.DEFAULT_LIMIT, criteria.getLimit());
            assertEquals(GetUsersAssembler.DEFAULT_ORDER, criteria.getOrder());
        }

        @Test
        @DisplayName("Trasferisce utenti, totale e stato nella risposta")
        void shouldBuildResponse() {
            UserDTO dto = new UserDTO();
            dto.setGuid("guid-1");
            GetUsersResult result = new GetUsersResult();
            result.setUsers(List.of(dto));
            result.setTotal(42);

            GetUsersResponse response = assembler.toResponse(result);

            assertEquals(1, response.getUsers().size());
            assertEquals(42, response.getTotal());
            assertEquals(200, response.getStatus().getCode());
            assertNotNull(response.getStatus().getTraceId());
        }

        @Test
        @DisplayName("Restituisce una risposta vuota se il risultato è nullo")
        void shouldHandleNullResult() {
            GetUsersResponse response = assembler.toResponse(null);
            assertTrue(response.getUsers().isEmpty());
            assertEquals(0, response.getTotal());
        }
    }
}
