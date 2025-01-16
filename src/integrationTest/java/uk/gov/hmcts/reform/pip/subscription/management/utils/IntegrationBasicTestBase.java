package uk.gov.hmcts.reform.pip.subscription.management.utils;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionListTypeRepository;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

public class IntegrationBasicTestBase {
    @MockitoBean
    SubscriptionRepository subscriptionRepository;

    @MockitoBean
    SubscriptionListTypeRepository subscriptionListTypeRepository;
}
