package uk.gov.hmcts.reform.pip.subscription.management.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.services.SubscriptionService;

import java.util.List;


@RestController
@Api(tags = "Subscription management API")
@RequestMapping("/subscription")
public class SubscriptionController {

    @Autowired
    SubscriptionService subscriptionService;

    @PostMapping(consumes = "application/json", produces = "application/json")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Subscription created"),
        @ApiResponse(code = 404, message = "Error reaching database")
    })
    public Subscription createSubscription(@RequestBody Subscription sub) {
        /*
          generate new unique subscription with json - 'id' field is hidden as auto-generated
         */
        return subscriptionService.createSubscription(sub);
    }



    @ApiResponses({
        @ApiResponse(code = 200, message = "Deleted all"),
        @ApiResponse(code = 404, message = "No subscriptions found")
    })
    @DeleteMapping("/all")
    public String deleteAll() {
        /*
          delete all data from the subscriptions table - probably unnecessary but useful for debug.
         */
        subscriptionService.deleteAll();
        return "all subs deleted";
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "Subscription {subId} deleted"),
        @ApiResponse(code = 404, message = "No subscription found with the subscription id {subId}")
    })
    @DeleteMapping("/{subId}")
    public String deleteById(@ApiParam(value = "The specific subscription ID to be deleted", required = true)
                                 @PathVariable Long subId) {

        subscriptionService.deleteById(subId);
        return String.format("Subscription %s deleted", subId);
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "All subscriptions found"),
        @ApiResponse(code = 404, message = "No subscriptions found")
    })
    @GetMapping
    public List<Subscription> findAll() {
        /*
          Returns the entire subscription db - for debug
         */
        return subscriptionService.findAll();
    }


    @ApiResponses({
        @ApiResponse(code = 200, message = "Subscription {subId} found"),
        @ApiResponse(code = 404, message = "No subscription found with the subscription id {subId}")
    })
    @GetMapping("/{subId}")
    public Subscription findBySubId(@ApiParam(value = "The specific subscription id to find", required = true)
                                         @PathVariable Long subId) {
        /*
          Returns all subscriptions associated with a given subscription id
         */
        return subscriptionService.findById(subId);
    }

}
