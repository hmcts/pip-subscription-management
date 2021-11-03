package uk.gov.hmcts.reform.demo.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.demo.models.SubRepo;
import uk.gov.hmcts.reform.demo.models.Subscription;



@RestController
@Api(tags = "Subscription management API")
@RequestMapping("/subscriptions")
public class SubscriptionTableController {

    @Autowired
    SubRepo repository;

    @ApiResponses({
        @ApiResponse(code = 200, message = "All subscriptions returned"),
    })
    @GetMapping("/all")
    public String getResponse() {
        return "Everything is fine.";

    }

    @GetMapping("/addnew")
    public String postResponse() {
        /**
         * generate new unique subscription
         */
        Subscription sub = new Subscription("12345",
                                            "oaow",
                                            "awdd",
                                            "wagaa",
                                            "awwagg",
                                            "af");
        repository.save(sub);

        return "subs created";

    }

    @GetMapping("/deleteall")
    public String deleteAll() {
        /**
         * delete all data from the subscriptions table - probably unnecessary.
         */
        repository.deleteAll();
        return "all subs deleted";
    }

    @GetMapping("/delete/{uniqueSubId}")
    public String deleteSpecific(@ApiParam(value="The specific subscription ID to be deleted", required = true)
                                 @PathVariable Long uniqueSubId){
        repository.delete(repository.getById(uniqueSubId));
        return String.format("Subscription %s deleted", uniqueSubId);

    }
    








}
