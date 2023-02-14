package uk.gov.hmcts.reform.pip.subscription.management.models.external.account.management;

public enum Roles {
    VERIFIED,
    INTERNAL_SUPER_ADMIN_CTSC,
    INTERNAL_SUPER_ADMIN_LOCAL,
    INTERNAL_ADMIN_CTSC,
    INTERNAL_ADMIN_LOCAL,
    GENERAL_THIRD_PARTY,
    VERIFIED_THIRD_PARTY_CRIME,
    VERIFIED_THIRD_PARTY_CFT,
    VERIFIED_THIRD_PARTY_PRESS,
    VERIFIED_THIRD_PARTY_CRIME_CFT,
    VERIFIED_THIRD_PARTY_CRIME_PRESS,
    VERIFIED_THIRD_PARTY_CFT_PRESS,
    VERIFIED_THIRD_PARTY_ALL,
    SYSTEM_ADMIN;
}