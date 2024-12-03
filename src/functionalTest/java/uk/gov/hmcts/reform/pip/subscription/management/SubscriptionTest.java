package uk.gov.hmcts.reform.pip.subscription.management;

import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.pip.model.publication.*;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.UserSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.utils.FunctionalTestBase;
import uk.gov.hmcts.reform.pip.subscription.management.utils.OAuthClient;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.*;
import static uk.gov.hmcts.reform.pip.subscription.management.utils.TestUtil.randomLocationId;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(profiles = "functional")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = {OAuthClient.class})
class SubscriptionTest extends FunctionalTestBase {

    @Value("${system-admin-provenance-id}")
    private String systemAdminProvenanceId;

    private static final String SUBSCRIPTION_URL = "/subscription";
    private static final String FIND_SUBSCRIPTION_BY_USER_ID_URL = "/subscription/user/";
    private static final String TESTING_SUPPORT_SUBSCRIPTION_URL = "/testing-support/subscription/";
    private static final String TESTING_SUPPORT_LOCATION_URL = "/testing-support/location/";
    private static final String TESTING_SUPPORT_PUBLICATION_URL = "/testing-support/publication/";
    private static final String BUILD_SUBSCRIBER_LIST_URL = SUBSCRIPTION_URL + "/artefact-recipients";
    private static final String BUILD_DELETED_ARTEFACT_SUBSCRIBER_URL = SUBSCRIPTION_URL + "/deleted-artefact";
    private static final String CONFIGURE_LIST_TYPE_URL = SUBSCRIPTION_URL + "/configure-list-types/";
    private static final String DELETE_SUBSCRIPTIONS_FOR_USER_URL = SUBSCRIPTION_URL + "/user/";
    private static final String SUBSCRIPTION_BY_LOCATION_URL = SUBSCRIPTION_URL + "/location/";

    private static final String LOCATION_ID = randomLocationId();
    private static final String LOCATION_NAME = "TestLocation" + LOCATION_ID;
    private static final String USER_ID = UUID.randomUUID().toString();
    private static final String USER_ID_HEADER = "x-user-id";
    private static final String BEARER = "Bearer ";

    private static final ListType LIST_TYPE = ListType.CIVIL_DAILY_CAUSE_LIST;
    private Map<String, String> authorisationHeaders;

    @BeforeAll
    public void setup() {
        authorisationHeaders = Map.of(AUTHORIZATION, BEARER + accessToken);

        doDataManagementPostRequest(
            TESTING_SUPPORT_LOCATION_URL + LOCATION_ID,
            Map.of(AUTHORIZATION, BEARER + dataManagementAccessToken), LOCATION_NAME
        );
    }

    @AfterAll
    public void teardown() {
        doDataManagementDeleteRequest(
            TESTING_SUPPORT_PUBLICATION_URL + LOCATION_NAME,
            Map.of(AUTHORIZATION, BEARER + dataManagementAccessToken)
        );
        doDeleteRequest(TESTING_SUPPORT_SUBSCRIPTION_URL + LOCATION_NAME, authorisationHeaders);
        doDataManagementDeleteRequest(
            TESTING_SUPPORT_LOCATION_URL + LOCATION_NAME,
            Map.of(AUTHORIZATION, BEARER + dataManagementAccessToken)
        );
    }

    @Test
    void subscriptionTestsSetOne()  {

        Map<String, String> headerMap = new ConcurrentHashMap<>();
        headerMap.putAll(authorisationHeaders);
        headerMap.put(USER_ID_HEADER, USER_ID);

        Response responseCreateSubscription = doPostRequest(
            SUBSCRIPTION_URL,
            headerMap,
            createTestSubscription(LOCATION_ID, USER_ID, LOCATION_NAME)
        );
        assertThat(responseCreateSubscription.getStatusCode()).isEqualTo(CREATED.value());
        assertThat(responseCreateSubscription.asString().contains(USER_ID));

        String subscriptionId = responseCreateSubscription.asString().split(" ")[5];
        Response responseFindBySubId = doGetRequest(SUBSCRIPTION_URL + "/" + subscriptionId, headerMap);
        assertThat(responseFindBySubId.getStatusCode()).isEqualTo(OK.value());
        Subscription returnedSubscriptionBySubId = responseFindBySubId.as(Subscription.class);
        assertThat(returnedSubscriptionBySubId.getUserId()).isEqualTo(USER_ID);
        assertThat(returnedSubscriptionBySubId.getLocationName()).isEqualTo(LOCATION_NAME);

        Response responseFindByUserId = doGetRequest(FIND_SUBSCRIPTION_BY_USER_ID_URL + USER_ID, headerMap);
        assertThat(responseFindByUserId.getStatusCode()).isEqualTo(OK.value());
        UserSubscription returnedSubscriptionByUserId = responseFindByUserId.as(UserSubscription.class);
        assertThat(returnedSubscriptionByUserId.getCaseSubscriptions().size()).isEqualTo(0);
        assertThat(returnedSubscriptionByUserId.getLocationSubscriptions().get(0).getLocationId()).isEqualTo(LOCATION_ID);
        assertThat(returnedSubscriptionByUserId.getLocationSubscriptions().get(0).getLocationName()).isEqualTo(
            LOCATION_NAME);

        Response responseUserCanDeleteSubscription = doDeleteRequest(
            SUBSCRIPTION_URL + "/" + subscriptionId,
            headerMap
        );
        assertThat(responseUserCanDeleteSubscription.getStatusCode()).isEqualTo(OK.value());
        assertThat(responseUserCanDeleteSubscription.asString()).isEqualTo("Subscription: " + subscriptionId + " was deleted");
    }

    @Test
    void subscriptionTestsSetTwo()  {

        Map<String, String> headerMap = new ConcurrentHashMap<>();
        headerMap.putAll(authorisationHeaders);
        headerMap.put(USER_ID_HEADER, USER_ID);

        doPostRequest(
            SUBSCRIPTION_URL,
            headerMap,
            createTestSubscription(LOCATION_ID, USER_ID, LOCATION_NAME)
        );

        Response responseConfigureListType = doPutRequest(
            CONFIGURE_LIST_TYPE_URL + USER_ID,
            headerMap,
            List.of(LIST_TYPE.name())
        );
        assertThat(responseConfigureListType.getStatusCode()).isEqualTo(OK.value());
        assertThat(responseConfigureListType.asString()).isEqualTo(
            String.format(
                "Location list Type successfully updated for user %s",
                USER_ID
            ));

        Response responseDeleteAllSubscriptionsForUser = doDeleteRequest(
            DELETE_SUBSCRIPTIONS_FOR_USER_URL + USER_ID,
            headerMap
        );
        assertThat(responseDeleteAllSubscriptionsForUser.getStatusCode()).isEqualTo(OK.value());
        assertThat(responseDeleteAllSubscriptionsForUser.asString()).isEqualTo("All subscriptions deleted for user id " + USER_ID);
    }

    @Test
    void subscriptionTestsSetThree() {

        Map<String, String> headerMap = new ConcurrentHashMap<>();
        headerMap.putAll(authorisationHeaders);
        headerMap.put(USER_ID_HEADER, USER_ID);

        Response responseCreateSubscription = doPostRequest(
            SUBSCRIPTION_URL,
            headerMap,
            createTestSubscription(LOCATION_ID, USER_ID, LOCATION_NAME)
        );
        String subscriptionId = responseCreateSubscription.asString().split(" ")[5];

        Response responseFindByLocationId = doGetRequest(SUBSCRIPTION_BY_LOCATION_URL + LOCATION_ID, headerMap);
        assertThat(responseFindByLocationId.getStatusCode()).isEqualTo(OK.value());
        List<Subscription> returnedSubscriptionsByLocationId = Arrays.asList(responseFindByLocationId.getBody().as(
            Subscription[].class));
        assertThat(returnedSubscriptionsByLocationId.get(0).getUserId()).isEqualTo(USER_ID);
        assertThat(returnedSubscriptionsByLocationId.get(0).getLocationName()).isEqualTo(LOCATION_NAME);

        headerMap.put("x-provenance-user-id", systemAdminProvenanceId);
        final Response responseDeleteSubscriptionByLocationId = doDeleteRequest(
            SUBSCRIPTION_BY_LOCATION_URL + LOCATION_ID,
            headerMap
        );
        assertThat(responseDeleteSubscriptionByLocationId.getStatusCode()).isEqualTo(OK.value());
        assertThat(responseDeleteSubscriptionByLocationId.asString().contains(String.format(
            "Subscription created with the id %s for user %s",
            subscriptionId,
            USER_ID
        )));
    }

    @Test
    void subscriptionTestsSetFour() throws Exception {

        Map<String, String> headerMap = new ConcurrentHashMap<>();
        headerMap.putAll(authorisationHeaders);
        headerMap.put(USER_ID_HEADER, USER_ID);

        Response responseCreateSubscription = doPostRequest(
            SUBSCRIPTION_URL,
            headerMap,
            createTestSubscription(LOCATION_ID, USER_ID, LOCATION_NAME)
        );
        String subscriptionId = responseCreateSubscription.asString().split(" ")[5];

        try (InputStream jsonFile = this.getClass().getClassLoader()
            .getResourceAsStream("subscription/civilDailyCauseList.json")) {
            final String jsonString = new String(jsonFile.readAllBytes(), StandardCharsets.UTF_8);

            Response responseBuildSubscriberList = doPostRequest(
                BUILD_SUBSCRIBER_LIST_URL,
                headerMap, jsonString
            );
            assertThat(responseBuildSubscriberList.getStatusCode()).isEqualTo(ACCEPTED.value());
            assertThat(responseBuildSubscriberList.asString()).isEqualTo(
                "Subscriber request has been accepted");

            Response responseBuildDeletedArtefactSubscriberList = doPostRequest(
                BUILD_DELETED_ARTEFACT_SUBSCRIBER_URL,
                headerMap, jsonString
            );
            assertThat(responseBuildDeletedArtefactSubscriberList.getStatusCode()).isEqualTo(ACCEPTED.value());
            assertThat(responseBuildDeletedArtefactSubscriberList.asString()).isEqualTo(
                "Deleted artefact third party subscriber notification request has been accepted");
        }

        Response responseBulkDeleteSubscriptions = doDeleteRequestWithBody(
            SUBSCRIPTION_URL + "/v2/bulk",
            headerMap, List.of(UUID.fromString(subscriptionId))
        );
        assertThat(responseBulkDeleteSubscriptions.getStatusCode()).isEqualTo(OK.value());
    }

    private Subscription createTestSubscription(String locationid, String userId, String locationName) {
        Subscription subscription = new Subscription();
        subscription.setUserId(userId);
        subscription.setSearchType(SearchType.LOCATION_ID);
        subscription.setSearchValue(locationid);
        subscription.setChannel(Channel.EMAIL);
        subscription.setCreatedDate(LocalDateTime.now());
        subscription.setLocationName(locationName);
        subscription.setLastUpdatedDate(LocalDateTime.now());
        subscription.setListType(List.of(LIST_TYPE.name()));
        return subscription;
    }
}
