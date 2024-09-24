package uk.gov.hmcts.reform.pip.subscription.management.models;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table
public class SubscriptionListType {

    /**
     * Unique ListType subscription ID.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", insertable = false, updatable = false, nullable = false)
    @Schema(hidden = true)
    private UUID id;

    /**
     *  P&I user id.
     */
    @Valid
    @NotNull
    private String userId;

    @Valid
    @NotNull
    private Integer locationId;

    @Valid
    @Type(ListArrayType.class)
    @Column(name = "list_type", columnDefinition = "text[]")
    private List<String> listType;

    @Valid
    @Type(ListArrayType.class)
    @Column(name = "list_language", columnDefinition = "text[]")
    private List<String> listLanguage;

    @Valid
    @NotNull
    private LocalDateTime createdDate;

    public SubscriptionListType(String userId, Integer locationId,
                                List<String> listType, List<String> listLanguage,
                                LocalDateTime createdDate) {
        this.userId = userId;
        this.locationId = locationId;
        this.listType = listType;
        this.listLanguage = listLanguage;
        this.createdDate = createdDate;

    }
}
