package uk.gov.hmcts.reform.pip.subscription.management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.model.account.AzureAccount;
import uk.gov.hmcts.reform.pip.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;
import static uk.gov.hmcts.reform.pip.model.account.Roles.SYSTEM_ADMIN;
import static uk.gov.hmcts.reform.pip.model.account.UserProvenances.PI_AAD;

@Slf4j
@Service
public class SubscriptionLocationService {

    @Autowired
    SubscriptionRepository repository;

    @Autowired
    DataManagementService dataManagementService;

    @Autowired
    AccountManagementService accountManagementService;

    @Autowired
    PublicationServicesService publicationServicesService;

    public List<Subscription> findSubscriptionsByLocationId(String value) {
        List<Subscription> locationSubscriptions = repository.findSubscriptionsByLocationId(value);
        if (locationSubscriptions.isEmpty()) {
            throw new SubscriptionNotFoundException(String.format(
                "No subscription found with the location id %s",
                value
            ));
        }
        return repository.findSubscriptionsByLocationId(value);
    }

    public String deleteSubscriptionByLocation(String locationId, String provenanceUserId)
        throws JsonProcessingException {

        log.info(writeLog(String.format("User %s attempting to delete all subscriptions for location %s",
                                        provenanceUserId, locationId)));
        List<Subscription> locationSubscriptions = findSubscriptionsByLocationId(locationId);

        List<UUID> subIds = locationSubscriptions.stream()
            .map(Subscription::getId).toList();
        repository.deleteByIdIn(subIds);

        log.info(writeLog(String.format("%s subscription(s) have been deleted for location %s by user %s",
                                        subIds.size(), locationId, provenanceUserId)));

        String locationName = dataManagementService.getCourtName(locationId);
        notifySubscriberAboutSubscriptionDeletion(locationSubscriptions, locationName);
        notifySystemAdminAboutSubscriptionDeletion(provenanceUserId,
            String.format("Total %s subscription(s) for location %s",
                          locationSubscriptions.size(), locationName));

        return String.format("Total %s subscriptions deleted for location id %s", subIds.size(), locationId);

    }

    public String deleteAllSubscriptionsWithLocationNamePrefix(String prefix) {
        List<UUID> subscriptionIds = repository.findAllByLocationNameStartingWithIgnoreCase(prefix).stream()
            .map(Subscription::getId)
            .toList();

        if (!subscriptionIds.isEmpty()) {
            repository.deleteByIdIn(subscriptionIds);
        }
        return String.format("%s subscription(s) deleted for location name starting with %s",
                             subscriptionIds.size(), prefix);
    }

    private void notifySubscriberAboutSubscriptionDeletion(List<Subscription> locationSubscriptions,
                                                           String locationName) {
        List<String> userEmails = getUserEmailsForAllSubscriptions(locationSubscriptions);
        publicationServicesService.sendLocationDeletionSubscriptionEmail(userEmails, locationName);
    }

    private void notifySystemAdminAboutSubscriptionDeletion(String provenanceUserId, String additionalDetails)
        throws JsonProcessingException {
        AzureAccount userInfo = accountManagementService.getUserInfo(provenanceUserId);
        List<PiUser> systemAdmins = accountManagementService.getAllAccounts(PI_AAD.toString(),
                                                                            SYSTEM_ADMIN.toString());
        List<String> systemAdminEmails = systemAdmins.stream().map(PiUser::getEmail).toList();
        publicationServicesService.sendSystemAdminEmail(systemAdminEmails, userInfo.getDisplayName(),
                                                        ActionResult.SUCCEEDED, additionalDetails);
    }

    private List<String> getUserEmailsForAllSubscriptions(List<Subscription> subscriptions) {
        List<String> userIds = subscriptions.stream()
            .map(Subscription::getUserId).toList();
        Map<String, Optional<String>> usersInfo =
            accountManagementService.getMappedEmails(userIds);

        List<String> userEmails = new ArrayList<>();

        usersInfo.forEach((userId, email) ->
            userEmails.add(
                email.isPresent() ? email.get() : ""
            )
        );
        userEmails.removeAll(Arrays.asList(""));
        return userEmails;
    }
}
