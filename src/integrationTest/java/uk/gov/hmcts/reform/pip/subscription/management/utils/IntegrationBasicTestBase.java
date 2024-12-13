package uk.gov.hmcts.reform.pip.subscription.management.utils;

import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionListTypeRepository;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

public class IntegrationBasicTestBase {
    @MockBean
    SubscriptionRepository subscriptionRepository;

    @MockBean
    SubscriptionListTypeRepository subscriptionListTypeRepository;
}
