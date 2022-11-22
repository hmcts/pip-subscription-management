package uk.gov.hmcts.reform.pip.subscription.management.config.flyway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class FlywayConfigurationTest {

    @Test
    void testFlywayMigrationStrategyReturn() {
        FlywayConfiguration flywayConfiguration = new FlywayConfiguration();
        assertNotNull(flywayConfiguration.flywayMigrationStrategy(),
                      "Flyway migration strategy should not be null");
    }

}
