package uk.gov.hmcts.reform.pip.subscription.management.models;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class BulkSubscriptionsSummary {

    private UUID artefactId;

    List<SubscriptionsSummary> subscriptionEmails = new ArrayList<>();

    public void addSubscriptionEmail(SubscriptionsSummary subscriptionsSummary) {
        subscriptionEmails.add(subscriptionsSummary);
    }
}
