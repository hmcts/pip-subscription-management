package uk.gov.hmcts.reform.rsecheck.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.demo.Application;
import uk.gov.hmcts.reform.demo.models.Channel;
import uk.gov.hmcts.reform.demo.models.SearchType;
import uk.gov.hmcts.reform.demo.models.Subscription;
import uk.gov.hmcts.reform.demo.services.SubscriptionService;
import uk.gov.hmcts.reform.rsecheck.helpers.SubscriptionHelper;


import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.demo.models.Channel.API;
import static uk.gov.hmcts.reform.demo.models.SearchType.COURT_ID;
import static uk.gov.hmcts.reform.rsecheck.helpers.SubscriptionHelper.createMockSubscriptionList;
import static uk.gov.hmcts.reform.rsecheck.helpers.SubscriptionHelper.createMockSubscription;
import static uk.gov.hmcts.reform.rsecheck.helpers.SubscriptionHelper.findableSubscription;
import static uk.gov.hmcts.reform.rsecheck.helpers.SubscriptionHelper.mockDbMap;

@SpringBootTest
class SubscriptionServiceTest {
    private static final String userId = "Ralph21";
    private static final Long idForFinding = 10L;
    private static final String searchValue = "193254";
    private static final Channel channel = API;

    private List<Subscription> mockSubscriptionList;
    private Subscription mockSubscription;
    private Subscription findableSubscription;
    private Map<Long, Subscription> mockedDb;

    @MockBean
    SubscriptionService subscriptionService;

    @BeforeEach
    void setup() {
        mockSubscription = createMockSubscription(userId, searchValue);
        mockSubscriptionList = createMockSubscriptionList();
        findableSubscription = findableSubscription();
        mockedDb = mockDbMap();
        when(subscriptionService.createSubscription(mockSubscription)).thenReturn(mockSubscription);
        when(subscriptionService.findAll()).thenReturn(mockSubscriptionList);
        when(subscriptionService.findById(3L)).thenReturn(Optional.ofNullable(mockedDb.get(3L)));


    }

    @Test
    void testGetSubscriptionReturnsExpected() {
        assertEquals(mockSubscriptionList, subscriptionService.findAll());
    }

    @Test
    void testCreateSubscription() {
        assertEquals(subscriptionService.createSubscription(mockSubscription), mockSubscription);
    }

    @Test
    void testDeleteSubscription() {
        mockedDb.remove(4L);
        assertTrue(mockedDb.containsKey(4L) && mockedDb.get(4L) == null);
    }

    @Test
    void testFindSubscription() {
        mockedDb.put(9L, findableSubscription);
        assertTrue(mockedDb.containsKey(9L) && mockedDb.get(9L) == findableSubscription);
    }
//    @Test
//    void testDeleteSubscription() {
//        subscriptionService.deleteById(10);
//        AssertThat()
//    }
}

