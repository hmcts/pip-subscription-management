package uk.gov.hmcts.reform.pip.subscription.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.ListType;

@Slf4j
@Service
public class AccountManagementService {

    @Value("${service-to-service.account-management}")
    private String url;

    private static final String IS_AUTHORISED = "account/isAuthorised";

    @Autowired
    private WebClient webClient;

    public Boolean isUserAuthenticated(String userId, ListType listType) {
        try {
            return webClient.get().uri(String.format("%s/%s/%s/%s", url, IS_AUTHORISED, userId, listType))
                .retrieve().bodyToMono(Boolean.class).block();
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
                log.info("User failed list type auth check with response: " + ex.getResponseBodyAsString());
            } else {
                log.error("Request to Account Management isAuthenticated failed due to: "
                              + ex.getResponseBodyAsString());
            }
        }
        return false;
    }
}
