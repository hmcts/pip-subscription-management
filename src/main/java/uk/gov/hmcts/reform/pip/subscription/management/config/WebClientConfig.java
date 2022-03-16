package uk.gov.hmcts.reform.pip.subscription.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.reactive.function.client.WebClient;

@Profile({"!test", "!non-async"})
@Configuration
@EnableAsync
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.create();
    }
}
