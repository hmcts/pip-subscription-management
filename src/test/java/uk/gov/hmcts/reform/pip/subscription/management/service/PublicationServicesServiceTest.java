package uk.gov.hmcts.reform.pip.subscription.management.service;

import com.azure.core.http.ContentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.altindag.log.LogCaptor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;
import uk.gov.hmcts.reform.pip.model.subscription.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.model.subscription.ThirdPartySubscriptionArtefact;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.subscription.management.models.BulkSubscriptionsSummary;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionsSummary;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionsSummaryDetails;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles({"test", "non-async"})
class PublicationServicesServiceTest {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String TEST_ID = "123";
    private static final UUID ARTEFACT_ID = UUID.randomUUID();
    private static final String EMAIL = "a@b.com";
    private static final String EMPTY_LOG_EMPTY_MESSAGE = "Error log not empty";
    private static final String EMPTY_LOG_MATCH_MESSAGE = "Error log does not match";
    private static final String TEST_API_DESTINATION = "http://www.abc.com";
    private static final String SUCCESSFULLY_SENT = "Successfully sent";
    private static final Artefact TEST_ARTEFACT = new Artefact();

    private final SubscriptionsSummary subscriptionsSummary = new SubscriptionsSummary();
    private final Subscription subscription = new Subscription();
    private final LogCaptor logCaptor = LogCaptor.forClass(PublicationServicesService.class);

    private final MockWebServer mockPublicationServicesEndpoint = new MockWebServer();
    private PublicationServicesService publicationServicesService;


    @BeforeEach
    void setup() {
        subscriptionsSummary.setEmail("a@b.com");
        subscription.setSearchType(SearchType.CASE_ID);
        subscription.setSearchValue(TEST_ID);

        WebClient mockedWebClient = WebClient.builder()
            .baseUrl(mockPublicationServicesEndpoint.url("/").toString())
            .build();
        publicationServicesService = new PublicationServicesService(mockedWebClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockPublicationServicesEndpoint.shutdown();
    }

    @Test
    void testPostSubscriptionSummariesRequestUrl() throws InterruptedException {
        subscription.setSearchType(SearchType.LIST_TYPE);
        Map<String, List<Subscription>> subscriptionsMap = new ConcurrentHashMap<>();
        subscriptionsMap.put(EMAIL, List.of(subscription));

        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(CONTENT_TYPE,
                                                               ContentType.APPLICATION_JSON)
                                                    .setResponseCode(200));

        publicationServicesService.postSubscriptionSummaries(ARTEFACT_ID, subscriptionsMap);

        RecordedRequest recordedRequest = mockPublicationServicesEndpoint.takeRequest();
        assertNotNull(recordedRequest.getRequestUrl(), "Request URL should not be null");
        assertTrue(recordedRequest.getRequestUrl().toString().contains("/notify/v2/subscription"),
                   "Request URL should be correct");
    }

    @Test
    void testPostSubscriptionSummariesRequestBodyEmail() throws IOException, InterruptedException {
        subscription.setSearchType(SearchType.LIST_TYPE);
        Map<String, List<Subscription>> subscriptionsMap = new ConcurrentHashMap<>();
        subscriptionsMap.put(EMAIL, List.of(subscription));

        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(CONTENT_TYPE,
                                                               ContentType.APPLICATION_JSON)
                                                    .setResponseCode(200));

        publicationServicesService.postSubscriptionSummaries(ARTEFACT_ID, subscriptionsMap);
        RecordedRequest recordedRequest = mockPublicationServicesEndpoint.takeRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        BulkSubscriptionsSummary bulkSubscriptionsSummary =
            objectMapper.readValue(recordedRequest.getBody().readByteArray(), BulkSubscriptionsSummary.class);

        SubscriptionsSummary subscriptionsSummary = bulkSubscriptionsSummary.getSubscriptionEmails().get(0);
        assertEquals(EMAIL, subscriptionsSummary.getEmail(), "Subscription email should match");
    }

    @Test
    void testPostSubscriptionSummariesRequestBodyArtefactId() throws IOException, InterruptedException {
        subscription.setSearchType(SearchType.LIST_TYPE);
        Map<String, List<Subscription>> subscriptionsMap = new ConcurrentHashMap<>();
        subscriptionsMap.put(EMAIL, List.of(subscription));

        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(CONTENT_TYPE,
                                                               ContentType.APPLICATION_JSON)
                                                    .setResponseCode(200));

        publicationServicesService.postSubscriptionSummaries(ARTEFACT_ID, subscriptionsMap);
        RecordedRequest recordedRequest = mockPublicationServicesEndpoint.takeRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        BulkSubscriptionsSummary bulkSubscriptionsSummary =
            objectMapper.readValue(recordedRequest.getBody().readByteArray(), BulkSubscriptionsSummary.class);

        assertEquals(ARTEFACT_ID, bulkSubscriptionsSummary.getArtefactId(), "Subscription artefact ID should match");
    }

    @ParameterizedTest
    @EnumSource(value = SearchType.class, names = {"LOCATION_ID", "CASE_URN", "CASE_ID"})
    void testPostSubscriptionDifferentTypes(SearchType searchType)
        throws InterruptedException, IOException {
        subscription.setSearchType(searchType);
        Map<String, List<Subscription>> subscriptionsMap = new ConcurrentHashMap<>();
        subscriptionsMap.put(EMAIL, List.of(subscription));

        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                        .addHeader(CONTENT_TYPE,
                                                                   ContentType.APPLICATION_JSON)
                                                        .setResponseCode(200));

        publicationServicesService.postSubscriptionSummaries(ARTEFACT_ID, subscriptionsMap);
        assertTrue(logCaptor.getErrorLogs().isEmpty(), EMPTY_LOG_EMPTY_MESSAGE);

        RecordedRequest recordedRequest = mockPublicationServicesEndpoint.takeRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        BulkSubscriptionsSummary bulkSubscriptionsSummary =
            objectMapper.readValue(recordedRequest.getBody().readByteArray(), BulkSubscriptionsSummary.class);

        SubscriptionsSummaryDetails subscriptionsSummaryDetailsReturned = bulkSubscriptionsSummary
            .getSubscriptionEmails().get(0).getSubscriptions();

        switch (searchType) {
            case LOCATION_ID -> {
                assertEquals(1, subscriptionsSummaryDetailsReturned.getLocationId().size(),
                             "Size of location IDs should match");
                assertEquals(TEST_ID, subscriptionsSummaryDetailsReturned.getLocationId().get(0),
                             "Location ID should match");
            }
            case CASE_URN -> {
                assertEquals(1, subscriptionsSummaryDetailsReturned.getCaseUrn().size(),
                             "Size of case URNs should match");
                assertEquals(TEST_ID, subscriptionsSummaryDetailsReturned.getCaseUrn().get(0),
                             "Case URN should match");
            }
            case CASE_ID -> {
                assertEquals(1, subscriptionsSummaryDetailsReturned.getCaseNumber().size(),
                             "Size of case numbers should match");
                assertEquals(TEST_ID, subscriptionsSummaryDetailsReturned.getCaseNumber().get(0),
                             "Case number should match");
            }
            default -> fail("Invalid search type");
        }
    }

    @Test
    void testPostSubscriptionSummariesWhenMultipleSubscriptions() throws InterruptedException, IOException {
        subscription.setSearchType(SearchType.LOCATION_ID);
        Map<String, List<Subscription>> subscriptionsMap = new ConcurrentHashMap<>();
        subscriptionsMap.put(EMAIL, List.of(subscription));
        subscriptionsMap.put("OtherTestEmail", List.of(subscription));

        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(CONTENT_TYPE,
                                                               ContentType.APPLICATION_JSON)
                                                    .setResponseCode(200));

        publicationServicesService.postSubscriptionSummaries(ARTEFACT_ID, subscriptionsMap);

        RecordedRequest recordedRequest = mockPublicationServicesEndpoint.takeRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        BulkSubscriptionsSummary bulkSubscriptionsSummary =
            objectMapper.readValue(recordedRequest.getBody().readByteArray(), BulkSubscriptionsSummary.class);

        assertEquals(2, bulkSubscriptionsSummary.getSubscriptionEmails().size(),
                     "Number of subscriptions should match when there are multiple subscriptions");
    }

    @Test
    void testPostSubscriptionSummariesThrows() {
        Map<String, List<Subscription>> subscriptionsMap = new ConcurrentHashMap<>();
        subscriptionsMap.put(EMAIL, List.of(subscription));

        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        publicationServicesService.postSubscriptionSummaries(ARTEFACT_ID,
                                                             subscriptionsMap);

        assertTrue(
            logCaptor.getErrorLogs().get(0).contains("Subscription email failed to send with error"),
            EMPTY_LOG_MATCH_MESSAGE
        );
    }

    @Test
    void testSendThirdPartyList() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                    .setResponseCode(200));

        publicationServicesService.sendThirdPartyList(
            new ThirdPartySubscription(TEST_API_DESTINATION, UUID.randomUUID())
        );
        assertTrue(logCaptor.getErrorLogs().isEmpty(), EMPTY_LOG_EMPTY_MESSAGE);
    }

    @Test
    void testSendEmptyArtefact() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                    .setResponseCode(200));

        publicationServicesService.sendEmptyArtefact(
            new ThirdPartySubscriptionArtefact(TEST_API_DESTINATION, TEST_ARTEFACT)
        );
        assertTrue(logCaptor.getErrorLogs().isEmpty(), EMPTY_LOG_EMPTY_MESSAGE);
    }

    @Test
    void testSendEmptyArtefactReturnsFailed() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .setResponseCode(404));

        publicationServicesService.sendEmptyArtefact(
            new ThirdPartySubscriptionArtefact(TEST_API_DESTINATION, TEST_ARTEFACT)
        );

        assertTrue(logCaptor.getErrorLogs().get(0)
                       .contains("Deleted artefact notification to third party failed to send with error"),
                   EMPTY_LOG_MATCH_MESSAGE
        );
    }

    @Test
    void testSendThirdPartyListReturnsFailed() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        publicationServicesService.sendThirdPartyList(
            new ThirdPartySubscription(TEST_API_DESTINATION, UUID.randomUUID())
        );
        assertTrue(
            logCaptor.getErrorLogs().get(0).contains("Publication to third party failed to send with error"),
            EMPTY_LOG_MATCH_MESSAGE
        );

    }

    @Test
    void testSendSystemAdminEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setBody(SUCCESSFULLY_SENT));

        publicationServicesService.sendSystemAdminEmail(List.of("test@test.com"), EMAIL,
                                                        ActionResult.ATTEMPTED, "Error");
        assertTrue(logCaptor.getErrorLogs().isEmpty(), EMPTY_LOG_EMPTY_MESSAGE);
    }

    @Test
    void testFailedSendSystemAdminEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .setResponseCode(400));

        publicationServicesService.sendSystemAdminEmail(List.of("test@test.com"), EMAIL,
                                                        ActionResult.ATTEMPTED, "Error");

        assertTrue(
            logCaptor.getErrorLogs().get(0).contains("System admin notification email failed to send with error"),
            EMPTY_LOG_MATCH_MESSAGE
        );
    }
}
