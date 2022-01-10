package uk.gov.hmcts.reform.pip.subscription.management.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Slf4j
@Component
public class DataManagementService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${service-to-service.data-management}")
    private String url;


    public String getCourtName(String courtId) {
        try {
            ResponseEntity<JsonNode> response = this.restTemplate.getForEntity(
                String.format("%s/courts/%s", url, courtId), JsonNode.class);
            return Objects.requireNonNull(response.getBody()).path("name").asText();
        } catch (HttpStatusCodeException ex) {
            log.error("Data management request failed for CourtId: {}. Response: {}",
                      courtId, ex.getResponseBodyAsString());
            return null;
        }
    }
}
