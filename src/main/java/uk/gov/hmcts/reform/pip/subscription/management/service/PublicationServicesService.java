package uk.gov.hmcts.reform.pip.subscription.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Component
public class PublicationServicesService {
    private static final String NOTIFY_SUBSCRIPTION_PATH = "/notify/subscription";

    @Autowired
    private WebClient webClient;

    @Value("${service-to-service.publication-services}")
    private String url;

    public void postSubscriptionSummaries(String subscriptionSummary) {
        try {
            webClient.post().uri(new URI(url + NOTIFY_SUBSCRIPTION_PATH))
                .body(BodyInserters.fromValue(subscriptionSummary));

        } catch (WebClientException | URISyntaxException ex) {
            log.error("request failed", ex.getMessage());
        }
    }
}
