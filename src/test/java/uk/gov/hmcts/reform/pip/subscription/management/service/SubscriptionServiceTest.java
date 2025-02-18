package uk.gov.hmcts.reform.pip.subscription.management.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.model.report.AllSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.report.LocationSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionListType;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionListTypeRepository;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.model.publication.ListType.CIVIL_DAILY_CAUSE_LIST;
import static uk.gov.hmcts.reform.pip.subscription.management.helpers.SubscriptionUtils.createMockSubscription;
import static uk.gov.hmcts.reform.pip.subscription.management.helpers.SubscriptionUtils.createMockSubscriptionList;
import static uk.gov.hmcts.reform.pip.subscription.management.helpers.SubscriptionUtils.findableSubscription;

@ActiveProfiles("non-async")
@ExtendWith({MockitoExtension.class})
class SubscriptionServiceTest {
    private static final String USER_ID = "Ralph21";
    private static final String SEARCH_VALUE = "193254";
    private static final Channel EMAIL = Channel.EMAIL;
    private static final String ACTIONING_USER_ID = "1234-1234";

    private static final String COURT_NAME = "test court name";
    private static final LocalDateTime DATE_ADDED = LocalDateTime.now();

    private static final String SUBSCRIPTION_CREATED_ERROR = "The returned subscription does "
        + "not match the expected subscription";

    private List<Subscription> mockSubscriptionList;
    private Subscription mockSubscription;
    private Subscription findableSubscription;
    private SubscriptionListType mockSubscriptionListType;

    @Mock
    DataManagementService dataManagementService;

    @Mock
    SubscriptionLocationService subscriptionLocationService;

    @Mock
    SubscriptionRepository subscriptionRepository;

    @Mock
    SubscriptionListTypeRepository subscriptionListTypeRepository;

    @InjectMocks
    SubscriptionService subscriptionService;

    @BeforeEach
    void setup() {
        mockSubscription = createMockSubscription(USER_ID, SEARCH_VALUE, EMAIL, DATE_ADDED);
        mockSubscriptionList = createMockSubscriptionList(DATE_ADDED);
        findableSubscription = findableSubscription();
        mockSubscription.setChannel(Channel.EMAIL);
        mockSubscriptionListType = new SubscriptionListType(USER_ID,
            List.of(CIVIL_DAILY_CAUSE_LIST.name()), List.of("ENGLISH"));
    }

    @Test
    void testGetSubscriptionReturnsExpected() {
        when(subscriptionRepository.findAll()).thenReturn(mockSubscriptionList);
        assertEquals(mockSubscriptionList, subscriptionService.findAll(), "The returned subscription list "
            + "does not match the expected list");
    }

    @Test
    void testCreateSubscription() {
        mockSubscription.setSearchType(SearchType.CASE_ID);
        when(subscriptionRepository.save(mockSubscription)).thenReturn(mockSubscription);
        assertEquals(subscriptionService.createSubscription(mockSubscription, ACTIONING_USER_ID), mockSubscription,
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testLastUpdatedDateIsSet() {
        ArgumentCaptor<Subscription> argumentCaptor = ArgumentCaptor.forClass(Subscription.class);

        mockSubscription.setSearchType(SearchType.CASE_ID);
        when(subscriptionRepository.save(argumentCaptor.capture())).thenReturn(mockSubscription);

        subscriptionService.createSubscription(mockSubscription, ACTIONING_USER_ID);

        Subscription subscription = argumentCaptor.getValue();

        assertEquals(subscription.getCreatedDate(), subscription.getLastUpdatedDate(),
                     "Last updated date should be equal to created date"
        );
    }

    @Test
    void testCreateSubscriptionWithCourtName() {
        mockSubscription.setSearchType(SearchType.LOCATION_ID);
        when(dataManagementService.getCourtName(SEARCH_VALUE)).thenReturn(COURT_NAME);
        when(subscriptionRepository.save(mockSubscription)).thenReturn(mockSubscription);
        assertEquals(subscriptionService.createSubscription(mockSubscription, ACTIONING_USER_ID), mockSubscription,
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testCreateSubscriptionWithCourtNameWithoutListType() {
        mockSubscription.setSearchType(SearchType.LOCATION_ID);
        when(dataManagementService.getCourtName(SEARCH_VALUE)).thenReturn(COURT_NAME);
        when(subscriptionRepository.save(mockSubscription)).thenReturn(mockSubscription);
        assertEquals(subscriptionService.createSubscription(mockSubscription, ACTIONING_USER_ID), mockSubscription,
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testCreateSubscriptionWithCourtNameWithMultipleListType() {
        mockSubscription.setSearchType(SearchType.LOCATION_ID);
        when(dataManagementService.getCourtName(SEARCH_VALUE)).thenReturn(COURT_NAME);
        when(subscriptionRepository.save(mockSubscription)).thenReturn(mockSubscription);
        assertEquals(subscriptionService.createSubscription(mockSubscription, ACTIONING_USER_ID), mockSubscription,
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testCreateDuplicateSubscription() {
        mockSubscription.setSearchType(SearchType.LOCATION_ID);
        mockSubscription.setSearchValue(SEARCH_VALUE);
        when(dataManagementService.getCourtName(SEARCH_VALUE)).thenReturn(COURT_NAME);
        when(subscriptionRepository.save(mockSubscription)).thenReturn(mockSubscription);
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of(mockSubscription));

        Subscription returnedSubscription =
            subscriptionService.createSubscription(mockSubscription, ACTIONING_USER_ID);

        verify(subscriptionRepository, times(1)).delete(mockSubscription);
        assertEquals(returnedSubscription, mockSubscription,
                     "The Returned subscription does match the expected subscription"
        );
    }

    @Test
    void testConfigureListTypesForLocationSubscription() {
        subscriptionService.configureListTypesForSubscription(mockSubscriptionListType, USER_ID);

        assertEquals(USER_ID, mockSubscription.getUserId(),
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testConfigureEmptyListTypesForLocationSubscription() {
        mockSubscriptionListType.setListType(new ArrayList<>());
        subscriptionService.configureListTypesForSubscription(mockSubscriptionListType, USER_ID);

        assertEquals(USER_ID, mockSubscription.getUserId(),
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testAddListTypesForLocationSubscription() {
        when(subscriptionListTypeRepository.save(mockSubscriptionListType))
            .thenReturn(mockSubscriptionListType);
        subscriptionService.addListTypesForSubscription(mockSubscriptionListType, USER_ID);

        assertEquals(USER_ID, mockSubscription.getUserId(),
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testDeleteSubscription() {
        UUID testUuid = UUID.randomUUID();
        ArgumentCaptor<UUID> captor = ArgumentCaptor.forClass(UUID.class);
        doNothing().when(subscriptionRepository).deleteById(captor.capture());
        when(subscriptionRepository.findById(testUuid)).thenReturn(Optional.of(findableSubscription));
        doNothing().when(subscriptionLocationService).deleteSubscriptionListTypeByUser(any());
        subscriptionService.deleteById(testUuid, ACTIONING_USER_ID);
        assertEquals(testUuid, captor.getValue(), "The service layer tried to delete the wrong subscription");
    }

    @Test
    void testDeleteException() {
        UUID testUuid = UUID.randomUUID();
        when(subscriptionRepository.findById(testUuid)).thenReturn(Optional.empty());
        assertThrows(SubscriptionNotFoundException.class, () -> subscriptionService.deleteById(
                         testUuid, ACTIONING_USER_ID),
                     "SubscriptionNotFoundException not thrown when trying to delete a subscription"
                         + " that does not exist"
        );
    }

    @Test
    void testFindException() {
        UUID testUuid = UUID.randomUUID();
        when(subscriptionRepository.findById(testUuid)).thenReturn(Optional.empty());
        assertThrows(SubscriptionNotFoundException.class, () -> subscriptionService.findById(testUuid),
                     "SubscriptionNotFoundException not thrown "
                         + "when trying to find a subscription that does not exist"
        );
    }

    @Test
    void testFindSubscription() {
        UUID testUuid = UUID.randomUUID();
        when(subscriptionRepository.findById(testUuid)).thenReturn(Optional.of(findableSubscription));
        assertEquals(subscriptionService.findById(testUuid), findableSubscription,
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testMiServiceLocationV2() {
        LocationSubscriptionMiData locationSubscriptionMiData = new LocationSubscriptionMiData();
        locationSubscriptionMiData.setId(UUID.randomUUID());

        when(subscriptionRepository.getLocationSubsDataForMiV2())
            .thenReturn(List.of(locationSubscriptionMiData));

        List<LocationSubscriptionMiData> locationSubscriptionsMiDataList = subscriptionService
            .getLocationSubscriptionsDataForMiReportingV2();

        assertThat(locationSubscriptionsMiDataList).contains(locationSubscriptionMiData);
    }

    @Test
    void testMiServiceAllV2() {
        AllSubscriptionMiData allSubscriptionMiData = new AllSubscriptionMiData();
        allSubscriptionMiData.setId(UUID.randomUUID());

        when(subscriptionRepository.getAllSubsDataForMiV2())
            .thenReturn(List.of(allSubscriptionMiData));

        List<AllSubscriptionMiData> allSubscriptionsMiDataList = subscriptionService
            .getAllSubscriptionsDataForMiReportingV2();

        assertThat(allSubscriptionsMiDataList).contains(allSubscriptionMiData);
    }
}

