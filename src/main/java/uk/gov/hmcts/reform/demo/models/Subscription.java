package uk.gov.hmcts.reform.demo.models;


import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import springfox.documentation.annotations.ApiIgnore;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;



@Data
@Entity
@Table(name = "subscription")
public class Subscription implements Serializable {

    /**
     * P&I user ID
     */
    private String uuid;

    private SearchType searchType;

    private String searchValue;

    private String courtId;

    private String channel;


    private String urnId;

    /**
     * Unique subscription ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @ApiModelProperty(hidden = true)
    private long id;

    private static final long serialVersionUID = -470332543681824967L;



    protected Subscription() {
        //intentionally empty constructor
    }

    public Subscription(String uuid, String courtId, String searchValue,
                        String channel, String urnId, String uniqueSubId) {
        this.uuid = uuid;
        this.courtId = courtId;
        this.searchValue = searchValue;
        this.channel = channel;
        this.urnId = urnId;

    }

    @Override
    public String toString() {
        return "Subscription{" +
            "uuid='" + uuid + '\'' +
            ", searchValue='" + searchValue + '\'' +
            ", courtId='" + courtId + '\'' +
            ", caseId='" + channel + '\'' +
            ", urnId='" + urnId + '\'' +
            ", subscriptionId=" + id +
            '}';
    }
}

