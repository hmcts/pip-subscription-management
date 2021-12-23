package uk.gov.hmcts.reform.pip.subscription.management.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.pip.subscription.management.Application;
import uk.gov.hmcts.reform.pip.subscription.management.config.RestTemplateConfig;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.ExceptionResponse;
import uk.gov.hmcts.reform.pip.subscription.management.models.Channel;
import uk.gov.hmcts.reform.pip.subscription.management.models.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionDto;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Court;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Hearing;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.UserSubscription;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {Application.class, RestTemplateConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SuppressWarnings("PMD.ExcessiveImports")
class SubscriptionControllerTests {

    private static final String COURT_NAME_1 = "Glasgow-Court-1";
    private static final String UUID_STRING = "f54c9783-7f56-4a69-91bc-55b582c0206f";

    private static final String VALIDATION_EMPTY_RESPONSE = "Returned response is empty";
    private static final String VALIDATION_CHANNEL_NAME = "Returned subscription channel "
        + "does not match expected channel";
    private static final String VALIDATION_SEARCH_TYPE = "Returned search type does not match expected type";
    private static final String VALIDATION_SEARCH_VALUE = "Returned search value does not match expected value";
    private static final String VALIDATION_USER_ID = "Returned user ID does not match expected user ID";

    private static final String COURT_ID = "53";
    private static final String CASE_ID = "T485913";
    private static final String CASE_URN = "IBRANE1BVW";
    private static final String CASE_NAME = "Tom Clancy";
    private static final String SUBSCRIPTION_USER_PATH = "/subscription/user/tom1";
    private static final String VALIDATION_COURT_ID = "Court ID returned does not match expected court ID";
    private static final String VALIDATION_COURT_NAME = "Court name returned does not match expected court name";
    private static final String VALIDATION_HEARING_ID = "Hearing ID does not match expected hearing";
    private static final String VALIDATION_CASE_ID = "Case ID does not match expected case";
    private static final String VALIDATION_CASE_URN = "Case URN does not match expected case";
    private static final String VALIDATION_COURT_LIST = "Court subscription list contains unknown courts";
    private static final String VALIDATION_SUBSCRIPTION_LIST = "The expected subscription is not displayed";
    private static final String VALIDATION_NO_SUBSCRIPTIONS = "User has unknown subscriptions";

    @Autowired
    protected MockMvc mvc;

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    protected static final String SUBSCRIPTION_PATH = "/subscription";
    protected static final SubscriptionDto SUBSCRIPTION = new SubscriptionDto();

    @BeforeAll
    static void setup() {
        OBJECT_MAPPER.findAndRegisterModules();
        SUBSCRIPTION.setChannel(Channel.API);
        SUBSCRIPTION.setSearchType(SearchType.COURT_ID);
        SUBSCRIPTION.setUserId("tom1");
    }

    protected MockHttpServletRequestBuilder setupMockSubscription(String searchValue) throws JsonProcessingException {

        SUBSCRIPTION.setSearchValue(searchValue);
        return MockMvcRequestBuilders.post(SUBSCRIPTION_PATH)
            .content(OBJECT_MAPPER.writeValueAsString(SUBSCRIPTION))
            .contentType(MediaType.APPLICATION_JSON);
    }

    protected MockHttpServletRequestBuilder setupMockSubscription(String searchValue, SearchType searchType)
        throws JsonProcessingException {

        SUBSCRIPTION.setSearchType(searchType);
        return setupMockSubscription(searchValue);
    }

    protected MockHttpServletRequestBuilder getSubscriptionByUuid(String searchValue) {
        return get(SUBSCRIPTION_PATH + '/' + searchValue);
    }

    protected MockHttpServletRequestBuilder setupRawJsonSubscription(String json) {
        return MockMvcRequestBuilders.post(SUBSCRIPTION_PATH)
            .content(json)
            .contentType(MediaType.APPLICATION_JSON);
    }

    @DisplayName("Post a new subscription and then get it from db.")
    @Test
    void postEndpoint() throws Exception {
        MockHttpServletRequestBuilder mappedSubscription = setupMockSubscription(COURT_NAME_1);

        MvcResult response = mvc.perform(mappedSubscription).andExpect(status().isOk()).andReturn();
        assertNotNull(response.getResponse().getContentAsString(), VALIDATION_EMPTY_RESPONSE);

        String subscriptionResponse = response.getResponse().getContentAsString();
        String ourUuid =
            Arrays.stream(subscriptionResponse.split(" ")).max(Comparator.comparingInt(String::length))
                .orElse(null);

        MvcResult getResponse = mvc.perform(getSubscriptionByUuid(ourUuid)).andReturn();
        Subscription returnedSubscription = OBJECT_MAPPER.readValue(
            getResponse.getResponse().getContentAsString(),
            Subscription.class
        );
        assertEquals(
            SUBSCRIPTION.getChannel(),
            returnedSubscription.getChannel(),
            VALIDATION_CHANNEL_NAME
        );
        assertEquals(
            SUBSCRIPTION.getSearchType(),
            returnedSubscription.getSearchType(),
            VALIDATION_SEARCH_TYPE
        );
        assertEquals(
            SUBSCRIPTION.getSearchValue(),
            returnedSubscription.getSearchValue(),
            VALIDATION_SEARCH_VALUE
        );
        assertEquals(
            SUBSCRIPTION.getUserId(),
            returnedSubscription.getUserId(),
            VALIDATION_USER_ID
        );
        assertNotEquals(
            returnedSubscription.getId(), 0L, "id should not equal zero"
        );
    }

    @DisplayName("Ensure post endpoint actually posts a subscription to db")
    @Test
    void checkPostToDb() throws Exception {
        MockHttpServletRequestBuilder mappedSubscription = setupMockSubscription(COURT_NAME_1);

        MvcResult response = mvc.perform(mappedSubscription).andExpect(status().isOk()).andReturn();
        assertNotNull(response.getResponse().getContentAsString(), VALIDATION_EMPTY_RESPONSE);

        String subscriptionResponse = response.getResponse().getContentAsString();
        String ourUuid =
            Arrays.stream(subscriptionResponse.split(" ")).max(Comparator.comparingInt(String::length))
                .orElse(null);

        MvcResult getResponse = mvc.perform(getSubscriptionByUuid(ourUuid)).andReturn();
        Subscription returnedSubscription = OBJECT_MAPPER.readValue(
            getResponse.getResponse().getContentAsString(),
            Subscription.class
        );
        MvcResult findResponse = mvc.perform(get(String.format(
            "/subscription/%s",
            returnedSubscription.getId()
        ))).andExpect(status().isOk()).andReturn();
        assertNotNull(findResponse.getResponse(), VALIDATION_EMPTY_RESPONSE);

        String subscriptionResponse2 = findResponse.getResponse().getContentAsString();
        Subscription returnedSubscription2 = OBJECT_MAPPER.readValue(subscriptionResponse2, Subscription.class);

        assertEquals(
            SUBSCRIPTION.getChannel(),
            returnedSubscription2.getChannel(),
            VALIDATION_CHANNEL_NAME
        );
        assertEquals(
            SUBSCRIPTION.getSearchType(),
            returnedSubscription2.getSearchType(),
            VALIDATION_SEARCH_TYPE
        );
        assertEquals(
            SUBSCRIPTION.getSearchValue(),
            returnedSubscription2.getSearchValue(),
            VALIDATION_SEARCH_VALUE
        );
        assertEquals(
            SUBSCRIPTION.getUserId(),
            returnedSubscription2.getUserId(),
            VALIDATION_USER_ID
        );
        assertNotEquals(
            returnedSubscription2.getId(), 0L, "id should not equal zero"
        );

    }

    @DisplayName("Checks for bad request for invalid searchType enum.")
    @Test
    void checkSearchTypeEnum() throws Exception {
        MockHttpServletRequestBuilder brokenSubscription = setupRawJsonSubscription(
            "{'searchType': 'INVALID_TYPE'}");
        MvcResult response = mvc.perform(brokenSubscription).andExpect(status().isBadRequest()).andReturn();
        assertEquals(400, response.getResponse().getStatus(), "Incorrect response - should be 400.");
    }

    @DisplayName("Checks for bad request for invalid channel enum.")
    @Test
    void checkChannelEnum() throws Exception {
        MockHttpServletRequestBuilder brokenSubscription = setupRawJsonSubscription(
            "{'channel': 'INVALID_TYPE'}");
        MvcResult response = mvc.perform(brokenSubscription).andExpect(status().isBadRequest()).andReturn();
        assertEquals(400, response.getResponse().getStatus(), "Incorrect response - should be 400.");

    }

    @DisplayName("Checks for bad request when empty json is sent")
    @Test
    void checkEmptyPost() throws Exception {
        MockHttpServletRequestBuilder brokenSubscription = setupRawJsonSubscription("{}");
        MvcResult response = mvc.perform(brokenSubscription).andExpect(status().isBadRequest()).andReturn();
        assertEquals(400, response.getResponse().getStatus(), "Incorrect response - should be 400.");
    }

    @DisplayName("Delete an individual subscription")
    @Test
    void deleteEndpoint() throws Exception {
        MockHttpServletRequestBuilder mappedSubscription = setupMockSubscription(COURT_NAME_1);

        MvcResult response = mvc.perform(mappedSubscription).andExpect(status().isOk()).andReturn();
        assertNotNull(response.getResponse().getContentAsString(), VALIDATION_EMPTY_RESPONSE);
        String subscriptionResponse = response.getResponse().getContentAsString();
        String ourUuid =
            Arrays.stream(subscriptionResponse.split(" ")).max(Comparator.comparingInt(String::length))
                .orElse(null);

        MvcResult getResponse = mvc.perform(getSubscriptionByUuid(ourUuid)).andReturn();
        Subscription returnedSubscription = OBJECT_MAPPER.readValue(
            getResponse.getResponse().getContentAsString(),
            Subscription.class
        );
        MvcResult deleteResponse = mvc.perform(delete(String.format(
            "/subscription/%s",
            returnedSubscription.getId()
        ))).andExpect(status().isOk()).andReturn();
        assertNotNull(deleteResponse.getResponse(), VALIDATION_EMPTY_RESPONSE);
        assertEquals(
            String.format("Subscription: %s was deleted", returnedSubscription.getId()),
            deleteResponse.getResponse().getContentAsString(),
            "Responses are not equal"
        );
    }

    @DisplayName("Check response if delete fails")
    @Test
    void failedDelete() throws Exception {
        String randomUuid = UUID_STRING;
        MvcResult response = mvc.perform(delete("/subscription/" + randomUuid))
            .andExpect(status().isNotFound()).andReturn();
        assertNotNull(response.getResponse().getContentAsString(), VALIDATION_EMPTY_RESPONSE);

        String errorResponse = response.getResponse().getContentAsString();
        ExceptionResponse exceptionResponse = OBJECT_MAPPER.readValue(errorResponse, ExceptionResponse.class);

        assertEquals(
            "No subscription found with the subscription id " + randomUuid,
            exceptionResponse.getMessage(),
            "Incorrect status code"
        );

    }

    @DisplayName("Check response if findBySubId fails")
    @Test
    void failedFind() throws Exception {
        String randomUuid = UUID_STRING;
        MvcResult response = mvc.perform(get("/subscription/" + randomUuid)).andExpect(status()
                                                                                           .isNotFound()).andReturn();
        assertNotNull(response.getResponse().getContentAsString(), VALIDATION_EMPTY_RESPONSE);

        String errorResponse = response.getResponse().getContentAsString();
        ExceptionResponse exceptionResponse = OBJECT_MAPPER.readValue(errorResponse, ExceptionResponse.class);

        assertEquals(
            "No subscription found with the subscription id " + randomUuid,
            exceptionResponse.getMessage(),
            "Incorrect status code"
        );
    }

    @Test
    void testGetUsersSubscriptionsByUserIdSuccessful() throws Exception {
        mvc.perform(setupMockSubscription(COURT_ID, SearchType.COURT_ID));
        mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID));
        mvc.perform(setupMockSubscription(CASE_URN, SearchType.CASE_URN));

        MvcResult response = mvc.perform(get(SUBSCRIPTION_USER_PATH))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(response.getResponse(), VALIDATION_EMPTY_RESPONSE);

        List<UserSubscription> userSubscriptions =
            Arrays.asList(
                OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), UserSubscription[].class));

        assertEquals(3, userSubscriptions.size(),
                     VALIDATION_SUBSCRIPTION_LIST);

        UserSubscription userSubscription = userSubscriptions.get(0);
        assertEquals(1, userSubscription.getCourtSubscriptions().size(),
                     "Court subscription list does not contain 1 court");

        assertEquals(0, userSubscription.getCaseSubscriptions().size(),
                     "Case subscription list does not contain 0 cases");

        Court court = userSubscription.getCourtSubscriptions().get(0);
        assertEquals(Integer.valueOf(COURT_ID), court.getCourtId(),
                     VALIDATION_COURT_ID);
        assertEquals("Blackpool Magistrates' Court", court.getName(),
                     VALIDATION_COURT_NAME);

        userSubscription = userSubscriptions.get(1);
        assertEquals(0, userSubscription.getCourtSubscriptions().size(),
                     "Court subscription list does not contain 0 courts");

        assertEquals(1, userSubscription.getCaseSubscriptions().size(),
                     "Case subscription list does not contain 1 case");
        Hearing hearing1 = userSubscription.getCaseSubscriptions().get(0);
        assertEquals(1, hearing1.getHearingId(), VALIDATION_HEARING_ID);
        assertEquals(CASE_ID, hearing1.getCaseNumber(), VALIDATION_CASE_ID);
        assertEquals("N363N6R4OG", hearing1.getUrn(), VALIDATION_CASE_URN);

        userSubscription = userSubscriptions.get(2);
        assertEquals(0, userSubscription.getCourtSubscriptions().size(),
                     "Court subscription list does not contain 0 courts");

        assertEquals(1, userSubscription.getCaseSubscriptions().size(),
                     "Case subscription list does not contain 1 case");
        Hearing hearing2 = userSubscription.getCaseSubscriptions().get(0);
        assertEquals(2, hearing2.getHearingId(), VALIDATION_HEARING_ID);
        assertEquals("T485914", hearing2.getCaseNumber(), VALIDATION_CASE_ID);
        assertEquals(CASE_URN, hearing2.getUrn(), VALIDATION_CASE_URN);
    }

    @Test
    void testGetUsersSubscriptionsByUserIdSingleCourt() throws Exception {
        mvc.perform(setupMockSubscription(COURT_ID, SearchType.COURT_ID));

        MvcResult response = mvc.perform(get(SUBSCRIPTION_USER_PATH))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(response.getResponse(), VALIDATION_EMPTY_RESPONSE);

        List<UserSubscription> userSubscriptions =
            Arrays.asList(
                OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), UserSubscription[].class));

        assertEquals(1, userSubscriptions.size(),
                     VALIDATION_SUBSCRIPTION_LIST);

        UserSubscription userSubscription = userSubscriptions.get(0);

        assertEquals(1, userSubscription.getCourtSubscriptions().size(),
                     "Court subscription list does not contain 1 court");

        assertEquals(0, userSubscription.getCaseSubscriptions().size(),
                     "Court subscription list contains unknown cases");

        Court court = userSubscription.getCourtSubscriptions().get(0);
        assertEquals(Integer.valueOf(COURT_ID), court.getCourtId(),
                     VALIDATION_COURT_ID);
        assertEquals("Blackpool Magistrates' Court", court.getName(),
                     VALIDATION_COURT_NAME);
    }

    @Test
    void testGetUsersSubscriptionsByUserIdSingleCaseId() throws Exception {
        mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID));

        MvcResult response = mvc.perform(get(SUBSCRIPTION_USER_PATH))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(response.getResponse(), VALIDATION_EMPTY_RESPONSE);

        List<UserSubscription> userSubscriptions =
            Arrays.asList(
                OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), UserSubscription[].class));

        assertEquals(1, userSubscriptions.size(),
                     VALIDATION_SUBSCRIPTION_LIST);

        UserSubscription userSubscription = userSubscriptions.get(0);

        assertEquals(0, userSubscription.getCourtSubscriptions().size(),
                     VALIDATION_COURT_LIST);

        assertEquals(1, userSubscription.getCaseSubscriptions().size(),
                     "Court subscription list does not contain 1 case");

        Hearing hearing1 = userSubscription.getCaseSubscriptions().get(0);
        assertEquals(1, hearing1.getHearingId(), VALIDATION_HEARING_ID);
        assertEquals(CASE_ID, hearing1.getCaseNumber(), VALIDATION_CASE_ID);
        assertEquals("N363N6R4OG", hearing1.getUrn(), VALIDATION_CASE_URN);
    }

    @Test
    void testGetUsersSubscriptionsByUserIdSingleCaseUrn() throws Exception {
        mvc.perform(setupMockSubscription(CASE_URN, SearchType.CASE_URN));

        MvcResult response = mvc.perform(get(SUBSCRIPTION_USER_PATH))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(response.getResponse(), VALIDATION_EMPTY_RESPONSE);

        List<UserSubscription> userSubscriptions =
            Arrays.asList(
                OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), UserSubscription[].class));

        assertEquals(1, userSubscriptions.size(),
                     VALIDATION_SUBSCRIPTION_LIST);

        UserSubscription userSubscription = userSubscriptions.get(0);

        assertEquals(0, userSubscription.getCourtSubscriptions().size(),
                     VALIDATION_COURT_LIST);

        assertEquals(1, userSubscription.getCaseSubscriptions().size(),
                     "Court subscription list does not contain 1 case");

        Hearing hearing1 = userSubscription.getCaseSubscriptions().get(0);
        assertEquals(2, hearing1.getHearingId(), VALIDATION_HEARING_ID);
        assertEquals("T485914", hearing1.getCaseNumber(), VALIDATION_CASE_ID);
        assertEquals(CASE_URN, hearing1.getUrn(), VALIDATION_CASE_URN);
    }

    @Test
    void testGetUsersSubscriptionsByUserIdSingleCaseName() throws Exception {
        mvc.perform(setupMockSubscription(CASE_NAME, SearchType.CASE_NAME));

        MvcResult response = mvc.perform(get(SUBSCRIPTION_USER_PATH))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(response.getResponse(), VALIDATION_EMPTY_RESPONSE);

        List<UserSubscription> userSubscriptions =
            Arrays.asList(
                OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), UserSubscription[].class));

        assertEquals(1, userSubscriptions.size(),
                     VALIDATION_SUBSCRIPTION_LIST);

        UserSubscription userSubscription = userSubscriptions.get(0);

        assertEquals(0, userSubscription.getCourtSubscriptions().size(),
                     VALIDATION_COURT_LIST);

        assertEquals(1, userSubscription.getCaseSubscriptions().size(),
                     "Court subscription list does not contain 1 case");

        Hearing hearing1 = userSubscription.getCaseSubscriptions().get(0);
        assertEquals(1, hearing1.getHearingId(), VALIDATION_HEARING_ID);
        assertEquals("T485913", hearing1.getCaseNumber(), VALIDATION_CASE_ID);
        assertEquals("N363N6R4OG", hearing1.getUrn(), VALIDATION_CASE_URN);
    }

    @Test
    void testGetUsersSubscriptionsByUserIdNoSubscriptions() throws Exception {
        MvcResult response = mvc.perform(get(SUBSCRIPTION_USER_PATH))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(response.getResponse(), VALIDATION_EMPTY_RESPONSE);

        List<UserSubscription> userSubscriptions =
            Arrays.asList(
                OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), UserSubscription[].class));

        assertEquals(0, userSubscriptions.size(),
                     VALIDATION_NO_SUBSCRIPTIONS);
    }

    @Test
    void testGetUsersSubscriptionsWithDatabaseInvalidSearchValue() throws Exception {
        mvc.perform(setupMockSubscription("1234", SearchType.CASE_ID));

        MvcResult response = mvc.perform(get(SUBSCRIPTION_USER_PATH))
            .andExpect(status().isBadGateway())
            .andReturn();

        assertNotNull(response.getResponse(), VALIDATION_EMPTY_RESPONSE);

        ExceptionResponse exceptionResponse =
            OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), ExceptionResponse.class);

        assertTrue(exceptionResponse.getMessage().contains("No hearing found for case number: 1234"),
                   "Error message not present for unknown case number");
    }

}
