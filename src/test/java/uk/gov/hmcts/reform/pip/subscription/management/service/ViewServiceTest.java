package uk.gov.hmcts.reform.pip.subscription.management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ViewServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private ViewService viewService;

    @Test
    void refreshViewTest() {
        viewService.refreshView();
        verify(subscriptionRepository, times(1)).refreshSubscriptionView();
    }

}
