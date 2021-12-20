package uk.gov.hmcts.reform.pip.subscription.management.config;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfigTest {

    @Mock
    private RestTemplate mockRestTemplate;

    public RestTemplateConfigTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Bean
    public RestTemplate restTemplate() {
        return mockRestTemplate;
    }
}
