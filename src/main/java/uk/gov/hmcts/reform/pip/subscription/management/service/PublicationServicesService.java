package uk.gov.hmcts.reform.pip.subscription.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionsSummary;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionsSummaryDetails;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class PublicationServicesService {
    private static final String NOTIFY_SUBSCRIPTION_PATH = "/notify/subscription";

    @Autowired
    private WebClient webClient;

    @Value("${service-to-service.publication-services}")
    private String url;

    public String postSubscriptionSummaries(UUID artefactId, String email, List<Subscription> listOfSubscriptions) {
        SubscriptionsSummary payload = formatSubscriptionsSummary(artefactId, email, listOfSubscriptions);
        try {
            webClient.post().uri(url + NOTIFY_SUBSCRIPTION_PATH)
                .body(BodyInserters.fromValue(payload)).retrieve()
                .bodyToMono(Void.class).block();
            return payload.toString();

        } catch (WebClientException ex) {
            log.error(String.format("Request with body: %s failed. With error message: %s",
                                    payload, ex.getMessage()));
        }
        return "Request failed";
    }

    /**
     * Process data to form a subscriptions summary model which can be sent to publication services.
     *
     * @param artefactId The artefact id associated with the list of subscriptions
     * @param email The email of the user associated with the list of subscriptions
     * @param listOfSubscriptions The list of subscriptions to format
     * @return A subscriptions summary model
     */
    private SubscriptionsSummary formatSubscriptionsSummary(UUID artefactId, String email,
                                                            List<Subscription> listOfSubscriptions) {

        SubscriptionsSummaryDetails subscriptionsSummaryDetails = new SubscriptionsSummaryDetails();

        listOfSubscriptions.forEach(subscription -> {
            switch (subscription.getSearchType()) {
                case CASE_URN:
                    subscriptionsSummaryDetails.addToCaseUrn(subscription.getSearchValue());
                    break;
                case CASE_ID:
                    subscriptionsSummaryDetails.addToCaseNumber(subscription.getSearchValue());
                    break;
                case LOCATION_ID:
                    subscriptionsSummaryDetails.addToLocationId(subscription.getSearchValue());
                    break;
                default:
                    break;
            }
        });

        SubscriptionsSummary subscriptionsSummary = new SubscriptionsSummary();
        subscriptionsSummary.setArtefactId(artefactId);
        subscriptionsSummary.setEmail(email);
        subscriptionsSummary.setSubscriptions(subscriptionsSummaryDetails);

        return subscriptionsSummary;
    }
}
