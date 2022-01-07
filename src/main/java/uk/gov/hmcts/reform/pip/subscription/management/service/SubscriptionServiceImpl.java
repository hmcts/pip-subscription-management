package uk.gov.hmcts.reform.pip.subscription.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.subscription.management.models.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.CaseSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.CourtSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.UserSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

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
    public UserSubscription findByUserId(String userId) {
        List<Subscription> subscriptions = repository.findByUserId(userId);
        if (subscriptions.isEmpty()) {
            return new UserSubscription();
        }
        return collectSubscriptions(subscriptions);
    }

    UserSubscription collectSubscriptions(List<Subscription> subscriptions) {
        UserSubscription userSubscription = new UserSubscription();
        subscriptions.forEach(subscription -> {
            if (subscription.getSearchType() == SearchType.COURT_ID) {
                CourtSubscription courtSubscription = new CourtSubscription();
                courtSubscription.setSubscriptionId(subscription.getId());
                courtSubscription.setCourtName(subscription.getCourtName());
                courtSubscription.setDateAdded(subscription.getCreatedDate());
                userSubscription.getCourtSubscriptions().add(courtSubscription);
            } else {
                CaseSubscription caseSubscription = new CaseSubscription();
                caseSubscription.setCaseName(subscription.getCaseName());
                caseSubscription.setSubscriptionId(subscription.getId());
                caseSubscription.setCaseNumber(subscription.getCaseNumber());
                caseSubscription.setUrn(subscription.getUrn());
                caseSubscription.setDateAdded(subscription.getCreatedDate());
                userSubscription.getCaseSubscriptions().add(caseSubscription);
            }
        });
        return userSubscription;
    }
}
