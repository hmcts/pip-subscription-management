package uk.gov.hmcts.reform.pip.subscription.management.models;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.vladmihalcea.hibernate.type.array.ListArrayType;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

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
    @ApiModelProperty(hidden = true)
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

    @ApiModelProperty(hidden = true)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Valid
    private String caseNumber;

    @Valid
    private String caseName;

    @Valid
    private String urn;

    @Valid
    private String locationName;

    @Valid
    @Type(type = "list-array")
    @Column(name = "list_type", columnDefinition = "text[]")
    private List<String> listType;

    public SubscriptionDto toDto() {
        SubscriptionDto dto = new SubscriptionDto();
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
        return dto;
    }
}

