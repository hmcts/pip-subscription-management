package uk.gov.hmcts.reform.pip.subscription.management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.subscription.management.helpers.SubscriptionUtils.createMockSubscriptionList;

@ActiveProfiles("non-async")
@ExtendWith({MockitoExtension.class})
class SubscriptionLocationServiceTest {

    private static final String COURT_NAME = "test court name";
    private static final LocalDateTime DATE_ADDED = LocalDateTime.now();
    private static final String REQUESTER_NAME = "ReqName";
    private static final String EMAIL_ADDRESS = "test@test.com";
    private static final String LOCATION_ID = "1";

    private List<Subscription> mockSubscriptionList;
    private List<UUID> mockSubscriptionIds;

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
    }

    @Test
    void testDeleteSubscriptionByLocation() throws JsonProcessingException {

        when(subscriptionRepository.findSubscriptionsByLocationId(LOCATION_ID))
            .thenReturn(mockSubscriptionList);
        when(dataManagementService.getCourtName(LOCATION_ID))
            .thenReturn(COURT_NAME);
        when(accountManagementService.getUserInfo(REQUESTER_NAME))
            .thenReturn("{\"displayName\": \"ReqName\"}");
        when(accountManagementService.getAllAccounts("PI_AAD", "SYSTEM_ADMIN"))
            .thenReturn(List.of(EMAIL_ADDRESS));
        when(accountManagementService.getAllAccounts("PI_AAD", "SYSTEM_ADMIN"))
            .thenReturn(List.of("test@test.com"));

        doNothing().when(subscriptionRepository).deleteByIdIn(mockSubscriptionIds);

        assertEquals("The subscription for given location is not deleted",
                     "All subscriptions deleted for location id 1",
                     subscriptionLocationService.deleteSubscriptionByLocation(LOCATION_ID, REQUESTER_NAME));
    }

    @Test
    void testDeleteSubscriptionByLocationWhenNoSubscriptionFound() {
        when(subscriptionRepository.findSubscriptionsByLocationId(LOCATION_ID)).thenReturn(List.of());
        assertThrows(SubscriptionNotFoundException.class, () ->
                         subscriptionLocationService.deleteSubscriptionByLocation(LOCATION_ID, REQUESTER_NAME),
                     "SubscriptionNotFoundException not thrown when trying to delete a subscription"
                         + " that does not exist");
    }

    @Test
    void testDeleteSubscriptionByLocationWhenSystemAdminEmpty() throws JsonProcessingException {

        when(subscriptionRepository.findSubscriptionsByLocationId(LOCATION_ID))
            .thenReturn(mockSubscriptionList);
        when(dataManagementService.getCourtName(LOCATION_ID))
            .thenReturn(COURT_NAME);
        when(publicationService.sendLocationDeletionSubscriptionEmail(any(), any())).thenReturn(any());
        when(accountManagementService.getUserInfo(REQUESTER_NAME))
            .thenReturn("{}");

        doNothing().when(subscriptionRepository).deleteByIdIn(mockSubscriptionIds);

        assertEquals("The subscription for given location is not deleted",
                     "All subscriptions deleted for location id 1",
                     subscriptionLocationService.deleteSubscriptionByLocation(LOCATION_ID, REQUESTER_NAME));
    }

    @Test
    void testDeleteSubscriptionByLocationWhenSystemAdminException() {

        when(subscriptionRepository.findSubscriptionsByLocationId(LOCATION_ID))
            .thenReturn(mockSubscriptionList);
        when(dataManagementService.getCourtName(LOCATION_ID))
            .thenReturn(COURT_NAME);
        when(accountManagementService.getUserInfo(REQUESTER_NAME))
            .thenReturn("{test}");

        assertThrows(JsonProcessingException.class, () -> subscriptionLocationService.deleteSubscriptionByLocation(
                         LOCATION_ID, REQUESTER_NAME),
                     "JsonProcessingException not thrown when trying to get errored system admin"
                         + " api response");
    }
}
