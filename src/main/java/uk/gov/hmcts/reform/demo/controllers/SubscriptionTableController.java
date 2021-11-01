package uk.gov.hmcts.reform.demo.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.demo.models.Subscription;



@RestController
@Api(tags = "Subscription management API")
@RequestMapping("/subscriptions")
public class SubscriptionTableController {

    //    the below code is required when we create our service
    //    @Autowired
    //    private SubscriptionService subscriptionService;


    @ApiResponses({
        @ApiResponse(code = 200, message = "All subscriptions returned"),
    })
    @GetMapping("/all")
    public String getResponse() {
        return "Everything is fine.";

    }

    @PostMapping("/addnew")
    public Subscription postResponse() {
        /**
         * generate new unique subscription
         */
        Subscription sub = new Subscription();
        sub.setUuid("12345-29-09-88");
        sub.setSubscriptionID("43321-gla-234");
        sub.setCaseID("392919-glasgow");
        return sub;

    }










}
