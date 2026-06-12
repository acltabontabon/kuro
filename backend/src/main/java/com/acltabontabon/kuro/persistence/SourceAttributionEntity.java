package com.acltabontabon.kuro.persistence;

import com.acltabontabon.kuro.domain.AccessedVia;
import com.acltabontabon.kuro.domain.SourceType;
import com.acltabontabon.kuro.domain.TrustTier;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "source_attribution")
class SourceAttributionEntity extends BaseEntity {

    String sourceDocumentId;
    SourceType sourceType;
    String url;
    String canonicalUrl;
    String title;
    String authorHandle;
    String publishedAt;
    String fetchedAt;
    AccessedVia accessedVia;
    TrustTier trustTier;
    String trustRationale;
    String metadataJson;
}
