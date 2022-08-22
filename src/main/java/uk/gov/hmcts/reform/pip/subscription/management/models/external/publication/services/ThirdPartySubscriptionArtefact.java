package uk.gov.hmcts.reform.pip.subscription.management.models.external.publication.services;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Artefact;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ThirdPartySubscriptionArtefact {
    String apiDestination;
    Artefact artefact;
}
