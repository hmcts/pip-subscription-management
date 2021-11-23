package uk.gov.hmcts.reform.pip.subscription.management.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.subscription.management.helpers.SubscriptionHelper.createMockSubscription;
import static uk.gov.hmcts.reform.pip.subscription.management.helpers.SubscriptionHelper.createMockSubscriptionList;
import static uk.gov.hmcts.reform.pip.subscription.management.helpers.SubscriptionHelper.findableSubscription;

@ExtendWith({MockitoExtension.class})
class SubscriptionServiceTest {
    private static final String USER_ID = "Ralph21";
    private static final String SEARCH_VALUE = "193254";

    private List<Subscription> mockSubscriptionList;
    private Subscription mockSubscription;
    private Subscription findableSubscription;


    @Mock
    SubscriptionRepository subscriptionRepository;

    @InjectMocks
    SubscriptionServiceImpl subscriptionService;

    @BeforeEach
    void setup() {
        mockSubscription = createMockSubscription(USER_ID, SEARCH_VALUE);
        mockSubscriptionList = createMockSubscriptionList();
        findableSubscription = findableSubscription();
    }

    @Test
    void testGetSubscriptionReturnsExpected() {
        when(subscriptionRepository.findAll()).thenReturn(mockSubscriptionList);
        assertEquals(mockSubscriptionList, subscriptionService.findAll(), "The returned subscription list "
            + "does not match the expected list");
    }

    @Test
    void testCreateSubscription() {
        when(subscriptionRepository.save(mockSubscription)).thenReturn(mockSubscription);
        assertEquals(subscriptionService.createSubscription(mockSubscription), mockSubscription,
                     "The returned subscription does not match the expected subscription"
        );
    }

    @Test
    void testDeleteSubscription() {
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        doNothing().when(subscriptionRepository).deleteById(captor.capture());
        when(subscriptionRepository.findById(3L)).thenReturn(Optional.of(findableSubscription));
        subscriptionService.deleteById(3L);
        assertEquals(3L, captor.getValue(), "The service layer tried to delete the wrong subscription");
    }

    @Test
    void testDeleteException() {
        when(subscriptionRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(SubscriptionNotFoundException.class, () -> subscriptionService.deleteById(2L),
                     "SubscriptionNotFoundException not thrown when trying to delete a subscription"
                         + " that does not exist");
    }

    @Test
    void testFindException() {
        when(subscriptionRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(SubscriptionNotFoundException.class, () -> subscriptionService.findById(10L),
                     "SubscriptionNotFoundException not thrown "
                         + "when trying to find a subscription that does not exist");
    }

    @Test
    void testFindSubscription() {
        when(subscriptionRepository.findById(any())).thenReturn(Optional.of(findableSubscription));
        assertEquals(subscriptionService.findById(9L), findableSubscription,
                     "The returned subscription does not match the expected subscription");
    }

}

