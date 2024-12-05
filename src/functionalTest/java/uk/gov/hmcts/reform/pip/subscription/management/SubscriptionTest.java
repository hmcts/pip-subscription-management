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
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionListType;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.UserSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.utils.FunctionalTestBase;
import uk.gov.hmcts.reform.pip.subscription.management.utils.OAuthClient;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
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
    private static final String BUILD_SUBSCRIBER_LIST_URL = SUBSCRIPTION_URL + "/artefact-recipients";
    private static final String BUILD_DELETED_ARTEFACT_SUBSCRIBER_URL = SUBSCRIPTION_URL + "/deleted-artefact";
    private static final String CONFIGURE_LIST_TYPE_URL = SUBSCRIPTION_URL + "/configure-list-types/";
    private static final String ADD_LIST_TYPE_URL = SUBSCRIPTION_URL + "/add-list-types/";

    private static final String DELETE_SUBSCRIPTIONS_FOR_USER_URL = SUBSCRIPTION_URL + "/user/";
    private static final String SUBSCRIPTION_BY_LOCATION_URL = SUBSCRIPTION_URL + "/location/";

    private static final String LOCATION_ID = randomLocationId();
    private static final String LOCATION_NAME = "TestLocation" + LOCATION_ID;
    private static final String USER_ID = UUID.randomUUID().toString();
    private static final String USER_ID_HEADER = "x-user-id";
    private static final String BEARER = "Bearer ";
    private static final String LIST_LANGUAGE = "ENGLISH";
    private static final String LIST_LANGUAGE_CONFIGURE = "WELSH";

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
        doDeleteRequest(TESTING_SUPPORT_SUBSCRIPTION_URL + LOCATION_NAME, authorisationHeaders);
        doDataManagementDeleteRequest(
            TESTING_SUPPORT_LOCATION_URL + LOCATION_NAME,
            Map.of(AUTHORIZATION, BEARER + dataManagementAccessToken)
        );
    }

    @Test
    void subscriptionTestsPartOne() {

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
        assertThat(returnedSubscriptionByUserId.getLocationSubscriptions().get(0).getLocationId())
            .isEqualTo(LOCATION_ID);
        assertThat(returnedSubscriptionByUserId.getLocationSubscriptions().get(0).getLocationName()).isEqualTo(
            LOCATION_NAME);

        Response responseUserCanDeleteSubscription = doDeleteRequest(
            SUBSCRIPTION_URL + "/" + subscriptionId,
            headerMap
        );
        assertThat(responseUserCanDeleteSubscription.getStatusCode()).isEqualTo(OK.value());
        assertThat(responseUserCanDeleteSubscription.asString()).isEqualTo("Subscription: "
                                                                               + subscriptionId + " was deleted");
    }

    @Test
    void subscriptionTestsPartTwo() {

        Map<String, String> headerMap = new ConcurrentHashMap<>();
        headerMap.putAll(authorisationHeaders);
        headerMap.put(USER_ID_HEADER, USER_ID);

        doPostRequest(
            SUBSCRIPTION_URL,
            headerMap,
            createTestSubscription(LOCATION_ID, USER_ID, LOCATION_NAME)
        );

        SubscriptionListType listType = new SubscriptionListType(
            USER_ID,
            List.of(LIST_TYPE.name()),
            List.of(LIST_LANGUAGE)
        );

        Response responseAddListType = doPostRequest(
            ADD_LIST_TYPE_URL + USER_ID,
            headerMap,
            listType
        );
        assertThat(responseAddListType.getStatusCode()).isEqualTo(CREATED.value());
        assertThat(responseAddListType.asString()).isEqualTo(
            String.format(
                "Location list Type successfully added for user %s",
                USER_ID
            ));

        SubscriptionListType listTypeToConfigure = new SubscriptionListType(
            USER_ID,
            List.of(LIST_TYPE.name(),ListType.CIVIL_DAILY_CAUSE_LIST.name()),
            List.of(LIST_LANGUAGE_CONFIGURE)
        );

        Response responseConfigureListType = doPutRequest(
            CONFIGURE_LIST_TYPE_URL + USER_ID,
            headerMap,
            listTypeToConfigure
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
        assertThat(responseDeleteAllSubscriptionsForUser.asString())
            .isEqualTo("All subscriptions deleted for user id " + USER_ID);
    }

    @Test
    void subscriptionTestsPartThree() {

        Map<String, String> headerMap = new ConcurrentHashMap<>();
        headerMap.putAll(authorisationHeaders);
        headerMap.put(USER_ID_HEADER, USER_ID);

        Response responseCreateSubscription = doPostRequest(
            SUBSCRIPTION_URL,
            headerMap,
            createTestSubscription(LOCATION_ID, USER_ID, LOCATION_NAME)
        );
        final String subscriptionId = responseCreateSubscription.asString().split(" ")[5];

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
    void subscriptionTestsPartFour() throws Exception {

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
        assertThat(responseBulkDeleteSubscriptions.asString()).isEqualTo(
            "Subscriptions with ID " + subscriptionId + " deleted");
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
        return subscription;
    }
}
