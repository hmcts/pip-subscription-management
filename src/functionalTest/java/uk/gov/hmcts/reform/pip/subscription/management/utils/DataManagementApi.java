package uk.gov.hmcts.reform.pip.subscription.management.utils;

import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders.AUTHORIZATION;
import static io.restassured.RestAssured.given;
import static uk.gov.hmcts.reform.pip.subscription.management.utils.TestUtil.BEARER;

@Component
public class DataManagementApi {
    private static final String TESTING_SUPPORT_LOCATION_URL = "/testing-support/location/";

    @Autowired
    private OAuthClient authClient;

    @Value("${service-to-service.data-management}")
    private String dataManagementUrl;

    public void testingSupportCreateLocation(String locationId, String locationName) {
        RestAssured.baseURI = dataManagementUrl;
        given()
            .relaxedHTTPSValidation()
            .headers(getHeaders())
            .body(locationName)
            .when()
            .post(TESTING_SUPPORT_LOCATION_URL + locationId);
    }

    public void testingSupportDeleteLocation(String locationName) {
        RestAssured.baseURI = dataManagementUrl;
        given()
            .relaxedHTTPSValidation()
            .headers(getHeaders())
            .when()
            .delete(TESTING_SUPPORT_LOCATION_URL + locationName);
    }

    private Map<String, String> getHeaders() {
        String accessToken = authClient.generateAccessToken();
        return Map.of(AUTHORIZATION, BEARER + accessToken);
    }
}

