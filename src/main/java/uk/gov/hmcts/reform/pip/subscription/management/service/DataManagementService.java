package uk.gov.hmcts.reform.pip.subscription.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import uk.gov.hmcts.reform.pip.subscription.management.models.subscriptionmanagement.Court;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Component
public class DataManagementService {

    @Autowired
    private WebClient webClient;

    @Value("${service-to-service.data-management}")
    private String url;


    public String getCourtName(String courtId) {
        try {
            Court court = webClient.get().uri(new URI(String.format("%s/courts/%s", url, courtId)))
                .retrieve()
                .bodyToMono(Court.class).block();
            return court.getName();
        } catch (WebClientException | URISyntaxException ex) {
            log.error("Data management request failed for CourtId: {}. Response: {}",
                      courtId, ex.getMessage());
            return null;
        }
    }
}
