package uk.gov.hmcts.reform.pip.subscription.management.models.external.data.management;

import lombok.Getter;

/**
 * Enum that represents the different list types.
 */
@Getter
public enum ListType {
    SJP_PUBLIC_LIST(true),
    SJP_PRESS_LIST(true),
    CROWN_DAILY_LIST,
    CROWN_FIRM_LIST,
    CROWN_WARNED_LIST,
    MAGISTRATES_PUBLIC_LIST,
    MAGISTRATES_STANDARD_LIST,
    IAC_DAILY_LIST,
    CIVIL_DAILY_CAUSE_LIST,
    FAMILY_DAILY_CAUSE_LIST,
    CIVIL_AND_FAMILY_DAILY_CAUSE_LIST,
    COP_DAILY_CAUSE_LIST,
    SSCS_DAILY_LIST;

    /**
     * Flag that represents whether the list type is SJP.
     */
    private final boolean isSjp;

    ListType(boolean isSjp) {
        this.isSjp = isSjp;
    }

    ListType() {
        this.isSjp = false;
    }

}
