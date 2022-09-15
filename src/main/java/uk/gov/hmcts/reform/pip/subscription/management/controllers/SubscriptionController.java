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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.subscription.management.authentication.roles.IsAdmin;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionDto;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Artefact;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.UserSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.service.SubscriptionService;

import java.util.List;
import java.util.UUID;
import javax.validation.Valid;

@RestController
@Api(tags = "Subscription management API")
@RequestMapping("/subscription")
@Valid
@IsAdmin
public class SubscriptionController {

    private static final String NOT_AUTHORIZED_MESSAGE = "User has not been authorized";

    @Autowired
    SubscriptionService subscriptionService;

    @PostMapping(consumes = "application/json")
    @ApiOperation("Endpoint to create a new unique subscription - the 'id' field is hidden from swagger as it is auto"
        + " generated on creation")
    @ApiResponses({
        @ApiResponse(code = 201, message = "Subscription successfully created with the id: {subscription id} "
            + "for user: {userId}"),
        @ApiResponse(code = 400, message = "This subscription object has an invalid format. Please check again."),
        @ApiResponse(code = 403, message = NOT_AUTHORIZED_MESSAGE)
    })
    public ResponseEntity<String> createSubscription(@RequestBody @Valid SubscriptionDto sub) {
        Subscription subscription = subscriptionService.createSubscription(sub.toEntity());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(String.format("Subscription created with the id %s for user %s",
                                subscription.getId(), subscription.getUserId()));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "Subscription: {subId} was deleted"),
        @ApiResponse(code = 403, message = NOT_AUTHORIZED_MESSAGE),
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
        @ApiResponse(code = 403, message = NOT_AUTHORIZED_MESSAGE),
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
        @ApiResponse(code = 403, message = NOT_AUTHORIZED_MESSAGE),
        @ApiResponse(code = 404, message = "No subscription found with the user id {userId}")
    })
    @ApiOperation("Returns the list of relevant subscriptions associated with a given user id.")
    @GetMapping("/user/{userId}")
    public ResponseEntity<UserSubscription> findByUserId(@ApiParam(
        value = "The specific user id to find subscription for", required = true)
                                                         @PathVariable String userId) {
        return ResponseEntity.ok(subscriptionService.findByUserId(userId));
    }

    @ApiResponses({
        @ApiResponse(code = 202, message = "Subscriber request has been accepted"),
    })
    @ApiOperation("Takes in artefact to build subscriber list.")
    @PostMapping("/artefact-recipients")
    public ResponseEntity<String> buildSubscriberList(@RequestBody Artefact artefact) {
        subscriptionService.collectSubscribers(artefact);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("Subscriber request has been accepted");
    }

    @ApiResponses({
        @ApiResponse(code = 202, message = "Third Parties list deletion accepted"),
    })
    @ApiOperation("Takes in a deleted artefact to notify subscribed third parties")
    @PostMapping("/deleted-artefact")
    public ResponseEntity<String> buildDeletedArtefactSubscribers(@RequestBody Artefact artefact) {
        subscriptionService.collectThirdPartyForDeletion(artefact);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            "Deleted artefact third party subscriber notification request has been accepted");
    }

    @PutMapping("/configure-list-types/{userId}")
    @ApiOperation("Endpoint to update list type for existing subscription")
    @ApiResponses({
        @ApiResponse(code = 201, message = "Subscription successfully updated for user: {userId}"),
        @ApiResponse(code = 400, message = "This request object has an invalid format. Please check again."),
        @ApiResponse(code = 403, message = NOT_AUTHORIZED_MESSAGE)
    })
    public ResponseEntity<String> configureListTypesForSubscription(@PathVariable String userId,
                                                    @RequestBody List<String> listType) {
        subscriptionService.configureListTypesForSubscription(userId, listType);
        return ResponseEntity.status(HttpStatus.OK)
            .body(String.format("Location list Type successfully updated for user %s",
                                userId));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "Subscription Management - MI Data request (all) accepted.")
    })
    @ApiOperation("Returns a list of metadata for all existing subscriptions for MI reporting.")
    @GetMapping("/mi-data-all")
    @IsAdmin
    public ResponseEntity<String> getSubscriptionDataForMiReportingAll() {
        return ResponseEntity.status(HttpStatus.OK)
            .body(subscriptionService.getAllSubscriptionsDataForMiReporting());
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "Subscription Management - MI Data request (local) accepted.")
    })
    @ApiOperation("Returns a list of subscription data for specifically location-based subscriptions for MI reporting.")
    @GetMapping("/mi-data-local")
    @IsAdmin
    public ResponseEntity<String> getSubscriptionDataForMiReportingLocal() {
        return ResponseEntity.status(HttpStatus.OK)
            .body(subscriptionService.getLocalSubscriptionsDataForMiReporting());
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "Deleted all subscriptions for user id {userId}"),
        @ApiResponse(code = 403, message = NOT_AUTHORIZED_MESSAGE)
    })
    @ApiOperation("Deletes all subscriptions for the supplied user id")
    @Transactional
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<String> deleteAllSubscriptionsForUser(@ApiParam(value = "The specific user id to delete "
        + "the subscriptions for", required = true) @PathVariable String userId) {
        return ResponseEntity.ok(subscriptionService.deleteAllByUserId(userId));
    }
}
