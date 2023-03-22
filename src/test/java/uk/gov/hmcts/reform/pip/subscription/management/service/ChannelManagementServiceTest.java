package uk.gov.hmcts.reform.pip.subscription.management.service;

import com.azure.core.http.ContentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.Application;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = {Application.class})
@ActiveProfiles({"test", "non-async"})
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
class ChannelManagementServiceTest {

    private static MockWebServer mockChannelManagementEmailsEndpoint;
    private static final String CONTENT_TYPE = "Content-Type";

    @Autowired
    WebClient webClient;

    @Autowired
    ChannelManagementService channelManagementService;

    private List<Subscription> subscriptionList;
    private final Map<String, List<Subscription>> expectedMap = new ConcurrentHashMap<>();
    private String jsonResponse;

    @BeforeEach
    void setup() throws IOException {
        Subscription mockSubscription = new Subscription();
        mockSubscription.setUserId(UUID.randomUUID().toString());
        mockSubscription.setSearchType(SearchType.LOCATION_ID);
        mockSubscription.setSearchValue("1");
        mockSubscription.setChannel(Channel.EMAIL);
        mockSubscription.setCaseNumber("1");

        subscriptionList = List.of(mockSubscription);
        expectedMap.put("a@b.com", subscriptionList);

        ObjectWriter ow = new ObjectMapper().findAndRegisterModules().writer().withDefaultPrettyPrinter();
        jsonResponse = ow.writeValueAsString(expectedMap);

        mockChannelManagementEmailsEndpoint = new MockWebServer();
        mockChannelManagementEmailsEndpoint.start(8181);
    }

    @AfterEach
    void teardown() throws IOException {
        mockChannelManagementEmailsEndpoint.shutdown();
    }

    @Test
    void testGetMappedEmails() {
        mockChannelManagementEmailsEndpoint.enqueue(new MockResponse()
                                                .addHeader(CONTENT_TYPE,
                                                           ContentType.APPLICATION_JSON)
                                                .setBody(jsonResponse));

        Map<String, List<Subscription>> returnedMap =
            channelManagementService.getMappedEmails(subscriptionList);

        assertEquals(expectedMap, returnedMap, "Returned map does not equal expected map");
    }

    @Test
    void testGetMappedEmailsThrows() {
        mockChannelManagementEmailsEndpoint.enqueue(new MockResponse().setResponseCode(404));

        Map<String, List<Subscription>> returnedMap =
            channelManagementService.getMappedEmails(subscriptionList);

        assertNotNull(returnedMap, "List was null when error occurred");
    }

    @Test
    void testGetMappedApis() {
        mockChannelManagementEmailsEndpoint.enqueue(new MockResponse()
                                                        .addHeader(CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                        .setBody(jsonResponse));

        Map<String, List<Subscription>> returnedMap = channelManagementService.getMappedApis(subscriptionList);
        assertEquals(expectedMap, returnedMap, "Maps should match");
    }

    @Test
    void testGetMappedApisReturnsEmptyMapOnFail() {
        mockChannelManagementEmailsEndpoint.enqueue(new MockResponse().setResponseCode(404));

        assertEquals(Collections.emptyMap(), channelManagementService.getMappedApis(subscriptionList),
                     "Should return empty map on fail");
    }
}
