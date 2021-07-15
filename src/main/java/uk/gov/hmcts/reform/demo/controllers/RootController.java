package uk.gov.hmcts.reform.demo.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.demo.errorhandling.exceptions.SubscriptionNotFoundException;

import static org.springframework.http.ResponseEntity.ok;

/**
 * Default endpoints per application.
 */
@RestController
public class RootController {

    /**
     * Root GET endpoint.
     *
     * <p>Azure application service has a hidden feature of making requests to root endpoint when
     * "Always On" is turned on.
     * This is the endpoint to deal with that and therefore silence the unnecessary 404s as a response code.
     *
     * @return Welcome message from the service.
     */
    @GetMapping("/")
    public ResponseEntity<String> welcome() {
        return ok("Welcome to spring-boot-template");
    }

    /**
     * Dummy endpoint, that demonstrates how the Global Exception handler can be used to capture
     * and parse exceptions into a standard format.
     * @return A ResponseEntity
     */
    @GetMapping("/subscription")
    public ResponseEntity<String> getSubscriptions() {
        throw new SubscriptionNotFoundException("No Subscriptions found");
    }




}
