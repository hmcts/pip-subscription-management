package uk.gov.hmcts.reform.pip.subscription.management.models;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
public class SubscriptionsSummaryDetails {
    @ToString.Include(name = "CASE_URN")
    private List<String> caseUrn = new ArrayList<>();
    @ToString.Include(name = "CASE_NUMBER")
    private List<String> caseNumber = new ArrayList<>();
    @ToString.Include(name = "LOCATION_ID")
    private List<String> locationId = new ArrayList<>();

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
