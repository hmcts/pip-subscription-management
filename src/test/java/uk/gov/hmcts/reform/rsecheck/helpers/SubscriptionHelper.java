package uk.gov.hmcts.reform.rsecheck.helpers;

import uk.gov.hmcts.reform.demo.models.Channel;
import uk.gov.hmcts.reform.demo.models.Subscription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class SubscriptionHelper {

    private SubscriptionHelper(){
    }

    public static Subscription createMockSubscription(String userId, String courtId) {
        Subscription subscription = new Subscription();
        subscription.setUserId(userId);
        subscription.setSearchValue(courtId);
        return subscription;
    }

    public static List<Subscription> createMockSubscriptionList() {
        List<Subscription> subs = new ArrayList<>();
        Map<Integer,String> mockData = new HashMap<>();
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
            Subscription subscription = createMockSubscription(mockData.get(i), String.format("court-%s", i));
            subscription.setChannel(i < 3 ? Channel.API : Channel.EMAIL);
            subscription.setId(i);
            subs.add(subscription);
        }
        return subs;
    }

    public static Subscription findableSubscription(){
        Subscription subscription = new Subscription();
        subscription.setUserId("Ralph");
        subscription.setId(10);
        return subscription;
    }

    /**
     * Converts list of subs from above to map, with id as identifier
     * @return
     */
    public static Map<Long, Subscription> mockDbMap() {
        List<Subscription> listOfSubs = createMockSubscriptionList();
        Map<Long, Subscription> map = listOfSubs.stream()
            .collect(Collectors.toMap(Subscription::getId, Function.identity()));
        return map;

    }

}
