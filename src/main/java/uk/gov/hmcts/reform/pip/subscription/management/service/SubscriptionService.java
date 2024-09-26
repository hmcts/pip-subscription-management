package uk.gov.hmcts.reform.pip.subscription.management.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.model.enums.UserActions;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionListType;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionListTypeRepository;
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
@SuppressWarnings("PMD.TooManyMethods")
public class SubscriptionService {

    private final SubscriptionRepository repository;

    private final SubscriptionListTypeRepository subscriptionListTypeRepository;
    private final DataManagementService dataManagementService;

    @Autowired
    public SubscriptionService(SubscriptionRepository repository, DataManagementService dataManagementService,
                               SubscriptionListTypeRepository subscriptionListTypeRepository) {
        this.repository = repository;
        this.dataManagementService = dataManagementService;
        this.subscriptionListTypeRepository = subscriptionListTypeRepository;
    }

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

    public void addListTypesForSubscription(SubscriptionListType subscriptionListType,
                                                  String actioningUserId) {
        log.info(writeLog(actioningUserId, UserActions.CREATE_SUBSCRIPTION, LOCATION_ID.name()));
        //DELETE EXISTING LIST TYPE CONFIG FOR A USER
        duplicateListTypeHandler(subscriptionListType);
        subscriptionListTypeRepository.save(subscriptionListType);
    }

    public void configureListTypesForSubscription(SubscriptionListType subscriptionListType,
                                                  String actioningUserId) {
        log.info(writeLog(actioningUserId, UserActions.CREATE_SUBSCRIPTION, LOCATION_ID.name()));

        repository.updateLocationSubscriptions(subscriptionListType.getUserId(),
            subscriptionListType.getListType() == null ? "" :
                StringUtils.join(subscriptionListType.getListType(), ','));
    }

    public void deleteById(UUID id, String actioningUserId) {
        Optional<Subscription> subscription = repository.findById(id);

        if (subscription.isEmpty()) {
            throw new SubscriptionNotFoundException(String.format(
                "No subscription found with the subscription id %s",
                id
            ));
        }

        if (subscription.get().getSearchType().equals(LOCATION_ID)) {
            Optional<SubscriptionListType> subscriptionListTypes =
                subscriptionListTypeRepository.findSubscriptionListTypeByLocationIdAndUserId(
                    Integer.parseInt(subscription.get().getSearchValue()), subscription.get().getUserId());

            if (subscriptionListTypes.isPresent()) {
                subscriptionListTypeRepository.delete(subscriptionListTypes.get());
            }
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

        List<SubscriptionListType> subscriptionListTypes =
            subscriptionListTypeRepository.findByUserId(subscriptions.get(0).getUserId());
        if (subscriptionListTypes != null) {
            List<UUID> subListTypeIds = subscriptionListTypes.stream()
                .map(SubscriptionListType::getId).toList();

            subscriptionListTypeRepository.deleteByIdIn(subListTypeIds);
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

    private void duplicateListTypeHandler(SubscriptionListType subscriptionListType) {
        subscriptionListTypeRepository.findByUserId(subscriptionListType.getUserId())
            .forEach(existingSub -> {
                if (existingSub.getLocationId().equals(subscriptionListType.getLocationId())) {
                    subscriptionListTypeRepository.delete(existingSub);
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
