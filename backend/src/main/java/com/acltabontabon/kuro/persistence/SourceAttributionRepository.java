package com.acltabontabon.kuro.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SourceAttributionRepository extends JpaRepository<SourceAttributionEntity, String> {

    List<SourceAttributionEntity> findBySourceDocumentIdInOrderById(Collection<String> sourceDocumentIds);
}
