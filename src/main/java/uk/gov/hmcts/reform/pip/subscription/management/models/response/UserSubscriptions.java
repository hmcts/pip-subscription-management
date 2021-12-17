package uk.gov.hmcts.reform.pip.subscription.management.models.response;

import lombok.Data;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Court;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Hearing;
import java.util.List;

/**
 * Response object for a users subscriptions that returns both Courts and Hearings that a user is subscribed to.
 */
@Data
public class UserSubscriptions {

    /**
     * Hearing object built from Data Management service containing case info on subscribed cases.
     */
    private List<Hearing> caseSubscriptions;

    /**
     * Court object built from Data Management service containing Court info on subscribed courts.
     */
    private List<Court> courtSubscriptions;
}
