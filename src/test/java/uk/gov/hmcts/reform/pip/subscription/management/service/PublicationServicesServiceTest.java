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
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;
import uk.gov.hmcts.reform.pip.model.subscription.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.model.subscription.ThirdPartySubscriptionArtefact;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.subscription.management.Application;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionsSummary;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionsSummaryDetails;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

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
    private static final String EMPTY_LOG_EMPTY_MESSAGE = "Error log not empty";
    private static final String EMPTY_LOG_MATCH_MESSAGE = "Error log does not match";
    private static final String TEST_API_DESTINATION = "http://www.abc.com";
    private static final String SUCCESSFULLY_SENT = "Successfully sent";
    private static final Artefact TEST_ARTEFACT = new Artefact();

    private final SubscriptionsSummary subscriptionsSummary = new SubscriptionsSummary();
    private final Subscription subscription = new Subscription();
    private SubscriptionsSummaryDetails subscriptionsSummaryDetails;
    LogCaptor logCaptor = LogCaptor.forClass(PublicationServicesService.class);

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

        publicationServicesService.postSubscriptionSummaries(subscriptionsSummary.getArtefactId(),
                                                             subscriptionsSummary.getEmail(),
                                                             List.of(subscription));
        assertTrue(logCaptor.getErrorLogs().isEmpty(), EMPTY_LOG_EMPTY_MESSAGE);

    }

    @Test
    void testPostSubscriptionSummariesThrows() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        publicationServicesService.postSubscriptionSummaries(subscriptionsSummary.getArtefactId(),
                                                             subscriptionsSummary.getEmail(),
                                                             List.of(subscription));

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

        publicationServicesService.sendSystemAdminEmail(List.of("test@test.com"), "Name",
                                                        ActionResult.ATTEMPTED, "Error");
        assertTrue(logCaptor.getErrorLogs().isEmpty(), EMPTY_LOG_EMPTY_MESSAGE);
    }

    @Test
    void testFailedSendSystemAdminEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .setResponseCode(400));

        publicationServicesService.sendSystemAdminEmail(List.of("test@test.com"), "Name",
                                                        ActionResult.ATTEMPTED, "Error");

        assertTrue(
            logCaptor.getErrorLogs().get(0).contains("System admin notification email failed to send with error"),
            EMPTY_LOG_MATCH_MESSAGE
        );
    }
}
