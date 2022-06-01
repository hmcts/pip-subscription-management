package uk.gov.hmcts.reform.pip.subscription.management.models;

import lombok.Data;

import java.util.UUID;

@Data
public class SubscriptionsSummary {
    private String email;
    private UUID artefactId;
    private SubscriptionsSummaryDetails subscriptions;
}
