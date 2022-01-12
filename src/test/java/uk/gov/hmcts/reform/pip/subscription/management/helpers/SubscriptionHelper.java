package uk.gov.hmcts.reform.pip.subscription.management.helpers;

import uk.gov.hmcts.reform.pip.subscription.management.models.Channel;
import uk.gov.hmcts.reform.pip.subscription.management.models.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SubscriptionHelper {

    private SubscriptionHelper() {
    }

    public static Subscription createMockSubscription(String userId, String courtId, Channel channel,
                                                      LocalDateTime createdDate) {
        Subscription subscription = new Subscription();
        subscription.setUserId(userId);
        subscription.setSearchValue(courtId);
        subscription.setChannel(channel);
        subscription.setId(UUID.randomUUID());
        subscription.setCreatedDate(createdDate);
        subscription.setSearchType(SearchType.COURT_ID);
        return subscription;
    }

    public static List<Subscription> createMockSubscriptionList(LocalDateTime createdDate) {
        List<Subscription> subs = new ArrayList<>();
        Map<Integer, String> mockData = new ConcurrentHashMap<>();
        mockData.put(0, "Ralph");
        mockData.put(1, "Ralph");
        mockData.put(2, "Jenny");
        mockData.put(3, "Thomas");
        mockData.put(4, "Pauline");
        mockData.put(5, "Cedric");
        mockData.put(6, "Adrian");
        mockData.put(7, "Cedric");
        mockData.put(8, "Jonathan");
        for (int i = 0; i < 8; i++) {
            Subscription subscription = createMockSubscription(mockData.get(i), String.format("court-%s", i),
                                                               i < 3 ? Channel.API : Channel.EMAIL, createdDate);
            subs.add(subscription);
        }
        return subs;
    }

    public static Subscription findableSubscription() {
        Subscription subscription = new Subscription();
        subscription.setUserId("Ralph");
        subscription.setId(UUID.randomUUID());
        return subscription;
    }



}
