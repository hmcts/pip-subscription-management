package uk.gov.hmcts.reform.pip.subscription.management.models;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.vladmihalcea.hibernate.type.array.ListArrayType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table
@JsonPropertyOrder({"id", "channel", "searchType", "searchValue", "userID"})
@TypeDef(
    name = "list-array",
    typeClass = ListArrayType.class
)
public class Subscription {

    /**
     * Unique subscription ID.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", insertable = false, updatable = false, nullable = false)
    @Schema(hidden = true)
    @Type(type = "org.hibernate.type.PostgresUUIDType")
    private UUID id;

    /**
     *  P&I user id.
     */
    @Valid
    @NotNull
    private String userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private SearchType searchType;

    @NotNull
    @Valid
    private String searchValue;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Schema(hidden = true)
    private LocalDateTime createdDate;

    @Valid
    private String caseNumber;

    @Valid
    private String caseName;

    @Valid
    private String urn;

    @Valid
    private String locationName;

    @Valid
    private LocalDateTime lastUpdatedDate;

    @Valid
    @Type(type = "list-array")
    @Column(name = "list_type", columnDefinition = "text[]")
    private List<String> listType;

    public Subscription(uk.gov.hmcts.reform.pip.model.subscription.Subscription dto) {
        this.id = dto.getId();
        this.userId = dto.getUserId();
        this.searchType = dto.getSearchType();
        this.searchValue = dto.getSearchValue();
        this.channel = dto.getChannel();
        this.createdDate = dto.getCreatedDate();
        this.caseNumber = dto.getCaseNumber();
        this.caseName = dto.getCaseName();
        this.urn = dto.getUrn();
        this.locationName = dto.getLocationName();
        this.lastUpdatedDate = dto.getLastUpdatedDate();
        this.listType = dto.getListType();
    }

    public uk.gov.hmcts.reform.pip.model.subscription.Subscription toDto() {
        uk.gov.hmcts.reform.pip.model.subscription.Subscription dto =
            new uk.gov.hmcts.reform.pip.model.subscription.Subscription();
        dto.setSearchValue(this.searchValue);
        dto.setChannel(this.channel);
        dto.setUserId(this.userId);
        dto.setSearchType(this.searchType);
        dto.setId(this.id);
        dto.setCreatedDate(this.createdDate);
        dto.setCaseNumber(this.caseNumber);
        dto.setCaseName(this.caseName);
        dto.setUrn(this.urn);
        dto.setLocationName(this.locationName);
        dto.setListType(this.listType);
        dto.setLastUpdatedDate(this.lastUpdatedDate);
        return dto;
    }
}
