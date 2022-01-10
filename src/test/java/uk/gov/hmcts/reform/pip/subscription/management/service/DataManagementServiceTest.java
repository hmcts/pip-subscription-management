package uk.gov.hmcts.reform.pip.subscription.management.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpServerErrorException;
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
    private static final String VALID_COURT_ID = "345";
    private static final String INVALID = "test";

    private ObjectNode returnedCourt;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    DataManagementService dataManagementService;


    @BeforeEach
    void setup() {
        returnedCourt = new JsonNodeFactory(false).objectNode();
        returnedCourt.put("name", "test court name");
        when(restTemplate.getForEntity(COURT_URL + VALID_COURT_ID, JsonNode.class))
            .thenReturn(ResponseEntity.ok(returnedCourt));
        HttpServerErrorException httpServerErrorException =
            new HttpServerErrorException(HttpStatus.BAD_GATEWAY, "Bad Gateway", null, null);
        doThrow(httpServerErrorException).when(restTemplate).getForEntity(COURT_URL + INVALID, JsonNode.class);
    }

    @Test
    void testGetCourt() {
        assertEquals(returnedCourt.get("name").asText(), dataManagementService.getCourtName(VALID_COURT_ID),
                     "Should match the returned court on successful GET"
        );
    }

    @Test
    void testGetCourtThrows() {
        assertNull(dataManagementService.getCourtName(INVALID), "Should return null on errored call");
    }

}
