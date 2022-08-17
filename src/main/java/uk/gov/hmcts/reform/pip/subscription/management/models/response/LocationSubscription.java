package uk.gov.hmcts.reform.pip.subscription.management.models.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class LocationSubscription {

    private UUID subscriptionId;
    private String locationName;
    private String locationId;
    private List<String> listType;
    private LocalDateTime dateAdded;
}
