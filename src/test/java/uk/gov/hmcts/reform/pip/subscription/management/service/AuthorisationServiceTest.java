package uk.gov.hmcts.reform.pip.subscription.management.service;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorisationServiceTest {
    private static final String USER_ID = UUID.randomUUID().toString();
    private static final String ANOTHER_USER_ID = UUID.randomUUID().toString();
    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID2 = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID3 = UUID.randomUUID();

    private static final String CAN_DELETE_SUBSCRIPTION_MESSAGE = "USer should be able to delete subscription";
    private static final String CANNOT_DELETE_SUBSCRIPTION_MESSAGE = "User should not be able to delete subscription";
    private static final String LOG_EMPTY_MESSAGE = "Error log should be empty";
    private static final String LOG_NOT_EMPTY_MESSAGE = "Error log should not be empty";
    private static final String LOG_MATCHED_MESSAGE = "Error log message does not match";
    private static final String ERROR_LOG = "User %s is forbidden to remove subscription with ID %s belongs to "
        + "another user %s";

    private static PiUser piUser = new PiUser();
    private static Subscription subscription = new Subscription();
    private static Subscription subscription2 = new Subscription();
    private static Subscription subscription3 = new Subscription();

    @Mock
    SubscriptionRepository subscriptionRepository;

    @Mock
    AccountManagementService accountManagementService;

    @InjectMocks
    AuthorisationService authorisationService;

    @BeforeAll
    static void setup() {
        piUser.setUserId(USER_ID);
        subscription.setId(SUBSCRIPTION_ID);
        subscription2.setId(SUBSCRIPTION_ID2);
        subscription3.setId(SUBSCRIPTION_ID3);
    }

    @Test
    void testSystemAdminUserCanDeleteSubscription() {
        piUser.setRoles(Roles.SYSTEM_ADMIN);
        when(accountManagementService.getUserByUserId(USER_ID)).thenReturn(Optional.of(piUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            assertThat(authorisationService.userCanDeleteSubscriptions(USER_ID, SUBSCRIPTION_ID))
                .as(CAN_DELETE_SUBSCRIPTION_MESSAGE)
                .isTrue();

            verifyNoInteractions(subscriptionRepository);
            assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @Test
    void testAdminUserCannotDeleteSubscriptionIfUserMismatched() {
        piUser.setRoles(Roles.INTERNAL_ADMIN_LOCAL);
        subscription.setUserId(ANOTHER_USER_ID);
        when(accountManagementService.getUserByUserId(USER_ID)).thenReturn(Optional.of(piUser));
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            assertThat(authorisationService.userCanDeleteSubscriptions(USER_ID, SUBSCRIPTION_ID))
                .as(CANNOT_DELETE_SUBSCRIPTION_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().get(0))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(ERROR_LOG, USER_ID, SUBSCRIPTION_ID, ANOTHER_USER_ID));
        }
    }

    @Test
    void testVerifiedUserCannotDeleteSubscriptionIfUserMismatched() {
        piUser.setRoles(Roles.VERIFIED);
        subscription.setUserId(ANOTHER_USER_ID);
        when(accountManagementService.getUserByUserId(USER_ID)).thenReturn(Optional.of(piUser));
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            assertThat(authorisationService.userCanDeleteSubscriptions(USER_ID, SUBSCRIPTION_ID))
                .as(CANNOT_DELETE_SUBSCRIPTION_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().get(0))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(ERROR_LOG, USER_ID, SUBSCRIPTION_ID, ANOTHER_USER_ID));
        }
    }

    @Test
    void testUserCanDeleteSubscriptionIfUserMatchedInSingleSubscription() {
        piUser.setRoles(Roles.INTERNAL_ADMIN_LOCAL);
        subscription.setUserId(USER_ID);
        when(accountManagementService.getUserByUserId(USER_ID)).thenReturn(Optional.of(piUser));
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            assertThat(authorisationService.userCanDeleteSubscriptions(USER_ID, SUBSCRIPTION_ID))
                .as(CAN_DELETE_SUBSCRIPTION_MESSAGE)
                .isTrue();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @Test
    void testUserCanDeleteSubscriptionIfUserMatchedInAllSubscriptions() {
        piUser.setRoles(Roles.INTERNAL_ADMIN_LOCAL);
        subscription.setUserId(USER_ID);
        subscription2.setUserId(USER_ID);
        subscription3.setUserId(USER_ID);

        when(accountManagementService.getUserByUserId(USER_ID)).thenReturn(Optional.of(piUser));
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.findById(SUBSCRIPTION_ID2)).thenReturn(Optional.of(subscription2));
        when(subscriptionRepository.findById(SUBSCRIPTION_ID3)).thenReturn(Optional.of(subscription3));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            assertThat(authorisationService.userCanDeleteSubscriptions(USER_ID, SUBSCRIPTION_ID, SUBSCRIPTION_ID2,
                                                                       SUBSCRIPTION_ID3))
                .as(CAN_DELETE_SUBSCRIPTION_MESSAGE)
                .isTrue();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @Test
    void testUserCannotDeleteSubscriptionIfUserMisMatchedInSomeOfTheSubscriptions() {
        piUser.setRoles(Roles.INTERNAL_ADMIN_LOCAL);
        subscription.setUserId(USER_ID);
        subscription2.setUserId(ANOTHER_USER_ID);
        subscription3.setUserId(USER_ID);

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            when(accountManagementService.getUserByUserId(USER_ID)).thenReturn(Optional.of(piUser));
            when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
            when(subscriptionRepository.findById(SUBSCRIPTION_ID2)).thenReturn(Optional.of(subscription2));

            assertThat(authorisationService.userCanDeleteSubscriptions(USER_ID, SUBSCRIPTION_ID, SUBSCRIPTION_ID2,
                                                                       SUBSCRIPTION_ID3
            ))
                .as(CANNOT_DELETE_SUBSCRIPTION_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().get(0))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(ERROR_LOG, USER_ID, SUBSCRIPTION_ID2, ANOTHER_USER_ID));

            verify(subscriptionRepository, never()).findById(SUBSCRIPTION_ID3);
        }
    }

    @Test
    void testUserCanDeleteSubscriptionReturnsTrueIfSubscriptionNotFound() {
        piUser.setRoles(Roles.INTERNAL_ADMIN_LOCAL);
        when(accountManagementService.getUserByUserId(USER_ID)).thenReturn(Optional.of(piUser));
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            assertThat(authorisationService.userCanDeleteSubscriptions(USER_ID, SUBSCRIPTION_ID))
                .as(CAN_DELETE_SUBSCRIPTION_MESSAGE)
                .isTrue();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }
}
