package uk.gov.hmcts.reform.pip.subscription.management.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.Application;
import uk.gov.hmcts.reform.pip.subscription.management.config.WebClientConfigurationTest;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {Application.class, WebClientConfigurationTest.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("functional")
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
class TestingSupportControllerTests {
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String TESTING_SUPPORT_BASE_URL = "/testing-support/";
    private static final String TESTING_SUPPORT_SUBSCRIPTION_URL = TESTING_SUPPORT_BASE_URL + "subscription/";

    private static final String SUBSCRIPTION_PATH = "/subscription";

    private static final String LOCATION_NAME_PREFIX = "TEST_123_";
    private static final String LOCATION_NAME = "Court1";

    private static final String USER_ID_HEADER = "x-user-id";
    private static final String ACTIONING_USER_ID = "1234-1234";

    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
    private static final String USER_ID = UUID.randomUUID().toString();
    private static final String CASE_ID = "T485913";

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void setup() {
        OBJECT_MAPPER.findAndRegisterModules();
    }

    @Test
    void testTestingSupportDeleteSubscriptionsWithLocationNamePrefix() throws Exception {
        Subscription subscription = createSubscription();

        MockHttpServletRequestBuilder postRequest = MockMvcRequestBuilders.post(SUBSCRIPTION_PATH)
            .content(OBJECT_MAPPER.writeValueAsString(subscription))
            .header(USER_ID_HEADER, ACTIONING_USER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(postRequest)
            .andExpect(status().isCreated());

        MvcResult deleteResponse = mockMvc.perform(delete(TESTING_SUPPORT_SUBSCRIPTION_URL + LOCATION_NAME_PREFIX))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(deleteResponse.getResponse().getContentAsString())
            .as("Subscription response does not match")
            .isEqualTo("1 subscription(s) deleted for location name starting with " + LOCATION_NAME_PREFIX);

        mockMvc.perform(get(SUBSCRIPTION_PATH + "/" + SUBSCRIPTION_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "unauthorized_isAuthorized", authorities = {"APPROLE_unknown.authorized"})
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void testUnauthorisedTestingSupportDeleteSubscriptions() throws Exception {
        mockMvc.perform(delete(TESTING_SUPPORT_SUBSCRIPTION_URL + LOCATION_NAME_PREFIX))
            .andExpect(status().isForbidden());
    }

    private Subscription createSubscription() {
        Subscription subscription = new Subscription();

        subscription.setId(SUBSCRIPTION_ID);
        subscription.setLocationName(LOCATION_NAME_PREFIX + LOCATION_NAME);
        subscription.setChannel(Channel.API_COURTEL);
        subscription.setSearchType(SearchType.CASE_ID);
        subscription.setSearchValue(CASE_ID);
        subscription.setCaseNumber(CASE_ID);
        subscription.setCreatedDate(LocalDateTime.now());
        subscription.setUserId(USER_ID);

        return subscription;
    }
}
