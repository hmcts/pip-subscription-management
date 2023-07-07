package uk.gov.hmcts.reform.pip.subscription.management.controllers;


import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.UserSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.service.SubscriptionLocationService;
import uk.gov.hmcts.reform.pip.subscription.management.service.SubscriptionNotificationService;
import uk.gov.hmcts.reform.pip.subscription.management.service.SubscriptionService;
import uk.gov.hmcts.reform.pip.subscription.management.service.UserSubscriptionService;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Subscription Management API")
@RequestMapping("/subscription")
@Valid
@IsAdmin
@SuppressWarnings({"PMD.TooManyMethods"})
public class SubscriptionController {

    private static final String NOT_AUTHORIZED_MESSAGE = "User has not been authorized";
    private static final String AUTH_ERROR_CODE = "403";
    private static final String OK_CODE = "200";
    private static final String NOT_FOUND_ERROR_CODE = "404";

    @Autowired
    SubscriptionService subscriptionService;

    @Autowired
    UserSubscriptionService userSubscriptionService;

    @Autowired
    SubscriptionNotificationService subscriptionNotificationService;

    @Autowired
    SubscriptionLocationService subscriptionLocationService;

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Endpoint to create a new unique subscription "
        + "- the 'id' field is hidden from swagger as it is auto generated on creation")
    @ApiResponse(responseCode = "201", description = "Subscription successfully created with the id: "
        + "{subscription id} "
        + "for user: {userId}")
    @ApiResponse(responseCode = "400", description = "This subscription object has an invalid format. Please "
        + "check again.")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    public ResponseEntity<String> createSubscription(
        @RequestBody @Valid uk.gov.hmcts.reform.pip.model.subscription.Subscription sub,
        @RequestHeader("x-user-id") String actioningUserId
    ) {
        Subscription subscription = subscriptionService.createSubscription(new Subscription(sub), actioningUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(String.format("Subscription created with the id %s for user %s",
                                subscription.getId(), subscription.getUserId()));
    }

    @ApiResponse(responseCode = OK_CODE, description = "Subscription: {subId} was deleted")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE,
        description = "No subscription found with the subscription id {subId}")
    @Transactional
    @Operation(summary = "Endpoint to delete a given unique subscription, using subscription ID as a parameter.")
    @DeleteMapping("/{subId}")
    public ResponseEntity<String> deleteById(@Parameter @PathVariable UUID subId,
                                             @RequestHeader("x-user-id") String actioningUserId) {

        subscriptionService.deleteById(subId, actioningUserId);
        return ResponseEntity.ok(String.format("Subscription: %s was deleted", subId));
    }

    @ApiResponse(responseCode = OK_CODE, description = "Subscription(s) with ID {subIds} deleted")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE,
        description = "No subscription found with the subscription IDs: {subIds}")
    @Transactional
    @Operation(summary = "Delete a set of subscriptions using the subscription ID")
    @DeleteMapping("/bulk")
    public ResponseEntity<String> bulkDeleteSubscriptions(@RequestBody List<UUID> subIds) {

        subscriptionService.bulkDeleteSubscriptions(subIds);
        return ResponseEntity.ok(String.format("Subscription(s) with ID %s deleted",
                                               subIds.toString().replace("[", "")
                                                   .replace("]", "")));
    }


    @ApiResponse(responseCode = OK_CODE, description = "Subscription {subId} found")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE,
        description = "No subscription found with the subscription id {subId}")
    @Operation(summary = "Returns the subscription object associated with a given subscription id.")
    @GetMapping("/{subId}")
    public ResponseEntity<Subscription> findBySubId(@Parameter @PathVariable UUID subId) {
        return ResponseEntity.ok(subscriptionService.findById(subId));
    }

    @ApiResponse(responseCode = OK_CODE, description = "Subscriptions list for user id {userId} found")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE,
        description = "No subscription found with the user id {userId}")
    @Operation(summary = "Returns the list of relevant subscriptions associated with a given user id.")
    @GetMapping("/user/{userId}")
    public ResponseEntity<UserSubscription> findByUserId(@Parameter @PathVariable String userId) {
        return ResponseEntity.ok(userSubscriptionService.findByUserId(userId));
    }

    @ApiResponse(responseCode = "202", description = "Subscriber request has been accepted")
    @Operation(summary = "Takes in artefact to build subscriber list.")
    @PostMapping("/artefact-recipients")
    public ResponseEntity<String> buildSubscriberList(@RequestBody Artefact artefact) {
        subscriptionNotificationService.collectSubscribers(artefact);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("Subscriber request has been accepted");
    }

    @ApiResponse(responseCode = "202", description = "Third Parties list deletion accepted")
    @Operation(summary = "Takes in a deleted artefact to notify subscribed third parties")
    @PostMapping("/deleted-artefact")
    public ResponseEntity<String> buildDeletedArtefactSubscribers(@RequestBody Artefact artefact) {
        subscriptionNotificationService.collectThirdPartyForDeletion(artefact);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            "Deleted artefact third party subscriber notification request has been accepted");
    }

    @PutMapping("/configure-list-types/{userId}")
    @Operation(summary = "Endpoint to update list type for existing subscription")
    @ApiResponse(responseCode = "201", description = "Subscription successfully updated for user: {userId}")
    @ApiResponse(responseCode = "400", description =
        "This request object has an invalid format. Please check again.")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    public ResponseEntity<String> configureListTypesForSubscription(@PathVariable String userId,
                                                    @RequestBody List<String> listType) {
        subscriptionService.configureListTypesForSubscription(userId, listType);
        return ResponseEntity.status(HttpStatus.OK)
            .body(String.format("Location list Type successfully updated for user %s",
                                userId));
    }

    @ApiResponse(responseCode = OK_CODE, description =
        "Subscription Management - MI Data request (all) accepted.")
    @Operation(summary = "Returns a list of metadata for all existing subscriptions for MI reporting.")
    @GetMapping("/mi-data-all")
    @IsAdmin
    public ResponseEntity<String> getSubscriptionDataForMiReportingAll() {
        return ResponseEntity.status(HttpStatus.OK)
            .body(subscriptionService.getAllSubscriptionsDataForMiReporting());
    }

    @ApiResponse(responseCode = OK_CODE, description =
        "Subscription Management - MI Data request (local) accepted.")
    @Operation(summary = "Returns a list of subscription data "
        + "for specifically location-based subscriptions for MI reporting.")
    @GetMapping("/mi-data-local")
    @IsAdmin
    public ResponseEntity<String> getSubscriptionDataForMiReportingLocal() {
        return ResponseEntity.status(HttpStatus.OK)
            .body(subscriptionService.getLocalSubscriptionsDataForMiReporting());
    }

    @ApiResponse(responseCode = OK_CODE, description = "Deleted all subscriptions for user id {userId}")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @Operation(summary = "Deletes all subscriptions for the supplied user id")
    @Transactional
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<String> deleteAllSubscriptionsForUser(@Parameter @PathVariable String userId) {
        return ResponseEntity.ok(userSubscriptionService.deleteAllByUserId(userId));
    }

    @ApiResponse(responseCode = OK_CODE, description =
        "Subscriptions list for location id {locationId} found")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description =
        "No subscription found with the location id {locationId}")
    @GetMapping("/location/{locationId}")
    public ResponseEntity<List<Subscription>> findSubscriptionsByLocationId(
                                                         @PathVariable String locationId) {
        return ResponseEntity.ok(subscriptionLocationService.findSubscriptionsByLocationId(locationId));
    }

    @ApiResponse(responseCode = OK_CODE, description = "Subscription for location {locationId} has been deleted")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = "User has not been authorized")
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = "No subscription found for location {locationId}")
    @Transactional
    @DeleteMapping("/location/{locationId}")
    @IsAdmin
    public ResponseEntity<String> deleteSubscriptionByLocation(
        @RequestHeader("x-provenance-user-id") String provenanceUserId,
        @PathVariable Integer locationId) throws JsonProcessingException {
        return ResponseEntity.ok(subscriptionLocationService.deleteSubscriptionByLocation(locationId.toString(),
                                                                                  provenanceUserId));
    }
}
