package uk.gov.hmcts.reform.pip.subscription.management.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionDto;
import uk.gov.hmcts.reform.pip.subscription.management.service.SubscriptionService;

import java.util.UUID;


@RestController
@Api(tags = "Subscription management API")
@RequestMapping("/subscription")
public class SubscriptionController {

    @Autowired
    SubscriptionService subscriptionService;

    @PostMapping(consumes = "application/json")
    @ApiOperation("Endpoint to create a new unique subscription - the 'id' field is hidden from swagger as it is auto"
        + " generated on creation")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Subscription successfully created with the id: {subscription id} "
            + "for user: {userId}")
    })
    public ResponseEntity<String> createSubscription(@RequestBody SubscriptionDto sub) {
        Subscription subscription = subscriptionService.createSubscription(sub.toEntity());
        return ResponseEntity.ok(String.format("Subscription created with the id %s for user %s",
                                               subscription.getId(), subscription.getUserId()
        ));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "Subscription {subId} deleted"),
        @ApiResponse(code = 404, message = "No subscription found with the subscription id {subId}")
    })
    @Transactional
    @ApiOperation("Endpoint to delete a given unique subscription, using subscription ID as a parameter.")
    @DeleteMapping("/{subId}")
    public String deleteById(@ApiParam(value = "The specific subscription ID to be deleted", required = true)
                             @PathVariable UUID subId) {

        subscriptionService.deleteById(subId);
        return String.format("Subscription %s deleted", subId);
    }


    @ApiResponses({
        @ApiResponse(code = 200, message = "Subscription {subId} found"),
        @ApiResponse(code = 404, message = "No subscription found with the subscription id {subId}")
    })
    @ApiOperation("Returns the subscription object associated with a given subscription id.")
    @GetMapping("/{subId}")
    public Subscription findBySubId(@ApiParam(value = "The specific subscription id to find", required = true)
                                    @PathVariable UUID subId) {
        return subscriptionService.findById(subId);
    }

}
