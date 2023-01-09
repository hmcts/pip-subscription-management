package uk.gov.hmcts.reform.pip.subscription.management.service;

import com.azure.core.http.ContentType;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
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
import uk.gov.hmcts.reform.pip.subscription.management.models.external.publication.services.ThirdPartySubscriptionArtefact;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {Application.class})
@ActiveProfiles({"test", "non-async"})
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
class PublicationServicesServiceTest {

    private static MockWebServer mockPublicationServicesEndpoint;
    private static final String CONTENT_TYPE = "Content-Type";

    @Autowired
    WebClient webClient;

    @Autowired
    PublicationServicesService publicationServicesService;

    private static final String TEST_ID = "123";
    private static final String RESULT_MATCH = "Returned strings should match";
    private static final String REQUEST_FAILED = "Request failed";
    private static final String TEST_API_DESTINATION = "http://www.abc.com";
    private static final Artefact TEST_ARTEFACT = new Artefact();

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
            case LOCATION_ID -> subscriptionsSummaryDetails.addToLocationId(TEST_ID);
            case CASE_URN -> subscriptionsSummaryDetails.addToCaseUrn(TEST_ID);
            case CASE_ID -> subscriptionsSummaryDetails.addToCaseNumber(TEST_ID);
            default -> { }
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
            .sendThirdPartyList(new ThirdPartySubscription(TEST_API_DESTINATION, UUID.randomUUID())),
                     RESULT_MATCH);
    }

    @Test
    void testSendEmptyArtefact() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                    .setResponseCode(200));

        assertEquals("Successfully sent", publicationServicesService.sendEmptyArtefact(
            new ThirdPartySubscriptionArtefact(TEST_API_DESTINATION, TEST_ARTEFACT)), RESULT_MATCH);
    }

    @Test
    void testSendEmptyArtefactReturnsFailed() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .setResponseCode(404));

        try (LogCaptor logCaptor = LogCaptor.forClass(PublicationServicesService.class)) {
            assertEquals(REQUEST_FAILED, publicationServicesService.sendEmptyArtefact(
                new ThirdPartySubscriptionArtefact(TEST_API_DESTINATION, TEST_ARTEFACT)), RESULT_MATCH);
            assertTrue(logCaptor.getErrorLogs().get(0).contains("Request to Publication Services /notify/api failed"),
                       "Log message does not contain expected message");
        }
    }

    @Test
    void testSendThirdPartyListReturnsFailed() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));
        assertEquals("Request failed", publicationServicesService
                         .sendThirdPartyList(new ThirdPartySubscription(TEST_API_DESTINATION, UUID.randomUUID())),
                     "Messages match");

    }
}
