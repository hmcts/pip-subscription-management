package uk.gov.hmcts.reform.pip.subscription.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.UserSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service layer for dealing with subscriptions.
 */
@Slf4j
@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    @Autowired
    SubscriptionRepository repository;

    @Autowired
    DataManagementService dataManagementService;

    @Override
    public Subscription createSubscription(Subscription subscription) {
        return repository.save(subscription);
    }

    @Override
    public void deleteById(UUID id) {
        Optional<Subscription> subscription = repository.findById(id);

        if (subscription.isEmpty()) {
            throw new SubscriptionNotFoundException(String.format(
                "No subscription found with the subscription id %s",
                id
            ));
        }
        repository.deleteById(id);
    }

    @Override
    public List<Subscription> findAll() {
        return repository.findAll();
    }


    @Override
    public Subscription findById(UUID subscriptionId) {
        Optional<Subscription> subscription = repository.findById(subscriptionId);
        if (subscription.isEmpty()) {
            throw new SubscriptionNotFoundException(String.format(
                "No subscription found with the subscription id %s",
                subscriptionId
            ));
        }
        return subscription.get();
    }

    @Override
    public List<UserSubscription> findByUserId(String userId) {
        List<Subscription> subscriptions = repository.findByUserId(userId);
        if (subscriptions.isEmpty()) {
            return new ArrayList<>();
        }
        return collectSubscriptions(subscriptions);
    }

    List<UserSubscription> collectSubscriptions(List<Subscription> subscriptions) {
        List<UserSubscription> userSubscriptions = new ArrayList<>();
        subscriptions.forEach(subscription -> {
            UserSubscription userSubscription = new UserSubscription(subscription);
            switch (subscription.getSearchType()) {
                case CASE_ID:
                    userSubscription.getCaseSubscriptions()
                        .add(dataManagementService.getHearingByCaseId(subscription.getSearchValue()));
                    break;
                case CASE_URN:
                    userSubscription.getCaseSubscriptions()
                        .add(dataManagementService.getHearingByUrn(subscription.getSearchValue()));
                    break;
                case CASE_NAME:
                    userSubscription.getCaseSubscriptions()
                        .addAll(dataManagementService.getHearingByName(subscription.getSearchValue()));
                    break;
                case COURT_ID:
                    userSubscription.getCourtSubscriptions()
                        .add(dataManagementService.getCourt(subscription.getSearchValue()));
                    break;
                default:
                    log.error("Subscription with id: {} did not have valid SearchType.", subscription.getId());
                    break;
            }
            userSubscriptions.add(userSubscription);
        });
        return userSubscriptions;
    }
}
