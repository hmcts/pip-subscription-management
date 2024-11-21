package uk.gov.hmcts.reform.pip.subscription.management.utils;

import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.pip.subscription.management.service.AccountManagementService;
import uk.gov.hmcts.reform.pip.subscription.management.service.DataManagementService;
import uk.gov.hmcts.reform.pip.subscription.management.service.PublicationServicesService;

public class IntegrationTestBase {

    @MockBean
    protected AccountManagementService accountManagementService;

    @MockBean
    protected DataManagementService dataManagementService;

    @MockBean
    protected PublicationServicesService publicationServicesService;

}
