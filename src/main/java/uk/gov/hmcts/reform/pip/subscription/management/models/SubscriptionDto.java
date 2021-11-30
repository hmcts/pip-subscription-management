package uk.gov.hmcts.reform.pip.subscription.management.models;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

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
     */
    private String userId;

    private SearchType searchType;

    private String searchValue;

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


