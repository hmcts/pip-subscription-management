package uk.gov.hmcts.reform.pip.subscription.management.models;

import lombok.Data;

import java.util.UUID;

@Data
public class UserSubscriptionDto {
    private String email;
    private UUID artefactId;
    private SubscriptionsSummaryDto subscriptions;
}
