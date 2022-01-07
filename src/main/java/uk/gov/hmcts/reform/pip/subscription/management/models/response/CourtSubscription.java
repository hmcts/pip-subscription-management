package uk.gov.hmcts.reform.pip.subscription.management.models.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CourtSubscription {

    private UUID subscriptionId;
    private String courtName;
    private LocalDateTime dateAdded;
}
