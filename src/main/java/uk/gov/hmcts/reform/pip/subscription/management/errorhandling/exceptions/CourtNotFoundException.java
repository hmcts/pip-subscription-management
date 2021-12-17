package uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions;

public class CourtNotFoundException extends NotFoundException {

    private static final long serialVersionUID = 3268666752467763178L;

    /**
     * Constructor for the Exception.
     * @param message The message to return to the end user
     */
    public CourtNotFoundException(String message) {
        super(message);
    }
}
