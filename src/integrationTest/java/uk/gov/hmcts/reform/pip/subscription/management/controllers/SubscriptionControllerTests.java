package uk.gov.hmcts.reform.pip.subscription.management.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.web.dependencies.apachecommons.io.IOUtils;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.pip.subscription.management.Application;
import uk.gov.hmcts.reform.pip.subscription.management.config.WebClientConfigurationTest;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.ExceptionResponse;
import uk.gov.hmcts.reform.pip.subscription.management.models.Channel;
import uk.gov.hmcts.reform.pip.subscription.management.models.SearchType;
import uk.gov.hmcts.reform.pip.subscription.management.models.Subscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.SubscriptionDto;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.ListType;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.CaseSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.LocationSubscription;
import uk.gov.hmcts.reform.pip.subscription.management.models.response.UserSubscription;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {Application.class, WebClientConfigurationTest.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.LawOfDemeter"})
class SubscriptionControllerTests {

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String LOCATION_NAME_1 = "Single Justice Procedure";
    private static final String UUID_STRING = "f54c9783-7f56-4a69-91bc-55b582c0206f";
    private static final String VALID_USER_ID = "60e75e34-ad8e-4ac3-8f26-7de73e5c987b";

    private static final String VALIDATION_EMPTY_RESPONSE = "Returned response is empty";
    private static final String VALIDATION_CHANNEL_NAME = "Returned subscription channel "
        + "does not match expected channel";
    private static final String VALIDATION_SEARCH_TYPE = "Returned search type does not match expected type";
    private static final String VALIDATION_SEARCH_VALUE = "Returned search value does not match expected value";
    private static final String VALIDATION_USER_ID = "Returned user ID does not match expected user ID";
    private static final String VALIDATION_CASE_NAME = "Returned case name does not match expected case name";
    private static final String VALIDATION_CASE_NUMBER = "Returned case number does not match expected case number";
    private static final String VALIDATION_CASE_URN = "Returned URN does not match expected URN";
    private static final String VALIDATION_LOCATION_NAME =
        "Returned location name does not match expected location name";
    public static final String VALIDATION_BAD_REQUEST = "Incorrect response - should be 400.";
    private static final String VALIDATION_CASE_ID = "Case ID does not match expected case";
    private static final String VALIDATION_LOCATION_LIST = "Location subscription list contains unknown locations";
    private static final String VALIDATION_SUBSCRIPTION_LIST = "The expected subscription is not displayed";
    private static final String VALIDATION_NO_SUBSCRIPTIONS = "User has unknown subscriptions";
    public static final String VALIDATION_ONE_CASE_LOCATION = "Location subscription list does not contain 1 case";
    public static final String VALIDATION_DATE_ADDED = "Date added does not match the expected date added";
    private static final String FORBIDDEN_STATUS_CODE = "Status code does not match forbidden";
    private static final String RESPONSE_MATCH = "Response should match";
    private static final String SUBSCRIBER_REQUEST_SUCCESS = "Subscriber request has been accepted";

    private static final String RAW_JSON_MISSING_SEARCH_VALUE =
        "{\"userId\": \"3\", \"searchType\": \"CASE_ID\",\"channel\": \"EMAIL\"}";
    private static final String RAW_JSON_MISSING_SEARCH_TYPE =
        "{\"userId\": \"3\", \"searchType\": \"123\",\"channel\": \"EMAIL\"}";
    private static final String RAW_JSON_MISSING_CHANNEL =
        "{\"userId\": \"3\", \"searchType\": \"CASE_ID\",\"searchValue\": \"321\"}";

    private static final String LOCATION_ID = "9";
    private static final String CASE_ID = "T485913";
    private static final String CASE_URN = "IBRANE1BVW";
    private static final String CASE_NAME = "Tom Clancy";
    private static final String MI_REPORTING_SUBSCRIPTION_DATA_ALL_URL = "/subscription/mi-data-all";
    private static final String MI_REPORTING_SUBSCRIPTION_DATA_LOCAL_URL = "/subscription/mi-data-local";
    private static final String SUBSCRIPTION_USER_PATH = "/subscription/user/" + UUID_STRING;
    private static final String UPDATE_LIST_TYPE_PATH = "/subscription/configure-list-types/" + VALID_USER_ID;
    private static final String ARTEFACT_RECIPIENT_PATH = "/subscription/artefact-recipients";
    private static final String DELETED_ARTEFACT_RECIPIENT_PATH = "/subscription/deleted-artefact";
    private static final String GET_SUBSCRIPTIONS_BY_LOCATION_ID = "/subscription/location/";
    private static final LocalDateTime DATE_ADDED = LocalDateTime.now();
    private static final String UPDATED_LIST_TYPE = "[\"CIVIL_DAILY_CAUSE_LIST\"]";
    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.authorized";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_isAuthorized";

    private static String rawArtefact;

    @Autowired
    protected MockMvc mvc;

    protected static final String SUBSCRIPTION_PATH = "/subscription";
    protected static final SubscriptionDto SUBSCRIPTION = new SubscriptionDto();

    private static final String ACTIONING_USER_ID = "1234-1234";

    private static final String USER_ID_HEADER = "x-user-id";

    @BeforeAll
    static void setup() throws IOException {
        OBJECT_MAPPER.findAndRegisterModules();
        SUBSCRIPTION.setChannel(Channel.API_COURTEL);
        SUBSCRIPTION.setSearchType(SearchType.LOCATION_ID);
        SUBSCRIPTION.setUserId(UUID_STRING);

        rawArtefact = new String(IOUtils.toByteArray(
            Objects.requireNonNull(SubscriptionControllerTests.class.getClassLoader()
                                       .getResourceAsStream("mock/artefact.json"))));
    }

    protected MockHttpServletRequestBuilder setupMockSubscription(String searchValue) throws JsonProcessingException {

        SUBSCRIPTION.setSearchValue(searchValue);
        SUBSCRIPTION.setLocationName(LOCATION_NAME_1);
        SUBSCRIPTION.setCaseName(CASE_NAME);
        SUBSCRIPTION.setCaseNumber(CASE_ID);
        SUBSCRIPTION.setUrn(CASE_URN);
        SUBSCRIPTION.setCreatedDate(DATE_ADDED);
        return MockMvcRequestBuilders.post(SUBSCRIPTION_PATH)
            .content(OBJECT_MAPPER.writeValueAsString(SUBSCRIPTION))
            .header(USER_ID_HEADER, ACTIONING_USER_ID)
            .contentType(MediaType.APPLICATION_JSON);
    }

    protected MockHttpServletRequestBuilder setupMockSubscription(String searchValue, SearchType searchType,
                                                                  String userId)
        throws JsonProcessingException {

        SUBSCRIPTION.setUserId(userId);
        SUBSCRIPTION.setSearchType(searchType);
        return setupMockSubscription(searchValue);
    }

    protected MockHttpServletRequestBuilder setupMockSubscription(String searchValue, SearchType searchType,
                                                                  String userId, String caseNumber, String caseUrn)
        throws JsonProcessingException {

        SUBSCRIPTION.setUserId(userId);
        SUBSCRIPTION.setSearchType(searchType);
        SUBSCRIPTION.setCaseNumber(caseNumber);
        SUBSCRIPTION.setUrn(caseUrn);
        return setupMockSubscription(searchValue);

    }

    protected MockHttpServletRequestBuilder setupMockSubscriptionWithListType(String searchValue,
                                                      SearchType searchType, String userId, ListType listType)
        throws JsonProcessingException {

        SUBSCRIPTION.setUserId(userId);
        SUBSCRIPTION.setSearchType(searchType);
        SUBSCRIPTION.setListType(List.of(listType.name()));
        return setupMockSubscription(searchValue);
    }

    protected MockHttpServletRequestBuilder getSubscriptionByUuid(String searchValue) {
        return get(SUBSCRIPTION_PATH + '/' + searchValue);
    }

    protected MockHttpServletRequestBuilder setupRawJsonSubscription(String json) {
        return MockMvcRequestBuilders.post(SUBSCRIPTION_PATH)
            .content(json)
            .header(USER_ID_HEADER, ACTIONING_USER_ID)
            .contentType(MediaType.APPLICATION_JSON);
    }

    @DisplayName("Post a new subscription and then get it from db.")
    @Test
    void postEndpoint() throws Exception {
        MockHttpServletRequestBuilder mappedSubscription = setupMockSubscription(LOCATION_ID);

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
                     VALIDATION_CASE_NAME
        );
        assertEquals(SUBSCRIPTION.getCaseNumber(), returnedSubscription.getCaseNumber(),
                     VALIDATION_CASE_NUMBER
        );
        assertEquals(SUBSCRIPTION.getUrn(), returnedSubscription.getUrn(),
                     VALIDATION_CASE_URN
        );
        assertEquals(LOCATION_NAME_1, returnedSubscription.getLocationName(),
                     VALIDATION_LOCATION_NAME
        );
    }

    @DisplayName("Ensure post endpoint actually posts a subscription to db")
    @Test
    void checkPostToDb() throws Exception {
        MockHttpServletRequestBuilder mappedSubscription = setupMockSubscription(LOCATION_ID);

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
                     VALIDATION_CASE_NAME
        );
        assertEquals(SUBSCRIPTION.getCaseNumber(), returnedSubscription.getCaseNumber(),
                     VALIDATION_CASE_NUMBER
        );
        assertEquals(SUBSCRIPTION.getUrn(), returnedSubscription.getUrn(),
                     VALIDATION_CASE_URN
        );
        assertEquals(LOCATION_NAME_1, returnedSubscription.getLocationName(),
                     VALIDATION_LOCATION_NAME
        );

    }

    @DisplayName("Checks for bad request for invalid searchType enum.")
    @Test
    void checkSearchTypeEnum() throws Exception {
        MockHttpServletRequestBuilder brokenSubscription = setupRawJsonSubscription(
            "{'searchType': 'INVALID_TYPE'}");
        MvcResult response = mvc.perform(brokenSubscription)
            .andExpect(status().isBadRequest()).andReturn();
        assertEquals(400, response.getResponse().getStatus(), VALIDATION_BAD_REQUEST);
    }

    @DisplayName("Checks for bad request for invalid channel enum.")
    @Test
    void checkChannelEnum() throws Exception {
        MockHttpServletRequestBuilder brokenSubscription = setupRawJsonSubscription(
            "{'channel': 'INVALID_TYPE'}");
        MvcResult response = mvc.perform(brokenSubscription)
            .andExpect(status().isBadRequest()).andReturn();
        assertEquals(400, response.getResponse().getStatus(), VALIDATION_BAD_REQUEST);

    }

    @DisplayName("Checks for bad request when empty json is sent")
    @Test
    void checkEmptyPost() throws Exception {
        MockHttpServletRequestBuilder brokenSubscription = setupRawJsonSubscription("{}");
        MvcResult response = mvc.perform(brokenSubscription)
            .andExpect(status().isBadRequest()).andReturn();
        assertEquals(400, response.getResponse().getStatus(), VALIDATION_BAD_REQUEST);
    }

    @Test
    void checkMissingSearchType() throws Exception {
        MockHttpServletRequestBuilder brokenSubscription = setupRawJsonSubscription(RAW_JSON_MISSING_SEARCH_TYPE);
        MvcResult response = mvc.perform(brokenSubscription)
            .andExpect(status().isBadRequest()).andReturn();
        assertEquals(400, response.getResponse().getStatus(), VALIDATION_BAD_REQUEST);
    }

    @Test
    void checkMissingSearchValue() throws Exception {
        MockHttpServletRequestBuilder brokenSubscription = setupRawJsonSubscription(RAW_JSON_MISSING_SEARCH_VALUE);
        MvcResult response = mvc.perform(brokenSubscription)
            .andExpect(status().isBadRequest()).andReturn();
        assertEquals(400, response.getResponse().getStatus(), VALIDATION_BAD_REQUEST);
    }

    @Test
    void checkMissingChannel() throws Exception {
        MockHttpServletRequestBuilder brokenSubscription = setupRawJsonSubscription(RAW_JSON_MISSING_CHANNEL);
        MvcResult response = mvc.perform(brokenSubscription)
            .andExpect(status().isBadRequest()).andReturn();
        assertEquals(400, response.getResponse().getStatus(), VALIDATION_BAD_REQUEST);
    }

    @DisplayName("Delete an individual subscription")
    @Test
    void deleteEndpoint() throws Exception {
        MockHttpServletRequestBuilder mappedSubscription = setupMockSubscription(LOCATION_ID);

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
        )).header(USER_ID_HEADER, ACTIONING_USER_ID)).andExpect(status().isOk()).andReturn();
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
        MvcResult response = mvc.perform(delete("/subscription/" + randomUuid)
                                             .header(USER_ID_HEADER, ACTIONING_USER_ID))
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
        MvcResult response = mvc.perform(get("/subscription/" + randomUuid))
            .andExpect(status()
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
        mvc.perform(setupMockSubscription(LOCATION_ID, SearchType.LOCATION_ID, UUID_STRING));
        mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, UUID_STRING));
        mvc.perform(setupMockSubscription(CASE_URN, SearchType.CASE_URN, UUID_STRING));

        MvcResult response = mvc.perform(get(SUBSCRIPTION_USER_PATH))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(response.getResponse(), VALIDATION_EMPTY_RESPONSE);

        UserSubscription userSubscriptions =
            OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), UserSubscription.class);

        assertEquals(
            3,
            userSubscriptions.getLocationSubscriptions().size() + userSubscriptions
                .getCaseSubscriptions().size(),
            VALIDATION_SUBSCRIPTION_LIST
        );

        LocationSubscription location = userSubscriptions.getLocationSubscriptions().get(0);
        assertEquals(LOCATION_NAME_1, location.getLocationName(), VALIDATION_LOCATION_NAME);
        assertEquals(DATE_ADDED.withNano(0), location.getDateAdded().withNano(0),
                     VALIDATION_DATE_ADDED
        );

        CaseSubscription caseSubscription = userSubscriptions.getCaseSubscriptions().get(0);
        assertEquals(CASE_NAME, caseSubscription.getCaseName(), VALIDATION_CASE_NAME);
        assertEquals(CASE_ID, caseSubscription.getCaseNumber(), VALIDATION_CASE_ID);
        assertEquals(CASE_URN, caseSubscription.getUrn(), VALIDATION_CASE_URN);
    }

    @Test
    void testGetUsersSubscriptionsByUserIdSingleLocation() throws Exception {
        mvc.perform(setupMockSubscription(LOCATION_ID, SearchType.LOCATION_ID, UUID_STRING));

        MvcResult response = mvc.perform(get(SUBSCRIPTION_USER_PATH))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(response.getResponse(), VALIDATION_EMPTY_RESPONSE);

        UserSubscription userSubscriptions =
            OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), UserSubscription.class);

        assertEquals(1, userSubscriptions.getLocationSubscriptions().size(),
                     "Court subscription list does not contain 1 court"
        );

        assertEquals(0, userSubscriptions.getCaseSubscriptions().size(),
                     "Court subscription list contains unknown cases"
        );

        LocationSubscription location = userSubscriptions.getLocationSubscriptions().get(0);
        assertEquals(LOCATION_NAME_1, location.getLocationName(), VALIDATION_LOCATION_NAME);
        assertEquals(DATE_ADDED.withNano(0), location.getDateAdded().withNano(0),
                     VALIDATION_DATE_ADDED
        );
    }

    @Test
    void testGetUsersSubscriptionsByUserIdSingleCaseId() throws Exception {
        mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, UUID_STRING));

        MvcResult response = mvc.perform(get(SUBSCRIPTION_USER_PATH))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(response.getResponse(), VALIDATION_EMPTY_RESPONSE);

        UserSubscription userSubscriptions =
            OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), UserSubscription.class);

        assertEquals(0, userSubscriptions.getLocationSubscriptions().size(),
                     VALIDATION_LOCATION_LIST
        );

        assertEquals(1, userSubscriptions.getCaseSubscriptions().size(),
                     VALIDATION_ONE_CASE_LOCATION
        );

        CaseSubscription caseSubscription = userSubscriptions.getCaseSubscriptions().get(0);
        assertEquals(CASE_NAME, caseSubscription.getCaseName(), VALIDATION_CASE_NAME);
        assertEquals(CASE_ID, caseSubscription.getCaseNumber(), VALIDATION_CASE_ID);
        assertEquals(CASE_URN, caseSubscription.getUrn(), VALIDATION_CASE_URN);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void testGetUsersSubscriptionsByUserIdSingleCaseUrn() throws Exception {
        mvc.perform(setupMockSubscription(CASE_URN, SearchType.CASE_URN, UUID_STRING));

        MvcResult response = mvc.perform(get(SUBSCRIPTION_USER_PATH))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(response.getResponse(), VALIDATION_EMPTY_RESPONSE);

        UserSubscription userSubscriptions =
            OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), UserSubscription.class);

        assertEquals(0, userSubscriptions.getLocationSubscriptions().size(),
                     VALIDATION_LOCATION_LIST
        );

        assertEquals(1, userSubscriptions.getCaseSubscriptions().size(),
                     VALIDATION_ONE_CASE_LOCATION
        );

        CaseSubscription caseSubscription = userSubscriptions.getCaseSubscriptions().get(0);
        assertEquals(CASE_NAME, caseSubscription.getCaseName(), VALIDATION_CASE_NAME);
        assertEquals(CASE_ID, caseSubscription.getCaseNumber(), VALIDATION_CASE_ID);
        assertEquals(CASE_URN, caseSubscription.getUrn(), VALIDATION_CASE_URN);
    }

    @Test
    void testGetUsersSubscriptionsByUserIdNoSubscriptions() throws Exception {
        MvcResult response = mvc.perform(get(SUBSCRIPTION_USER_PATH))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(response.getResponse(), VALIDATION_EMPTY_RESPONSE);

        UserSubscription userSubscriptions =
            OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), UserSubscription.class);

        assertEquals(new UserSubscription(), userSubscriptions,
                     VALIDATION_NO_SUBSCRIPTIONS
        );
    }

    @Test
    void testBuildSubscriberListReturnsAccepted() throws Exception {
        mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, VALID_USER_ID));
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(ARTEFACT_RECIPIENT_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(rawArtefact);
        MvcResult result = mvc.perform(request).andExpect(status().isAccepted()).andReturn();

        assertEquals(SUBSCRIBER_REQUEST_SUCCESS, result.getResponse().getContentAsString(),
                     RESPONSE_MATCH
        );
    }

    @Test
    void testBuildSubscriberListCaseUrnNull() throws Exception {
        mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, VALID_USER_ID, CASE_ID,null));
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(ARTEFACT_RECIPIENT_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(rawArtefact);
        MvcResult result = mvc.perform(request).andExpect(status().isAccepted()).andReturn();

        assertEquals(SUBSCRIBER_REQUEST_SUCCESS, result.getResponse().getContentAsString(),
                     RESPONSE_MATCH
        );
    }

    @Test
    void testBuildSubscriberListCaseNumberNull() throws Exception {
        mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, VALID_USER_ID, null,CASE_URN));
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(ARTEFACT_RECIPIENT_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(rawArtefact);
        MvcResult result = mvc.perform(request).andExpect(status().isAccepted()).andReturn();

        assertEquals(SUBSCRIBER_REQUEST_SUCCESS, result.getResponse().getContentAsString(),
                     RESPONSE_MATCH
        );
    }

    @Test
    @WithMockUser(username = "unauthorized_find_by_id", authorities = {"APPROLE_unknown.find"})
    void testUnauthorizedBuildSubscriberList() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(ARTEFACT_RECIPIENT_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(rawArtefact);

        MvcResult mvcResult = mvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testBuildCourtSubscribersListReturnsAccepted() throws Exception {
        mvc.perform(setupMockSubscriptionWithListType(LOCATION_ID, SearchType.LOCATION_ID,
                                                      VALID_USER_ID, ListType.CIVIL_DAILY_CAUSE_LIST
        ));
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(ARTEFACT_RECIPIENT_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(rawArtefact);
        MvcResult result = mvc.perform(request).andExpect(status().isAccepted()).andReturn();

        assertEquals(SUBSCRIBER_REQUEST_SUCCESS, result.getResponse().getContentAsString(),
                     RESPONSE_MATCH
        );
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedCreateSubscription() throws Exception {
        MockHttpServletRequestBuilder mappedSubscription = setupMockSubscription(LOCATION_ID);

        MvcResult mvcResult =
            mvc.perform(mappedSubscription).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    @WithMockUser(username = "unauthorized_delete", authorities = {"APPROLE_unknown.delete"})
    void testUnauthorizedDeleteById() throws Exception {
        MvcResult mvcResult = mvc.perform(getSubscriptionByUuid(UUID.randomUUID().toString())
        ).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    @WithMockUser(username = "unauthorized_find_by_id", authorities = {"APPROLE_unknown.find"})
    void testUnauthorizedFindSubscriptionById() throws Exception {
        MvcResult mvcResult = mvc.perform(get(String.format("/subscription/%s", UUID.randomUUID())))
            .andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    @WithMockUser(username = "unauthorized_find_by_user_id", authorities = {"APPROLE_unknown.find"})
    void testUnauthorizedFindByUserId() throws Exception {
        MvcResult mvcResult = mvc.perform(get(SUBSCRIPTION_USER_PATH)).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testConfigureListTypesForSubscription() throws Exception {
        mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, VALID_USER_ID));
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .put(UPDATE_LIST_TYPE_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(UPDATED_LIST_TYPE);
        MvcResult result = mvc.perform(request).andExpect(status().isOk()).andReturn();

        assertEquals(String.format(
                         "Location list Type successfully updated for user %s",
                         VALID_USER_ID
                     ),
                     result.getResponse().getContentAsString(), RESPONSE_MATCH
        );
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedConfigureListTypesForSubscription() throws Exception {

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .put(UPDATE_LIST_TYPE_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(UPDATED_LIST_TYPE);
        MvcResult mvcResult = mvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testBuildDeletedArtefactSubscribersReturnsAccepted() throws Exception {
        mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, VALID_USER_ID));
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(DELETED_ARTEFACT_RECIPIENT_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(rawArtefact);
        MvcResult result = mvc.perform(request).andExpect(status().isAccepted()).andReturn();

        assertEquals("Deleted artefact third party subscriber notification request has been accepted",
                     result.getResponse().getContentAsString(), RESPONSE_MATCH
        );
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedBuildDeletedArtefactSubscribers() throws Exception {

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(DELETED_ARTEFACT_RECIPIENT_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(rawArtefact);
        MvcResult mvcResult = mvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }


    @DisplayName("Delete all subscriptions by user id")
    @Test
    void testDeleteAllSubscriptionsForUser() throws Exception {
        MockHttpServletRequestBuilder mappedSubscription = setupMockSubscription(LOCATION_ID);
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
            "/subscription/user/%s",
            returnedSubscription.getUserId()
        )).header(USER_ID_HEADER, ACTIONING_USER_ID)).andExpect(status().isOk()).andReturn();

        assertNotNull(deleteResponse.getResponse(), VALIDATION_EMPTY_RESPONSE);

        assertEquals(
            String.format("All subscriptions deleted for user id %s", returnedSubscription.getUserId()),
            deleteResponse.getResponse().getContentAsString(),
            "Responses are not equal"
        );
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedDeleteAllSubscriptionsForUser() throws Exception {
        MvcResult mvcResult =
            mvc.perform(delete(String.format(
                "/subscription/user/%s",
                SUBSCRIPTION.getUserId()
            )).header(USER_ID_HEADER, ACTIONING_USER_ID)).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testGetSubscriptionDataForMiReportingAll() throws Exception {
        mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, VALID_USER_ID));
        MvcResult response = mvc.perform(get(MI_REPORTING_SUBSCRIPTION_DATA_ALL_URL))
            .andExpect(status().isOk()).andReturn();
        assertThat(response.getResponse().getContentAsString()).contains(VALID_USER_ID);
    }

    @Test
    @WithMockUser(username = "unauthorized_account", authorities = {"APPROLE_unknown.account"})
    void testGetSubscriptionDataForMiReportingAllUnauthorized() throws Exception {
        mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, VALID_USER_ID));
        MvcResult response = mvc.perform(get(MI_REPORTING_SUBSCRIPTION_DATA_ALL_URL))
            .andExpect(status().isForbidden())
            .andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testGetSubscriptionDataForMiReportingLocal() throws Exception {
        mvc.perform(setupMockSubscription(LOCATION_ID, SearchType.LOCATION_ID, VALID_USER_ID));
        MvcResult response = mvc.perform(get(MI_REPORTING_SUBSCRIPTION_DATA_LOCAL_URL))
            .andExpect(status().isOk()).andReturn();
        assertThat(response.getResponse().getContentAsString()).contains(VALID_USER_ID);
    }

    @Test
    @WithMockUser(username = "unauthorized_account", authorities = {"APPROLE_unknown.account"})
    void testGetSubscriptionDataForMiReportingLocalUnauthorized() throws Exception {
        mvc.perform(setupMockSubscription(LOCATION_ID, SearchType.LOCATION_ID, VALID_USER_ID));
        MvcResult response = mvc.perform(get(MI_REPORTING_SUBSCRIPTION_DATA_LOCAL_URL))
            .andExpect(status().isForbidden())
            .andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testFindSubscriptionsByLocationId() throws Exception {
        mvc.perform(setupMockSubscription(LOCATION_ID, SearchType.LOCATION_ID, UUID_STRING));

        MvcResult response = mvc.perform(get(GET_SUBSCRIPTIONS_BY_LOCATION_ID + LOCATION_ID))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(response.getResponse(), VALIDATION_EMPTY_RESPONSE);

        List<Subscription> userSubscriptions =
            Arrays.asList(OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), Subscription[].class));

        assertEquals(1, userSubscriptions.size(),
                     "Subscriptions list for location id " + LOCATION_ID + " not found"
        );
        assertEquals(LOCATION_ID, userSubscriptions.get(0).getSearchValue(),
                     "Subscriptions list for location id " + LOCATION_ID + " not found"
        );
    }

    @Test
    void testFindSubscriptionsByLocationIdNotFound() throws Exception {

        MvcResult response = mvc.perform(get(GET_SUBSCRIPTIONS_BY_LOCATION_ID + LOCATION_ID))
            .andExpect(status().isNotFound())
            .andReturn();

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    @WithMockUser(username = "unauthorized_account", authorities = {"APPROLE_unknown.account"})
    void testFindSubscriptionsByLocationIdUnauthorized() throws Exception {

        MvcResult response = mvc.perform(get(GET_SUBSCRIPTIONS_BY_LOCATION_ID + LOCATION_ID))
            .andExpect(status().isForbidden())
            .andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }
}

