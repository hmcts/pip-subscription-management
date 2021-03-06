package uk.gov.hmcts.reform.pip.subscription.management.models.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Response object for a users subscriptions that returns both Courts and Hearings that a user is subscribed to.
 */
@Data
public class UserSubscription {

    /**
     * Hearing object built from Data Management service containing case info on subscribed cases.
     */
    private List<CaseSubscription> caseSubscriptions = new ArrayList<>();

    /**
     * Court object built from Data Management service containing Court info on subscribed courts.
     */
    private List<LocationSubscription> locationSubscriptions = new ArrayList<>();

}
