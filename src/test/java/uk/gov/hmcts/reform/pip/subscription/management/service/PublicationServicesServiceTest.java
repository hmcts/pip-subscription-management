package uk.gov.hmcts.reform.pip.subscription.management.service;

import com.azure.core.http.ContentType;
import nl.altindag.log.LogCaptor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pip.subscription.management.Application;
import uk.gov.hmcts.reform.pip.subscription.management.models.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionsSummary;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionsSummaryDetails;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Artefact;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.publication.services.ThirdPartySubscription;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {Application.class})
@ActiveProfiles({"test", "non-async"})
class PublicationServicesServiceTest {

    private static MockWebServer mockPublicationServicesEndpoint;
    private static final String CONTENT_TYPE = "Content-Type";

    @Autowired
    WebClient webClient;

    @Autowired
    PublicationServicesService publicationServicesService;

    private static final String TEST_ID = "123";
    private static final String RESULT_MATCH = "Returned strings should match";
    private static final String REQUEST_FAILED = "Request Failed";

    private final SubscriptionsSummary subscriptionsSummary = new SubscriptionsSummary();
    private final Subscription subscription = new Subscription();
    private SubscriptionsSummaryDetails subscriptionsSummaryDetails;

    @BeforeEach
    void setup() throws IOException {
        subscriptionsSummary.setEmail("a@b.com");
        subscriptionsSummary.setArtefactId(UUID.randomUUID());

        subscriptionsSummaryDetails = new SubscriptionsSummaryDetails();

        subscription.setSearchType(SearchType.CASE_ID);
        subscription.setSearchValue(TEST_ID);
        mockPublicationServicesEndpoint = new MockWebServer();
        mockPublicationServicesEndpoint.start(8081);
    }


    @AfterEach
    void tearDown() throws IOException {
        mockPublicationServicesEndpoint.shutdown();
    }

    @ParameterizedTest
    @EnumSource(value = SearchType.class, names = {"LOCATION_ID", "CASE_URN", "CASE_ID"})
    void testPostSubscriptionSummaries(SearchType searchType) {
        switch (searchType) {
            case LOCATION_ID:
                subscriptionsSummaryDetails.addToLocationId(TEST_ID);
                break;
            case CASE_URN:
                subscriptionsSummaryDetails.addToCaseUrn(TEST_ID);
                break;
            case CASE_ID:
                subscriptionsSummaryDetails.addToCaseNumber(TEST_ID);
                break;
            default:
                break;
        }
        subscriptionsSummary.setSubscriptions(subscriptionsSummaryDetails);

        subscription.setSearchType(searchType);
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                        .addHeader(CONTENT_TYPE,
                                                                   ContentType.APPLICATION_JSON)
                                                        .setResponseCode(200));



        String result = publicationServicesService.postSubscriptionSummaries(subscriptionsSummary.getArtefactId(),
                                                             subscriptionsSummary.getEmail(), List.of(subscription));
        assertEquals(subscriptionsSummary.toString(), result, RESULT_MATCH);
    }

    @Test
    void testPostSubscriptionSummariesThrows() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        String result = publicationServicesService.postSubscriptionSummaries(subscriptionsSummary.getArtefactId(),
                                                             subscriptionsSummary.getEmail(), List.of(subscription));

        assertEquals(REQUEST_FAILED, result, RESULT_MATCH);
    }

    @Test
    void testSendThirdPartyList() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                    .setResponseCode(200));
        assertEquals("Successfully sent", publicationServicesService
            .sendThirdPartyList(new ThirdPartySubscription("test", UUID.randomUUID())),
                     RESULT_MATCH);
    }

    @Test
    void testSendThirdPartyListReturnsFailed() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        assertEquals(REQUEST_FAILED, publicationServicesService
                         .sendThirdPartyList(new ThirdPartySubscription("test", UUID.randomUUID())),
                     RESULT_MATCH);

    }

    @Test
    void testSendEmptyArtefact() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                    .setResponseCode(200));

        assertEquals("Successfully sent", publicationServicesService.sendEmptyArtefact(TEST_ID), RESULT_MATCH);
    }

    @Test
    void testSendEmptyArtefactReturnsFailed() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        try(LogCaptor logCaptor = LogCaptor.forClass(PublicationServicesService.class)) {
            assertEquals(REQUEST_FAILED, publicationServicesService.sendEmptyArtefact(TEST_ID), RESULT_MATCH);
            assertTrue(logCaptor.getErrorLogs().get(0).contains("Request to Publication Services /notify/api failed"));
        }
    }
}
