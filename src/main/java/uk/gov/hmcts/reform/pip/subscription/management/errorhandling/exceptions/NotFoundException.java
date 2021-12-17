package uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions;

public class NotFoundException extends RuntimeException{

    private static final long serialVersionUID = -4323013763492278931L;

    /**
     * Constructor for the Exception.
     * @param message The message to return to the end user
     */
    public NotFoundException(String message) {
        super(message);
    }
}
