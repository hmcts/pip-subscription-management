package uk.gov.hmcts.reform.pip.subscription.management.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.subscription.management.helpers.SubscriptionUtils;
import uk.gov.hmcts.reform.pip.subscription.management.models.Channel;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Artefact;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.ListType;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.UserSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.service.SubscriptionService;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class SubscriptionControllerTest {

    private Subscription mockSubscription;

    private static final String USER_ID = "Ralph21";
    private static final String SEARCH_VALUE = "193254";
    private static final String STATUS_CODE_MATCH = "Status codes should match";
    private static final Channel EMAIL = Channel.EMAIL;

    @Mock
    SubscriptionService subscriptionService;

    @InjectMocks
    SubscriptionController subscriptionController;

    UserSubscription userSubscription;

    @BeforeEach
    void setup() {
        mockSubscription = SubscriptionUtils.createMockSubscription(USER_ID, SEARCH_VALUE, EMAIL, LocalDateTime.now(),
                                                                    ListType.CIVIL_DAILY_CAUSE_LIST);
        userSubscription = new UserSubscription();
    }

    @Test
    void testCreateSubscription() {
        when(subscriptionService.createSubscription(mockSubscription))
            .thenReturn(mockSubscription);
        assertEquals(new ResponseEntity<>(String.format("Subscription created with the id %s for user %s",
                                                          mockSubscription.getId(), mockSubscription.getUserId()),
                                          HttpStatus.CREATED),
                     subscriptionController.createSubscription(mockSubscription.toDto()),
                     "Returned subscription does not match expected subscription"
        );
    }

    @Test
    void testDeleteSubscription() {
        UUID testUuid = UUID.randomUUID();
        doNothing().when(subscriptionService).deleteById(testUuid);
        assertEquals(String.format("Subscription: %s was deleted", testUuid),
                     subscriptionController.deleteById(testUuid).getBody(), "Subscription should be deleted");
    }

    @Test
    void testDeleteSubscriptionReturnsOk() {
        UUID testUuid = UUID.randomUUID();
        doNothing().when(subscriptionService).deleteById(testUuid);
        assertEquals(HttpStatus.OK, subscriptionController.deleteById(testUuid).getStatusCode(),
                     STATUS_CODE_MATCH);
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
                     STATUS_CODE_MATCH);
    }

    @Test
    void testFindByUserId() {
        when(subscriptionService.findByUserId(USER_ID)).thenReturn(userSubscription);
        assertEquals(userSubscription, subscriptionController.findByUserId(USER_ID).getBody(),
                     "Should return users subscriptions");
    }

    @Test
    void testFindByUserIdReturnsOk() {
        when(subscriptionService.findByUserId(USER_ID)).thenReturn(userSubscription);
        assertEquals(HttpStatus.OK, subscriptionController.findByUserId(USER_ID).getStatusCode(),
                     STATUS_CODE_MATCH);
    }

    @Test
    void testArtefactRecipientsReturnsAccepted() {
        doNothing().when(subscriptionService).collectSubscribers(any());
        assertEquals(HttpStatus.ACCEPTED, subscriptionController.buildSubscriberList(new Artefact()).getStatusCode(),
                     STATUS_CODE_MATCH);
    }

    @Test
    void testBuildDeletedArtefactSubscribers() {
        doNothing().when(subscriptionService).collectThirdPartyForDeletion(any());
        assertEquals(HttpStatus.ACCEPTED, subscriptionController.buildDeletedArtefactSubscribers(new Artefact())
                         .getStatusCode(), STATUS_CODE_MATCH);
    }

    @Test
    void testDeleteSubscriptionsByUserId() {
        when(subscriptionService.deleteAllByUserId("test string")).thenReturn(
            "All subscriptions deleted for user id");
        assertEquals("All subscriptions deleted for user id",
                     subscriptionController.deleteAllSubscriptionsForUser("test string").getBody(),
                     "Subscription for user should be deleted");
    }
}
