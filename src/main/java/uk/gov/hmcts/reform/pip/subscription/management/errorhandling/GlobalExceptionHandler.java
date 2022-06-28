package uk.gov.hmcts.reform.pip.subscription.management.errorhandling;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.SubscriptionNotFoundException;

import java.time.LocalDateTime;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

/**
 * Global exception handler, that captures exceptions thrown by the controllers, and encapsulates
 * the logic to handle them and return a standardised response to the user.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Template exception handler, that handles a custom SubscriptionNotFoundException,
     * and returns a 404 in the standard format.
     *
     * @param ex      The exception that has been thrown.
     * @return The error response, modelled using the ExceptionResponse object.
     */

    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handleSubscriptionNotFound(
        SubscriptionNotFoundException ex, WebRequest request) {

        log.error(writeLog(
            "404, Subscription has not been found. Cause: " + ex.getMessage()));

        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(ex.getMessage());
        exceptionResponse.setTimestamp(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exceptionResponse);
    }

    /**
     * This exception creates the following style of message:
     * "Bad Request: userId must not be null, searchValue must not be blank" etc.
     *
     * @param ex - a MethodArgumentNotValidException, created when an invalid json object is passed as a parameter to
     *           the post endpoint (e.g. an empty request)
     * @return - a ResponseEntity containing the exception response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handle(MethodArgumentNotValidException ex) {

        ExceptionResponse exceptionResponse = new ExceptionResponse();
        StringBuilder responseText = new StringBuilder("Bad Request: ");
        for (int i = 0; i < ex.getBindingResult().getErrorCount(); i++) {
            responseText.append(ex.getFieldErrors().get(i).getField());
            responseText.append(' ');
            responseText.append(ex.getBindingResult().getAllErrors().get(i).getDefaultMessage());
            responseText.append(", ");
        }
        exceptionResponse.setMessage(responseText.substring(0, responseText.length() - 2));
        exceptionResponse.setTimestamp(LocalDateTime.now());

        log.error(writeLog(
            "400, Invalid argument provided when creating subscriptions. Cause: "
                + exceptionResponse.getMessage()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exceptionResponse);
    }

    /**
     * This will create a message of the following style:
     * "Bad Request: SearchType {search} should be one of the following types: [ LOCATION_ID CASE_ID CASE_URN ]"
     * Note: this exception also covers the "channel" enum in the same way.
     *
     * @param ex - an invalidformatexception, created when e.g. a value that does not adhere to an enum restriction
     * @return - a ResponseEntity containing the exception response
     */
    @ExceptionHandler(InvalidFormatException.class)
    public ResponseEntity<ExceptionResponse> handle(InvalidFormatException ex) {
        StringBuilder responseText = new StringBuilder(100);
        responseText.append("Bad Request: ").append(ex.getTargetType().getSimpleName()).append(' ')
            .append(ex.getValue()).append(" should be one of the following types: [ ");
        for (int i = 0; i < ex.getTargetType().getFields().length; i++) {
            responseText.append(ex.getTargetType().getFields()[i].getName());
            responseText.append(' ');
        }
        responseText.append(']');
        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(responseText.toString());
        exceptionResponse.setTimestamp(LocalDateTime.now());

        log.error(writeLog(
            "400, Invalid argument provided when creating subscriptions. Cause: " + ex.getMessage()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exceptionResponse);
    }
}
