package uk.gov.hmcts.reform.demo.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.demo.models.SubRepo;
import uk.gov.hmcts.reform.demo.models.Subscription;
import uk.gov.hmcts.reform.demo.services.SubscriptionService;

import java.util.List;


@RestController
@Api(tags = "Subscription management API")
@RequestMapping("/subscription")
public class SubscriptionTableController {

    @Autowired
    SubRepo repository;


    @PostMapping(value = "/new/{input}", consumes = "application/json", produces= "application/json")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Subscriptions created"),
        @ApiResponse(code = 404, message = "Error reaching database")
    })
    public Subscription postSub(@RequestBody Subscription sub) {
        /*
          generate new unique subscription with json - 'id' field is hidden as auto-generated
         */
        repository.save(sub);

        return sub;

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
        repository.deleteAll();
        return "all subs deleted";
    }


    @ApiResponses({
        @ApiResponse(code = 200, message = "Subscription {subid} deleted"),
        @ApiResponse(code = 404, message = "No subscription found with the subscription id {subid}")
    })
    @DeleteMapping("/{subid}")
    public String deleteSpecific(@ApiParam(value="The specific subscription ID to be deleted", required = true)
                                 @PathVariable Long subid) {
        /*
          Deletes a subscription from the given subscriptionID
         */

        repository.deleteAll(repository.findAllById(subid));
        return String.format("Subscription %s deleted", subid);
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "All subscriptions found"),
        @ApiResponse(code = 404, message = "No subscriptions found")
    })
    @GetMapping("/findall")
    public List<Subscription> findAll() {
        /*
          Returns the entire subscription db - for debug
         */
        return repository.findAll();
    }


    @ApiResponses({
        @ApiResponse(code = 200, message = "Subscription {subid} found"),
        @ApiResponse(code = 404, message = "No subscription found with the subscription id {subid}")
    })
    @GetMapping("/subscription/{subid}")
    public List<Subscription> findBySubId(@ApiParam(value="The specific subscription id to find", required = true)
                                         @PathVariable Long subid) {
        /*
          Returns all subscriptions associated with a given subscription id
         */
        return repository.findAllById(subid);
    }






}
