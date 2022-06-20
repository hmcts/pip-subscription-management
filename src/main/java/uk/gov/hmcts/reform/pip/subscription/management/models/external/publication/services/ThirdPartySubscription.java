package uk.gov.hmcts.reform.pip.subscription.management.models.external.publication.services;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ThirdPartySubscription {

    String apiDestination;
    UUID artefactId;
}
