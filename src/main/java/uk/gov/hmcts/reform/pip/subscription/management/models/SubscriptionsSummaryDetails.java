package uk.gov.hmcts.reform.pip.subscription.management.models;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;

@Data
public class SubscriptionsSummaryDetails {
    @ToString.Include(name = "CASE_URN")
    private ArrayList<String> caseUrn;
    @ToString.Include(name = "CASE_NUMBER")
    private ArrayList<String> caseNumber;
    @ToString.Include(name = "LOCATION_ID")
    private ArrayList<String> locationId;

    public void addToCaseUrn(String caseUrn) {
        this.caseUrn.add(caseUrn);
    }

    public void addToCaseNumber(String caseNumber) {
        this.caseNumber.add(caseNumber);
    }

    public void addToLocationId(String locationId) {
        this.locationId.add(locationId);
    }
}
