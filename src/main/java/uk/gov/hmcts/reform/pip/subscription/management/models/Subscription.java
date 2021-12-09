package uk.gov.hmcts.reform.pip.subscription.management.models;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.Type;

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
@Entity
@Table
@JsonPropertyOrder(alphabetic = true)
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

    @Enumerated(EnumType.STRING)
    private SearchType searchType;

    @Valid
    private String searchValue;

    @Enumerated(EnumType.STRING)
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

