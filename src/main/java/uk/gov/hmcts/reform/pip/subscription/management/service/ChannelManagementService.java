package uk.gov.hmcts.reform.pip.subscription.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ChannelManagementService {

    private String emailPath = "/account/emails";
    private String apiPath = "/account/api";

    @Autowired
    private WebClient webClient;

    @Value("${service-to-service.channel-management}")
    private String url;

    public Map<String, List<Subscription>> getMappedEmails(List<Subscription> listOfSubs) {
        try {
            return webClient.post().uri(new URI(url + emailPath))
                .body(BodyInserters.fromValue(listOfSubs))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, List<Subscription>>>() {})
                .block();

        } catch (WebClientException | URISyntaxException ex) {
            ex.printStackTrace();
            log.error("request failed", ex.getMessage());
            return null;
        }
    }

}

