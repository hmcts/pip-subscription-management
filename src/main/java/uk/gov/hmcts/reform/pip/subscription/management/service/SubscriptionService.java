package uk.gov.hmcts.reform.pip.subscription.management.service;

import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Artefact;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.UserSubscription;

import java.util.List;
import java.util.UUID;

public interface SubscriptionService {

    /**
     * Method to create a new subscription.
     * @param subscription The subscription to create.
     * @return The created subscription.
     */
    Subscription createSubscription(Subscription subscription);

    /**
     * Delete subscription by id.
     * @param id The id of the subscription to delete.
     */
    void deleteById(UUID id);

    /**
     * Find all subscriptions.
     * @return The list of subscriptions that have been found.
     */
    List<Subscription> findAll();

    /**
     * Find subscription by id.
     * @param subscriptionId The id of the subscription to be found.
     * @return The subscription that has been found.
     */
    Subscription findById(UUID subscriptionId);

    /**
     * Find all subscriptions for a given user.
     * @return The list of subscriptions that have been found.
     */
    UserSubscription findByUserId(String userId);

    void collectSubscribers(Artefact artefact);

    void collectThirdPartyForDeletion(Artefact artefact);
}
