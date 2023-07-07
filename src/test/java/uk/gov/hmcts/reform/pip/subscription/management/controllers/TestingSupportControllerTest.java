package uk.gov.hmcts.reform.pip.subscription.management.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.subscription.management.service.SubscriptionLocationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestingSupportControllerTest {
    private static final String LOCATION_NAME_PREFIX = "TEST_PIP_1234_";

    @Mock
    SubscriptionLocationService subscriptionLocationService;

    @InjectMocks
    TestingSupportController testingSupportController;

    @Test
    void testDeleteSubscriptionsWithLocationNamePrefixReturnsOk() {
        String responseMessage = "5 subscription(s) deleted for location name starting with " + LOCATION_NAME_PREFIX;
        when(subscriptionLocationService.deleteAllSubscriptionsWithLocationNamePrefix(LOCATION_NAME_PREFIX))
            .thenReturn(responseMessage);

        ResponseEntity<String> response = testingSupportController.deleteSubscriptionsWithLocationNamePrefix(
            LOCATION_NAME_PREFIX
        );

        assertThat(response.getStatusCode())
            .as("Response status does not match")
            .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
            .as("Response body does not match")
            .isEqualTo(responseMessage);
    }
}
