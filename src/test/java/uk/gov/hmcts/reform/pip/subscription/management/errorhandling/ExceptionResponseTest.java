package uk.gov.hmcts.reform.pip.subscription.management.errorhandling;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExceptionResponseTest {

    @Test
    void testCreationOfExceptionResponse() {

        ExceptionResponse exceptionResponse = new ExceptionResponse();

        LocalDateTime localDateTime = LocalDateTime.now();

        exceptionResponse.setTimestamp(localDateTime);
        exceptionResponse.setMessage("This is a new message");
        assertEquals(localDateTime, exceptionResponse.getTimestamp(),
                     "The timestamp should match the timestamp created");
        assertEquals("This is a new message", exceptionResponse.getMessage(),
                     "The message should match the message passed in");
    }
}
