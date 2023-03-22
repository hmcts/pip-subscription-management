package uk.gov.hmcts.reform.pip.subscription.management.helpers;

import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SubscriptionUtils {

    private SubscriptionUtils() {
    }

    public static Subscription createMockSubscription(String userId, String locationId, Channel channel,
                                                      LocalDateTime createdDate, ListType listType) {
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setUserId(userId);
        subscription.setSearchValue(locationId);
        subscription.setChannel(channel);
        subscription.setId(UUID.randomUUID());
        subscription.setCreatedDate(createdDate);
        subscription.setSearchType(SearchType.LOCATION_ID);
        subscription.setListType(List.of(listType.toString()));

        return subscription;
    }

    public static List<Subscription> createMockSubscriptionList(LocalDateTime createdDate) {
        final int caseIdInterval = 3;
        final int caseUrnInterval = 6;

        List<Subscription> subs = new ArrayList<>();
        Map<Integer, String> mockData = Map.of(
            0, "Ralph",
            1, "Ralph",
            2, "Jenny",
            3, "Thomas",
            4, "Pauline",
            5, "Cedric",
            6, "Adrian",
            7, "Cedric",
            8, "Jonathan"
        );

        for (int i = 0; i < 8; i++) {
            Subscription subscription = createMockSubscription(mockData.get(i), String.format("court-%s", i),
                                                               i < 3 ? Channel.API_COURTEL : Channel.EMAIL,
                                                               createdDate, ListType.CIVIL_DAILY_CAUSE_LIST);
            subscription.setChannel(i < 3 ? Channel.API_COURTEL : Channel.EMAIL);
            subscription.setCaseName("test name");
            subscription.setUrn("321" + i);
            subscription.setCaseNumber("123" + i);
            if (i < caseIdInterval) {
                subscription.setSearchType(SearchType.CASE_ID);
            } else if (i < caseUrnInterval) {
                subscription.setSearchType(SearchType.CASE_URN);
            } else {
                subscription.setSearchType(SearchType.LOCATION_ID);
                subscription.setCaseName(null);
                subscription.setUrn(null);
                subscription.setCaseNumber(null);
                subscription.setLocationName("test court name");
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

}
