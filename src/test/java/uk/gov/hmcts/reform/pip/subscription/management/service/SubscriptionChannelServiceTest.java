package uk.gov.hmcts.reform.pip.subscription.management.service;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.subscription.management.config.ThirdPartyApiConfigurationProperties;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionChannelServiceTest {

    private static final String COURTEL_VALUE = "testCourtelValue";
    private static final Subscription SUB1 = new Subscription();
    private static final Subscription SUB2 = new Subscription();
    private static final Subscription SUB3 = new Subscription();
    private static final String USER1 = "testUser1";
    private static final String USER2 = "testUser2";
    private static final String TEST_EMAIL_1 = "test@user.com";
    private static final String TEST_EMAIL_2 = "dave@email.com";

    private static final String EMAIL_SUBSCRIPTIONS_MESSAGE = "Email subscriptions do not match";
    private static final String API_SUBSCRIPTIONS_MESSAGE = "API subscriptions do not match";
    private static final String DEDUPLICATION_MESSAGE = "Deduplication result does not match";
    private static final String USER_ID_SWITCH_MESSAGE = "User ID switch result does not match";
    private static final String ERROR_LOG_SIZE_MESSAGE = "Error log size does not match";
    private static final String ERROR_LOG_MESSAGE = "Error log message does not match";

    private final LogCaptor logCaptor = LogCaptor.forClass(SubscriptionChannelService.class);

    @Mock
    private AccountManagementService accountManagementService;

    @Mock
    private ThirdPartyApiConfigurationProperties thirdPartyApi;

    @InjectMocks
    SubscriptionChannelService subscriptionChannelService;

    @Test
    void buildEmailSubscriptionsSuccess() {
        Map<String, Optional<String>> userEmailsMap = Map.of(
            USER2, Optional.of(TEST_EMAIL_2),
            USER1, Optional.of(TEST_EMAIL_1)
        );

        SUB1.setUserId(USER1);
        SUB2.setUserId(USER2);

        Map<String, List<Subscription>> expectedMap = Map.of(
            TEST_EMAIL_1, List.of(SUB1),
            TEST_EMAIL_2, List.of(SUB2)
        );

        doReturn(userEmailsMap).when(accountManagementService).getMappedEmails(any());

        assertThat(subscriptionChannelService.buildEmailSubscriptions(List.of(SUB1, SUB2)))
            .as(EMAIL_SUBSCRIPTIONS_MESSAGE)
            .isEqualTo(expectedMap);

        assertThat(logCaptor.getErrorLogs())
            .as(ERROR_LOG_SIZE_MESSAGE)
            .isEmpty();
    }

    @Test
    void testBuildEmailSubscriptionsWithNoMappedEmails() {
        when(accountManagementService.getMappedEmails(any())).thenReturn(new ConcurrentHashMap<>());

        assertThat(subscriptionChannelService.buildEmailSubscriptions(List.of(SUB1, SUB2)))
            .as(EMAIL_SUBSCRIPTIONS_MESSAGE)
            .isEmpty();

        assertThat(logCaptor.getErrorLogs())
            .as(ERROR_LOG_SIZE_MESSAGE)
            .isNotEmpty();

        assertThat(logCaptor.getErrorLogs().get(0))
            .as(ERROR_LOG_MESSAGE)
            .contains("No email channel found for any of the users provided");
    }

    @Test
    void testDeduplicateSubscriptionsWithNoDuplication() {
        SUB1.setUserId(USER1);
        SUB2.setUserId(USER2);

        Map<String, List<Subscription>> expectedResponse = Map.of(
            USER1, List.of(SUB1),
            USER2, List.of(SUB2)
        );

        assertThat(subscriptionChannelService.deduplicateSubscriptions(List.of(SUB1, SUB2)))
            .as(DEDUPLICATION_MESSAGE)
            .isEqualTo(expectedResponse);
    }

    @Test
    void testDeduplicateSubscriptionsWithDuplications() {
        SUB1.setUserId(USER1);
        SUB2.setUserId(USER1);
        SUB3.setUserId(USER2);

        Map<String, List<Subscription>> expectedResponse = Map.of(
            USER1, List.of(SUB1, SUB2),
            USER2, List.of(SUB3)
        );

        assertThat(subscriptionChannelService.deduplicateSubscriptions(List.of(SUB1, SUB2, SUB3)))
            .as(DEDUPLICATION_MESSAGE)
            .isEqualTo(expectedResponse);
    }

    @Test
    void testUserIdToUserSwitcherSuccess() {
        Map<String, Optional<String>> emailMap = new ConcurrentHashMap<>();
        emailMap.put(USER1, Optional.of(TEST_EMAIL_1));
        emailMap.put(USER2, Optional.of(TEST_EMAIL_2));

        Map<String, List<Subscription>> subsMap = new ConcurrentHashMap<>();
        subsMap.put(USER1, List.of(SUB1, SUB2));
        subsMap.put(USER2, List.of(SUB3));

        Map<String, List<Subscription>> expectedResponse = Map.of(
            TEST_EMAIL_1, List.of(SUB1, SUB2),
            TEST_EMAIL_2, List.of(SUB3)
        );

        assertThat(subscriptionChannelService.userIdToUserEmailSwitcher(subsMap, emailMap))
            .as(USER_ID_SWITCH_MESSAGE)
            .isEqualTo(expectedResponse);
    }

    @Test
    void testUserIdToUserSwitcherWithEmailNotFound() {
        Map<String, Optional<String>> emailMap = new ConcurrentHashMap<>();
        emailMap.put(USER1, Optional.of(TEST_EMAIL_1));
        emailMap.put(USER2, Optional.empty());

        Map<String, List<Subscription>> subsMap = new ConcurrentHashMap<>();
        subsMap.put(USER1, List.of(SUB1, SUB2));
        subsMap.put(USER2, List.of(SUB3));

        Map<String, List<Subscription>> expectedResponse = new ConcurrentHashMap<>();
        expectedResponse.put(TEST_EMAIL_1, List.of(SUB1, SUB2));

        assertThat(subscriptionChannelService.userIdToUserEmailSwitcher(subsMap, emailMap))
            .as(USER_ID_SWITCH_MESSAGE)
            .isEqualTo(expectedResponse);

    }

    @Test
    void testBuildApiSubscriptionsSuccess() {
        when(thirdPartyApi.getCourtel()).thenReturn(COURTEL_VALUE);

        SUB1.setUserId(USER1);
        SUB1.setChannel(Channel.API_COURTEL);
        SUB2.setUserId(USER1);
        SUB2.setChannel(Channel.API_COURTEL);

        Map<String, List<Subscription>> expected = Map.of(
            COURTEL_VALUE, List.of(SUB1, SUB2)
        );

        assertThat(subscriptionChannelService.buildApiSubscriptions(List.of(SUB1, SUB2)))
            .as(API_SUBSCRIPTIONS_MESSAGE)
            .isEqualTo(expected);

        assertThat(logCaptor.getErrorLogs())
            .as(ERROR_LOG_SIZE_MESSAGE)
            .isEmpty();
    }

    @Test
    void testBuildApiSubscriptionsWithInvalidApiChannel() {
        SUB1.setUserId(USER1);
        SUB1.setChannel(Channel.API_COURTEL);
        SUB2.setUserId(USER2);
        SUB2.setChannel(Channel.EMAIL);

        when(thirdPartyApi.getCourtel()).thenReturn(COURTEL_VALUE);

        assertThat(subscriptionChannelService.buildApiSubscriptions(List.of(SUB1, SUB2)))
            .as(API_SUBSCRIPTIONS_MESSAGE)
            .isEmpty();

        assertThat(logCaptor.getErrorLogs())
            .as(ERROR_LOG_SIZE_MESSAGE)
            .isNotEmpty();

        assertThat(logCaptor.getErrorLogs().get(0))
            .as(ERROR_LOG_MESSAGE)
            .contains("Invalid channel for API subscriptions: EMAIL");
    }
}
