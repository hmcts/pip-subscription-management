package uk.gov.hmcts.reform.pip.subscription.management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionListType;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionListTypeRepository;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;
import static uk.gov.hmcts.reform.pip.model.account.Roles.SYSTEM_ADMIN;
import static uk.gov.hmcts.reform.pip.model.account.UserProvenances.PI_AAD;
import static uk.gov.hmcts.reform.pip.model.account.UserProvenances.SSO;


@Slf4j
@Service
public class SubscriptionLocationService {

    private final SubscriptionRepository repository;

    private final SubscriptionListTypeRepository subscriptionListTypeRepository;

    private final DataManagementService dataManagementService;

    private final AccountManagementService accountManagementService;

    private final PublicationServicesService publicationServicesService;

    @Autowired
    public SubscriptionLocationService(
        SubscriptionRepository repository,
        DataManagementService dataManagementService,
        AccountManagementService accountManagementService,
        PublicationServicesService publicationServicesService,
        SubscriptionListTypeRepository subscriptionListTypeRepository
    ) {
        this.repository = repository;
        this.dataManagementService = dataManagementService;
        this.accountManagementService = accountManagementService;
        this.publicationServicesService = publicationServicesService;
        this.subscriptionListTypeRepository = subscriptionListTypeRepository;
    }

    public List<Subscription> findSubscriptionsByLocationId(String value) {
        List<Subscription> locationSubscriptions = repository.findSubscriptionsByLocationId(value);
        if (locationSubscriptions.isEmpty()) {
            throw new SubscriptionNotFoundException(String.format(
                "No subscription found with the location id %s",
                value
            ));
        }
        return locationSubscriptions;
    }

    public String deleteSubscriptionByLocation(String locationId, String userId)
        throws JsonProcessingException {

        log.info(writeLog(String.format("User %s attempting to delete all subscriptions for location %s",
                                        userId, locationId)));
        List<Subscription> locationSubscriptions = findSubscriptionsByLocationId(locationId);

        List<UUID> subIds = locationSubscriptions.stream()
            .map(Subscription::getId).toList();
        repository.deleteByIdIn(subIds);

        //DELETE DATA FROM SUBSCRIPTION LIST TYPE TABLE AS WELL.
        this.deleteAllSubscriptionListTypeForLocation(locationSubscriptions);

        log.info(writeLog(String.format("%s subscription(s) have been deleted for location %s by user %s",
                                        subIds.size(), locationId, userId)));

        String locationName = dataManagementService.getCourtName(locationId);
        notifySubscriberAboutSubscriptionDeletion(locationSubscriptions, locationName);
        notifySystemAdminAboutSubscriptionDeletion(userId,
            String.format("Total %s subscription(s) for location %s",
                          locationSubscriptions.size(), locationName));

        return String.format("Total %s subscriptions deleted for location id %s", subIds.size(), locationId);

    }

    private void deleteAllSubscriptionListTypeForLocation(List<Subscription> locationSubscriptions) {
        List<String> uniqueUsers = locationSubscriptions.stream()
            .map(Subscription::getUserId).distinct().toList();

        if (!uniqueUsers.isEmpty()) {

            for (String userId : uniqueUsers) {
                this.deleteSubscriptionListTypeByUser(userId);
            }
        }
    }

    public void deleteSubscriptionListTypeByUser(String userId) {
        List<Subscription> userLocationSubscriptions =
            repository.findLocationSubscriptionsByUserId(userId);

        if (userLocationSubscriptions.isEmpty()) {
            Optional<SubscriptionListType> subscriptionListType =
                subscriptionListTypeRepository.findByUserId(userId);
            subscriptionListType.ifPresent(subscriptionListTypeRepository::delete);
        }
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

    private void notifySystemAdminAboutSubscriptionDeletion(String userId, String additionalDetails)
        throws JsonProcessingException {
        Optional<PiUser> piUserOptional = accountManagementService.getUserByUserId(userId);
        if (piUserOptional.isPresent()) {
            PiUser piUser = piUserOptional.get();
            List<PiUser> systemAdminsAad = accountManagementService.getAllAccounts(PI_AAD.toString(),
                                                                                   SYSTEM_ADMIN.toString());
            List<PiUser> systemAdminsSso = accountManagementService
                .getAllAccounts(SSO.toString(), SYSTEM_ADMIN.toString());

            List<PiUser> systemAdmins = Stream.concat(systemAdminsAad.stream(), systemAdminsSso.stream()).toList();
            List<String> systemAdminEmails = systemAdmins.stream().map(PiUser::getEmail).toList();
            publicationServicesService.sendSystemAdminEmail(systemAdminEmails, piUser.getEmail(),
                                                            ActionResult.SUCCEEDED, additionalDetails);
        } else {
            log.error(writeLog(String.format("User %s not found in the system when notifying system admins", userId)));
        }
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
