package uk.gov.hmcts.reform.pip.subscription.management.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.subscription.management.helpers.SubscriptionUtils;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.UserSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.service.SubscriptionLocationService;
import uk.gov.hmcts.reform.pip.subscription.management.service.SubscriptionNotificationService;
import uk.gov.hmcts.reform.pip.subscription.management.service.SubscriptionService;
import uk.gov.hmcts.reform.pip.subscription.management.service.UserSubscriptionService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class SubscriptionControllerTest {

    private Subscription mockSubscription;
    private List<Subscription> mockSubscriptionList;

    private static final String USER_ID = "Ralph21";
    private static final String SEARCH_VALUE = "193254";
    private static final String STATUS_CODE_MATCH = "Status codes should match";
    private static final Channel EMAIL = Channel.EMAIL;
    private static final List<String> LIST_TYPES = Arrays.asList(ListType.CIVIL_DAILY_CAUSE_LIST.name());
    private static final String LOCATION_ID = "1";
    private static final String ACTIONING_USER_ID = "1234-1234";
    private static final String REQUESTER_NAME = "ReqName";

    @Mock
    SubscriptionService subscriptionService;

    @Mock
    UserSubscriptionService userSubscriptionService;

    @Mock
    SubscriptionNotificationService subscriptionNotificationService;

    @Mock
    SubscriptionLocationService subscriptionLocationService;

    @InjectMocks
    SubscriptionController subscriptionController;

    UserSubscription userSubscription;

    @BeforeEach
    void setup() {
        mockSubscription = SubscriptionUtils.createMockSubscription(USER_ID, SEARCH_VALUE, EMAIL, LocalDateTime.now(),
                                                                    ListType.CIVIL_DAILY_CAUSE_LIST
        );
        userSubscription = new UserSubscription();
    }

    @Test
    void testCreateSubscription() {
        when(subscriptionService.createSubscription(mockSubscription, ACTIONING_USER_ID))
            .thenReturn(mockSubscription);
        assertEquals(
            new ResponseEntity<>(
                String.format("Subscription created with the id %s for user %s",
                              mockSubscription.getId(), mockSubscription.getUserId()
                ),
                HttpStatus.CREATED
            ),
            subscriptionController.createSubscription(mockSubscription.toDto(), ACTIONING_USER_ID),
            "Returned subscription does not match expected subscription"
        );
    }

    @Test
    void testDeleteSubscription() {
        UUID testUuid = UUID.randomUUID();
        doNothing().when(subscriptionService).deleteById(testUuid, ACTIONING_USER_ID);
        assertEquals(String.format(
            "Subscription: %s was deleted", testUuid), subscriptionController.deleteById(
                testUuid, ACTIONING_USER_ID).getBody(), "Subscription should be deleted"
        );
    }

    @Test
    void testDeleteSubscriptionReturnsOk() {
        UUID testUuid = UUID.randomUUID();
        doNothing().when(subscriptionService).deleteById(testUuid, ACTIONING_USER_ID);
        assertEquals(HttpStatus.OK, subscriptionController.deleteById(testUuid, ACTIONING_USER_ID).getStatusCode(),
                     STATUS_CODE_MATCH
        );
    }

    @Test
    void testBulkDeleteSubscriptionsSuccess() {
        UUID testId1 = UUID.randomUUID();
        UUID testId2 = UUID.randomUUID();
        UUID testId3 = UUID.randomUUID();
        List<UUID> testIds = List.of(testId1, testId2, testId3);
        String expectedTestIds = String.join(", ", new String[]{
            testId1.toString(),
            testId2.toString(),
            testId3.toString()
        });

        doNothing().when(subscriptionService).bulkDeleteSubscriptions(testIds);
        ResponseEntity<String> response = subscriptionController.bulkDeleteSubscriptions(testIds);

        assertEquals(String.format("Subscription(s) with ID %s deleted", expectedTestIds), response.getBody(),
                     "Subscription should be deleted");
        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);
    }

    @Test
    void testFindSubscription() {
        when(subscriptionService.findById(any())).thenReturn(mockSubscription);
        assertEquals(mockSubscription, subscriptionController.findBySubId(UUID.randomUUID()).getBody(), "The found "
            + "subscription does not match expected subscription");
    }

    @Test
    void testFindSubscriptionReturnsOk() {
        when(subscriptionService.findById(any())).thenReturn(mockSubscription);
        assertEquals(HttpStatus.OK, subscriptionController.findBySubId(UUID.randomUUID()).getStatusCode(),
                     STATUS_CODE_MATCH
        );
    }

    @Test
    void testFindByUserId() {
        when(userSubscriptionService.findByUserId(USER_ID)).thenReturn(userSubscription);
        assertEquals(userSubscription, subscriptionController.findByUserId(USER_ID).getBody(),
                     "Should return users subscriptions"
        );
    }

    @Test
    void testFindByUserIdReturnsOk() {
        when(userSubscriptionService.findByUserId(USER_ID)).thenReturn(userSubscription);
        assertEquals(HttpStatus.OK, subscriptionController.findByUserId(USER_ID).getStatusCode(),
                     STATUS_CODE_MATCH
        );
    }

    @Test
    void testArtefactRecipientsReturnsAccepted() {
        doNothing().when(subscriptionNotificationService).collectSubscribers(any());
        assertEquals(HttpStatus.ACCEPTED, subscriptionController.buildSubscriberList(new Artefact()).getStatusCode(),
                     STATUS_CODE_MATCH
        );
    }

    @Test
    void testBuildDeletedArtefactSubscribers() {
        doNothing().when(subscriptionNotificationService).collectThirdPartyForDeletion(any());
        assertEquals(HttpStatus.ACCEPTED, subscriptionController.buildDeletedArtefactSubscribers(new Artefact())
            .getStatusCode(), STATUS_CODE_MATCH);
    }

    @Test
    void testMiDataReturnsOk() {
        assertEquals(
            HttpStatus.OK,
            subscriptionController.getSubscriptionDataForMiReportingLocal().getStatusCode(),
            STATUS_CODE_MATCH
        );
        assertEquals(
            HttpStatus.OK,
            subscriptionController.getSubscriptionDataForMiReportingAll().getStatusCode(),
            STATUS_CODE_MATCH
        );
    }

    @Test
    void testConfigureListTypesForSubscription() {
        doNothing().when(subscriptionService).configureListTypesForSubscription(USER_ID, LIST_TYPES);

        assertEquals(
            new ResponseEntity<>(
                String.format(
                    "Location list Type successfully updated for user %s",
                    USER_ID
                ),
                HttpStatus.OK
            ),
            subscriptionController.configureListTypesForSubscription(USER_ID, LIST_TYPES),
            "Returned subscription does not match expected subscription"
        );
    }

    @Test
    void testDeleteSubscriptionsByUserId() {
        when(userSubscriptionService.deleteAllByUserId("test string")).thenReturn(
            "All subscriptions deleted for user id");
        assertEquals(
            "All subscriptions deleted for user id",
            subscriptionController.deleteAllSubscriptionsForUser("test string").getBody(),
            "Subscription for user should be deleted"
        );
    }

    @Test
    void testFindSubscriptionsByLocationId() {
        when(subscriptionLocationService.findSubscriptionsByLocationId(LOCATION_ID))
            .thenReturn(mockSubscriptionList);
        assertEquals(mockSubscriptionList, subscriptionController.findSubscriptionsByLocationId(LOCATION_ID).getBody(),
                     "The found subscription does not match expected subscription");
    }

    @Test
    void testFindSubscriptionsByLocationIdReturnsOk() {
        when(subscriptionLocationService.findSubscriptionsByLocationId(LOCATION_ID))
            .thenReturn(mockSubscriptionList);
        assertEquals(HttpStatus.OK, subscriptionController.findSubscriptionsByLocationId(LOCATION_ID)
                         .getStatusCode(), STATUS_CODE_MATCH);
    }

    @Test
    void testDeleteSubscriptionByLocationReturnsOk() throws JsonProcessingException {
        when(subscriptionLocationService.deleteSubscriptionByLocation(LOCATION_ID, REQUESTER_NAME))
            .thenReturn("Total 10 subscriptions deleted for location id");

        assertEquals(HttpStatus.OK, subscriptionController.deleteSubscriptionByLocation(
            REQUESTER_NAME, Integer.parseInt(LOCATION_ID)).getStatusCode(),
                     "Delete subscription location endpoint has not returned OK");
    }
}
