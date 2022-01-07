package uk.gov.hmcts.reform.pip.subscription.management.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.subscription.management.models.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.CaseSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.CourtSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.UserSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.subscription.management.helpers.SubscriptionUtils.createMockSubscription;
import static uk.gov.hmcts.reform.pip.subscription.management.helpers.SubscriptionUtils.createMockSubscriptionList;
import static uk.gov.hmcts.reform.pip.subscription.management.helpers.SubscriptionUtils.findableSubscription;

@ExtendWith({MockitoExtension.class})
class SubscriptionServiceTest {
    private static final String USER_ID = "Ralph21";
    private static final String USER_ID_NO_SUBS = "Tina21";
    private static final String SEARCH_VALUE = "193254";
    private static final String CASE_ID = "123";
    private static final String URN = "312";
    private static final String CASE_NAME = "case-name";

    private List<Subscription> mockSubscriptionList;
    private Subscription mockSubscription;
    private Subscription findableSubscription;
    private LocalDateTime dateAdded;

    @Mock
    SubscriptionRepository subscriptionRepository;

    @InjectMocks
    SubscriptionServiceImpl subscriptionService;

    @BeforeEach
    void setup() {
        dateAdded = LocalDateTime.now();
        mockSubscription = createMockSubscription(USER_ID, SEARCH_VALUE, dateAdded);
        mockSubscriptionList = createMockSubscriptionList(dateAdded);
        findableSubscription = findableSubscription();

        lenient().when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(mockSubscriptionList);
        lenient().when(subscriptionRepository.findByUserId(USER_ID_NO_SUBS)).thenReturn(new ArrayList<>());
    }

    @Test
    void testGetSubscriptionReturnsExpected() {
        when(subscriptionRepository.findAll()).thenReturn(mockSubscriptionList);
        assertEquals(mockSubscriptionList, subscriptionService.findAll(), "The returned subscription list "
            + "does not match the expected list");
    }

    @Test
    void testCreateSubscription() {
        when(subscriptionRepository.save(mockSubscription)).thenReturn(mockSubscription);
        assertEquals(subscriptionService.createSubscription(mockSubscription), mockSubscription,
                     "The returned subscription does not match the expected subscription"
        );
    }

    @Test
    void testDeleteSubscription() {
        UUID testUuid = UUID.randomUUID();
        ArgumentCaptor<UUID> captor = ArgumentCaptor.forClass(UUID.class);
        doNothing().when(subscriptionRepository).deleteById(captor.capture());
        when(subscriptionRepository.findById(testUuid)).thenReturn(Optional.of(findableSubscription));
        subscriptionService.deleteById(testUuid);
        assertEquals(testUuid, captor.getValue(), "The service layer tried to delete the wrong subscription");
    }

    @Test
    void testDeleteException() {
        UUID testUuid = UUID.randomUUID();
        when(subscriptionRepository.findById(testUuid)).thenReturn(Optional.empty());
        assertThrows(SubscriptionNotFoundException.class, () -> subscriptionService.deleteById(testUuid),
                     "SubscriptionNotFoundException not thrown when trying to delete a subscription"
                         + " that does not exist");
    }

    @Test
    void testFindException() {
        UUID testUuid = UUID.randomUUID();
        when(subscriptionRepository.findById(testUuid)).thenReturn(Optional.empty());
        assertThrows(SubscriptionNotFoundException.class, () -> subscriptionService.findById(testUuid),
                     "SubscriptionNotFoundException not thrown "
                         + "when trying to find a subscription that does not exist");
    }

    @Test
    void testFindSubscription() {
        UUID testUuid = UUID.randomUUID();
        when(subscriptionRepository.findById(testUuid)).thenReturn(Optional.of(findableSubscription));
        assertEquals(subscriptionService.findById(testUuid), findableSubscription,
                     "The returned subscription does not match the expected subscription");
    }

    @Test
    void testNoSubscriptionsReturnsEmpty() {
        assertEquals(new UserSubscription(), subscriptionService.findByUserId(USER_ID_NO_SUBS),
                     "Should return empty user subscriptions");
    }

    @Test
    void testFindByUserIdOnlyCourt() {
        mockSubscription.setSearchType(SearchType.COURT_ID);
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of(mockSubscription));
        UserSubscription result = subscriptionService.findByUserId(USER_ID);
        CourtSubscription expected = new CourtSubscription();
        expected.setCourtName("Test court");
        expected.setDateAdded(dateAdded);

        assertEquals(List.of(expected), result.getCourtSubscriptions(),
                     "Should return court name");
        assertEquals(0, result.getCaseSubscriptions().size(), "Cases should be empty");
    }

    @Test
    void testFindByUserIdCaseType() {
        mockSubscription.setSearchType(SearchType.CASE_ID);
        mockSubscription.setCaseNumber(CASE_ID);
        mockSubscription.setCaseName(CASE_NAME);
        mockSubscription.setUrn(URN);

        CaseSubscription expected = new CaseSubscription();
        expected.setCaseNumber(CASE_ID);
        expected.setCaseName(CASE_NAME);
        expected.setUrn(URN);
        expected.setUrn(URN);
        expected.setDateAdded(dateAdded);
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of(mockSubscription));

        assertEquals(List.of(expected), subscriptionService.findByUserId(USER_ID).getCaseSubscriptions(),
                     "Should return populated case");
    }

    @Test
    void testFindByUserIdLength() {
        UserSubscription result = subscriptionService.findByUserId(USER_ID);
        assertEquals(6, result.getCaseSubscriptions().size(),
                     "Should add all CaseSubscriptions to UserSubscriptions");
        assertEquals(2, result.getCourtSubscriptions().size(), "Should add all court names");
    }

    @Test
    void testFindByUserId() {
        UserSubscription result = subscriptionService.findByUserId(USER_ID);
        for (int i = 0; i < 6; i++) {
            assertEquals(CASE_ID + i, result.getCaseSubscriptions().get(i).getCaseNumber(),
                         "Should contain correct caseNumber");
        }
        assertEquals("test court name", result.getCourtSubscriptions().get(0).getCourtName(),
                     "Should match court name");
    }

    @Test
    void testFindByUserIdCreatedDates() {
        UserSubscription result = subscriptionService.findByUserId(USER_ID);
        for (int i = 0; i < 6; i++) {
            assertEquals(dateAdded, result.getCaseSubscriptions().get(i).getDateAdded(),
                         "Should match dateAdded");
        }
        assertEquals(dateAdded, result.getCourtSubscriptions().get(0).getDateAdded(), "Should match dateAdded");
    }

    @Test
    void testFindByUserIdAssignsIdForCourt() {
        mockSubscription.setSearchType(SearchType.COURT_ID);
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of(mockSubscription));

        assertEquals(mockSubscription.getId(),
                     subscriptionService.findByUserId(USER_ID).getCourtSubscriptions().get(0).getSubscriptionId(),
                     "Should match subscriptionId");
    }

    @Test
    void testFindByUserIdAssignsIdForCase() {
        mockSubscription.setSearchType(SearchType.CASE_ID);
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of(mockSubscription));

        assertEquals(mockSubscription.getId(),
                     subscriptionService.findByUserId(USER_ID).getCaseSubscriptions().get(0).getSubscriptionId(),
                     "Should match subscriptionId");
    }
}

