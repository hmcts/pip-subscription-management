package uk.gov.hmcts.reform.demo.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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










}
