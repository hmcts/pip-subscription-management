package uk.gov.hmcts.reform.pip.subscription.management.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import uk.gov.hmcts.reform.pip.subscription.management.models.response.UserSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.service.SubscriptionService;

import java.util.UUID;
import javax.validation.Valid;

@RestController
@Api(tags = "Subscription management API")
@RequestMapping("/subscription")
@Valid
public class SubscriptionController {

    @Autowired
    SubscriptionService subscriptionService;

    @PostMapping(consumes = "application/json")
    @ApiOperation("Endpoint to create a new unique subscription - the 'id' field is hidden from swagger as it is auto"
        + " generated on creation")
    @ApiResponses({
        @ApiResponse(code = 201, message = "Subscription successfully created with the id: {subscription id} "
            + "for user: {userId}"),
        @ApiResponse(code = 400, message = "This subscription object has an invalid format. Please check again.")
    })
    public ResponseEntity<String> createSubscription(@RequestBody @Valid SubscriptionDto sub) {
        Subscription subscription = subscriptionService.createSubscription(sub.toEntity());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(String.format("Subscription created with the id %s for user '%s'",
                                                        subscription.getId(), subscription.getUserId()));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "Subscription: {subId} was deleted"),
        @ApiResponse(code = 404, message = "No subscription found with the subscription id {subId}")

    })
    @Transactional
    @ApiOperation("Endpoint to delete a given unique subscription, using subscription ID as a parameter.")
    @DeleteMapping("/{subId}")
    public ResponseEntity<String> deleteById(@ApiParam(value = "The specific subscription ID to be deleted",
        required = true) @PathVariable UUID subId) {

        subscriptionService.deleteById(subId);
        return ResponseEntity.ok(String.format("Subscription: %s was deleted", subId));
    }


    @ApiResponses({
        @ApiResponse(code = 200, message = "Subscription {subId} found"),
        @ApiResponse(code = 404, message = "No subscription found with the subscription id {subId}")
    })
    @ApiOperation("Returns the subscription object associated with a given subscription id.")
    @GetMapping("/{subId}")
    public ResponseEntity<Subscription> findBySubId(@ApiParam(value = "The specific subscription id to find",
        required = true)
                                    @PathVariable UUID subId) {
        return ResponseEntity.ok(subscriptionService.findById(subId));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "Subscriptions list for user id {userId} found"),
        @ApiResponse(code = 404, message = "No subscription found with the user id {userId}")
    })
    @ApiOperation("Returns the list of relevant subscriptions associated with a given user id.")
    @GetMapping("/user/{userId}")
    public ResponseEntity<UserSubscription> findByUserId(@ApiParam(
        value = "The specific user id to find subscription for", required = true)
                                    @PathVariable String userId) {
        return ResponseEntity.ok(subscriptionService.findByUserId(userId));
    }

}
