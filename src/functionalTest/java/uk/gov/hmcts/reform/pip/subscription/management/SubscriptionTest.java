package uk.gov.hmcts.reform.pip.subscription.management;

import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.utils.FunctionalTestBase;
import uk.gov.hmcts.reform.pip.subscription.management.utils.OAuthClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static uk.gov.hmcts.reform.pip.subscription.management.utils.TestUtil.randomLocationId;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(profiles = "functional")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = {OAuthClient.class})
class SubscriptionTest extends FunctionalTestBase {
    private static final String CREATE_SUBSCRIPTION_URL = "/subscription";
    private static final String DELETE_SUBSCRIPTION_BY_LOCATION_URL = "/subscription/location/";
    private static final String LOCATION_ID = randomLocationId();
    private static final String LOCATION_NAME = "TestLocation";
    private static final String USER_ID = UUID.randomUUID().toString();
    private static final String BEARER = "Bearer ";
    private Map<String, String> authorisationHeaders;

    @BeforeAll
    public void setup() {
        authorisationHeaders = Map.of(AUTHORIZATION, BEARER + accessToken);
    }

    @AfterAll
    public void shutdown() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.putAll(authorisationHeaders);
        headers.put("x-provenance-user-id", USER_ID);
        doDeleteRequest(DELETE_SUBSCRIPTION_BY_LOCATION_URL + LOCATION_ID, headers);
    }

    @Test
    void shouldBeAbleToCreateASubscription() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.putAll(authorisationHeaders);
        headers.put("x-user-id", USER_ID);

        Response response = doPostRequest(CREATE_SUBSCRIPTION_URL, headers, createTestSubscription());
        assertThat(response.getStatusCode()).isEqualTo(CREATED.value());
    }

    private Subscription createTestSubscription() {
        Subscription subscription = new Subscription();
        subscription.setUserId(USER_ID);
        subscription.setSearchType(SearchType.LOCATION_ID);
        subscription.setSearchValue(LOCATION_ID);
        subscription.setChannel(Channel.EMAIL);
        subscription.setCreatedDate(LocalDateTime.now());
        subscription.setLocationName(LOCATION_NAME);
        subscription.setLastUpdatedDate(LocalDateTime.now());
        subscription.setListType(Collections.emptyList());
        return subscription;
    }
}
