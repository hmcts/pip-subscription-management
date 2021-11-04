package uk.gov.hmcts.reform.demo.models;


import lombok.Data;

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


    private String uuid;

    //private Enum subscriptionLevel;

    private String subscriptionId;

    private String courtId;

    private String caseId;

    private String urnId;

    private String uniqueSubId;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private static final long serialVersionUID = -470332543681824967L;



    protected Subscription() {
        //intentionally empty constructor
    }

    public Subscription(String uuid, String courtId, String subscriptionId,
                        String caseId, String urnId, String uniqueSubId) {
        this.uuid = uuid;
        this.courtId = courtId;
        this.subscriptionId = subscriptionId;
        this.caseId = caseId;
        this.urnId = urnId;
        this.uniqueSubId = uniqueSubId;
    }

    @Override
    public String toString() {
        return "Subscription{" +
            "uuid='" + uuid + '\'' +
            ", subscriptionID='" + subscriptionId + '\'' +
            ", courtID='" + courtId + '\'' +
            ", caseID='" + caseId + '\'' +
            ", urnID='" + urnId + '\'' +
            ", uniqueSubID='" + uniqueSubId + '\'' +
            ", id=" + id +
            '}';
    }
}

