package uk.gov.hmcts.reform.demo.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.demo.models.SubRepo;
import uk.gov.hmcts.reform.demo.models.Subscription;

import java.util.List;


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
        Subscription sub2 = new Subscription("12344",
                                             "oiiawg",
                                             "14440",
                                             "12444",
                                             "daw21",
                                             "124f21");
        repository.save(sub);
        repository.save(sub2);

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

    @GetMapping("/delete/{uuid}")
    public String deleteSpecific(@ApiParam(value="The specific subscription ID to be deleted", required = true)
                                 @PathVariable String uuid) {
        if (uuid.matches("^[a-zA-Z0-9]*$")){
            repository.deleteAll(repository.findAllByUuid(uuid));
            return String.format("Subscription %s deleted", uuid);
        }
        return "Error - incorrect uuid format";
    }

    @GetMapping("/findall")
    public List<Subscription> findAll() {
        return repository.findAll();
    }

    @GetMapping("/find/uuid/{uuid}")
    public List<Subscription> findByUuid(@ApiParam(value="The specific uuid to find", required = true)
                                   @PathVariable String uuid) {
        return repository.findAllByUuid(uuid);
    }

    @GetMapping("/find/subscription/{subid}")
    public List<Subscription> findBySubId(@ApiParam(value="The specific subscription id to find", required = true)
                                         @PathVariable String subid) {
        return repository.findAllBySubscriptionID(subid);
    }

    @GetMapping("/find/case/{caseid}")
    public List<Subscription> findByCaseId(@ApiParam(value="The specific case id to find", required = true)
                                         @PathVariable String caseid) {
        return repository.findAllByCaseID(caseid);
    }

    @GetMapping("/find/urn/{urnid}")
    public List<Subscription> findByUrnId(@ApiParam(value="The specific urn id to find", required = true)
                                           @PathVariable String urnid) {
        return repository.findAllByUrnID(urnid);
    }


}
