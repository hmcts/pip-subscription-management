package uk.gov.hmcts.reform.pip.subscription.management.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;
import uk.gov.hmcts.reform.pip.subscription.management.service.SubscriptionLocationService;

@RestController
@Tag(name = "Subscription Management Testing Support API")
@RequestMapping("/testing-support")
@ApiResponse(responseCode = "401", description = "Invalid access credential")
@ApiResponse(responseCode = "403", description = "User has not been authorized")
@IsAdmin
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(prefix = "testingSupport", name = "enableApi", havingValue = "true")
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class TestingSupportController {
    private static final String OK_CODE = "200";

    private final SubscriptionLocationService subscriptionLocationService;

    @Autowired
    public TestingSupportController(SubscriptionLocationService subscriptionLocationService) {
        this.subscriptionLocationService = subscriptionLocationService;
    }

    @ApiResponse(responseCode = OK_CODE,
        description = "Subscription(s) deleted for location name starting with {locationNamePrefix}")
    @Operation(summary = "Delete all subscriptions with location name prefix")
    @DeleteMapping("/subscription/{locationNamePrefix}")
    @Transactional
    public ResponseEntity<String> deleteSubscriptionsWithLocationNamePrefix(@PathVariable String locationNamePrefix) {
        return ResponseEntity.ok(
            subscriptionLocationService.deleteAllSubscriptionsWithLocationNamePrefix(locationNamePrefix)
        );
    }
}
