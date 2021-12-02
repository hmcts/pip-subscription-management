package uk.gov.hmcts.reform.pip.subscription.management.errorhandling;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.SubscriptionNotFoundException;

import java.time.LocalDateTime;

/**
 * Global exception handler, that captures exceptions thrown by the controllers, and encapsulates
 * the logic to handle them and return a standardised response to the user.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Template exception handler, that handles a custom SubscriptionNotFoundException,
     * and returns a 404 in the standard format.
     *
     * @param ex      The exception that has been thrown.
     * @param request The request made to the endpoint.
     * @return The error response, modelled using the ExceptionResponse object.
     */
    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handleSubscriptionNotFound(
        SubscriptionNotFoundException ex, WebRequest request) {

        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(ex.getMessage());
        exceptionResponse.setTimestamp(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exceptionResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleArgumentNotValid(
        MethodArgumentNotValidException ex, WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse();
        StringBuilder responseText = new StringBuilder("Bad Request: ");
        for (int i = 0; i < ex.getBindingResult().getErrorCount(); i++) {
            responseText.append(ex.getFieldErrors().get(i).getField());
            responseText.append(' ');
            responseText.append(ex.getBindingResult().getAllErrors().get(i).getDefaultMessage());
            responseText.append(", ");
        }
        exceptionResponse.setMessage(responseText.toString());
        exceptionResponse.setTimestamp(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exceptionResponse);
    }

    @ExceptionHandler(InvalidFormatException.class)
    public ResponseEntity<ExceptionResponse> handleArgumentNotValid(
        InvalidFormatException ex, WebRequest request) {
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

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exceptionResponse);
    }

}
