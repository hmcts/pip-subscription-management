package uk.gov.hmcts.reform.pip.subscription.management.service;

import com.azure.core.http.ContentType;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pip.subscription.management.Application;
import uk.gov.hmcts.reform.pip.subscription.management.models.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionsSummary;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionsSummaryDetails;

import java.io.IOException;
import java.util.List;
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

    private static final String TEST_ID = "123";
    private static final String RESULT_MATCH = "Returned strings should match";

    private final SubscriptionsSummary subscriptionsSummary = new SubscriptionsSummary();
    private final Subscription subscription = new Subscription();

    @BeforeEach
    void setup() {
        subscriptionsSummary.setEmail("a@b.com");
        subscriptionsSummary.setArtefactId(UUID.randomUUID());

        SubscriptionsSummaryDetails subscriptionsSummaryDetails = new SubscriptionsSummaryDetails();
        subscriptionsSummaryDetails.addToCaseNumber(TEST_ID);

        subscriptionsSummary.setSubscriptions(subscriptionsSummaryDetails);

        subscription.setSearchType(SearchType.CASE_ID);
        subscription.setSearchValue(TEST_ID);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockPublicationServicesEndpoint.shutdown();
    }

    @Test
    void testPostSubscriptionSummaries() throws IOException {
        mockPublicationServicesEndpoint = new MockWebServer();
        mockPublicationServicesEndpoint.start(8081);
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                        .addHeader("Content-Type",
                                                                   ContentType.APPLICATION_JSON)
                                                        .setResponseCode(200));



        String result = publicationServicesService.postSubscriptionSummaries(subscriptionsSummary.getArtefactId(),
                                                             subscriptionsSummary.getEmail(), List.of(subscription));
        assertEquals(subscriptionsSummary.toString(), result, RESULT_MATCH);
    }

    @Test
    void testPostSubscriptionSummariesThrows() throws IOException {
        mockPublicationServicesEndpoint = new MockWebServer();
        mockPublicationServicesEndpoint.start(8081);
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        String result = publicationServicesService.postSubscriptionSummaries(subscriptionsSummary.getArtefactId(),
                                                             subscriptionsSummary.getEmail(), List.of(subscription));

        assertEquals("Request failed", result, RESULT_MATCH);
    }

    //TODO: add my tests for the method i moved and the tiny extra bit on list types
}
