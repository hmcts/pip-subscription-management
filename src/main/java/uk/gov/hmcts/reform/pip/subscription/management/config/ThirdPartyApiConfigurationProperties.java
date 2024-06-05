package uk.gov.hmcts.reform.pip.subscription.management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "third-party-api")
@Data
public class ThirdPartyApiConfigurationProperties {
    private String courtel;
}
