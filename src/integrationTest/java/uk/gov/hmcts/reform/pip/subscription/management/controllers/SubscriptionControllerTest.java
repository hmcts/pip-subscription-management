package uk.gov.hmcts.reform.pip.subscription.management.controllers;


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
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.ExceptionResponse;
import uk.gov.hmcts.reform.pip.subscription.management.models.Channel;
import uk.gov.hmcts.reform.pip.subscription.management.models.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionDto;

import java.util.Arrays;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = "test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class SubscriptionControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static SubscriptionDto subscription;

    private static final String COURT_NAME_1 = "Glasgow-Court-1";
    private static final String VALIDATION_EMPTY_RESPONSE = "Returned response is empty";
    private static final String VALIDATION_CHANNEL_NAME = "Returned subscription channel "
        + "does not match expected channel";
    private static final String VALIDATION_SEARCH_TYPE = "Returned search type does not match expected type";
    private static final String VALIDATION_SEARCH_VALUE = "Returned search value does not match expected value";
    private static final String VALIDATION_USER_ID = "Returned user ID does not match expected user ID";
    private static final String SUBSCRIPTION_PATH = "/subscription";
    private static final String UUID_STRING = "f54c9783-7f56-4a69-91bc-55b582c0206f";
    @Autowired
    private MockMvc mvc;


    @BeforeAll
    static void setup() {
        OBJECT_MAPPER.findAndRegisterModules();
        subscription = new SubscriptionDto();
        subscription.setChannel(Channel.API);
        subscription.setSearchType(SearchType.COURT_ID);
        subscription.setUserId("tom1");
    }


    private MockHttpServletRequestBuilder setupMockSubscription(String searchValue) throws Exception {

        subscription.setSearchValue(searchValue);
        return MockMvcRequestBuilders.post(SUBSCRIPTION_PATH)
            .content(OBJECT_MAPPER.writeValueAsString(subscription))
            .contentType(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder getSubscriptionbyuuid(String searchValue) throws Exception {
        return get(SUBSCRIPTION_PATH + '/' + searchValue);
    }

    private MockHttpServletRequestBuilder setupRawJsonSubscription(String json) throws Exception {
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

        MvcResult getResponse = mvc.perform(getSubscriptionbyuuid(ourUuid)).andReturn();
        Subscription returnedSubscription = OBJECT_MAPPER.readValue(
            getResponse.getResponse().getContentAsString(),
            Subscription.class
        );
        assertEquals(
            subscription.getChannel(),
            returnedSubscription.getChannel(),
            VALIDATION_CHANNEL_NAME
        );
        assertEquals(
            subscription.getSearchType(),
            returnedSubscription.getSearchType(),
            VALIDATION_SEARCH_TYPE
        );
        assertEquals(
            subscription.getSearchValue(),
            returnedSubscription.getSearchValue(),
            VALIDATION_SEARCH_VALUE
        );
        assertEquals(
            subscription.getUserId(),
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

        MvcResult getResponse = mvc.perform(getSubscriptionbyuuid(ourUuid)).andReturn();
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
            subscription.getChannel(),
            returnedSubscription2.getChannel(),
            VALIDATION_CHANNEL_NAME
        );
        assertEquals(
            subscription.getSearchType(),
            returnedSubscription2.getSearchType(),
            VALIDATION_SEARCH_TYPE
        );
        assertEquals(
            subscription.getSearchValue(),
            returnedSubscription2.getSearchValue(),
            VALIDATION_SEARCH_VALUE
        );
        assertEquals(
            subscription.getUserId(),
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

        MvcResult getResponse = mvc.perform(getSubscriptionbyuuid(ourUuid)).andReturn();
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
            deleteResponse.getResponse().getContentAsString(),
            String.format("Subscription %s deleted", returnedSubscription.getId()),
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

