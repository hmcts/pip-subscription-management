package uk.gov.hmcts.reform.pip.subscription.management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
        List<Subscription> locationSubscriptions = findSubscriptionsByLocationId(locationId);
        if (locationSubscriptions.isEmpty()) {
            return String.format("No subscriptions found for location id %s", locationId);
        } else {
            List<UUID> subIds = locationSubscriptions.stream()
                .map(Subscription::getId).toList();
            repository.deleteByIdIn(subIds);

            String locationName = dataManagementService.getCourtName(locationId);
            notifySubscriberAboutSubscriptionDeletion(locationSubscriptions, locationName);
            notifySystemAdminAboutSubscriptionDeletion(provenanceUserId,
                String.format("Total %s subscription(s) for location %s",
                              locationSubscriptions.size(), locationName));

            return String.format("Total %s subscriptions deleted for location id %s", subIds.size(), locationId);
        }

    }

    private void notifySubscriberAboutSubscriptionDeletion(List<Subscription> locationSubscriptions,
                                                           String locationName) {
        List<String> userEmails = getUserEmailsForAllSubscriptions(locationSubscriptions);
        publicationServicesService.sendLocationDeletionSubscriptionEmail(userEmails, locationName);
    }

    private void notifySystemAdminAboutSubscriptionDeletion(String provenanceUserId, String additionalDetails)
        throws JsonProcessingException {
        String result = accountManagementService.getUserInfo(provenanceUserId);
        try {
            JsonNode node = new ObjectMapper().readTree(result);
            if (!node.isEmpty()) {
                String requesterName = node.get("displayName").asText();
                List<String> systemAdmins = accountManagementService.getAllAccounts("PI_AAD", "SYSTEM_ADMIN");
                publicationServicesService.sendSystemAdminEmail(systemAdmins, requesterName,
                                                                ActionResult.SUCCEEDED, additionalDetails);
            }
        } catch (JsonProcessingException e) {
            log.error(String.format("Failed to get userInfo: %s",
                                    e.getMessage()));
            throw e;
        }
    }

    private List<String> getUserEmailsForAllSubscriptions(List<Subscription> subscriptions) {
        List<String> userIds = subscriptions.stream()
            .map(Subscription::getUserId).toList();
        Map<String, Optional<String>> usersInfo =
            accountManagementService.getMappedEmails(userIds);

        List<String> userEmails = new ArrayList<>();

        usersInfo.forEach((userId, email) ->
            userEmails.add(email.get())
        );
        return userEmails;
    }
}
