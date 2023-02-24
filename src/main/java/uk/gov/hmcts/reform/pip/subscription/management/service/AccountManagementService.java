package uk.gov.hmcts.reform.pip.subscription.management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.account.management.AzureAccount;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.account.management.PiUser;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.ListType;
import uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management.Sensitivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Service
public class AccountManagementService {

    @Value("${service-to-service.account-management}")
    private String url;

    private static final String IS_AUTHORISED = "account/isAuthorised";
    private static final String GET_USERS_EMAIL = "account/emails";
    private static final String ACCOUNT_MANAGEMENT_API = "accountManagementApi";

    @Autowired
    private WebClient webClient;

    /**
     * REST call to account management to check if user is authorised to see classified publication.
     *
     * @param userId user id to check is authorised
     * @param listType the artefact ID to check if the user is authorized for
     * @param sensitivity the artefact ID to check if the user is authorized for
     * @return bool of true if user can see, false if they are forbidden or if request errored
     */
    public Boolean isUserAuthorised(String userId, ListType listType, Sensitivity sensitivity) {
        try {
            return webClient.get().uri(
                String.format("%s/%s/%s/%s/%s", url, IS_AUTHORISED, userId, listType, sensitivity))
                .attributes(clientRegistrationId(ACCOUNT_MANAGEMENT_API))
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

    public Map<String, Optional<String>> getMappedEmails(List<String> listOfUsers) {
        try {
            return webClient.post().uri(url + "/" + GET_USERS_EMAIL)
                .attributes(clientRegistrationId(ACCOUNT_MANAGEMENT_API))
                .body(BodyInserters.fromValue(listOfUsers))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Optional<String>>>() {})
                .block();
        } catch (WebClientException ex) {
            log.error(String.format("Request with body failed. With error message: %s", ex.getMessage()));
            return Collections.emptyMap();
        }
    }

    public AzureAccount getUserInfo(String provenanceUserId) {
        try {
            return webClient.get().uri(url + "/account/azure/" + provenanceUserId)
                .attributes(clientRegistrationId(ACCOUNT_MANAGEMENT_API))
                .retrieve().bodyToMono(AzureAccount.class).block();
        } catch (WebClientException ex) {
            log.error(String.format("Request to account management failed with error message: %s", ex.getMessage()));
            return new AzureAccount();
        }
    }

    public List<PiUser> getAllAccounts(String provenances, String role)
        throws JsonProcessingException {
        try {
            String result = webClient.get().uri(String.format(
                    "%s/account/all?provenances=%s&roles=%s", url, provenances, role))
                .attributes(clientRegistrationId(ACCOUNT_MANAGEMENT_API))
                .retrieve().bodyToMono(String.class).block();
            return findUserEmails(result);
        } catch (WebClientException ex) {
            log.error(String.format("Request to account management failed with error message: %s", ex.getMessage()));
            return new ArrayList<>();
        }
    }

    private List<PiUser> findUserEmails(String jsonResponse) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        JsonNode node = new ObjectMapper().readTree(jsonResponse);
        JsonNode content = node.get("content");
        return mapper.readValue(content.toString(), new TypeReference<>(){});
    }
}
