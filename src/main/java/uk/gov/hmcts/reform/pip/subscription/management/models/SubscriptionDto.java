package uk.gov.hmcts.reform.pip.subscription.management.models;


import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;


@Data
@JsonPropertyOrder({"id", "channel", "searchType", "searchValue", "userID"})
public class SubscriptionDto {

    /**
     * Unique subscription ID.
     */
    @ApiModelProperty(hidden = true)
    private UUID id;

    /**
     *  P&I user id.
     **/
    @ApiModelProperty(example = "e.g. 410129ka214k")
    @Valid
    @NotNull
    @NotBlank
    private String userId;

    @Valid
    @NotNull
    private SearchType searchType;

    @ApiModelProperty(example = "Value to categorise your entry")
    @Valid
    @NotNull
    @NotBlank
    private String searchValue;

    @Valid
    @NotNull
    private Channel channel;

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
    private List<String> listType;

    public Subscription toEntity() {
        Subscription entity = new Subscription();
        entity.setSearchValue(this.searchValue);
        entity.setChannel(this.channel);
        entity.setUserId(this.userId);
        entity.setSearchType(this.searchType);
        entity.setId(this.id);
        entity.setCreatedDate(this.createdDate);
        entity.setCaseName(this.caseName);
        entity.setCaseNumber(this.caseNumber);
        entity.setUrn(this.urn);
        entity.setLocationName(this.locationName);
        entity.setListType(this.listType);
        return entity;
    }
}
