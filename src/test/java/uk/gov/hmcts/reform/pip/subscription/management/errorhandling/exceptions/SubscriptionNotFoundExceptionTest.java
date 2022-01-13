package uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubscriptionNotFoundExceptionTest {

    @Test
    void testCreationOfSubscriptionNotFoundException() {
        SubscriptionNotFoundException subscriptionNotFoundException
            = new SubscriptionNotFoundException("This is a test message");
        assertEquals("This is a test message", subscriptionNotFoundException.getMessage(),
                     "The message should match the message passed in");
    }
}
