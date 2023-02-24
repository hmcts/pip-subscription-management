package uk.gov.hmcts.reform.pip.subscription.management.models.external.publication.services;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LocationSubscriptionDeletion {

    String locationName;

    List<String> subscriberEmails = new ArrayList<>();
}
