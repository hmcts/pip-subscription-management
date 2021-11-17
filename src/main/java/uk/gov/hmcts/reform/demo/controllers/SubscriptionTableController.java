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
@RequestMapping("/subscriptions")
public class SubscriptionTableController {

    @Autowired
    SubRepo repository;


    @GetMapping("/addexample")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Subscriptions created"),
        @ApiResponse(code = 404, message = "Error reaching database")
    })
    public String createSubExample() {
        /**
         * generate new unique subscription
         * is currently a get but will be made into a post later
         */
        Subscription sub = new Subscription("danny33",
                                            "glasgow-1",
                                            "sub-12345-321",
                                            "m1ur2d",
                                            "4444-5555-6666-7324",
                                            "fa9fka9k");
        Subscription sub2 = new Subscription("rudolph21",
                                             "london-3",
                                             "sub-32345-232",
                                             "th13f2",
                                             "3321-0402-0214-9580",
                                             "124f21a2");
        repository.save(sub);
        repository.save(sub2);

        return "subs created";

    }

    @PostMapping(value = "/addnew/{input}", consumes = "application/json", produces="application/json")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Subscriptions created"),
        @ApiResponse(code = 404, message = "Error reaching database")
    })
    public String postSub(@RequestBody Subscription sub) {
        /**
         * generate new unique subscription
         * is currently a get but will be made into a post later
         */
        repository.save(sub);

        return "data added";

    }



    @ApiResponses({
        @ApiResponse(code = 200, message = "Deleted all"),
        @ApiResponse(code = 404, message = "No subscriptions found")
    })
    @GetMapping("/deleteall")
    public String deleteAll() {
        /**
         * delete all data from the subscriptions table - probably unnecessary.
         */
        repository.deleteAll();
        return "all subs deleted";
    }


    @ApiResponses({
        @ApiResponse(code = 200, message = "Subscription {subid} deleted"),
        @ApiResponse(code = 404, message = "No subscription found with the subscription id {subid}")
    })
    @GetMapping("/delete/{subid}")
    public String deleteSpecific(@ApiParam(value="The specific subscription ID to be deleted", required = true)
                                 @PathVariable String subid) {
        /**
         * Deletes a subscription from the given subscriptionID
         */
        if (subid.matches("^[a-zA-Z0-9.-]*$")){
            repository.deleteAll(repository.findAllBySubscriptionId(subid));
            return String.format("Subscription %s deleted", subid);
        }
        return "Error - incorrect uuid format";
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "All subscriptions found"),
        @ApiResponse(code = 404, message = "No subscriptions found")
    })
    @GetMapping("/findall")
    public List<Subscription> findAll() {
        /**
         * Returns the entire subscription db - for debug
         */
        return repository.findAll();
    }


    @ApiResponses({
        @ApiResponse(code = 200, message = "User {uuid} found"),
        @ApiResponse(code = 404, message = "No subscription found with the uuid {caseid}")
    })
    @GetMapping("/find/uuid/{uuid}")
    public List<Subscription> findByUuid(@ApiParam(value="The specific uuid to find", required = true)
                                   @PathVariable String uuid) {
        /**
         * Returns all subscriptions associated with a given user
         */
        return repository.findAllByUuid(uuid);
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "Subscription {subid} found"),
        @ApiResponse(code = 404, message = "No subscription found with the subscription id {subid}")
    })
    @GetMapping("/find/subscription/sub/{subid}")
    public List<Subscription> findBySubId(@ApiParam(value="The specific subscription id to find", required = true)
                                         @PathVariable String subid) {
        /**
         * Returns all subscriptions associated with a given subscription id
         */
        return repository.findAllBySubscriptionId(subid);
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "Subscriptions associated with court {courtid} found"),
        @ApiResponse(code = 404, message = "No subscription found with the courtid {courtid}")
    })
    @GetMapping("/find/subscription/court/{courtid}")
    public List<Subscription> findByCourtId(@ApiParam(value="The specific court id to find", required = true)
                                          @PathVariable String courtid) {
        /**
         * Returns all subscriptions associated with a given courtID
         */
        return repository.findAllByCourtId(courtid);
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "Subscriptions associated with case {caseid} found"),
        @ApiResponse(code = 404, message = "No subscription found with the caseid {caseid}")
    })
    @GetMapping("/find/case/{caseid}")
    public List<Subscription> findByCaseId(@ApiParam(value="The specific case id to find", required = true)
                                         @PathVariable String caseid) {
        /**
         * Returns all subscriptions associated with a given caseID
         */
        return repository.findAllByCaseId(caseid);
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "Subscriptions associated with urn {urnid} found"),
        @ApiResponse(code = 404, message = "No subscription found with the urn {urnid}")
    })
    @GetMapping("/find/urn/{urnid}")
    public List<Subscription> findByUrnId(@ApiParam(value="The specific urn id to find", required = true)
                                           @PathVariable String urnid) {
        /**
         * Returns all subscriptions associated with a given urnid
         */
        return repository.findAllByUrnId(urnid);
    }


}
