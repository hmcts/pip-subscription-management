package uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomExceptionTest {

    private static final String TEST_MESSAGE = "This is a test message";
    private static final String ASSERTION_MESSAGE = "The message should match the message passed in";

    @Test
    void testCreationOfSubscriptionNotFoundException() {

        SubscriptionNotFoundException subscriptionNotFoundException
            = new SubscriptionNotFoundException(TEST_MESSAGE);
        assertEquals(TEST_MESSAGE, subscriptionNotFoundException.getMessage(),
                     ASSERTION_MESSAGE);

    }
}
