package uk.gov.hmcts.reform.pip.subscription.management.models;


import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;
import java.util.UUID;


@Data
public class SubscriptionDto {

    /**
     * Unique subscription ID.
     */
    @ApiModelProperty(hidden = true)
    private UUID id;

    /**
     *  P&I user id.
     **/
    @Valid
    @NotNull
    private String userId;

    @Valid
    @NotNull
    private SearchType searchType;

    @Valid
    @javax.validation.constraints.NotNull
    @NotBlank
    private String searchValue;

    @Valid
    @NotNull
    private Channel channel;


    public Subscription toEntity() {
        Subscription entity = new Subscription();
        entity.setSearchValue(this.searchValue);
        entity.setChannel(this.channel);
        entity.setUserId(this.userId);
        entity.setSearchType(this.searchType);
        entity.setId(this.id);
        return entity;
    }

}


