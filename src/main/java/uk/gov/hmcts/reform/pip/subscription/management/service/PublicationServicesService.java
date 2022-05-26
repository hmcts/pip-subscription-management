package uk.gov.hmcts.reform.pip.subscription.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionsSummary;

@Slf4j
@Component
public class PublicationServicesService {
    private static final String NOTIFY_SUBSCRIPTION_PATH = "/notify/subscription";

    @Autowired
    private WebClient webClient;

    @Value("${service-to-service.publication-services}")
    private String url;

    public void postSubscriptionSummaries(SubscriptionsSummary subscriptionSummary) {
        try {
            webClient.post().uri(url + NOTIFY_SUBSCRIPTION_PATH)
                .body(BodyInserters.fromValue(subscriptionSummary)).retrieve().bodyToMono(Void.class).block();

        } catch (WebClientException ex) {
            log.error(String.format("Request with body: %s failed. With error message: %s",
                                    subscriptionSummary, ex.getMessage()));
        }
    }
}
