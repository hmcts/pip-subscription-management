package uk.gov.hmcts.reform.pip.subscription.management.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RestTemplateTest {

    @Test
    void testGetConfig() {
        RestTemplate restTemplate = new RestTemplateConfig().restTemplate();
        assertNotNull(restTemplate, "Rest Template config has not created a rest template");
    }

}
