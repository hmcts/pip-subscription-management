package uk.gov.hmcts.reform.pip.subscription.management.helpers;

import uk.gov.hmcts.reform.pip.subscription.management.models.Channel;
import uk.gov.hmcts.reform.pip.subscription.management.models.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Court;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Hearing;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.UserSubscriptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SubscriptionHelper {

    private SubscriptionHelper() {
    }

    public static Subscription createMockSubscription(String userId, String courtId) {
        Subscription subscription = new Subscription();
        subscription.setUserId(userId);
        subscription.setSearchValue(courtId);
        return subscription;
    }

    public static List<Subscription> createMockSubscriptionList() {
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
            Subscription subscription = createMockSubscription(mockData.get(i), String.format("court-%s", i));
            subscription.setChannel(i < 3 ? Channel.API : Channel.EMAIL);
            subscription.setId(UUID.randomUUID());
            if (i < 3) {
                subscription.setSearchType(SearchType.CASE_ID);
            } else if (i < 6) {
                subscription.setSearchType(SearchType.CASE_URN);
            } else {
                subscription.setSearchType(SearchType.COURT_ID);
            }
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

    public static UserSubscriptions mockUserSubscriptions() {
        List<Hearing> hearings = new ArrayList<>();
        List<Court> courts = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            if (i < 6) {
                hearings.add(new Hearing());
            } else {
                courts.add(new Court());
            }
        }

        UserSubscriptions userSubscriptions = new UserSubscriptions();
        userSubscriptions.setCaseSubscriptions(hearings);
        userSubscriptions.setCourtSubscriptions(courts);

        return userSubscriptions;
    }

}
