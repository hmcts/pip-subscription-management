package uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions;

public class HearingNotFoundException extends NotFoundException {

    private static final long serialVersionUID = -5055482438766866116L;

    /**
     * Constructor for the Exception.
     * @param message The message to return to the end user
     */
    public HearingNotFoundException(String message) {
        super(message);
    }
}
