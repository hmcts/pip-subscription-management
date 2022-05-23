package uk.gov.hmcts.reform.pip.subscription.management.service;

import com.azure.core.http.ContentType;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pip.subscription.management.Application;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionsSummary;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionsSummaryDetails;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {Application.class})
@ActiveProfiles({"test", "non-async"})
class PublicationServicesServiceTest {

    private static MockWebServer mockPublicationServicesEndpoint;

    @Autowired
    WebClient webClient;

    @Autowired
    PublicationServicesService publicationServicesService;

    private final SubscriptionsSummary subscriptionsSummary = new SubscriptionsSummary();

    @BeforeEach
    void setup() {
        subscriptionsSummary.setEmail("a@b.com");
        subscriptionsSummary.setArtefactId(UUID.randomUUID());

        SubscriptionsSummaryDetails subscriptionsSummaryDetails = new SubscriptionsSummaryDetails();
        subscriptionsSummaryDetails.addToCaseNumber("1");

        subscriptionsSummary.setSubscriptions(subscriptionsSummaryDetails);
    }

    @Test
    void testPostSubscriptionSummaries() throws IOException, InterruptedException {
        mockPublicationServicesEndpoint = new MockWebServer();
        mockPublicationServicesEndpoint.start(8081);
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                        .addHeader("Content-Type",
                                                                   ContentType.APPLICATION_JSON)
                                                        .setResponseCode(200));



        publicationServicesService.postSubscriptionSummaries(subscriptionsSummary.toString());

        RecordedRequest request = mockPublicationServicesEndpoint.takeRequest();

        assertEquals("POST", request.getMethod(), "Request method was not correct");
        assertTrue(request.getBody().toString().contains("a@b.com"), "Body does not contain email");
        mockPublicationServicesEndpoint.shutdown();
    }

    @Test
    void testPostSubscriptionSummariesThrows() throws IOException, InterruptedException {
        mockPublicationServicesEndpoint = new MockWebServer();
        mockPublicationServicesEndpoint.start(8081);
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        publicationServicesService.postSubscriptionSummaries(subscriptionsSummary.toString());

        RecordedRequest request = mockPublicationServicesEndpoint.takeRequest();

        assertNotNull(request.getBody(), "Request body was null");
        mockPublicationServicesEndpoint.shutdown();
    }
}
