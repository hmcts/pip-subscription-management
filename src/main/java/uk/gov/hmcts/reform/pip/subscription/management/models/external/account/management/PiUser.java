package uk.gov.hmcts.reform.pip.subscription.management.models.external.account.management;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PiUser {

    /**
     * The ID of the user as they exist in P&I.
     */
    private String userId;

    /**
     * The Sign in entry system the user was added with. (CFT IDAM, Crime IDAM, P&I AAD).
     */
    @Enumerated(EnumType.STRING)
    private UserProvenances userProvenance;

    /**
     * The user id of the user as per their provenance system.
     */
    private String provenanceUserId;

    /**
     * Email of the user. Validated at the class level by PiEmailConditionalValidation interface.
     */
    private String email;

    /**
     * Role of the user, Verified, Internal or Technical.
     */
    @Enumerated(EnumType.STRING)
    private Roles roles;

    /**
     * The forenames of the user.
     */
    private String forenames;

    /**
     * The surnames of the user.
     */
    private String surname;

    /**
     * The timestamp of when the user was created.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdDate;

    /**
     * The timestamp of when the user was last verified.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime lastVerifiedDate;

    /**
     * The timestamp when the user was last signed in.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime lastSignedInDate;
}
