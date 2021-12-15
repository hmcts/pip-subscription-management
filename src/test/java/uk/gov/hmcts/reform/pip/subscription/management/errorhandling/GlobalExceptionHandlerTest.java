package uk.gov.hmcts.reform.pip.subscription.management.errorhandling;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.subscription.management.models.SearchType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private static final String BAD_REQUEST_ASSERTION = "Status code should be of type: Not Found";

    @Mock
    InvalidFormatException invalidFormatException;

    @Mock
    MethodArgumentNotValidException methodArgumentNotValidException;

    @Mock
    BindingResult bindingResult;


    @Test
    void testHandleSubscriptionNotFoundMethod() {

        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

        SubscriptionNotFoundException subscriptionNotFoundException
            = new SubscriptionNotFoundException("This is a test message");


        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        ServletWebRequest servletWebRequest = new ServletWebRequest(mockHttpServletRequest);

        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handleSubscriptionNotFound(subscriptionNotFoundException, servletWebRequest);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode(), "Status code should be not found");
        assertNotNull(responseEntity.getBody(), "Response should contain a body");
        assertEquals("This is a test message", responseEntity.getBody().getMessage(),
                     "The message should match the message passed in");
    }

    @Test
    @DisplayName("Tests that the response entity returned from exception handler in case of "
        + "methodargumentnotvalidexception contains the expected status code and body")
    void testMethodArgumentNotValidException() {
        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();
        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getErrorCount()).thenReturn(1);
        FieldError newFieldError = new FieldError("hello", "hello", "Hello");
        when(methodArgumentNotValidException.getFieldErrors()).thenReturn(List.of(newFieldError));
        ObjectError newObjectError = new ObjectError("must not be null", "must not be null");
        when(bindingResult.getAllErrors()).thenReturn(List.of(newObjectError));
        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handleArgumentNotValid(methodArgumentNotValidException);
        assertTrue(responseEntity.getBody().getMessage().contains("must not be null"), "Incorrect response text");
        assertTrue(responseEntity.getBody().getMessage().contains("Bad Request: "), "Incorrect response type");
    }

    @Test
    @DisplayName("Tests that the response entity returned from the exception handler in case of "
        + "invalidformatexception contains the correct status code and body")
    void testInvalidFormatException() throws ClassNotFoundException {

        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();
        doReturn(SearchType.class).when(invalidFormatException).getTargetType();
        when(invalidFormatException.getValue()).thenReturn("valueString");
        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handleInvalidFormat(invalidFormatException);
        assertTrue(responseEntity.getBody().getMessage().contains("Bad Request: "), "Incorrect response");
        assertTrue(responseEntity.getBody().getMessage().contains("COURT_ID CASE_ID CASE_URN"), "Incorrect response "
            + "text");
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), BAD_REQUEST_ASSERTION);

    }
}