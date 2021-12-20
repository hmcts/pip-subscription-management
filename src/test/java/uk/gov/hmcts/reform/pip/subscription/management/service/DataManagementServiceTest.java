package uk.gov.hmcts.reform.pip.subscription.management.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.pip.subscription.management.Application;
import uk.gov.hmcts.reform.pip.subscription.management.config.RestTemplateConfigTest;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.CourtNotFoundException;
import uk.gov.hmcts.reform.pip.subscription.management.errorhandling.exceptions.HearingNotFoundException;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Court;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Hearing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {RestTemplateConfigTest.class, Application.class})
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class DataManagementServiceTest {

    private static final String CASE_ID_URL = "testUrl/hearings/case-number/";
    private static final String CASE_URN_URL = "testUrl/hearings/urn/";
    private static final String COURT_URL = "testUrl/courts/";
    private static final String VALID_CASE_ID = "123";
    private static final String VALID_URN = "321";
    private static final String VALID_COURT_ID = "345";
    private static final String INVALID = "test";

    private Hearing returnedHearing;
    private Court returnedCourt;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    DataManagementService dataManagementService;

    @BeforeEach
    void setup() {
        returnedHearing = new Hearing();
        returnedHearing.setCaseNumber(VALID_CASE_ID);
        returnedHearing.setUrn(VALID_URN);
        returnedHearing.setCourtId(Integer.parseInt(VALID_COURT_ID));

        returnedCourt = new Court();
        returnedCourt.setCourtId(Integer.parseInt(VALID_COURT_ID));

        when(restTemplate.getForEntity(CASE_ID_URL + VALID_CASE_ID, Hearing.class))
            .thenReturn(ResponseEntity.ok(returnedHearing));
        doThrow(HearingNotFoundException.class).when(restTemplate).getForEntity(CASE_ID_URL + INVALID, Hearing.class);
        when(restTemplate.getForEntity(CASE_URN_URL + VALID_URN, Hearing.class))
            .thenReturn(ResponseEntity.ok(returnedHearing));
        doThrow(HearingNotFoundException.class).when(restTemplate).getForEntity(CASE_URN_URL + INVALID, Hearing.class);
        when(restTemplate.getForEntity(COURT_URL + VALID_COURT_ID, Court.class))
            .thenReturn(ResponseEntity.ok(returnedCourt));
        doThrow(CourtNotFoundException.class).when(restTemplate).getForEntity(COURT_URL + INVALID, Court.class);

    }

    @Test
    void testGetHearingByCaseId() {
        assertEquals(returnedHearing, dataManagementService.getHearingByCaseId(VALID_CASE_ID),
                     "Should match the returned hearings on successful GET"
        );
    }

    @Test
    void testGetHearingByCaseIdThrows() {
        assertThrows(HearingNotFoundException.class, () -> dataManagementService.getHearingByCaseId(INVALID));
    }

    @Test
    void testGetHearingByUrn() {
        assertEquals(returnedHearing, dataManagementService.getHearingByUrn(VALID_URN),
                     "Should match the returned hearings on successful GET"
        );
    }

    @Test
    void testGetHearingByUrnThrows() {
        assertThrows(HearingNotFoundException.class, () -> dataManagementService.getHearingByUrn(INVALID));
    }

    @Test
    void testGetCourt() {
        assertEquals(returnedCourt, dataManagementService.getCourt(VALID_COURT_ID),
                     "Should match the returned hearings on successful GET"
        );
    }

    @Test
    void testGetCourtThrows() {
        assertThrows(CourtNotFoundException.class, () -> dataManagementService.getCourt(INVALID));

    }

}
