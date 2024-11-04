package uk.gov.hmcts.reform.pip.subscription.management.utils;

import org.springframework.beans.factory.annotation.Value;

import java.util.Random;

@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class TestUtil {
    public static final String BEARER = "Bearer ";

    @Value("${service-to-service.data-management}")
    private String dataManagementUrl;

    private TestUtil() {
    }

    public static String randomLocationId() {
        Random number = new Random(System.currentTimeMillis());
        Integer randomNumber = 10_000 + number.nextInt(20_000);
        return randomNumber.toString();
    }
}
