package uk.gov.hmcts.reform.pip.subscription.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.hmcts.reform.pip.model.subscription.LocationSubscriptionDeletion;
import uk.gov.hmcts.reform.pip.model.subscription.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.model.subscription.ThirdPartySubscriptionArtefact;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.model.system.admin.DeleteLocationSubscriptionAction;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionsSummary;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionsSummaryDetails;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;
import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Slf4j
@Component
public class PublicationServicesService {
    private static final String NOTIFY_SUBSCRIPTION_PATH = "notify/subscription";
    private static final String NOTIFY_API_PATH = "notify/api";
    private static final String NOTIFY_LOCATION_SUBSCRIPTION_PATH = "notify/location-subscription-delete";
    private static final String PUBLICATION_SERVICE_API = "publicationServicesApi";

    @Autowired
    private WebClient webClient;

    @Value("${service-to-service.publication-services}")
    private String url;

    public void postSubscriptionSummaries(UUID artefactId, String email, List<Subscription> listOfSubscriptions) {
        SubscriptionsSummary payload = formatSubscriptionsSummary(artefactId, email, listOfSubscriptions);
        try {
            webClient.post().uri(url + "/" + NOTIFY_SUBSCRIPTION_PATH)
                .attributes(clientRegistrationId(PUBLICATION_SERVICE_API))
                .body(BodyInserters.fromValue(payload)).retrieve()
                .bodyToMono(Void.class)
                .block();

        } catch (WebClientException ex) {
            log.error(writeLog(
                String.format("Subscription email failed to send with error: %s", ex.getMessage())
            ));
        }
    }

    public void sendThirdPartyList(ThirdPartySubscription subscriptions) {
        try {
            webClient.post().uri(url + "/" + NOTIFY_API_PATH)
                .attributes(clientRegistrationId(PUBLICATION_SERVICE_API))
                .bodyValue(subscriptions).retrieve()
                .bodyToMono(Void.class)
                .block();
        } catch (WebClientResponseException ex) {
            log.error(writeLog(
                String.format("Publication to third party failed to send with error: %s",
                              ex.getResponseBodyAsString())
            ));
        }
    }

    public void sendEmptyArtefact(ThirdPartySubscriptionArtefact subscriptionArtefact) {
        try {
            webClient.put().uri(url + "/" + NOTIFY_API_PATH)
                .attributes(clientRegistrationId(PUBLICATION_SERVICE_API))
                .bodyValue(subscriptionArtefact).retrieve()
                .bodyToMono(Void.class)
                .block();
        } catch (WebClientResponseException ex) {
            log.error(writeLog(
                String.format("Deleted artefact notification to third party failed to send with error: %s",
                              ex.getResponseBodyAsString())
            ));
        }
    }

    public void sendLocationDeletionSubscriptionEmail(List<String> emails, String locationName) {
        LocationSubscriptionDeletion payload = formatLocationSubscriptionDeletion(emails, locationName);
        try {
            webClient.post().uri(url + "/" + NOTIFY_LOCATION_SUBSCRIPTION_PATH)
                .attributes(clientRegistrationId(PUBLICATION_SERVICE_API))
                .body(BodyInserters.fromValue(payload)).retrieve()
                .bodyToMono(Void.class)
                .block();

        } catch (WebClientException ex) {
            log.error(writeLog(
                String.format("Location deletion notification email failed to send with error: %s",
                              ex.getMessage())
            ));
        }
    }

    public void sendSystemAdminEmail(List<String> emails, String requesterName, ActionResult actionResult,
                                       String additionalDetails) {
        DeleteLocationSubscriptionAction payload =
            formatSystemAdminAction(emails, requesterName, actionResult, additionalDetails);
        try {
            webClient.post().uri(url + "/notify/sysadmin/update")
                .body(BodyInserters.fromValue(payload))
                .attributes(clientRegistrationId(PUBLICATION_SERVICE_API))
                .retrieve().bodyToMono(String.class)
                .block();

        } catch (WebClientException ex) {
            log.error(writeLog(
                String.format("System admin notification email failed to send with error: %s",
                              ex.getMessage())
            ));
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
                default -> log.error(writeLog(
                    String.format("Search type was not one of allowed options: %s", subscription.getSearchType())
                ));
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
