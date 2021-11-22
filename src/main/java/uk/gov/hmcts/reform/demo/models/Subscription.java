package uk.gov.hmcts.reform.demo.models;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table
public class Subscription implements Serializable {

    /**
     * Unique subscription ID.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @ApiModelProperty(hidden = true)
    private long id;

    private String userId; //P&I user id

    private SearchType searchType;

    private String searchValue;

    private Channel channel;

    private static final long serialVersionUID = -470332543681824967L;

}

