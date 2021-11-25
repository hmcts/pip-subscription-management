package uk.gov.hmcts.reform.pip.subscription.management.models;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.Type;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;


@Data
@Entity
@Table
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

    private String userId; //P&I user id

    private SearchType searchType;

    private String searchValue;

    private Channel channel;

    public SubscriptionDto toDto() {
        SubscriptionDto dto = new SubscriptionDto();
        dto.setSearchValue(this.searchValue);
        dto.setChannel(this.channel);
        dto.setUserId(this.userId);
        dto.setSearchType(this.searchType);
        dto.setId(this.id);
        return dto;
    }
}

