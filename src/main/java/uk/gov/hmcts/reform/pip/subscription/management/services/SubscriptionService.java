package uk.gov.hmcts.reform.pip.subscription.management.services;


import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;

import java.util.List;

public interface SubscriptionService {

    /**
     * Method to create a new subscription.
     * @param subscription The subscription to create.
     * @return The created subscription.
     */
    Subscription createSubscription(Subscription subscription);

    /**
     * Deletes all subscriptions.
     */
    void deleteAll();

    /**
     * Delete subscription by id.
     * @param id The id of the subscription to delete.
     */
    void deleteById(Long id);

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
    Subscription findById(Long subscriptionId);

}
