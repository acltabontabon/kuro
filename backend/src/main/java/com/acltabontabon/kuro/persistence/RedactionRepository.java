package com.acltabontabon.kuro.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface RedactionRepository extends JpaRepository<RedactionEntity, String> {

    List<RedactionEntity> findBySourceAttributionIdInOrderById(Collection<String> sourceAttributionIds);
}
