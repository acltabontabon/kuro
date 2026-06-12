package com.acltabontabon.kuro.persistence;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.data.domain.Persistable;

/**
 * Shared id + created_at audit column for every table. Ids are application
 * generated (schema Id = non-empty string), so {@link Persistable} tells
 * Spring Data a fresh instance is new — save() persists directly instead of
 * merge's select-then-insert.
 */
@MappedSuperclass
abstract class BaseEntity implements Persistable<String> {

    @Id
    String id;

    String createdAt;

    @Transient
    private boolean loaded;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return !loaded;
    }

    @PostLoad
    @PostPersist
    void markLoaded() {
        loaded = true;
    }

    @PrePersist
    void stampCreatedAt() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
    }
}
