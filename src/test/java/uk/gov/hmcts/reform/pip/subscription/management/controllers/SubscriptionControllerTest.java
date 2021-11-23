package uk.gov.hmcts.reform.pip.subscription.management.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.subscription.management.helpers.SubscriptionHelper;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.services.SubscriptionService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class SubscriptionControllerTest {

    private List<Subscription> mockSubscriptionList;
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
        mockSubscriptionList = SubscriptionHelper.createMockSubscriptionList();
        findableSubscription = SubscriptionHelper.findableSubscription();

    }

    @Test
    void testGetSubscriptionReturnsExpected() {
        when(subscriptionService.findAll()).thenReturn(mockSubscriptionList);
        assertEquals(mockSubscriptionList, subscriptionController.findAll(), "Subscription list "
            + "does not match expected list");
    }

    @Test
    void testCreateSubscription() {
        when(subscriptionService.createSubscription(mockSubscription)).thenReturn(mockSubscription);
        assertEquals(subscriptionController.createSubscription(mockSubscription), mockSubscription,
                     "Returned subscription does not match expected subscription"
        );
    }

    @Test
    void testDeleteSubscription() {
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        doNothing().when(subscriptionService).deleteById(captor.capture());
        subscriptionController.deleteById(1L);
        assertEquals(1L, captor.getValue(), "The service layer tried to delete the wrong subscription");
    }


    @Test
    void testFindSubscription() {
        when(subscriptionService.findById(any())).thenReturn(findableSubscription);
        assertEquals(subscriptionController.findBySubId(3L), findableSubscription, "The found "
            + "subscription does not match expected subscription");
    }

}
