package uk.gov.hmcts.reform.pip.subscription.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.model.system.admin.DeleteLocationSubscriptionAction;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionsSummary;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionsSummaryDetails;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.publication.services.LocationSubscriptionDeletion;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.publication.services.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.publication.services.ThirdPartySubscriptionArtefact;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Component
public class PublicationServicesService {
    private static final String NOTIFY_SUBSCRIPTION_PATH = "notify/subscription";
    private static final String NOTIFY_API_PATH = "notify/api";
    private static final String NOTIFY_LOCATION_SUBSCRIPTION_PATH = "notify/location-subscription-delete";
    private static final String PUBLICATION_SERVICE_API = "publicationServicesApi";
    private static final String REQUEST_FAILED = "Request failed";
    private static final String REQUEST_FAILED_ERROR = "Request failed with error message: %s";

    @Autowired
    private WebClient webClient;

    @Value("${service-to-service.publication-services}")
    private String url;

    public String postSubscriptionSummaries(UUID artefactId, String email, List<Subscription> listOfSubscriptions) {
        SubscriptionsSummary payload = formatSubscriptionsSummary(artefactId, email, listOfSubscriptions);
        try {
            webClient.post().uri(url + "/" + NOTIFY_SUBSCRIPTION_PATH)
                .attributes(clientRegistrationId(PUBLICATION_SERVICE_API))
                .body(BodyInserters.fromValue(payload)).retrieve()
                .bodyToMono(Void.class).block();
            return payload.toString();

        } catch (WebClientException ex) {
            log.error(String.format(REQUEST_FAILED_ERROR, ex.getMessage()));
        }
        return REQUEST_FAILED;
    }

    public String sendThirdPartyList(ThirdPartySubscription subscriptions) {
        try {
            webClient.post().uri(url + "/" + NOTIFY_API_PATH)
                .attributes(clientRegistrationId(PUBLICATION_SERVICE_API))
                .bodyValue(subscriptions).retrieve()
                .bodyToMono(Void.class).block();
            return "Successfully sent";
        } catch (WebClientResponseException ex) {
            log.error("Request to Publication Services {} failed due to: {}", "/" + NOTIFY_API_PATH,
                      ex.getResponseBodyAsString()
            );
            return REQUEST_FAILED;
        }
    }

    public String sendEmptyArtefact(ThirdPartySubscriptionArtefact subscriptionArtefact) {
        try {
            webClient.put().uri(url + "/" + NOTIFY_API_PATH)
                .attributes(clientRegistrationId(PUBLICATION_SERVICE_API))
                .bodyValue(subscriptionArtefact).retrieve()
                .bodyToMono(Void.class).block();
            return "Successfully sent";
        } catch (WebClientResponseException ex) {
            log.error("Request to Publication Services {} failed due to: {}", "/" + NOTIFY_API_PATH,
                      ex.getResponseBodyAsString()
            );
            return REQUEST_FAILED;
        }
    }

    public String sendLocationDeletionSubscriptionEmail(List<String> emails, String locationName) {
        LocationSubscriptionDeletion payload = formatLocationSubscriptionDeletion(emails, locationName);
        try {
            webClient.post().uri(url + "/" + NOTIFY_LOCATION_SUBSCRIPTION_PATH)
                .attributes(clientRegistrationId(PUBLICATION_SERVICE_API))
                .body(BodyInserters.fromValue(payload)).retrieve()
                .bodyToMono(Void.class).block();
            return payload.toString();

        } catch (WebClientException ex) {
            log.error(String.format(REQUEST_FAILED_ERROR, ex.getMessage()));
        }
        return REQUEST_FAILED;
    }

    public String sendSystemAdminEmail(List<String> emails, String requesterName, ActionResult actionResult,
                                       String additionalDetails) {
        DeleteLocationSubscriptionAction payload =
            formatSystemAdminAction(emails, requesterName, actionResult, additionalDetails);
        try {
            return webClient.post().uri(url + "/notify/sysadmin/update")
                .body(BodyInserters.fromValue(payload))
                .attributes(clientRegistrationId(PUBLICATION_SERVICE_API))
                .retrieve().bodyToMono(String.class).block();

        } catch (WebClientException ex) {
            log.error(String.format(REQUEST_FAILED_ERROR, ex.getMessage()));
            return "";
        }
    }

    private DeleteLocationSubscriptionAction formatSystemAdminAction(List<String> emails,
        String requesterName, ActionResult actionResult, String additionalDetails) {
        DeleteLocationSubscriptionAction systemAdminAction = new DeleteLocationSubscriptionAction();
        systemAdminAction.setEmailList(emails);
        systemAdminAction.setRequesterName(requesterName);
        systemAdminAction.setActionResult(actionResult);
        systemAdminAction.setDetailString(additionalDetails);
        return systemAdminAction;
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
                case CASE_URN -> subscriptionsSummaryDetails.addToCaseUrn(subscription.getSearchValue());
                case CASE_ID -> subscriptionsSummaryDetails.addToCaseNumber(subscription.getSearchValue());
                case LOCATION_ID -> subscriptionsSummaryDetails.addToLocationId(subscription.getSearchValue());
                default -> log.error(String.format("Search type was not one of allowed options: %s",
                                                   subscription.getSearchType()));
            }
        });

        SubscriptionsSummary subscriptionsSummary = new SubscriptionsSummary();
        subscriptionsSummary.setArtefactId(artefactId);
        subscriptionsSummary.setEmail(email);
        subscriptionsSummary.setSubscriptions(subscriptionsSummaryDetails);

        return subscriptionsSummary;
    }

    private LocationSubscriptionDeletion formatLocationSubscriptionDeletion(
        List<String> emails, String locationName) {
        LocationSubscriptionDeletion locationSubscriptionDeletion = new LocationSubscriptionDeletion();
        locationSubscriptionDeletion.setLocationName(locationName);
        locationSubscriptionDeletion.setSubscriberEmails(emails);
        return locationSubscriptionDeletion;
    }
}
