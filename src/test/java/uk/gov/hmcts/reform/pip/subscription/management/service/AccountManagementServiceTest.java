package uk.gov.hmcts.reform.pip.subscription.management.service;

import com.azure.core.http.ContentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import nl.altindag.log.LogCaptor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.model.account.AzureAccount;
import uk.gov.hmcts.reform.pip.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.Application;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {Application.class})
@ActiveProfiles({"test", "non-async"})
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@SuppressWarnings("PMD.LawOfDemeter")
class AccountManagementServiceTest {

    private static final String LOG_MESSAGE_MATCH = "Log messages should match.";
    private static final String INVALID_ID = "2";

    @Autowired
    private AccountManagementService accountManagementService;

    private static MockWebServer mockAccountManagementEndpoint;
    private static final String TRIGGER_RECEIVED = "Trigger has been received";
    private static List<Subscription> subscriptionList;
    private final Map<String, Optional<String>> expectedMap = new ConcurrentHashMap<>();
    private String jsonResponse;
    private static final String CONTENT_TYPE = "Content-Type";

    private List<String> subscriptionIds;

    @BeforeEach
    void setup() throws IOException {
        Subscription mockSubscription = new Subscription();
        mockSubscription.setUserId(UUID.randomUUID().toString());
        mockSubscription.setSearchType(SearchType.LOCATION_ID);
        mockSubscription.setSearchValue("1");
        mockSubscription.setChannel(Channel.EMAIL);
        mockSubscription.setCaseNumber("1");
        mockSubscription.setId(UUID.randomUUID());

        subscriptionList = List.of(mockSubscription);
        expectedMap.put("a@b.com", Optional.empty());

        ObjectWriter ow = new ObjectMapper().findAndRegisterModules().writer().withDefaultPrettyPrinter();
        jsonResponse = ow.writeValueAsString(expectedMap);

        subscriptionIds = subscriptionList.stream()
            .map(subscription -> subscription.getId().toString()).toList();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockAccountManagementEndpoint.shutdown();
    }

    @Test
    void testIsAuthenticatedReturnsTrue() throws IOException {
        mockAccountManagementEndpoint = new MockWebServer();
        mockAccountManagementEndpoint.start(6969);
        mockAccountManagementEndpoint.enqueue(new MockResponse()
                                  .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                  .setBody(String.valueOf(true)));

        assertEquals(true, accountManagementService.isUserAuthorised(
            "1", ListType.CIVIL_DAILY_CAUSE_LIST, Sensitivity.PUBLIC),
                     "should be true"
        );
    }

    @Test
    void testIsAuthenticatedReturnsFalseAndLogsForbidden() throws IOException {
        mockAccountManagementEndpoint = new MockWebServer();
        mockAccountManagementEndpoint.start(6969);
        mockAccountManagementEndpoint.enqueue(new MockResponse()
                                  .setResponseCode(HttpStatus.FORBIDDEN.value()));
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
        mockAccountManagementEndpoint = new MockWebServer();
        mockAccountManagementEndpoint.start(6969);
        mockAccountManagementEndpoint.enqueue(new MockResponse()
                                  .setResponseCode(HttpStatus.BAD_REQUEST.value()));
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
        mockAccountManagementEndpoint = new MockWebServer();
        mockAccountManagementEndpoint.start(6969);
        mockAccountManagementEndpoint.enqueue(new MockResponse()
                                  .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value()));
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
    void testGetMappedEmails() throws IOException {
        mockAccountManagementEndpoint = new MockWebServer();
        mockAccountManagementEndpoint.start(6969);
        mockAccountManagementEndpoint.enqueue(new MockResponse()
                                                  .addHeader(CONTENT_TYPE,
                                                             ContentType.APPLICATION_JSON)
                                                  .setBody(jsonResponse));

        Map<String, Optional<String>> returnedMap =
            accountManagementService.getMappedEmails(subscriptionIds);

        assertEquals(expectedMap, returnedMap, "Returned map does not equal expected map");

        mockAccountManagementEndpoint.shutdown();
    }

    @Test
    void testGetMappedEmailsThrows() throws IOException {
        mockAccountManagementEndpoint = new MockWebServer();
        mockAccountManagementEndpoint.start(6969);
        mockAccountManagementEndpoint.enqueue(new MockResponse().setResponseCode(404));

        Map<String, Optional<String>> returnedMap =
            accountManagementService.getMappedEmails(subscriptionIds);

        assertNotNull(returnedMap, "List was null when error occurred");
        mockAccountManagementEndpoint.shutdown();
    }

    @Test
    void testGetUserInfo() throws IOException {
        mockAccountManagementEndpoint = new MockWebServer();
        mockAccountManagementEndpoint.start(6969);
        mockAccountManagementEndpoint.enqueue(new MockResponse().setBody(TRIGGER_RECEIVED));

        AzureAccount result =
            accountManagementService.getUserInfo(UUID.randomUUID().toString());

        assertNotNull(result,
                      "User information has not been returned from the server");

        mockAccountManagementEndpoint.shutdown();
    }

    @Test
    void testGetUserInfoError() throws IOException {
        mockAccountManagementEndpoint = new MockWebServer();
        mockAccountManagementEndpoint.start(6969);
        mockAccountManagementEndpoint.enqueue(new MockResponse().setResponseCode(HttpStatus.BAD_REQUEST.value()));

        AzureAccount result =
            accountManagementService.getUserInfo(UUID.randomUUID().toString());

        assertNull(result.getDisplayName(),
                   "User information has not been returned from the server");

        mockAccountManagementEndpoint.shutdown();
    }

    @Test
    void testGetAllAccounts() throws IOException {
        mockAccountManagementEndpoint = new MockWebServer();
        mockAccountManagementEndpoint.start(6969);
        mockAccountManagementEndpoint.enqueue(new MockResponse()
            .setBody("{\"content\":[{\"email\":\"junaid335@yahoo.com\",\"roles\":\"SYSTEM_ADMIN\"}]}"));

        List<PiUser> result =
            accountManagementService.getAllAccounts("prov", "role");

        assertFalse(result.isEmpty(),
                    "System admin users have not been returned from the server");

        mockAccountManagementEndpoint.shutdown();
    }

    @Test
    void testGetAllAccountsError() throws IOException {
        mockAccountManagementEndpoint = new MockWebServer();
        mockAccountManagementEndpoint.start(6969);
        mockAccountManagementEndpoint.enqueue(new MockResponse().setResponseCode(HttpStatus.BAD_REQUEST.value()));

        List<PiUser> result =
            accountManagementService.getAllAccounts("prov", "role");

        assertTrue(result.isEmpty(),
                   "System admin users have not been returned from the server");

        mockAccountManagementEndpoint.shutdown();
    }

}
