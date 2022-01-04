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
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Court;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Hearing;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.UserSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    private static final String COURT_ID = "354";
    private static final String CASE_NAME = "case-name";

    private static final String VALIDATION_SEARCH_TYPE = "Search type does not match expected search type";
    private static final String VALIDATION_SEARCH_VALUE = "Search value does not match expected search value";
    private static final String VALIDATION_SINGLE_USER_SUBSCRIPTION = "A single user subscription is returned";

    private List<Subscription> mockSubscriptionList;
    private Subscription mockSubscription;
    private Subscription findableSubscription;

    @Mock
    DataManagementService dataManagementService;

    @Mock
    SubscriptionRepository subscriptionRepository;

    @InjectMocks
    SubscriptionServiceImpl subscriptionService;

    @BeforeEach
    void setup() {
        mockSubscription = createMockSubscription(USER_ID, SEARCH_VALUE);
        mockSubscriptionList = createMockSubscriptionList();
        findableSubscription = findableSubscription();

        lenient().when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(mockSubscriptionList);
        lenient().when(subscriptionRepository.findByUserId(USER_ID_NO_SUBS)).thenReturn(new ArrayList<>());
        lenient().when(dataManagementService.getHearingByCaseId(any())).thenReturn(new Hearing());
        lenient().when(dataManagementService.getHearingByUrn(any())).thenReturn(new Hearing());
        lenient().when(dataManagementService.getCourt(any())).thenReturn(new Court());
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
        assertEquals(new ArrayList<UserSubscription>(), subscriptionService.findByUserId(USER_ID_NO_SUBS),
                     "Should return empty user subscriptions");
    }

    @Test
    void testUserSubscriptionsCaseId() {
        mockSubscription.setSearchType(SearchType.CASE_ID);
        mockSubscription.setSearchValue(CASE_ID);
        when(subscriptionRepository.findByUserId(USER_ID))
            .thenReturn(new ArrayList<>(Collections.singleton(mockSubscription)));

        List<UserSubscription> userSubscriptions = subscriptionService.findByUserId(USER_ID);

        assertEquals(1, userSubscriptions.size(), VALIDATION_SINGLE_USER_SUBSCRIPTION);
        UserSubscription userSubscription = userSubscriptions.get(0);
        assertEquals(mockSubscription.getSearchType(), userSubscription.getSearchType(),
                     VALIDATION_SEARCH_TYPE);
        assertEquals(mockSubscription.getSearchValue(), userSubscription.getSearchValue(),
                     VALIDATION_SEARCH_VALUE);
        assertEquals(1, userSubscription.getCaseSubscriptions().size(),
                     "Should populate 1 case");
    }

    @Test
    void testUserSubscriptionsCaseUrn() {
        mockSubscription.setSearchType(SearchType.CASE_URN);
        mockSubscription.setSearchValue(URN);
        when(subscriptionRepository.findByUserId(USER_ID))
            .thenReturn(new ArrayList<>(Collections.singleton(mockSubscription)));

        List<UserSubscription> userSubscriptions = subscriptionService.findByUserId(USER_ID);

        assertEquals(1, userSubscriptions.size(), VALIDATION_SINGLE_USER_SUBSCRIPTION);
        UserSubscription userSubscription = userSubscriptions.get(0);
        assertEquals(mockSubscription.getSearchType(), userSubscription.getSearchType(),
                     VALIDATION_SEARCH_TYPE);
        assertEquals(mockSubscription.getSearchValue(), userSubscription.getSearchValue(),
                     VALIDATION_SEARCH_VALUE);
        assertEquals(1, userSubscription.getCaseSubscriptions().size(),
                     "Should populate 1 case");
    }

    @Test
    void testUserSubscriptionsCourt() {
        mockSubscription.setSearchType(SearchType.COURT_ID);
        mockSubscription.setSearchValue(COURT_ID);
        when(subscriptionRepository.findByUserId(USER_ID))
            .thenReturn(new ArrayList<>(Collections.singleton(mockSubscription)));

        List<UserSubscription> userSubscriptions = subscriptionService.findByUserId(USER_ID);

        assertEquals(1, userSubscriptions.size(), VALIDATION_SINGLE_USER_SUBSCRIPTION);
        UserSubscription userSubscription = userSubscriptions.get(0);
        assertEquals(mockSubscription.getSearchType(), userSubscription.getSearchType(),
                     VALIDATION_SEARCH_TYPE);
        assertEquals(mockSubscription.getSearchValue(), userSubscription.getSearchValue(),
                     VALIDATION_SEARCH_VALUE);

        assertEquals(1, userSubscription.getCourtSubscriptions().size(),
                     "Should populate 1 court");
    }

    @Test
    void testUserSubscriptions() {

        List<UserSubscription> userSubscriptions = subscriptionService.findByUserId(USER_ID);

        assertEquals(mockSubscriptionList.size(), userSubscriptions.size(),
                     "Number of returned subscriptions does not match mock subscriptions");

        for (UserSubscription userSubscription : userSubscriptions) {
            if (userSubscription.getSearchType().equals(SearchType.COURT_ID)) {
                assertEquals(1, userSubscription.getCourtSubscriptions().size(),
                             "Does not contain expected number of courts");
                assertEquals(0, userSubscription.getCaseSubscriptions().size(),
                             "Does not contain expected number of cases");
            } else {
                assertEquals(0, userSubscription.getCourtSubscriptions().size(),
                             "Does not contain expected number of courts");
                assertEquals(1, userSubscription.getCaseSubscriptions().size(),
                             "Does not contain expected number of cases");
            }
        }
    }
}

