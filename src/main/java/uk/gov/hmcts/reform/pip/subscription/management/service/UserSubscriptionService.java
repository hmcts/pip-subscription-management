package uk.gov.hmcts.reform.pip.subscription.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionListType;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.CaseSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.ListTypeSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.LocationSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.UserSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionListTypeRepository;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Service
@Slf4j
public class UserSubscriptionService {
    private final SubscriptionRepository repository;

    private final SubscriptionListTypeRepository subscriptionListTypeRepository;

    @Autowired
    public UserSubscriptionService(SubscriptionRepository repository,
                                   SubscriptionListTypeRepository subscriptionListTypeRepository) {
        this.repository = repository;
        this.subscriptionListTypeRepository = subscriptionListTypeRepository;
    }

    /**
     * Find all subscriptions for a given user.
     * @param userId The user id to find the subscriptions for.
     * @return The list of subscriptions that have been found.
     */
    public UserSubscription findByUserId(String userId) {
        List<Subscription> subscriptions = repository.findByUserId(userId);
        if (subscriptions.isEmpty()) {
            return new UserSubscription();
        }
        return collectSubscriptions(subscriptions);
    }

    /**
     * Delete all subscriptions by the user id.
     * @param userId The user id to delete the subscriptions from.
     * @return A confirmation message.
     */
    public String deleteAllByUserId(String userId) {
        subscriptionListTypeRepository.deleteByUserId(userId);
        repository.deleteAllByUserId(userId);
        String message = String.format("All subscriptions deleted for user id %s", userId);
        log.info(writeLog(message));
        return message;
    }

    // New version of PMD check that the default case should not be supplied for enum switch if all enum cases are
    // supplied. However this will fail checkstyle if the default case is removed
    @SuppressWarnings("PMD.ExhaustiveSwitchHasDefault")
    private UserSubscription collectSubscriptions(List<Subscription> subscriptions) {
        UserSubscription userSubscription = new UserSubscription();
        subscriptions.forEach(subscription -> {
            switch (subscription.getSearchType()) {
                case LOCATION_ID -> {
                    LocationSubscription locationSubscription = new LocationSubscription();
                    locationSubscription.setSubscriptionId(subscription.getId());
                    locationSubscription.setLocationName(subscription.getLocationName());
                    locationSubscription.setLocationId(subscription.getSearchValue());
                    Optional<SubscriptionListType> subscriptionListType = subscriptionListTypeRepository
                        .findByUserId(subscription.getUserId());
                    if (subscriptionListType.isPresent()) {
                        locationSubscription.setListType(subscriptionListType.get().getListType());
                        locationSubscription.setListLanguage(subscriptionListType.get().getListLanguage());
                    }
                    locationSubscription.setDateAdded(subscription.getCreatedDate());
                    userSubscription.getLocationSubscriptions().add(locationSubscription);
                }
                case LIST_TYPE -> {
                    ListTypeSubscription listTypeSubscription = new ListTypeSubscription();
                    listTypeSubscription.setSubscriptionId(subscription.getId());
                    listTypeSubscription.setListType(subscription.getSearchValue());
                    listTypeSubscription.setDateAdded(subscription.getCreatedDate());
                    listTypeSubscription.setChannel(subscription.getChannel());
                    userSubscription.getListTypeSubscriptions().add(listTypeSubscription);
                }
                case CASE_ID, CASE_URN -> {
                    CaseSubscription caseSubscription = new CaseSubscription();
                    caseSubscription.setCaseName(subscription.getCaseName());
                    caseSubscription.setSubscriptionId(subscription.getId());
                    caseSubscription.setCaseNumber(subscription.getCaseNumber());
                    caseSubscription.setUrn(subscription.getUrn());
                    caseSubscription.setPartyNames(subscription.getPartyNames());
                    caseSubscription.setSearchType(subscription.getSearchType());
                    caseSubscription.setDateAdded(subscription.getCreatedDate());
                    userSubscription.getCaseSubscriptions().add(caseSubscription);
                }
                default -> { // No default case
                }
            }
        });
        return userSubscription;
    }
}
