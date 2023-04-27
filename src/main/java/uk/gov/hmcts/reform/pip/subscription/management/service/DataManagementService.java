package uk.gov.hmcts.reform.pip.subscription.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import uk.gov.hmcts.reform.pip.model.location.Location;

import java.net.URI;
import java.net.URISyntaxException;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Slf4j
@Service
public class DataManagementService {

    @Autowired
    private WebClient webClient;

    @Value("${service-to-service.data-management}")
    private String url;


    public String getCourtName(String locationId) {
        try {
            Location location = webClient.get().uri(new URI(String.format("%s/locations/%s", url, locationId)))
                .retrieve()
                .bodyToMono(Location.class)
                .block();
            if (location != null) {
                return location.getName();
            }
            return null;
        } catch (WebClientException | URISyntaxException ex) {
            log.error(writeLog(
                String.format("Data management request to get location name failed for LocationId: %s. Response: %s",
                              locationId, ex.getMessage())
            ));
            return null;
        }
    }

}
