package uk.gov.hmcts.reform.pip.subscription.management.models.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CaseSubscription {

    private UUID subscriptionId;
    private String caseName;
    private String caseNumber;
    private String urn;
    private LocalDateTime dateAdded;
}
