package uk.gov.hmcts.reform.pip.subscription.management.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.pip.subscription.management.Application;
import uk.gov.hmcts.reform.pip.subscription.management.config.RestTemplateConfigTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {RestTemplateConfigTest.class, Application.class})
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class DataManagementServiceTest {

    private static final String COURT_URL = "testUrl/courts/";
    private static final String VALID_CASE_ID = "123";
    private static final String VALID_URN = "321";
    private static final String VALID_COURT_ID = "345";
    private static final String VALID_CASE_NAME = "court-name";
    private static final String INVALID = "test";

    private JsonNode returnedCourt;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    DataManagementService dataManagementService;


    @BeforeEach
    void setup() {
        returnedCourt = new JsonNodeFactory(false).textNode("test court name");

        when(restTemplate.getForEntity(COURT_URL + VALID_COURT_ID, JsonNode.class))
            .thenReturn(ResponseEntity.ok(returnedCourt));
        doThrow(HttpStatusCodeException.class).when(restTemplate).getForEntity(COURT_URL + INVALID, JsonNode.class);
    }

    @Test
    void testGetCourt() {
        assertEquals(returnedCourt.asText(), dataManagementService.getCourtName(VALID_COURT_ID),
                     "Should match the returned court on successful GET"
        );
    }

    @Test
    void testGetCourtThrows() {
        String returned = "";
        try {
            returned = dataManagementService.getCourtName(INVALID);
        } catch (HttpStatusCodeException ex) {
            assertNull(returned);
        }
        assertNull(dataManagementService.getCourtName(INVALID));
    }

}
