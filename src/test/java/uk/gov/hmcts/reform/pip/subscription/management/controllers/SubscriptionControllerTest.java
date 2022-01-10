package uk.gov.hmcts.reform.pip.subscription.management.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.subscription.management.helpers.SubscriptionHelper;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.service.SubscriptionService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class SubscriptionControllerTest {

    private Subscription mockSubscription;
    private Subscription findableSubscription;
    private static final String USER_ID = "Ralph21";
    private static final String SEARCH_VALUE = "193254";

    @Mock
    SubscriptionService subscriptionService;

    @InjectMocks
    SubscriptionController subscriptionController;

    @BeforeEach
    void setup() {
        mockSubscription = SubscriptionHelper.createMockSubscription(USER_ID, SEARCH_VALUE);
        findableSubscription = SubscriptionHelper.findableSubscription();

    }


    @Test
    void testCreateSubscription() {
        when(subscriptionService.createSubscription(mockSubscription))
            .thenReturn(mockSubscription);
        assertEquals(new ResponseEntity<>(String.format("Subscription created with the id %s for user '%s'",
                                                          mockSubscription.getId(), mockSubscription.getUserId()),
                                          HttpStatus.CREATED),
                     subscriptionController.createSubscription(mockSubscription.toDto()),
                     "Returned subscription does not match expected subscription"
        );
    }

    @Test
    void testDeleteSubscription() {
        ArgumentCaptor<UUID> captor = ArgumentCaptor.forClass(UUID.class);
        doNothing().when(subscriptionService).deleteById(captor.capture());
        UUID testUuid = UUID.randomUUID();
        subscriptionController.deleteById(testUuid);
        assertEquals(testUuid, captor.getValue(), "The service layer tried to delete the wrong subscription");
    }


    @Test
    void testFindSubscription() {
        when(subscriptionService.findById(any())).thenReturn(findableSubscription);
        assertEquals(subscriptionController.findBySubId(UUID.randomUUID()), findableSubscription, "The found "
            + "subscription does not match expected subscription");
    }

}
