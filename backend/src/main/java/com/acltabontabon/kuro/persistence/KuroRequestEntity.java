package com.acltabontabon.kuro.persistence;

import com.acltabontabon.kuro.domain.RequestStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "kuro_request")
class KuroRequestEntity extends BaseEntity {

    RequestStatus status;
}
