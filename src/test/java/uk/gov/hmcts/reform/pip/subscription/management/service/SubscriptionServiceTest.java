package uk.gov.hmcts.reform.pip.subscription.management.service;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.subscription.management.models.Channel;
import uk.gov.hmcts.reform.pip.subscription.management.models.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Artefact;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.ListType;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Sensitivity;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.CaseSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.CourtSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.UserSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.repository.SubscriptionRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.subscription.management.helpers.SubscriptionUtils.createMockSubscription;
import static uk.gov.hmcts.reform.pip.subscription.management.helpers.SubscriptionUtils.createMockSubscriptionList;
import static uk.gov.hmcts.reform.pip.subscription.management.helpers.SubscriptionUtils.findableSubscription;

@ActiveProfiles("non-async")
@ExtendWith({MockitoExtension.class})
@SuppressWarnings("PMD.LawOfDemeter")

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
    private static final String SUBSCRIBER_NOTIFICATION_LOG = "Subscriber list created. Notifying %s subscribers.";
    private static final String LOG_MESSAGE_MATCH = "Log messages should match.";
    private static final String CASE_NUMBER_KEY = "caseNumber";
    private static final String CASE_URN_KEY = "caseUrn";

    private List<Subscription> mockSubscriptionList;
    private Subscription mockSubscription;
    private Subscription findableSubscription;
    private LocalDateTime dateAdded;

    private final Artefact classifiedArtefactMatches = new Artefact();
    private final Artefact publicArtefactMatches = new Artefact();
    private final Subscription returnedSubscription = new Subscription();
    private final Subscription restrictedSubscription = new Subscription();
    private final List<Object> cases = new ArrayList<>();
    private final Map<String, List<Object>> searchTerms = new ConcurrentHashMap<>();

    @Mock
    DataManagementService dataManagementService;

    @Mock
    SubscriptionRepository subscriptionRepository;

    @Mock
    AccountManagementService accountManagementService;

    @InjectMocks
    SubscriptionServiceImpl subscriptionService;

    @BeforeEach
    void setup() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        LinkedHashMap<String, String> map2 = new LinkedHashMap<>();
        map.put(CASE_NUMBER_KEY, CASE_MATCH);
        map.put(CASE_URN_KEY, "test");
        map2.put(CASE_NUMBER_KEY, "test");
        map2.put(CASE_URN_KEY, CASE_MATCH);

        cases.add(map);
        cases.add(map2);

        searchTerms.put("cases", cases);
        classifiedArtefactMatches.setSensitivity(Sensitivity.CLASSIFIED);
        classifiedArtefactMatches.setSearch(searchTerms);
        classifiedArtefactMatches.setCourtId(COURT_MATCH);
        classifiedArtefactMatches.setArtefactId(UUID.randomUUID());
        classifiedArtefactMatches.setListType(ListType.SJP_PRESS_LIST);

        publicArtefactMatches.setSensitivity(Sensitivity.PUBLIC);
        publicArtefactMatches.setCourtId(COURT_MATCH);
        publicArtefactMatches.setSearch(searchTerms);

        returnedSubscription.setUserId(ACCEPTED_USER_ID);
        restrictedSubscription.setUserId(FORBIDDEN_USER_ID);

        dateAdded = LocalDateTime.now();
        mockSubscription = createMockSubscription(USER_ID, SEARCH_VALUE, EMAIL, dateAdded);
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
        mockSubscription.setSearchType(SearchType.CASE_ID);
        when(subscriptionRepository.save(mockSubscription)).thenReturn(mockSubscription);
        assertEquals(subscriptionService.createSubscription(mockSubscription), mockSubscription,
                     "The returned subscription does not match the expected subscription"
        );
    }

    @Test
    void testCreateSubscriptionWithCourtName() {
        mockSubscription.setSearchType(SearchType.COURT_ID);
        when(dataManagementService.getCourtName(SEARCH_VALUE)).thenReturn("test court name");
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
        mockSubscription.setCourtName("Test court");
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of(mockSubscription));
        CourtSubscription expected = new CourtSubscription();
        expected.setSubscriptionId(mockSubscription.getId());
        expected.setCourtName("Test court");
        expected.setDateAdded(dateAdded);

        UserSubscription result = subscriptionService.findByUserId(USER_ID);

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

    @Test
    void testCollectSubscribersCourtSubscriptionNotClassified() throws IOException {
        when(subscriptionRepository.findSubscriptionsBySearchValue(SearchType.COURT_ID.toString(), COURT_MATCH))
            .thenReturn(List.of(returnedSubscription));
        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionServiceImpl.class)) {
            subscriptionService.collectSubscribers(publicArtefactMatches);
            assertEquals(String.format(SUBSCRIBER_NOTIFICATION_LOG, 1), logCaptor.getInfoLogs().get(0),
                         LOG_MESSAGE_MATCH);
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    @Test
    void testCollectSubscribersCaseSubscriptionsNotClassified() throws IOException {
        lenient().when(subscriptionRepository.findSubscriptionsBySearchValue(SearchType.CASE_ID.name(), CASE_MATCH))
            .thenReturn(List.of(returnedSubscription));
        lenient().when(subscriptionRepository.findSubscriptionsBySearchValue(SearchType.CASE_URN.name(), CASE_MATCH))
            .thenReturn(List.of(returnedSubscription));
        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionServiceImpl.class)) {
            subscriptionService.collectSubscribers(publicArtefactMatches);
            assertEquals(String.format(SUBSCRIBER_NOTIFICATION_LOG, 2), logCaptor.getInfoLogs().get(0),
                         LOG_MESSAGE_MATCH);
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    @Test
    void testCollectSubscribersNoSubscribers() throws IOException {
        lenient().when(subscriptionRepository.findSubscriptionsBySearchValue(SearchType.CASE_ID.name(), "test"))
            .thenReturn(List.of());
        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionServiceImpl.class)) {
            subscriptionService.collectSubscribers(publicArtefactMatches);
            assertEquals(String.format(SUBSCRIBER_NOTIFICATION_LOG, 0), logCaptor.getInfoLogs().get(0),
                         LOG_MESSAGE_MATCH);
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    @Test
    void testCollectSubscribersWarnsMissingSearchValues() throws IOException {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put(CASE_NUMBER_KEY, null);
        map.put(CASE_URN_KEY, null);
        cases.add(map);
        searchTerms.put("cases", cases);
        publicArtefactMatches.setSearch(searchTerms);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionServiceImpl.class)) {
            subscriptionService.collectSubscribers(publicArtefactMatches);
            assertEquals(1, logCaptor.getWarnLogs().size(),
                         LOG_MESSAGE_MATCH);
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    @Test
    void testCollectSubscribersRestrictsClassified() throws IOException {
        lenient().when(subscriptionRepository.findSubscriptionsBySearchValue(SearchType.CASE_ID.name(), CASE_MATCH))
            .thenReturn(List.of(returnedSubscription, restrictedSubscription));
        when(accountManagementService.isUserAuthorised(
            ACCEPTED_USER_ID, ListType.SJP_PRESS_LIST, Sensitivity.CLASSIFIED)).thenReturn(true);
        when(accountManagementService.isUserAuthorised(
            FORBIDDEN_USER_ID, ListType.SJP_PRESS_LIST, Sensitivity.CLASSIFIED))
            .thenReturn(false);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionServiceImpl.class)) {
            subscriptionService.collectSubscribers(classifiedArtefactMatches);
            assertEquals(1, logCaptor.getInfoLogs().size(),
                         LOG_MESSAGE_MATCH);
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }
}

