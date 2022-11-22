package uk.gov.hmcts.reform.pip.subscription.management.service;

import com.azure.core.http.ContentType;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pip.subscription.management.Application;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(classes = {Application.class})
@ActiveProfiles({"test", "non-async"})
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
class DataManagementServiceTest {

    private static final String INVALID = "test";

    private static MockWebServer mockDataManagementCourtEndpoint;

    @Autowired
    WebClient webClient;

    @Autowired
    DataManagementService dataManagementService;

    @Test
    void testGetCourt() throws IOException {
        mockDataManagementCourtEndpoint = new MockWebServer();
        mockDataManagementCourtEndpoint.start(4550);
        mockDataManagementCourtEndpoint.enqueue(new MockResponse().addHeader("Content-Type",
                                                                             ContentType.APPLICATION_JSON)
                                                    .setBody("{\"name\": \"SJP Court\"}"));

        String courtName = dataManagementService.getCourtName("1");
        assertEquals("SJP Court", courtName, "Court name does not match returned value");
        mockDataManagementCourtEndpoint.shutdown();
    }

    @Test
    void testNullCourt() throws IOException {
        mockDataManagementCourtEndpoint = new MockWebServer();
        mockDataManagementCourtEndpoint.start(4550);
        mockDataManagementCourtEndpoint.enqueue(new MockResponse());

        String courtName = dataManagementService.getCourtName("1");
        assertNull(courtName, "Court return is null");
        mockDataManagementCourtEndpoint.shutdown();
    }

    @Test
    void testGetCourtThrows() throws IOException {
        mockDataManagementCourtEndpoint = new MockWebServer();
        mockDataManagementCourtEndpoint.start(4550);
        mockDataManagementCourtEndpoint.enqueue(new MockResponse().setResponseCode(404));
        String courtName = dataManagementService.getCourtName(INVALID);
        assertNull(courtName, "Court name not null when error occured");
        mockDataManagementCourtEndpoint.shutdown();
    }

}
