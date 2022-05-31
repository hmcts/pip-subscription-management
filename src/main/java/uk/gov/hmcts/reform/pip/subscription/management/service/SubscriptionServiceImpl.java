package uk.gov.hmcts.reform.pip.subscription.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.subscription.management.models.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Artefact;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.ListType;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Sensitivity;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.CaseSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.LocationSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.UserSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service layer for dealing with subscriptions.
 */
@Slf4j
@Service
@SuppressWarnings({"PMD.LawOfDemeter", "PMD.AvoidCatchingNPE", "PMD.TooManyMethods"})
public class SubscriptionServiceImpl implements SubscriptionService {

    @Autowired
    SubscriptionRepository repository;

    @Autowired
    DataManagementService dataManagementService;

    @Autowired
    AccountManagementService accountManagementService;

    @Override
    public Subscription createSubscription(Subscription subscription) {
        duplicateSubscriptionHandler(subscription);

        if (subscription.getSearchType().equals(SearchType.LOCATION_ID)) {
            subscription.setLocationName(dataManagementService.getCourtName(subscription.getSearchValue()));
        }
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
            if (subscription.getSearchType() == SearchType.LOCATION_ID) {
                LocationSubscription locationSubscription = new LocationSubscription();
                locationSubscription.setSubscriptionId(subscription.getId());
                locationSubscription.setLocationName(subscription.getLocationName());
                locationSubscription.setDateAdded(subscription.getCreatedDate());
                userSubscription.getLocationSubscriptions().add(locationSubscription);
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

    @Async
    @Override
    public void collectSubscribers(Artefact artefact) {
        List<Subscription> subscriptionList = new ArrayList<>(querySubscriptionValue(
            SearchType.LOCATION_ID.name(), artefact.getLocationId()));
        artefact.getSearch().get("cases").forEach(object -> subscriptionList.addAll(extractSearchValue(object)));

        List<Subscription> subscriptionsToEmail;
        if (artefact.getSensitivity().equals(Sensitivity.CLASSIFIED)) {
            subscriptionsToEmail = validateSubscriptionPermissions(subscriptionList, artefact.getListType());
        } else {
            subscriptionsToEmail = subscriptionList;
        }

        log.info("Subscriber list created. Notifying {} subscribers.", subscriptionsToEmail.size());
    }

    private List<Subscription> validateSubscriptionPermissions(List<Subscription> subscriptions, ListType listType) {
        List<Subscription> filteredList = new ArrayList<>();
        subscriptions.forEach(subscription -> {
            if (accountManagementService.isUserAuthenticated(subscription.getUserId(), listType)) {
                filteredList.add(subscription);
            }
        });
        return filteredList;
    }

    private List<Subscription> querySubscriptionValue(String term, String value) {
        return repository.findSubscriptionsBySearchValue(term, value);
    }

    private List<Subscription> extractSearchValue(Object caseObject) {
        List<Subscription> subscriptionList = new ArrayList<>();
        try {
            subscriptionList.addAll(querySubscriptionValue(SearchType.CASE_ID.name(),
                                                           ((LinkedHashMap) caseObject).get("caseNumber").toString()));
            subscriptionList.addAll(querySubscriptionValue(SearchType.CASE_URN.name(),
                                                           ((LinkedHashMap) caseObject).get("caseUrn").toString()));
        } catch (NullPointerException ex) {
            log.warn("No value found in {} for case number or urn. Method threw: {}", caseObject, ex.getMessage());
        }
        return subscriptionList;
    }

    /**
     * Take in a new user subscription and check if any with the same criteria already exist.
     * If it does then delete the original subscription as the new one will supersede it.
     *
     * @param subscription The new subscription that will be created
     */
    private void duplicateSubscriptionHandler(Subscription subscription) {
        repository.findByUserId(subscription.getUserId()).forEach(existingSub -> {
            if (existingSub.getSearchType().equals(subscription.getSearchType())
                && existingSub.getSearchValue().equals(subscription.getSearchValue())) {
                repository.delete(existingSub);
            }
        });
    }
}
