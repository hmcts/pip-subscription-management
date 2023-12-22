package uk.gov.hmcts.reform.pip.subscription.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Service("authorisationService")
@Slf4j
public class AuthorisationService {
    private final SubscriptionRepository repository;

    private final AccountManagementService accountManagementService;

    @Autowired
    public AuthorisationService(SubscriptionRepository repository, AccountManagementService accountManagementService) {
        this.repository = repository;
        this.accountManagementService = accountManagementService;
    }

    public boolean userCanDeleteSubscriptions(String userId, UUID... subscriptionIds) {
        if (isSystemAdmin(userId)) {
            return true;
        }
        return Arrays.stream(subscriptionIds)
            .allMatch(id -> isSubscriptionUserMatch(id, userId));

    }

    private boolean isSystemAdmin(String userId) {
        Optional<PiUser> user = accountManagementService.getUserByUserId(userId);
        return user.isPresent()
            && user.get().getRoles() == Roles.SYSTEM_ADMIN;
    }

    private boolean isSubscriptionUserMatch(UUID subscriptionId, String userId) {
        Optional<Subscription> subscription = repository.findById(subscriptionId);

        if (subscription.isPresent()) {
            if (userId.equals(subscription.get().getUserId())) {
                return true;
            }
            log.error(writeLog(
                String.format("User %s is forbidden to remove subscription with ID %s belongs to another user %s",
                              userId, subscriptionId, subscription.get().getUserId())
            ));
            return false;
        }
        // Return true if not subscription found. It will then go to the delete subscription method and return
        // 404 HTTP status rather than a 403 forbidden status.
        return true;
    }
}
