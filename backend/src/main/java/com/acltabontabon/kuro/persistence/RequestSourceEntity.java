package com.acltabontabon.kuro.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A user-attached source staged on a request before collection (#13). {@code
 * kind} is 'url' or 'text' — a staging concern, not a schema enum, so it is a
 * plain String matched by the DDL CHECK.
 */
@Entity
@Table(name = "request_source")
class RequestSourceEntity extends BaseEntity {

    String requestId;
    String kind;
    String value;
}
