package uk.gov.hmcts.reform.pip.subscription.management;

import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.utils.DataManagementApi;
import uk.gov.hmcts.reform.pip.subscription.management.utils.FunctionalTestBase;
import uk.gov.hmcts.reform.pip.subscription.management.utils.OAuthClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static uk.gov.hmcts.reform.pip.subscription.management.utils.TestUtil.BEARER;
import static uk.gov.hmcts.reform.pip.subscription.management.utils.TestUtil.randomLocationId;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(profiles = "functional")
@SpringBootTest(classes = {OAuthClient.class})
class SubscriptionTest extends FunctionalTestBase {
    private static final String TESTING_SUPPORT_SUBSCRIPTION_URL = "/testing-support/subscription/";
    private static final String CREATE_SUBSCRIPTION_URL = "/subscription";
    private static final String LOCATION_ID = randomLocationId();
    private static final String LOCATION_NAME = "TestLocation" + LOCATION_ID;
    private static final String ACTIONING_USER_ID = "1234";
    Map<String, String> authorisationHeaders = Map.of(
        AUTHORIZATION, BEARER + accessToken);

    @Autowired
    private DataManagementApi dataManagementApi;

    @BeforeAll
    public void setup() {
        dataManagementApi.testingSupportCreateLocation(LOCATION_ID, LOCATION_NAME);
    }

    @AfterAll
    public void shutdown() {
        doDeleteRequest(TESTING_SUPPORT_SUBSCRIPTION_URL + LOCATION_NAME, authorisationHeaders);
        dataManagementApi.testingSupportDeleteLocation(LOCATION_NAME);
    }

    @Test
    void shouldBeAbleToCreateASubscription() {
        Map<String, String> headers = Map.of(
            AUTHORIZATION, BEARER + accessToken,
            "x-user-id", ACTIONING_USER_ID
        );

        Subscription subscription = new Subscription();
        subscription.setUserId("123");
        subscription.setSearchType(SearchType.LOCATION_ID);
        subscription.setChannel(Channel.EMAIL);
        subscription.setCreatedDate(LocalDateTime.now());
        subscription.setLocationName(LOCATION_NAME);
        subscription.setLastUpdatedDate(LocalDateTime.now());
        subscription.setListType(Collections.emptyList());

        Response response = doPostRequest(CREATE_SUBSCRIPTION_URL, headers, subscription);

        assertThat(response.getStatusCode()).isEqualTo(CREATED.value());
    }
}
