package uk.gov.hmcts.reform.pip.subscription.management.models;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;


@Data
@Entity
@Table
public class SubscriptionDto {

    /**
     * Unique subscription ID.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @ApiModelProperty(hidden = true)
    private UUID id;

    private String userId; //P&I user id

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


