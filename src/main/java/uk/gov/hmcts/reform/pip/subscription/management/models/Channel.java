package uk.gov.hmcts.reform.pip.subscription.management.models;

public enum Channel {
    // add as required - additional api values to have the route of API
    EMAIL("EMAIL"),
    API_COURTEL("API");

    public final String notificationRoute;

    Channel(String notificationRoute) {
        this.notificationRoute = notificationRoute;
    }
}
