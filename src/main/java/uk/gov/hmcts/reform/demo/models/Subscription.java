package uk.gov.hmcts.reform.demo.models;


import lombok.Data;


@Data
public class Subscription {

    private String uuid;

    //private Enum subscriptionLevel;

    private String subscriptionID;

    private String courtID;

    private String caseID;

    private String urnID;

    private String uniqueSubID;


}

