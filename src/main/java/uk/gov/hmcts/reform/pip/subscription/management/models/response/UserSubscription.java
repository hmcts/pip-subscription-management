package uk.gov.hmcts.reform.pip.subscription.management.models.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Court;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Hearing;

import java.util.ArrayList;
import java.util.List;

/**
 * Response object for a users subscriptions that returns both Courts and Hearings that a user is subscribed to.
 */
@Getter
@Setter
@NoArgsConstructor
public class UserSubscription extends Subscription {

    /**
     * Hearing object built from Data Management service containing case info on subscribed cases.
     */
    private List<Hearing> caseSubscriptions = new ArrayList<>();

    /**
     * Court object built from Data Management service containing Court info on subscribed courts.
     */
    private List<Court> courtSubscriptions = new ArrayList<>();

    /**
     * Constructor that takes in a Subscription to convert it to a UserSubscription object.
     * @param subscription The subscription to convert.
     */
    public UserSubscription(Subscription subscription) {
        super(subscription.getId(),
              subscription.getUserId(),
              subscription.getSearchType(),
              subscription.getSearchValue(),
              subscription.getChannel());
    }

}
