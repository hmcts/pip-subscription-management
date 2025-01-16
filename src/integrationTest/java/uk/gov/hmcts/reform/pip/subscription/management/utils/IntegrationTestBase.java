package uk.gov.hmcts.reform.pip.subscription.management.utils;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.pip.subscription.management.service.AccountManagementService;
import uk.gov.hmcts.reform.pip.subscription.management.service.DataManagementService;
import uk.gov.hmcts.reform.pip.subscription.management.service.PublicationServicesService;

public class IntegrationTestBase {

    @MockitoBean
    protected AccountManagementService accountManagementService;

    @MockitoBean
    protected DataManagementService dataManagementService;

    @MockitoBean
    protected PublicationServicesService publicationServicesService;

}
