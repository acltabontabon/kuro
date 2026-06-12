package com.acltabontabon.kuro.persistence;

import com.acltabontabon.kuro.domain.RequestStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * One row per request lifecycle transition (#14). Append-only: rows are
 * inserted alongside the kuro_request.status update in the same transaction
 * and never modified, so the history is a faithful audit trail.
 */
@Entity
@Table(name = "request_status_transition")
class RequestStatusTransitionEntity extends BaseEntity {

    String requestId;
    RequestStatus fromStatus;
    RequestStatus toStatus;
    String at;
    String note;
}
