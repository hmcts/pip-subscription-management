package uk.gov.hmcts.reform.pip.subscription.management.service;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.subscription.management.models.Channel;
import uk.gov.hmcts.reform.pip.subscription.management.models.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionsSummary;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionsSummaryDetails;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Artefact;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.ListType;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Sensitivity;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.publication.services.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.publication.services.ThirdPartySubscriptionArtefact;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.CaseSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.LocationSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.UserSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.subscription.management.helpers.SubscriptionUtils.createMockSubscription;
import static uk.gov.hmcts.reform.pip.subscription.management.helpers.SubscriptionUtils.createMockSubscriptionList;
import static uk.gov.hmcts.reform.pip.subscription.management.helpers.SubscriptionUtils.findableSubscription;

@ActiveProfiles("non-async")
@ExtendWith({MockitoExtension.class})
@SuppressWarnings({"PMD.LawOfDemeter", "PMD.TooManyFields", "PMD.ExcessiveImports"})
class SubscriptionServiceTest {
    private static final String USER_ID = "Ralph21";
    private static final String USER_ID_NO_SUBS = "Tina21";
    private static final String SEARCH_VALUE = "193254";
    private static final String CASE_ID = "123";
    private static final String URN = "312";
    private static final String CASE_NAME = "case-name";
    private static final Channel EMAIL = Channel.EMAIL;
    private static final String COURT_MATCH = "1";
    private static final String CASE_MATCH = "case match";
    private static final String ACCEPTED_USER_ID = "2";
    private static final String FORBIDDEN_USER_ID = "3";
    private static final String SUBSCRIBER_NOTIFICATION_LOG = "Summary being sent to publication services for id";
    private static final String LOG_MESSAGE_MATCH = "Log messages should match.";
    private static final String CASE_NUMBER_KEY = "caseNumber";
    private static final String CASE_URN_KEY = "caseUrn";
    private static final UUID TEST_UUID = UUID.randomUUID();
    private static final String TEST_USER_EMAIL = "a@b.com";
    private static final String SUCCESS = "Success";
    private static final String TEST = "test";
    private static final String LOCATION_ID = "1";
    public static final List<String> EXAMPLE_CSV_ALL = List.of(
        "a01d52c0-5c95-4f75-8994-a1c42cb45aaa,EMAIL,CASE_ID,2fe899ff-96ed-435a-bcad-1411bbe96d2a,string",
        "370963e2-9d2f-423e-b6a1-3f1f8905cdf0,EMAIL,CASE_ID,2fe899ff-96ed-435a-bcad-1411bbe96d2a,1234",
        "052cda55-30fd-4a0d-939a-2c7b03ab3392,EMAIL,CASE_ID,2fe899ff-96ed-435a-bcad-1411bbe96d2a,1234"
        );
    public static final List<String> EXAMPLE_CSV_LOCAL = List.of(
        "212c8b34-f6c3-424d-90e2-f874f528eebf,2,EMAIL,2fe899ff-96ed-435a-bcad-1411bbe96d2a,null",
        "f4a0cb33-f211-4b46-8bdb-6320f6382a29,1234,API,2fe899ff-96ed-435a-bcad-1411bbe96d2a,null",
        "34edfcde-4546-46b8-98e6-2717da3185e8,3,API,2fe899ff-96ed-435a-bcad-1411bbe96d2a,Oxford Combined Court Centre");

    private static final String COURT_NAME = "test court name";

    private static final String SUBSCRIPTION_CREATED_ERROR = "The returned subscription does "
        + "not match the expected subscription";

    private List<Subscription> mockSubscriptionList;
    private Subscription mockSubscription;
    private final SubscriptionsSummary mockSubscriptionsSummary = new SubscriptionsSummary();
    private final SubscriptionsSummaryDetails mockSubscriptionsSummaryDetails = new SubscriptionsSummaryDetails();
    private Subscription findableSubscription;
    private LocalDateTime dateAdded;

    private final Artefact classifiedArtefactMatches = new Artefact();
    private final Artefact publicArtefactMatches = new Artefact();
    private final Subscription returnedSubscription = new Subscription();
    private final Subscription restrictedSubscription = new Subscription();
    private final List<Object> cases = new ArrayList<>();
    private final Map<String, List<Object>> searchTerms = new ConcurrentHashMap<>();
    private final Map<String, List<Subscription>> returnedMappedEmails = new ConcurrentHashMap<>();

    @Captor
    private ArgumentCaptor<ArrayList<UUID>> listCaptor;

    @Mock
    DataManagementService dataManagementService;

    @Mock
    PublicationServicesService publicationServicesService;

    @Mock
    SubscriptionRepository subscriptionRepository;

    @Mock
    AccountManagementService accountManagementService;

    @Mock
    ChannelManagementService channelManagementService;

    @InjectMocks
    SubscriptionServiceImpl subscriptionService;

    @BeforeEach
    void setup() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        LinkedHashMap<String, String> map2 = new LinkedHashMap<>();
        map.put(CASE_NUMBER_KEY, CASE_MATCH);
        map.put(CASE_URN_KEY, TEST);
        map2.put(CASE_NUMBER_KEY, TEST);
        map2.put(CASE_URN_KEY, CASE_MATCH);

        cases.add(map);
        cases.add(map2);

        searchTerms.put("cases", cases);
        classifiedArtefactMatches.setArtefactId(TEST_UUID);
        classifiedArtefactMatches.setSensitivity(Sensitivity.CLASSIFIED);
        classifiedArtefactMatches.setSearch(searchTerms);
        classifiedArtefactMatches.setLocationId(COURT_MATCH);
        classifiedArtefactMatches.setListType(ListType.SJP_PRESS_LIST);

        publicArtefactMatches.setArtefactId(TEST_UUID);
        publicArtefactMatches.setSensitivity(Sensitivity.PUBLIC);
        publicArtefactMatches.setLocationId(COURT_MATCH);
        publicArtefactMatches.setSearch(searchTerms);
        publicArtefactMatches.setListType(ListType.MAGISTRATES_PUBLIC_LIST);

        returnedSubscription.setUserId(ACCEPTED_USER_ID);
        restrictedSubscription.setUserId(FORBIDDEN_USER_ID);

        dateAdded = LocalDateTime.now();
        mockSubscription = createMockSubscription(USER_ID, SEARCH_VALUE, EMAIL, dateAdded,
                                                  ListType.CIVIL_DAILY_CAUSE_LIST);
        mockSubscriptionList = createMockSubscriptionList(dateAdded);
        findableSubscription = findableSubscription();

        mockSubscriptionsSummary.setEmail(TEST_USER_EMAIL);
        mockSubscriptionsSummary.setArtefactId(TEST_UUID);
        mockSubscription.setChannel(Channel.EMAIL);

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
        mockSubscription.setSearchType(SearchType.CASE_ID);
        when(subscriptionRepository.save(mockSubscription)).thenReturn(mockSubscription);
        assertEquals(subscriptionService.createSubscription(mockSubscription), mockSubscription,
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testLastUpdatedDateIsSet() {
        ArgumentCaptor<Subscription> argumentCaptor = ArgumentCaptor.forClass(Subscription.class);

        mockSubscription.setSearchType(SearchType.CASE_ID);
        when(subscriptionRepository.save(argumentCaptor.capture())).thenReturn(mockSubscription);

        subscriptionService.createSubscription(mockSubscription);

        Subscription subscription = argumentCaptor.getValue();

        assertEquals(subscription.getCreatedDate(), subscription.getLastUpdatedDate(),
                     "Last updated date should be equal to created date");
    }

    @Test
    void testCreateSubscriptionWithCourtName() {
        mockSubscription.setSearchType(SearchType.LOCATION_ID);
        when(dataManagementService.getCourtName(SEARCH_VALUE)).thenReturn(COURT_NAME);
        when(subscriptionRepository.save(mockSubscription)).thenReturn(mockSubscription);
        assertEquals(subscriptionService.createSubscription(mockSubscription), mockSubscription,
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testCreateSubscriptionWithCourtNameWithoutListType() {
        mockSubscription.setSearchType(SearchType.LOCATION_ID);
        mockSubscription.setListType(null);
        when(dataManagementService.getCourtName(SEARCH_VALUE)).thenReturn(COURT_NAME);
        when(subscriptionRepository.save(mockSubscription)).thenReturn(mockSubscription);
        assertEquals(subscriptionService.createSubscription(mockSubscription), mockSubscription,
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testCreateSubscriptionWithCourtNameWithMultipleListType() {
        mockSubscription.setSearchType(SearchType.LOCATION_ID);
        mockSubscription.setListType(List.of(ListType.CIVIL_DAILY_CAUSE_LIST.toString(),
                                             ListType.CIVIL_AND_FAMILY_DAILY_CAUSE_LIST.toString()));
        when(dataManagementService.getCourtName(SEARCH_VALUE)).thenReturn(COURT_NAME);
        when(subscriptionRepository.save(mockSubscription)).thenReturn(mockSubscription);
        assertEquals(subscriptionService.createSubscription(mockSubscription), mockSubscription,
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
            subscriptionService.createSubscription(mockSubscription);

        verify(subscriptionRepository, times(1)).delete(mockSubscription);
        assertEquals(returnedSubscription, mockSubscription,
                     "The Returned subscription does match the expected subscription");
    }

    @Test
    void testConfigureListTypesForLocationSubscription() {
        doNothing().when(subscriptionRepository).updateLocationSubscriptions(any(), any());
        subscriptionService.configureListTypesForSubscription(USER_ID, List.of(ListType.CIVIL_DAILY_CAUSE_LIST.name()));

        assertEquals(mockSubscription.getUserId(), USER_ID,
                     SUBSCRIPTION_CREATED_ERROR);
    }

    @Test
    void testConfigureEmptyListTypesForLocationSubscription() {
        doNothing().when(subscriptionRepository).updateLocationSubscriptions(USER_ID, "");
        subscriptionService.configureListTypesForSubscription(USER_ID, null);

        assertEquals(mockSubscription.getUserId(), USER_ID,
                     SUBSCRIPTION_CREATED_ERROR);
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
    void testBulkDeleteSubscriptionsSuccess() {
        UUID testId1 = UUID.randomUUID();
        UUID testId2 = UUID.randomUUID();

        Subscription subscription1 = new Subscription();
        subscription1.setId(testId1);
        Subscription subscription2 = new Subscription();
        subscription2.setId(testId2);

        List<UUID> testIds = List.of(testId1, testId2);
        List<Subscription> subscriptions = List.of(subscription1, subscription2);

        doNothing().when(subscriptionRepository).deleteByIdIn(listCaptor.capture());
        when(subscriptionRepository.findByIdIn(testIds)).thenReturn(subscriptions);
        subscriptionService.bulkDeleteSubscriptions(testIds);
        assertThat(listCaptor.getValue())
            .as("Subscription IDs to delete do not match")
            .isEqualTo(testIds);
    }

    @Test
    void testBulkDeleteSubscriptionsException() {
        UUID testId1 = UUID.randomUUID();
        UUID testId2 = UUID.randomUUID();
        List<UUID> testIds = List.of(testId1, testId2);

        Subscription subscription = new Subscription();
        subscription.setId(testId2);

        when(subscriptionRepository.findByIdIn(testIds)).thenReturn(List.of(subscription));
        assertThatThrownBy(() -> subscriptionService.bulkDeleteSubscriptions(testIds))
            .as("Exception does not match")
            .isInstanceOf(SubscriptionNotFoundException.class)
            .hasMessage("No subscription found with the subscription ID(s): " + testId1);
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
                     SUBSCRIPTION_CREATED_ERROR);
    }

    @Test
    void testNoSubscriptionsReturnsEmpty() {
        assertEquals(new UserSubscription(), subscriptionService.findByUserId(USER_ID_NO_SUBS),
                     "Should return empty user subscriptions");
    }

    @Test
    void testFindByUserIdOnlyCourt() {
        mockSubscription.setSearchType(SearchType.LOCATION_ID);
        mockSubscription.setLocationName("Test court");
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of(mockSubscription));
        LocationSubscription expected = new LocationSubscription();
        expected.setSubscriptionId(mockSubscription.getId());
        expected.setLocationName("Test court");
        expected.setLocationId("193254");
        expected.setListType(List.of(ListType.CIVIL_DAILY_CAUSE_LIST.name()));
        expected.setDateAdded(dateAdded);

        UserSubscription result = subscriptionService.findByUserId(USER_ID);

        assertEquals(List.of(expected), result.getLocationSubscriptions(),
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
        expected.setSubscriptionId(mockSubscription.getId());
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
        assertEquals(2, result.getLocationSubscriptions().size(), "Should add all court names");
    }

    @Test
    void testFindByUserId() {
        UserSubscription result = subscriptionService.findByUserId(USER_ID);
        for (int i = 0; i < 6; i++) {
            assertEquals(CASE_ID + i, result.getCaseSubscriptions().get(i).getCaseNumber(),
                         "Should contain correct caseNumber");
        }
        assertEquals(COURT_NAME, result.getLocationSubscriptions().get(0).getLocationName(),
                     "Should match court name");
    }

    @Test
    void testFindByUserIdCreatedDates() {
        UserSubscription result = subscriptionService.findByUserId(USER_ID);
        for (int i = 0; i < 6; i++) {
            assertEquals(dateAdded, result.getCaseSubscriptions().get(i).getDateAdded(),
                         "Should match dateAdded");
        }
        assertEquals(dateAdded, result.getLocationSubscriptions().get(0).getDateAdded(), "Should match dateAdded");
    }

    @Test
    void testFindByUserIdAssignsIdForCourt() {
        mockSubscription.setSearchType(SearchType.LOCATION_ID);
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of(mockSubscription));

        assertEquals(mockSubscription.getId(),
                     subscriptionService.findByUserId(USER_ID).getLocationSubscriptions().get(0).getSubscriptionId(),
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

    @Test
    void testCollectSubscribersCourtSubscriptionNotClassified() throws IOException {
        returnedSubscription.setChannel(Channel.EMAIL);
        returnedMappedEmails.put(TEST_USER_EMAIL, List.of(returnedSubscription));
        when(subscriptionRepository.findSubscriptionsByLocationSearchValue(SearchType.LOCATION_ID.toString(),
            COURT_MATCH, ListType.MAGISTRATES_PUBLIC_LIST.name()))
                .thenReturn(List.of(returnedSubscription));
        when(channelManagementService.getMappedEmails(any())).thenReturn(returnedMappedEmails);
        when(publicationServicesService.postSubscriptionSummaries(any(), any(), any())).thenReturn(SUCCESS);
        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionServiceImpl.class)) {
            subscriptionService.collectSubscribers(publicArtefactMatches);
            assertTrue(logCaptor.getInfoLogs().get(0).contains(SUBSCRIBER_NOTIFICATION_LOG),
                       LOG_MESSAGE_MATCH);
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    @Test
    void testCollectSubscribersCourtSubscriptionWithListTypeNotClassified() throws IOException {
        returnedSubscription.setChannel(Channel.EMAIL);
        returnedSubscription.setListType(List.of(ListType.MAGISTRATES_PUBLIC_LIST.toString(),
                                             ListType.CIVIL_DAILY_CAUSE_LIST.toString()));
        returnedMappedEmails.put(TEST_USER_EMAIL, List.of(returnedSubscription));
        when(subscriptionRepository.findSubscriptionsByLocationSearchValue(SearchType.LOCATION_ID.toString(),
            COURT_MATCH, ListType.MAGISTRATES_PUBLIC_LIST.name()))
                .thenReturn(List.of(returnedSubscription));
        when(channelManagementService.getMappedEmails(any())).thenReturn(returnedMappedEmails);
        when(publicationServicesService.postSubscriptionSummaries(any(), any(), any())).thenReturn(SUCCESS);
        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionServiceImpl.class)) {
            subscriptionService.collectSubscribers(publicArtefactMatches);
            assertTrue(logCaptor.getInfoLogs().get(0).contains(SUBSCRIBER_NOTIFICATION_LOG),
                       LOG_MESSAGE_MATCH);
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    @Test
    void testCollectSubscribersCaseUrn() {
        mockSubscription.setSearchType(SearchType.CASE_URN);
        mockSubscription.setSearchValue(CASE_URN_KEY);

        mockSubscriptionsSummaryDetails.addToCaseUrn(CASE_URN_KEY);
        mockSubscriptionsSummary.setSubscriptions(mockSubscriptionsSummaryDetails);

        when(subscriptionRepository.findSubscriptionsByLocationSearchValue(SearchType.LOCATION_ID.toString(),
                                                                   COURT_MATCH,
                                                                   ListType.MAGISTRATES_PUBLIC_LIST.name()))
            .thenReturn(List.of(mockSubscription));

        returnedMappedEmails.put(TEST_USER_EMAIL, List.of(mockSubscription));

        when(channelManagementService.getMappedEmails(List.of(mockSubscription))).thenReturn(returnedMappedEmails);

        when(publicationServicesService.postSubscriptionSummaries(publicArtefactMatches.getArtefactId(),
                                                                               TEST_USER_EMAIL,
                                                                  List.of(mockSubscription))).thenReturn(SUCCESS);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionServiceImpl.class)) {
            subscriptionService.collectSubscribers(publicArtefactMatches);

            assertTrue(logCaptor.getInfoLogs().get(0).contains(SUBSCRIBER_NOTIFICATION_LOG),
                       LOG_MESSAGE_MATCH);
        }
    }

    @Test
    void testCollectSubscribersCaseId() {
        mockSubscription.setSearchType(SearchType.CASE_ID);
        mockSubscription.setSearchValue(CASE_ID);

        mockSubscriptionsSummaryDetails.addToCaseNumber(CASE_ID);
        mockSubscriptionsSummary.setSubscriptions(mockSubscriptionsSummaryDetails);

        when(subscriptionRepository.findSubscriptionsByLocationSearchValue(SearchType.LOCATION_ID.toString(),
                                                                   COURT_MATCH,
                                                                   ListType.MAGISTRATES_PUBLIC_LIST.name()))
            .thenReturn(List.of(mockSubscription));

        returnedMappedEmails.put(TEST_USER_EMAIL, List.of(mockSubscription));

        when(channelManagementService.getMappedEmails(List.of(mockSubscription))).thenReturn(returnedMappedEmails);

        when(publicationServicesService.postSubscriptionSummaries(publicArtefactMatches.getArtefactId(),
                                                                               TEST_USER_EMAIL,
                                                                  List.of(mockSubscription))).thenReturn(SUCCESS);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionServiceImpl.class)) {
            subscriptionService.collectSubscribers(publicArtefactMatches);

            assertTrue(logCaptor.getInfoLogs().get(0).contains(SUBSCRIBER_NOTIFICATION_LOG),
                       LOG_MESSAGE_MATCH);
        }
    }

    @Test
    void testCollectSubscribersLocationId() {
        mockSubscription.setSearchType(SearchType.LOCATION_ID);
        mockSubscription.setSearchValue(COURT_MATCH);

        mockSubscriptionsSummaryDetails.addToLocationId(COURT_MATCH);
        mockSubscriptionsSummary.setSubscriptions(mockSubscriptionsSummaryDetails);

        when(subscriptionRepository.findSubscriptionsByLocationSearchValue(SearchType.LOCATION_ID.toString(),
                                                                   COURT_MATCH,
                                                                   ListType.MAGISTRATES_PUBLIC_LIST.name()))
            .thenReturn(List.of(mockSubscription));

        returnedMappedEmails.put(TEST_USER_EMAIL, List.of(mockSubscription));

        when(channelManagementService.getMappedEmails(List.of(mockSubscription))).thenReturn(returnedMappedEmails);

        when(publicationServicesService.postSubscriptionSummaries(publicArtefactMatches.getArtefactId(),
                                                                               TEST_USER_EMAIL,
                                                                  List.of(mockSubscription))).thenReturn(SUCCESS);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionServiceImpl.class)) {
            subscriptionService.collectSubscribers(publicArtefactMatches);

            assertTrue(logCaptor.getInfoLogs().get(0).contains(SUBSCRIBER_NOTIFICATION_LOG),
                       LOG_MESSAGE_MATCH);
        }
    }

    @Test
    void testCollectListTypeSubscription() throws IOException {
        mockSubscription.setSearchType(SearchType.LIST_TYPE);
        mockSubscription.setSearchValue(ListType.MAGISTRATES_PUBLIC_LIST.toString());
        returnedMappedEmails.put(TEST_USER_EMAIL, List.of(mockSubscription));
        lenient().when(subscriptionRepository.findSubscriptionsBySearchValue(SearchType.LIST_TYPE.toString(),
                                                                   ListType.MAGISTRATES_PUBLIC_LIST.toString()))
            .thenReturn(List.of(mockSubscription));
        when(channelManagementService.getMappedEmails(List.of(mockSubscription))).thenReturn(returnedMappedEmails);
        lenient().when(publicationServicesService.postSubscriptionSummaries(any(), any(), any())).thenReturn(SUCCESS);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionServiceImpl.class)) {
            subscriptionService.collectSubscribers(publicArtefactMatches);
            assertTrue(logCaptor.getInfoLogs().get(0).contains(SUBSCRIBER_NOTIFICATION_LOG),
                       LOG_MESSAGE_MATCH);
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    @Test
    void testCollectApiSubscribers() throws IOException {
        mockSubscription.setChannel(Channel.API_COURTEL);
        Map<String, List<Subscription>> returnedMap = new ConcurrentHashMap<>();
        returnedMap.put(TEST, List.of(mockSubscription));
        ThirdPartySubscription thirdPartySubscription = new ThirdPartySubscription(TEST, TEST_UUID);
        when(subscriptionRepository.findSubscriptionsByLocationSearchValue(SearchType.LOCATION_ID.toString(),
                                                                   COURT_MATCH,
                                                                   ListType.MAGISTRATES_PUBLIC_LIST.name()))
            .thenReturn(List.of(mockSubscription));
        when(channelManagementService.getMappedApis(List.of(mockSubscription))).thenReturn(returnedMap);
        when(publicationServicesService.sendThirdPartyList(thirdPartySubscription)).thenReturn(SUCCESS);
        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionServiceImpl.class)) {
            subscriptionService.collectSubscribers(publicArtefactMatches);
            assertTrue(logCaptor.getInfoLogs().get(0).contains(SUCCESS),
                       LOG_MESSAGE_MATCH);
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    @Test
    void testCollectSubscribersRestrictsClassified() {
        returnedSubscription.setChannel(Channel.EMAIL);
        returnedSubscription.setUserId(ACCEPTED_USER_ID);
        returnedSubscription.setSearchType(SearchType.CASE_ID);
        returnedSubscription.setSearchValue(CASE_ID);
        restrictedSubscription.setChannel(Channel.EMAIL);
        restrictedSubscription.setUserId(FORBIDDEN_USER_ID);
        returnedSubscription.setSearchType(SearchType.CASE_ID);
        returnedSubscription.setSearchValue(CASE_ID);

        mockSubscriptionsSummary.setArtefactId(classifiedArtefactMatches.getArtefactId());
        mockSubscriptionsSummaryDetails.addToCaseNumber(CASE_ID);
        mockSubscriptionsSummary.setSubscriptions(mockSubscriptionsSummaryDetails);

        lenient().when(subscriptionRepository.findSubscriptionsBySearchValue(SearchType.CASE_ID.name(),
                                                                             CASE_MATCH))
            .thenReturn(List.of(returnedSubscription, restrictedSubscription));

        when(accountManagementService.isUserAuthorised(
            ACCEPTED_USER_ID, ListType.SJP_PRESS_LIST, Sensitivity.CLASSIFIED)).thenReturn(true);
        when(accountManagementService.isUserAuthorised(
            FORBIDDEN_USER_ID, ListType.SJP_PRESS_LIST, Sensitivity.CLASSIFIED))
            .thenReturn(false);

        returnedMappedEmails.put(TEST_USER_EMAIL, List.of(returnedSubscription));
        when(channelManagementService.getMappedEmails(List.of(returnedSubscription)))
            .thenReturn(returnedMappedEmails);

        when(publicationServicesService.postSubscriptionSummaries(publicArtefactMatches.getArtefactId(),
                                                                  TEST_USER_EMAIL,
                                                                  List.of(returnedSubscription))).thenReturn(SUCCESS);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionServiceImpl.class)) {
            subscriptionService.collectSubscribers(classifiedArtefactMatches);

            assertTrue(logCaptor.getInfoLogs().get(0).contains(SUBSCRIBER_NOTIFICATION_LOG), LOG_MESSAGE_MATCH);
        }
    }

    @Test
    void testCollectThirdPartyForDeletion() {
        mockSubscription.setChannel(Channel.API_COURTEL);
        Map<String, List<Subscription>> returnedMap = Collections.singletonMap(TEST, List.of(mockSubscription));
        when(subscriptionRepository.findSubscriptionsBySearchValue(SearchType.LIST_TYPE.toString(),
                                                                   publicArtefactMatches.getListType().name()))
            .thenReturn(List.of(mockSubscription));
        when(channelManagementService.getMappedApis(List.of(mockSubscription))).thenReturn(returnedMap);
        ThirdPartySubscriptionArtefact subscriptionArtefact = new ThirdPartySubscriptionArtefact(
            TEST, publicArtefactMatches);
        when(publicationServicesService.sendEmptyArtefact(subscriptionArtefact))
            .thenReturn(SUBSCRIBER_NOTIFICATION_LOG);
        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionServiceImpl.class)) {
            subscriptionService.collectThirdPartyForDeletion(publicArtefactMatches);
            assertTrue(logCaptor.getInfoLogs().get(0).contains(SUBSCRIBER_NOTIFICATION_LOG),
                       LOG_MESSAGE_MATCH);
        }
    }

    @Test
    void testCollectThirdPartyForDeletionClassifiedExcluded() {
        mockSubscription.setChannel(Channel.API_COURTEL);
        Map<String, List<Subscription>> returnedMap = new ConcurrentHashMap<>();
        returnedMap.put(TEST, List.of(mockSubscription));
        lenient().when(subscriptionRepository.findSubscriptionsBySearchValue(SearchType.LIST_TYPE.toString(),
                                                                   classifiedArtefactMatches.getListType().name()))
            .thenReturn(List.of(mockSubscription));
        when(accountManagementService.isUserAuthorised(mockSubscription.getUserId(),
                                                       classifiedArtefactMatches.getListType(),
                                                       classifiedArtefactMatches.getSensitivity())).thenReturn(false);
        subscriptionService.collectThirdPartyForDeletion(classifiedArtefactMatches);
        ThirdPartySubscriptionArtefact subscriptionArtefact = new ThirdPartySubscriptionArtefact(
            TEST, classifiedArtefactMatches);
        verify(publicationServicesService, never()).sendEmptyArtefact(subscriptionArtefact);
    }

    @Test
    void testMiServiceLocal() {
        when(subscriptionRepository.getLocalSubsDataForMi()).thenReturn(EXAMPLE_CSV_LOCAL);
        String testString = subscriptionService.getLocalSubscriptionsDataForMiReporting();
        String[] splitLineString = testString.split("\r\n|\r|\n");
        long countLine1 = splitLineString[0].chars().filter(character -> character == ',').count();
        assertThat(testString)
            .as("Header row missing")
            .contains("user_id");
        assertThat(testString)
            .as("Json parsing has probably failed")
            .contains("Oxford")
            .hasLineCount(4);
        assertThat(splitLineString)
            .as("Wrong comma count compared to header row!")
            .allSatisfy(
                e -> assertThat(e.chars().filter(character -> character == ',').count()).isEqualTo(countLine1));

    }

    @Test
    void testMiServiceAll() {
        when(subscriptionRepository.getAllSubsDataForMi()).thenReturn(EXAMPLE_CSV_ALL);
        String testString = subscriptionService.getAllSubscriptionsDataForMiReporting();
        String[] splitLineString = testString.split("\r\n|\r|\n");
        long countLine1 = splitLineString[0].chars().filter(character -> character == ',').count();
        assertThat(testString)
            .as("Json parsing has probably failed")
            .contains("CASE_ID")
            .hasLineCount(4);
        assertThat(testString)
            .as("Header row missing")
            .contains("user_id");
        assertThat(splitLineString)
            .as("Wrong comma count compared to header row!")
            .allSatisfy(
                e -> assertThat(e.chars().filter(character -> character == ',').count()).isEqualTo(countLine1));
    }

    @Test
    void testDeleteAllByUserId() {
        String testString = "testId";
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        doNothing().when(subscriptionRepository).deleteAllByUserId(captor.capture());
        subscriptionService.deleteAllByUserId(testString);
        assertEquals(testString, captor.getValue(),
                     "The service layer failed to delete the correct user id subscriptions");
    }

    @Test
    void testFindSubscriptionsByLocationIdReturnsExpected() {
        when(subscriptionRepository.findSubscriptionsByLocationId(LOCATION_ID))
            .thenReturn(mockSubscriptionList);
        assertEquals(mockSubscriptionList, subscriptionService.findSubscriptionsByLocationId(LOCATION_ID),
                     "The returned subscription list does not match the expected list");
    }
}

