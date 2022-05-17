package uk.gov.hmcts.reform.pip.subscription.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Component
public class ChannelManagementService {

    @Autowired
    private WebClient webClient;

    @Value("${service-to-service.channel-management}")
    private String url;

    public String testConnection() {
        try {
            return webClient.get().uri(new URI(url)).retrieve()
                .bodyToMono(String.class).block();
        } catch (WebClientException | URISyntaxException ex) {
            log.error("request failed", ex.getMessage());
            return null;
        }
    }
}

