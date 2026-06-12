package com.acltabontabon.kuro.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface RequestStatusTransitionRepository extends JpaRepository<RequestStatusTransitionEntity, String> {

    List<RequestStatusTransitionEntity> findByRequestIdOrderByCreatedAt(String requestId);
}
