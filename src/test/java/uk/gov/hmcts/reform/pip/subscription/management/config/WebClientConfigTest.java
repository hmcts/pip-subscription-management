package uk.gov.hmcts.reform.pip.subscription.management.config;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Profile("non-async")
@Configuration
public class WebClientConfigTest {

    @Mock
    private WebClient webClientMock;

    public WebClientConfigTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Bean
    public WebClient webClient() {
        return webClientMock;
    }
}
