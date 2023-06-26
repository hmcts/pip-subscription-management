package uk.gov.hmcts.reform.pip.subscription.management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.model.account.AzureAccount;
import uk.gov.hmcts.reform.pip.model.account.PiUser;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.model.account.Roles.SYSTEM_ADMIN;
import static uk.gov.hmcts.reform.pip.model.account.UserProvenances.PI_AAD;
import static uk.gov.hmcts.reform.pip.subscription.management.helpers.SubscriptionUtils.createMockSubscriptionList;

@ActiveProfiles("non-async")
@ExtendWith({MockitoExtension.class})
class SubscriptionLocationServiceTest {

    private static final String COURT_NAME = "test court name";
    private static final LocalDateTime DATE_ADDED = LocalDateTime.now();
    private static final String REQUESTER_NAME = "ReqName";
    private static final String EMAIL_ADDRESS = "test@test.com";
    private static final String LOCATION_ID = "1";
    private static final String LOCATION_NAME_PREFIX = "TEST_PIP_1234_";

    private List<Subscription> mockSubscriptionList;
    private List<UUID> mockSubscriptionIds;
    AzureAccount azureAccount;

    @Mock
    DataManagementService dataManagementService;

    @Mock
    private AccountManagementService accountManagementService;

    @Mock
    private PublicationServicesService publicationService;

    @Mock
    SubscriptionRepository subscriptionRepository;

    @InjectMocks
    SubscriptionLocationService subscriptionLocationService;

    @BeforeEach
    void setup() {
        mockSubscriptionList = createMockSubscriptionList(DATE_ADDED);
        mockSubscriptionIds = mockSubscriptionList.stream()
            .map(subscription -> subscription.getId()).toList();

        azureAccount = new AzureAccount();
        azureAccount.setDisplayName("ReqName");
    }

    @Test
    void testDeleteSubscriptionByLocation() throws JsonProcessingException {

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionLocationService.class)) {
            PiUser sysAdminUser = new PiUser();
            sysAdminUser.setEmail(EMAIL_ADDRESS);

            when(subscriptionRepository.findSubscriptionsByLocationId(LOCATION_ID))
                .thenReturn(mockSubscriptionList);
            when(dataManagementService.getCourtName(LOCATION_ID))
                .thenReturn(COURT_NAME);
            when(accountManagementService.getUserInfo(REQUESTER_NAME))
                .thenReturn(azureAccount);
            when(accountManagementService.getAllAccounts(PI_AAD.toString(), SYSTEM_ADMIN.toString()))
                .thenReturn(List.of(sysAdminUser));

            doNothing().when(subscriptionRepository).deleteByIdIn(mockSubscriptionIds);

            assertEquals(
                "The subscription for given location is not deleted",
                "Total 8 subscriptions deleted for location id 1",
                subscriptionLocationService.deleteSubscriptionByLocation(LOCATION_ID, REQUESTER_NAME)
            );

            assertTrue(logCaptor.getInfoLogs().get(0).contains("User "
                                                                   + REQUESTER_NAME
                                                                   + " attempting to delete all "
                                                                   + "subscriptions for location "
                                                                   + LOCATION_ID),
                       "Expected log message not found");
            assertTrue(logCaptor.getInfoLogs().get(1).contains("8 subscription(s) have been deleted for location "
                                                                   + LOCATION_ID
                                                                   + " by user "
                                                                   + REQUESTER_NAME),
                       "Expected log message not found");
        }
    }

    @Test
    void testDeleteSubscriptionByLocationWhenNoSubscriptionFound() {
        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionLocationService.class)) {
            when(subscriptionRepository.findSubscriptionsByLocationId(LOCATION_ID)).thenReturn(List.of());
            assertThrows(SubscriptionNotFoundException.class, () ->
                             subscriptionLocationService.deleteSubscriptionByLocation(LOCATION_ID, REQUESTER_NAME),
                         "SubscriptionNotFoundException not thrown when trying to delete a subscription"
                             + " that does not exist");

            assertTrue(logCaptor.getInfoLogs().get(0).contains("User "
                                                                   + REQUESTER_NAME
                                                                   + " attempting to delete all"
                                                                   + " subscriptions for location "
                                                                   + LOCATION_ID),
                       "Expected log message not found");
        }

    }

    @Test
    void testDeleteSubscriptionByLocationWhenSystemAdminEmpty() throws JsonProcessingException {

        when(subscriptionRepository.findSubscriptionsByLocationId(LOCATION_ID))
            .thenReturn(mockSubscriptionList);
        when(dataManagementService.getCourtName(LOCATION_ID))
            .thenReturn(COURT_NAME);
        doNothing().when(publicationService).sendLocationDeletionSubscriptionEmail(any(), any());
        when(accountManagementService.getUserInfo(REQUESTER_NAME))
            .thenReturn(azureAccount);

        doNothing().when(subscriptionRepository).deleteByIdIn(mockSubscriptionIds);

        assertEquals("The subscription for given location is not deleted",
                     "Total 8 subscriptions deleted for location id 1",
                     subscriptionLocationService.deleteSubscriptionByLocation(LOCATION_ID, REQUESTER_NAME));
    }

    @Test
    void testDeleteAllSubscriptionsWithLocationNamePrefix() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Subscription subscription1 = new Subscription();
        subscription1.setId(id1);
        subscription1.setLocationName(LOCATION_NAME_PREFIX + 1);

        Subscription subscription2 = new Subscription();
        subscription2.setId(id2);
        subscription2.setLocationName(LOCATION_NAME_PREFIX + 2);

        when(subscriptionRepository.findAllByLocationNameStartingWithIgnoreCase(LOCATION_NAME_PREFIX))
            .thenReturn(List.of(subscription1, subscription2));

        assertThat(subscriptionLocationService.deleteAllSubscriptionsWithLocationNamePrefix(LOCATION_NAME_PREFIX))
            .as("Subscription deleted message does not match")
            .isEqualTo("2 subscription(s) deleted for location name starting with " + LOCATION_NAME_PREFIX);

        verify(subscriptionRepository).deleteByIdIn(List.of(id1, id2));
    }

    @Test
    void testDeleteAllSubscriptionsWithLocationNamePrefixWhenLocationNotFound() {
        when(subscriptionRepository.findAllByLocationNameStartingWithIgnoreCase(LOCATION_NAME_PREFIX))
            .thenReturn(Collections.emptyList());

        assertThat(subscriptionLocationService.deleteAllSubscriptionsWithLocationNamePrefix(LOCATION_NAME_PREFIX))
            .as("Subscription deleted message does not match")
            .isEqualTo("0 subscription(s) deleted for location name starting with " + LOCATION_NAME_PREFIX);

        verify(subscriptionRepository, never()).deleteByIdIn(anyList());
    }
}
