package uk.gov.hmcts.reform.pip.subscription.management.models.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CourtSubscription {

    private String courtName;
    private LocalDateTime dateAdded;
}
