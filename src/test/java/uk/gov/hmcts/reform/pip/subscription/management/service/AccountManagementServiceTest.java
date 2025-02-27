package uk.gov.hmcts.reform.pip.subscription.management.service;

import com.azure.core.http.ContentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import nl.altindag.log.LogCaptor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pip.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ActiveProfiles({"test", "non-async"})
class AccountManagementServiceTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ObjectWriter OBJECT_WRITER = OBJECT_MAPPER.findAndRegisterModules()
        .writer()
        .withDefaultPrettyPrinter();
    private static final String LOG_MESSAGE_MATCH = "Log messages should match.";
    private static final String VALID_ID = "1";
    private static final String INVALID_ID = "2";

    private final Map<String, Optional<String>> expectedMap = new ConcurrentHashMap<>();
    private String jsonResponse;
    private static final String CONTENT_TYPE = "Content-Type";

    private List<String> subscriptionIds;

    private final MockWebServer mockAccountManagementEndpoint = new MockWebServer();
    private AccountManagementService accountManagementService;

    @BeforeEach
    void setup() throws IOException {
        Subscription mockSubscription = new Subscription();
        mockSubscription.setUserId(UUID.randomUUID().toString());
        mockSubscription.setSearchType(SearchType.LOCATION_ID);
        mockSubscription.setSearchValue("1");
        mockSubscription.setChannel(Channel.EMAIL);
        mockSubscription.setCaseNumber("1");
        mockSubscription.setId(UUID.randomUUID());

        List<Subscription> subscriptionList = List.of(mockSubscription);
        expectedMap.put("a@b.com", Optional.empty());
        jsonResponse = OBJECT_WRITER.writeValueAsString(expectedMap);

        subscriptionIds = subscriptionList.stream()
            .map(subscription -> subscription.getId().toString()).toList();

        WebClient mockedWebClient = WebClient.builder()
            .baseUrl(mockAccountManagementEndpoint.url("/").toString())
            .build();
        accountManagementService = new AccountManagementService(mockedWebClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockAccountManagementEndpoint.shutdown();
    }

    @Test
    void testIsAuthenticatedReturnsTrue() {
        mockAccountManagementEndpoint.enqueue(new MockResponse()
                                  .setHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                  .setBody(String.valueOf(true)));

        assertEquals(true, accountManagementService.isUserAuthorised(
            "1", ListType.CIVIL_DAILY_CAUSE_LIST, Sensitivity.PUBLIC),
                     "should be true"
        );
    }

    @Test
    void testIsAuthenticatedReturnsFalseAndLogsForbidden() throws IOException {
        mockAccountManagementEndpoint.enqueue(new MockResponse()
                                  .setResponseCode(FORBIDDEN.value()));
        try (LogCaptor logCaptor = LogCaptor.forClass(AccountManagementService.class)) {
            accountManagementService.isUserAuthorised(INVALID_ID, ListType.CIVIL_DAILY_CAUSE_LIST, Sensitivity.PUBLIC);
            assertEquals(1, logCaptor.getInfoLogs().size(), LOG_MESSAGE_MATCH);
            assertTrue(logCaptor.getInfoLogs().get(0).contains("User failed list type auth check with response"),
                       LOG_MESSAGE_MATCH);
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    @Test
    void testIsAuthenticatedReturnsFalseAndLogsClientError() throws IOException {
        mockAccountManagementEndpoint.enqueue(new MockResponse()
                                  .setResponseCode(BAD_REQUEST.value()));
        try (LogCaptor logCaptor = LogCaptor.forClass(AccountManagementService.class)) {
            boolean response = accountManagementService.isUserAuthorised(
                INVALID_ID, ListType.CIVIL_DAILY_CAUSE_LIST, Sensitivity.PUBLIC);
            assertEquals(1, logCaptor.getErrorLogs().size(), LOG_MESSAGE_MATCH);
            assertTrue(logCaptor.getErrorLogs().get(0)
                           .contains("Request to Account Management isAuthenticated failed due to"), LOG_MESSAGE_MATCH);
            assertFalse(response, "should return false");
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    @Test
    void testIsAuthenticatedReturnsFalseAndLogsServerError() throws IOException {
        mockAccountManagementEndpoint.enqueue(new MockResponse()
                                  .setResponseCode(INTERNAL_SERVER_ERROR.value()));
        try (LogCaptor logCaptor = LogCaptor.forClass(AccountManagementService.class)) {
            boolean response = accountManagementService.isUserAuthorised(
                INVALID_ID, ListType.CIVIL_DAILY_CAUSE_LIST, Sensitivity.PUBLIC);
            assertEquals(1, logCaptor.getErrorLogs().size(), LOG_MESSAGE_MATCH);
            assertTrue(logCaptor.getErrorLogs().get(0)
                           .contains("Request to Account Management isAuthenticated failed due to"), LOG_MESSAGE_MATCH);
            assertFalse(response, "should return false");
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    @Test
    void testGetMappedEmails() {
        mockAccountManagementEndpoint.enqueue(new MockResponse()
                                                  .addHeader(CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                  .setBody(jsonResponse));

        Map<String, Optional<String>> returnedMap = accountManagementService.getMappedEmails(subscriptionIds);

        assertEquals(expectedMap, returnedMap, "Returned map does not equal expected map");
    }

    @Test
    void testGetMappedEmailsThrows() {
        mockAccountManagementEndpoint.enqueue(new MockResponse().setResponseCode(404));

        Map<String, Optional<String>> returnedMap = accountManagementService.getMappedEmails(subscriptionIds);

        assertNotNull(returnedMap, "List was null when error occurred");
    }

    @Test
    void testGetUserByUserIdSuccess() throws IOException {
        PiUser response = new PiUser();
        response.setUserId(VALID_ID);
        jsonResponse = OBJECT_WRITER.writeValueAsString(Optional.of(response));

        mockAccountManagementEndpoint.enqueue(new MockResponse()
                                                  .addHeader(CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                  .setBody(jsonResponse));

        Optional<PiUser> result = accountManagementService.getUserByUserId(VALID_ID);
        assertThat(result)
            .as("Valid user should be returned")
            .isPresent()
            .get()
            .extracting(u -> u.getUserId())
            .isEqualTo(VALID_ID);
    }

    @Test
    void testGetUserByUserIdError() {
        mockAccountManagementEndpoint.enqueue(new MockResponse().setResponseCode(BAD_REQUEST.value()));

        Optional<PiUser> result = accountManagementService.getUserByUserId(VALID_ID);
        assertThat(result)
            .as("User should not be returned")
            .isEmpty();
    }

    @Test
    void testGetAllAccounts() throws IOException {
        mockAccountManagementEndpoint.enqueue(new MockResponse()
            .setBody("{\"content\":[{\"email\":\"junaid335@yahoo.com\",\"roles\":\"SYSTEM_ADMIN\"}]}"));

        List<PiUser> result = accountManagementService.getAllAccounts("prov", "role");

        assertFalse(result.isEmpty(),
                    "System admin users have not been returned from the server");
    }

    @Test
    void testGetAllAccountsError() throws IOException {
        mockAccountManagementEndpoint.enqueue(new MockResponse().setResponseCode(BAD_REQUEST.value()));

        List<PiUser> result = accountManagementService.getAllAccounts("prov", "role");

        assertTrue(result.isEmpty(),
                   "System admin users have not been returned from the server");
    }
}
