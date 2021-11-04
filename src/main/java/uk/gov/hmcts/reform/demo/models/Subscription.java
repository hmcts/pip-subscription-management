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

    private String subscriptionID;

    private String courtID;

    private String caseID;

    private String urnID;

    private String uniqueSubID;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private static final long serialVersionUID = -470332543681824967L;



    protected Subscription() {
        //intentionally empty constructor
    }

    public Subscription(String uuid, String courtID, String subscriptionID,
                        String caseID, String urnID, String uniqueSubID) {
        this.uuid = uuid;
        this.courtID = courtID;
        this.subscriptionID = subscriptionID;
        this.caseID = caseID;
        this.urnID = urnID;
        this.uniqueSubID = uniqueSubID;
    }

    @Override
    public String toString() {
        return "Subscription{" +
            "uuid='" + uuid + '\'' +
            ", subscriptionID='" + subscriptionID + '\'' +
            ", courtID='" + courtID + '\'' +
            ", caseID='" + caseID + '\'' +
            ", urnID='" + urnID + '\'' +
            ", uniqueSubID='" + uniqueSubID + '\'' +
            ", id=" + id +
            '}';
    }
}

