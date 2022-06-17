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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Component
public class ChannelManagementService {

    private static final String EMAIL_PATH = "/channel/emails";

    @Autowired
    private WebClient webClient;

    @Value("${service-to-service.channel-management}")
    private String url;

    public Map<String, List<Subscription>> getMappedEmails(List<Subscription> listOfSubs) {
        try {
            return webClient.post().uri(url + EMAIL_PATH)
                .attributes(clientRegistrationId("channelManagementApi"))
                .body(BodyInserters.fromValue(listOfSubs))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, List<Subscription>>>() {})
                .block();
        } catch (WebClientException ex) {
            log.error(String.format("Request with body: %s failed. With error message: %s",
                                    listOfSubs, ex.getMessage()));
            return Collections.emptyMap();
        }
    }
}

