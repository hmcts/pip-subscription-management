package uk.gov.hmcts.reform.pip.subscription.management.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.CourtNotFoundException;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.HearingNotFoundException;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Court;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Hearing;

import java.util.Arrays;
import java.util.List;

@Component
public class DataManagementService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${service-to-service.data-management}")
    private String url;

    public Hearing getHearingByCaseId(String caseNum) {
        try {
            ResponseEntity<Hearing> response = this.restTemplate.getForEntity(
                String.format("%s/hearings/case-number/%s", url, caseNum), Hearing.class);
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            throw new HearingNotFoundException(ex.getResponseBodyAsString());
        }
    }

    public Hearing getHearingByUrn(String urn) {
        try {
            ResponseEntity<Hearing> response = this.restTemplate.getForEntity(
                String.format("%s/hearings/urn/%s", url, urn), Hearing.class);
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            throw new HearingNotFoundException(ex.getResponseBodyAsString());
        }
    }

    public List<Hearing> getHearingByName(String caseName) {
        try {
            ResponseEntity<Hearing[]> response = this.restTemplate.getForEntity(
                String.format("%s/hearings/case-name/%s", url, caseName), Hearing[].class);

            if (response.getBody() == null) {
                return List.of();
            }

            return Arrays.asList(response.getBody());
        } catch (HttpStatusCodeException ex) {
            throw new HearingNotFoundException(ex.getResponseBodyAsString());
        }
    }

    public Court getCourt(String courtId) {
        try {
            ResponseEntity<Court> response = this.restTemplate.getForEntity(
                String.format("%s/courts/%s", url, courtId), Court.class);
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            throw new CourtNotFoundException(ex.getResponseBodyAsString());
        }
    }
}
