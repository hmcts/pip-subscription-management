package uk.gov.hmcts.reform.pip.subscription.management.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.model.enums.UserActions;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;
import static uk.gov.hmcts.reform.pip.model.subscription.SearchType.LOCATION_ID;

/**
 * Service layer for dealing with subscriptions.
 */
@Slf4j
@Service
public class SubscriptionService {

    @Autowired
    SubscriptionRepository repository;

    @Autowired
    DataManagementService dataManagementService;

    @Autowired
    AccountManagementService accountManagementService;

    @Autowired
    PublicationServicesService publicationServicesService;

    public Subscription createSubscription(Subscription subscription, String actioningUserId) {
        log.info(writeLog(actioningUserId, UserActions.CREATE_SUBSCRIPTION,
                          subscription.getSearchType().toString()));

        duplicateSubscriptionHandler(subscription);

        subscription.setLastUpdatedDate(subscription.getCreatedDate());

        if (subscription.getSearchType().equals(LOCATION_ID)) {
            subscription.setLocationName(dataManagementService.getCourtName(subscription.getSearchValue()));
        }
        return repository.save(subscription);
    }

    public void configureListTypesForSubscription(String userId, List<String> listType) {
        log.info(writeLog(userId, UserActions.CREATE_SUBSCRIPTION, LOCATION_ID.name()));

        repository.updateLocationSubscriptions(userId,
            listType == null ? "" : StringUtils.join(listType, ','));
    }

    public void deleteById(UUID id, String actioningUserId) {
        Optional<Subscription> subscription = repository.findById(id);

        if (subscription.isEmpty()) {
            throw new SubscriptionNotFoundException(String.format(
                "No subscription found with the subscription id %s",
                id
            ));
        }

        log.info(writeLog(actioningUserId, UserActions.DELETE_SUBSCRIPTION,
                          id.toString()));

        repository.deleteById(id);
    }

    public void bulkDeleteSubscriptions(List<UUID> ids) {
        List<Subscription> subscriptions = repository.findByIdIn(ids);
        if (ids.size() > subscriptions.size()) {
            List<UUID> missingIds = new ArrayList<>(ids);
            missingIds.removeAll(subscriptions.stream()
                                     .map(Subscription::getId)
                                     .toList());
            throw new SubscriptionNotFoundException("No subscription found with the subscription ID(s): "
                    + missingIds.toString().replace("[", "").replace("]", ""));
        }

        repository.deleteByIdIn(ids);
        subscriptions.forEach(s -> log.info(writeLog(s.getUserId(), UserActions.DELETE_SUBSCRIPTION,
                                                     s.getId().toString())));
    }

    public List<Subscription> findAll() {
        return repository.findAll();
    }

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

    public String getAllSubscriptionsDataForMiReporting() {
        StringBuilder builder = new StringBuilder(60);
        builder.append("id,channel,search_type,user_id,court_name,created_date").append(System.lineSeparator());
        repository.getAllSubsDataForMi()
            .forEach(line -> builder.append(line).append(System.lineSeparator()));
        return builder.toString();
    }

    public String getLocalSubscriptionsDataForMiReporting() {
        StringBuilder builder = new StringBuilder(60);
        builder.append("id,search_value,channel,user_id,court_name,created_date").append(System.lineSeparator());
        repository.getLocalSubsDataForMi()
            .forEach(line -> builder.append(line).append(System.lineSeparator()));
        return builder.toString();
    }
}
