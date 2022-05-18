package uk.gov.hmcts.reform.pip.subscription.management.models;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SubscriptionsSummaryDto {
    private ArrayList<String> caseUrn;
    private ArrayList<String> caseNumber;
    private ArrayList<String> locationId;
}
