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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {Application.class, RestTemplateConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = "integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class SubscriptionControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String COURT_NAME_1 = "Blackpool Magistrates' Court";
    private static final String UUID_STRING = "f54c9783-7f56-4a69-91bc-55b582c0206f";

    private static final String VALIDATION_EMPTY_RESPONSE = "Returned response is empty";
    private static final String VALIDATION_CHANNEL_NAME = "Returned subscription channel "
        + "does not match expected channel";
    private static final String VALIDATION_SEARCH_TYPE = "Returned search type does not match expected type";
    private static final String VALIDATION_SEARCH_VALUE = "Returned search value does not match expected value";
    private static final String VALIDATION_USER_ID = "Returned user ID does not match expected user ID";
    private static final String VALIDATION_CASE_NAME = "Returned case name does not match expected case name";
    private static final String VALIDATION_CASE_NUMBER = "Returned case number does not match expected case number";
    private static final String VALIDATION_CASE_URN = "Returned URN does not match expected URN";
    private static final String VALIDATION_COURT_NAME = "Returned court name does not match expected court name";
    public static final String VALIDATION_BAD_REQUEST = "Incorrect response - should be 400.";

    private static final String RAW_JSON_MISSING_SEARCH_VALUE =
        "{\"userId\": \"3\", \"searchType\": \"CASE_ID\",\"channel\": \"EMAIL\"}";
    private static final String RAW_JSON_MISSING_SEARCH_TYPE =
        "{\"userId\": \"3\", \"searchType\": \"123\",\"channel\": \"EMAIL\"}";
    private static final String RAW_JSON_MISSING_CHANNEL =
        "{\"userId\": \"3\", \"searchType\": \"CASE_ID\",\"searchValue\": \"321\"}";

    private static final String COURT_ID = "53";
    private static final String CASE_ID = "T485913";
    private static final String CASE_URN = "IBRANE1BVW";
    private static final LocalDateTime DATE_ADDED = LocalDateTime.now();

    @Autowired
    protected MockMvc mvc;

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
        SUBSCRIPTION.setCaseNumber(CASE_ID);
        SUBSCRIPTION.setUrn(CASE_URN);
        SUBSCRIPTION.setCreatedDate(DATE_ADDED);
        return MockMvcRequestBuilders.post(SUBSCRIPTION_PATH)
            .content(OBJECT_MAPPER.writeValueAsString(SUBSCRIPTION))
            .contentType(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder getSubscriptionByUuid(String searchValue) throws Exception {
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
        MockHttpServletRequestBuilder mappedSubscription = setupMockSubscription(COURT_ID);

        MvcResult response = mvc.perform(mappedSubscription).andExpect(status().isCreated()).andReturn();
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
        assertEquals(SUBSCRIPTION.getCaseName(), returnedSubscription.getCaseName(),
                     VALIDATION_CASE_NAME);
        assertEquals(SUBSCRIPTION.getCaseNumber(), returnedSubscription.getCaseNumber(),
                     VALIDATION_CASE_NUMBER);
        assertEquals(SUBSCRIPTION.getUrn(), returnedSubscription.getUrn(),
                     VALIDATION_CASE_URN);
        assertEquals(COURT_NAME_1, returnedSubscription.getCourtName(),
                     VALIDATION_COURT_NAME);
    }

    @DisplayName("Ensure post endpoint actually posts a subscription to db")
    @Test
    void checkPostToDb() throws Exception {
        MockHttpServletRequestBuilder mappedSubscription = setupMockSubscription(COURT_ID);

        MvcResult response = mvc.perform(mappedSubscription).andExpect(status().isCreated()).andReturn();
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
        assertEquals(SUBSCRIPTION.getCaseName(), returnedSubscription.getCaseName(),
                     VALIDATION_CASE_NAME);
        assertEquals(SUBSCRIPTION.getCaseNumber(), returnedSubscription.getCaseNumber(),
                     VALIDATION_CASE_NUMBER);
        assertEquals(SUBSCRIPTION.getUrn(), returnedSubscription.getUrn(),
                     VALIDATION_CASE_URN);
        assertEquals(COURT_NAME_1, returnedSubscription.getCourtName(),
                     VALIDATION_COURT_NAME);

    }

    @DisplayName("Checks for bad request for invalid searchType enum.")
    @Test
    void checkSearchTypeEnum() throws Exception {
        MockHttpServletRequestBuilder brokenSubscription = setupRawJsonSubscription(
            "{'searchType': 'INVALID_TYPE'}");
        MvcResult response = mvc.perform(brokenSubscription).andExpect(status().isBadRequest()).andReturn();
        assertEquals(400, response.getResponse().getStatus(), VALIDATION_BAD_REQUEST);
    }

    @DisplayName("Checks for bad request for invalid channel enum.")
    @Test
    void checkChannelEnum() throws Exception {
        MockHttpServletRequestBuilder brokenSubscription = setupRawJsonSubscription(
            "{'channel': 'INVALID_TYPE'}");
        MvcResult response = mvc.perform(brokenSubscription).andExpect(status().isBadRequest()).andReturn();
        assertEquals(400, response.getResponse().getStatus(), VALIDATION_BAD_REQUEST);

    }

    @DisplayName("Checks for bad request when empty json is sent")
    @Test
    void checkEmptyPost() throws Exception {
        MockHttpServletRequestBuilder brokenSubscription = setupRawJsonSubscription("{}");
        MvcResult response = mvc.perform(brokenSubscription).andExpect(status().isBadRequest()).andReturn();
        assertEquals(400, response.getResponse().getStatus(), VALIDATION_BAD_REQUEST);
    }

    @Test
    void checkMissingSearchType() throws Exception {
        MockHttpServletRequestBuilder brokenSubscription = setupRawJsonSubscription(RAW_JSON_MISSING_SEARCH_TYPE);
        MvcResult response = mvc.perform(brokenSubscription).andExpect(status().isBadRequest()).andReturn();
        assertEquals(400, response.getResponse().getStatus(), VALIDATION_BAD_REQUEST);
    }

    @Test
    void checkMissingSearchValue() throws Exception {
        MockHttpServletRequestBuilder brokenSubscription = setupRawJsonSubscription(RAW_JSON_MISSING_SEARCH_VALUE);
        MvcResult response = mvc.perform(brokenSubscription).andExpect(status().isBadRequest()).andReturn();
        assertEquals(400, response.getResponse().getStatus(), VALIDATION_BAD_REQUEST);
    }

    @Test
    void checkMissingChannel() throws Exception {
        MockHttpServletRequestBuilder brokenSubscription = setupRawJsonSubscription(RAW_JSON_MISSING_CHANNEL);
        MvcResult response = mvc.perform(brokenSubscription).andExpect(status().isBadRequest()).andReturn();
        assertEquals(400, response.getResponse().getStatus(), VALIDATION_BAD_REQUEST);
    }

    @DisplayName("Delete an individual subscription")
    @Test
    void deleteEndpoint() throws Exception {
        MockHttpServletRequestBuilder mappedSubscription = setupMockSubscription(COURT_ID);

        MvcResult response = mvc.perform(mappedSubscription).andExpect(status().isCreated()).andReturn();
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
            String.format("Subscription: %s deleted", returnedSubscription.getId()),
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

}

