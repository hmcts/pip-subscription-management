package uk.gov.hmcts.reform.pip.subscription.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.model.enums.UserActions;
import uk.gov.hmcts.reform.pip.model.report.AllSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.report.LocationSubscriptionMiData;
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

    private final SubscriptionLocationService subscriptionLocationService;

    @Autowired
    public SubscriptionService(SubscriptionRepository repository, DataManagementService dataManagementService,
                               SubscriptionListTypeRepository subscriptionListTypeRepository,
                               SubscriptionLocationService subscriptionLocationService) {
        this.repository = repository;
        this.dataManagementService = dataManagementService;
        this.subscriptionListTypeRepository = subscriptionListTypeRepository;
        this.subscriptionLocationService = subscriptionLocationService;
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
        subscriptionListTypeRepository.deleteByUserId(subscriptionListType.getUserId());
        subscriptionListTypeRepository.save(subscriptionListType);
    }

    public void configureListTypesForSubscription(SubscriptionListType subscriptionListType,
                                                  String actioningUserId) {
        log.info(writeLog(actioningUserId, UserActions.CREATE_SUBSCRIPTION, LOCATION_ID.name()));

        Optional<SubscriptionListType> existingSubscriptionListType = subscriptionListTypeRepository
            .findByUserId(subscriptionListType.getUserId());
        existingSubscriptionListType.ifPresent(listType -> subscriptionListType.setId(listType.getId()));
        subscriptionListTypeRepository.save(subscriptionListType);
    }

    public void deleteById(UUID id, String actioningUserId) {
        Optional<Subscription> subscription = repository.findById(id);
        if (subscription.isEmpty()) {
            throw new SubscriptionNotFoundException(String.format(
                "No subscription found with the subscription id %s",
                id
            ));
        }

        repository.deleteById(id);

        if (subscription.get().getSearchType().equals(LOCATION_ID)) {
            subscriptionLocationService
                .deleteSubscriptionListTypeByUser(subscription.get().getUserId());
        }

        log.info(writeLog(actioningUserId, UserActions.DELETE_SUBSCRIPTION,
                          id.toString()));
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

        //FIND ALL THE LOCATION SUBSCRIPTION FOR THE USER AND CHECK IF MORE THAN ONE LOCATION SUBSCRIPTION EXISTS
        //DO NOT DELETE RECORD FROM SUBSCRIPTION LIST TYPE BECAUSE ONE RECORD IS LINKED WITH ALL THE LOCATION
        //SUBSCRIPTIONS
        List<Subscription> bulkDeleteLocationSubscriptions = subscriptions.stream()
            .filter(s -> s.getSearchType()
            .equals(LOCATION_ID)).toList();

        List<Subscription> userLocationSubscriptions = repository
            .findLocationSubscriptionsByUserId(subscriptions.get(0).getUserId());

        if (!userLocationSubscriptions.isEmpty()
            && bulkDeleteLocationSubscriptions.size()
            == repository.findLocationSubscriptionsByUserId(subscriptions.get(0).getUserId()).size()) {
            Optional<SubscriptionListType> subscriptionListTypes =
                subscriptionListTypeRepository.findByUserId(subscriptions.get(0).getUserId());
            subscriptionListTypes.ifPresent(subscriptionListType -> subscriptionListTypeRepository
                .deleteByUserId(subscriptionListType.getUserId()));
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

    /**
     * Previous version of the MI Reporting service method. No longer used and soon to be removed.
     * @deprecated This method will be removed in the future in favour of the V2 equivalent.
     */
    @Deprecated(since = "2")
    public String getAllSubscriptionsDataForMiReporting() {
        StringBuilder builder = new StringBuilder(60);
        builder.append("id,channel,search_type,user_id,court_name,created_date").append(System.lineSeparator());
        repository.getAllSubsDataForMi()
            .forEach(line -> builder.append(line).append(System.lineSeparator()));
        return builder.toString();
    }

    public List<AllSubscriptionMiData> getAllSubscriptionsDataForMiReportingV2() {
        return repository.getAllSubsDataForMiV2();
    }

    /**
     * Previous version of the MI Reporting service method. No longer used and soon to be removed.
     * @deprecated This method will be removed in the future in favour of the V2 equivalent.
     */
    @Deprecated(since = "2")
    public String getLocalSubscriptionsDataForMiReporting() {
        StringBuilder builder = new StringBuilder(60);
        builder.append("id,search_value,channel,user_id,court_name,created_date").append(System.lineSeparator());
        repository.getLocalSubsDataForMi()
            .forEach(line -> builder.append(line).append(System.lineSeparator()));
        return builder.toString();
    }

    public List<LocationSubscriptionMiData> getLocationSubscriptionsDataForMiReportingV2() {
        return repository.getLocationSubsDataForMiV2();
    }
}
