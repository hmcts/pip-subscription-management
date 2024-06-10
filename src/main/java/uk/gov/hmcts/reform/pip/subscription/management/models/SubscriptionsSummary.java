package uk.gov.hmcts.reform.pip.subscription.management.models;

import lombok.Data;

@Data
public class SubscriptionsSummary {
    private String email;
    private SubscriptionsSummaryDetails subscriptions;
}
