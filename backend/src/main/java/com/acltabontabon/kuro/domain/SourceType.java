package com.acltabontabon.kuro.domain;

// mirrors packages/schemas/src/sourceAttribution.ts
public enum SourceType implements WireEnum {
    REVIEW_SITE("review_site"),
    FORUM("forum"),
    SOCIAL_MEDIA("social_media"),
    BLOG("blog"),
    NEWS("news"),
    COMPANY_SITE("company_site"),
    JOB_BOARD("job_board"),
    DOCUMENTATION("documentation"),
    OTHER("other");

    private final String wire;

    SourceType(String wire) {
        this.wire = wire;
    }

    @Override
    public String wire() {
        return wire;
    }
}
