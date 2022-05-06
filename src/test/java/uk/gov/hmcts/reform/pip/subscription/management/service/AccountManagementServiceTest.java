package uk.gov.hmcts.reform.pip.subscription.management.service;

import nl.altindag.log.LogCaptor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.subscription.management.Application;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.ListType;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test", "non-async"})
@SpringBootTest(classes = {Application.class})
@SuppressWarnings("PMD.LawOfDemeter")
class AccountManagementServiceTest {

    private static final String LOG_MESSAGE_MATCH = "Log messages should match.";
    private static final String INVALID_ID = "2";

    @Autowired
    private AccountManagementService accountManagementService;

    private static MockWebServer mockWebServer;

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testIsAuthenticatedReturnsTrue() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(6969);
        mockWebServer.enqueue(new MockResponse()
                                  .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                  .setBody(String.valueOf(true)));

        assertEquals(true, accountManagementService.isUserAuthenticated("1", ListType.SJP_PRESS_LIST),
                     "should be true"
        );
    }

    @Test
    void testIsAuthenticatedReturnsFalseAndLogsForbidden() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(6969);
        mockWebServer.enqueue(new MockResponse()
                                  .setResponseCode(HttpStatus.FORBIDDEN.value()));
        try (LogCaptor logCaptor = LogCaptor.forClass(AccountManagementService.class)) {
            accountManagementService.isUserAuthenticated(INVALID_ID, ListType.SJP_PRESS_LIST);
            assertEquals(1, logCaptor.getInfoLogs().size(), LOG_MESSAGE_MATCH);
            assertTrue(logCaptor.getInfoLogs().get(0).contains("User failed list type auth check with response"),
                       LOG_MESSAGE_MATCH);
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    @Test
    void testIsAuthenticatedReturnsFalseAndLogsClientError() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(6969);
        mockWebServer.enqueue(new MockResponse()
                                  .setResponseCode(HttpStatus.BAD_REQUEST.value()));
        try (LogCaptor logCaptor = LogCaptor.forClass(AccountManagementService.class)) {
            boolean response = accountManagementService.isUserAuthenticated(INVALID_ID, ListType.SJP_PRESS_LIST);
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
        mockWebServer = new MockWebServer();
        mockWebServer.start(6969);
        mockWebServer.enqueue(new MockResponse()
                                  .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value()));
        try (LogCaptor logCaptor = LogCaptor.forClass(AccountManagementService.class)) {
            boolean response = accountManagementService.isUserAuthenticated(INVALID_ID, ListType.SJP_PRESS_LIST);
            assertEquals(1, logCaptor.getErrorLogs().size(), LOG_MESSAGE_MATCH);
            assertTrue(logCaptor.getErrorLogs().get(0)
                           .contains("Request to Account Management isAuthenticated failed due to"), LOG_MESSAGE_MATCH);
            assertFalse(response, "should return false");
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

}
