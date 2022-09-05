package uk.gov.hmcts.reform.pip.subscription.management.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.model.enums.UserActions;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.subscription.management.models.Channel;
import uk.gov.hmcts.reform.pip.subscription.management.models.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Artefact;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Sensitivity;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.publication.services.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.publication.services.ThirdPartySubscriptionArtefact;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.CaseSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.LocationSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.UserSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

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
    ChannelManagementService channelManagementService;

    @Autowired
    AccountManagementService accountManagementService;

    @Autowired
    PublicationServicesService publicationServicesService;

    @Override
    public Subscription createSubscription(Subscription subscription) {
        log.info(writeLog(subscription.getUserId(), UserActions.CREATE_SUBSCRIPTION,
                          subscription.getSearchType().toString()));

        duplicateSubscriptionHandler(subscription);

        if (subscription.getSearchType().equals(SearchType.LOCATION_ID)) {
            subscription.setLocationName(dataManagementService.getCourtName(subscription.getSearchValue()));
        }
        return repository.save(subscription);
    }

    @Override
    public void configureListTypesForSubscription(String userId, List<String> listType) {
        log.info(writeLog(userId, UserActions.CREATE_SUBSCRIPTION,
                          SearchType.LOCATION_ID.name()));

        repository.updateLocationSubscriptions(userId,
            listType == null ? "" : StringUtils.join(listType, ','));
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

        Subscription returnedSubscription = subscription.get();
        log.info(writeLog(returnedSubscription.getUserId(), UserActions.DELETE_SUBSCRIPTION,
                          id.toString()));

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
                locationSubscription.setLocationId(subscription.getSearchValue());
                locationSubscription.setListType(subscription.getListType());
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

        List<Subscription> subscriptionList = new ArrayList<>(querySubscriptionValueForLocation(
            SearchType.LOCATION_ID.name(), artefact.getLocationId(), artefact.getListType().toString()));


        subscriptionList.addAll(querySubscriptionValue(SearchType.LIST_TYPE.name(), artefact.getListType().name()));

        if (artefact.getSearch().get("cases") != null) {
            artefact.getSearch().get("cases").forEach(object -> subscriptionList.addAll(extractSearchValue(object)));
        }

        List<Subscription> subscriptionsToContact;
        if (artefact.getSensitivity().equals(Sensitivity.CLASSIFIED)) {
            subscriptionsToContact = validateSubscriptionPermissions(subscriptionList, artefact);
        } else {
            subscriptionsToContact = subscriptionList;
        }

        handleSubscriptionSending(artefact.getArtefactId(), subscriptionsToContact);
    }

    private List<Subscription> validateSubscriptionPermissions(List<Subscription> subscriptions, Artefact artefact) {
        List<Subscription> filteredList = new ArrayList<>();
        subscriptions.forEach(subscription -> {
            if (accountManagementService.isUserAuthorised(subscription.getUserId(),
                                                          artefact.getListType(), artefact.getSensitivity())) {
                filteredList.add(subscription);
            }
        });
        return filteredList;
    }

    private List<Subscription> querySubscriptionValue(String term, String value) {
        return repository.findSubscriptionsBySearchValue(term, value);
    }

    private List<Subscription> querySubscriptionValueForLocation(String term, String value, String listType) {
        return repository.findSubscriptionsByLocationSearchValue(term, value, listType);
    }

    private List<Subscription> extractSearchValue(Object caseObject) {
        List<Subscription> subscriptionList = new ArrayList<>();
        try {
            subscriptionList.addAll(querySubscriptionValue(
                SearchType.CASE_ID.name(),
                ((LinkedHashMap) caseObject).get("caseNumber").toString()
            ));
            subscriptionList.addAll(querySubscriptionValue(
                SearchType.CASE_URN.name(),
                ((LinkedHashMap) caseObject).get("caseUrn").toString()
            ));
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

    /**
     * Handle forming and sending of subscriptions to publication services.
     *
     * @param artefactId The id of the artefact being sent
     * @param subscriptionsList The list of subscriptions being sent
     */
    private void handleSubscriptionSending(UUID artefactId, List<Subscription> subscriptionsList) {
        List<Subscription> emailList = sortSubscriptionByChannel(subscriptionsList, Channel.EMAIL.notificationRoute);
        List<Subscription> apiList = sortSubscriptionByChannel(subscriptionsList,
                                                               Channel.API_COURTEL.notificationRoute);

        channelManagementService.getMappedEmails(emailList).forEach((email, listOfSubscriptions) -> {
            log.info(writeLog("Summary being sent to publication services for id " + artefactId));
            publicationServicesService.postSubscriptionSummaries(artefactId, email, listOfSubscriptions);
        });

        channelManagementService.getMappedApis(apiList)
            .forEach((api, subscriptions) -> log.info(writeLog(publicationServicesService
                                                          .sendThirdPartyList(new ThirdPartySubscription(
                                                                                     api,
                                                                                     artefactId)))));
        log.info(writeLog(String.format("Collected %s api subscribers", apiList.size())));
    }

    /**
     * Create a list of subscriptions for the correct channel.
     *
     * @param subscriptionsList The list of subscriptions to sort through
     * @param channel The channel we want the subscriptions of
     * @return A list of subscriptions
     */
    private List<Subscription> sortSubscriptionByChannel(List<Subscription> subscriptionsList, String channel) {
        List<Subscription> sortedSubscriptionsList = new ArrayList<>();

        subscriptionsList.forEach((Subscription subscription) -> {
            if (channel.equals(subscription.getChannel().notificationRoute)) {
                sortedSubscriptionsList.add(subscription);
            }
        });

        return sortedSubscriptionsList;
    }

    @Async
    @Override
    public void collectThirdPartyForDeletion(Artefact artefactBeingDeleted) {
        List<Subscription> subscriptionList = new ArrayList<>(querySubscriptionValue(
            SearchType.LIST_TYPE.name(), artefactBeingDeleted.getListType().name()));

        List<Subscription> subscriptionsToContact;
        if (artefactBeingDeleted.getSensitivity().equals(Sensitivity.CLASSIFIED)) {
            subscriptionsToContact = validateSubscriptionPermissions(subscriptionList, artefactBeingDeleted);
        } else {
            subscriptionsToContact = subscriptionList;
        }

        handleDeletedArtefactSending(subscriptionsToContact, artefactBeingDeleted);
    }

    private void handleDeletedArtefactSending(List<Subscription> subscriptions, Artefact artefactBeingDeleted) {
        List<Subscription> apiList = sortSubscriptionByChannel(subscriptions,
                                                               Channel.API_COURTEL.notificationRoute);

        channelManagementService.getMappedApis(apiList).forEach((api, subscription) ->
            log.info(writeLog(publicationServicesService.sendEmptyArtefact(
                new ThirdPartySubscriptionArtefact(api, artefactBeingDeleted)
            ))));
    }

    @Override
    public String deleteAllByUserId(String userId) {
        repository.deleteAllByUserId(userId);
        return String.format("All subscriptions deleted for user id %s", userId);
    }
}
