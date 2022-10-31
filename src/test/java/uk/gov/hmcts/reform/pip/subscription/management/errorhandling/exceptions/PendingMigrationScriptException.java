package uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions;

/**
 * Exception class which captures Flyway exceptions.
 */
public class PendingMigrationScriptException extends RuntimeException {

    private static final long serialVersionUID = -7333237701605622780L;

    public PendingMigrationScriptException(String script) {
        super("Found migration not yet applied: " + script);
    }

}
