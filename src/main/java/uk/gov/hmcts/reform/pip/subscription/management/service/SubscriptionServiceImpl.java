package uk.gov.hmcts.reform.pip.subscription.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.subscription.management.models.*;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Artefact;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.ListType;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Sensitivity;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.CaseSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.CourtSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.UserSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

import java.util.*;

/**
 * Service layer for dealing with subscriptions.
 */
@Slf4j
@Service
@SuppressWarnings({"PMD.LawOfDemeter", "PMD.AvoidCatchingNPE"})
public class SubscriptionServiceImpl implements SubscriptionService {

    @Autowired
    SubscriptionRepository repository;

    @Autowired
    DataManagementService dataManagementService;

    @Autowired
    ChannelManagementService channelManagementService;

    @Autowired
    AccountManagementService accountManagementService;

    @Override
    public Subscription createSubscription(Subscription subscription) {
        if (subscription.getSearchType().equals(SearchType.COURT_ID)) {
            subscription.setCourtName(dataManagementService.getCourtName(subscription.getSearchValue()));
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

    @Async
    @Override
    public void collectSubscribers(Artefact artefact) {
        List<Subscription> subscriptionList = new ArrayList<>(querySubscriptionValue(
            SearchType.COURT_ID.name(), artefact.getCourtId()));

        if(artefact.getSearch().get("cases") != null) {
            artefact.getSearch().get("cases").forEach(object -> subscriptionList.addAll(extractSearchValue(object)));
        }

        List<Subscription> subscriptionsToContact;
        if (artefact.getSensitivity().equals(Sensitivity.CLASSIFIED)) {
            subscriptionsToContact = validateSubscriptionPermissions(subscriptionList, artefact.getListType());
        } else {
            subscriptionsToContact = subscriptionList;
        }

        List<Subscription> subscriptionsForEmail = sortSubscriptionByChannel(subscriptionsToContact, Channel.EMAIL);
        List<Subscription> subscriptionsForAPI = sortSubscriptionByChannel(subscriptionsToContact, Channel.API);

        Map<String, List<Subscription>> returnedMappings = channelManagementService.getMappedEmails(subscriptionsForEmail);


        returnedMappings.forEach((email, listOfSubscriptions) -> {
            UserSubscriptionDto test = formatNotifySubscription(artefact.getArtefactId(), email, listOfSubscriptions);
            log.info(test.toString());
        });




        log.info("Subscriber list created. Found {} subscribers (pre-de-duplication). {} in the email channel, "
                     + "{} via API",
                 subscriptionsToContact.size(), subscriptionsForEmail.size(), subscriptionsForAPI.size());
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

    private List<Subscription> sortSubscriptionByChannel(List<Subscription> subscriptionsList, Channel channel) {
        List<Subscription> sortedSubscriptionsList = new ArrayList<>();

        subscriptionsList.forEach((Subscription subscription) -> {
            if (channel.equals(subscription.getChannel())) {
                sortedSubscriptionsList.add(subscription);
            }
        });

        return sortedSubscriptionsList;
    }

    private UserSubscriptionDto formatNotifySubscription(UUID artefactId, String email, List<Subscription> listOfSubscriptions) {
        UserSubscriptionDto userSubscriptionDto = new UserSubscriptionDto();
        userSubscriptionDto.setArtefactId(artefactId);
        userSubscriptionDto.setEmail(email);

        SubscriptionsSummaryDto subscriptionsSummaryDto = new SubscriptionsSummaryDto();

        ArrayList<String> caseUrnList = new ArrayList<>();
        ArrayList<String> caseIdList = new ArrayList<>();
        ArrayList<String> locationIdList = new ArrayList<>();

        listOfSubscriptions.forEach(subscription -> {
            switch(subscription.getSearchType()) {
                case CASE_URN:
                    caseUrnList.add(subscription.getSearchValue());
                    break;
                case CASE_ID:
                    caseIdList.add(subscription.getSearchValue());
                    break;
                case COURT_ID:
                    locationIdList.add(subscription.getSearchValue());
                    break;
            }
        });

        subscriptionsSummaryDto.setCaseUrn(caseUrnList);
        subscriptionsSummaryDto.setCaseNumber(caseIdList);
        subscriptionsSummaryDto.setLocationId(locationIdList);

        userSubscriptionDto.setSubscriptions(subscriptionsSummaryDto);

        return userSubscriptionDto;
    }
}
