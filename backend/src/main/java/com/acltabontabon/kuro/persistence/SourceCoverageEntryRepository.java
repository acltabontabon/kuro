package com.acltabontabon.kuro.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SourceCoverageEntryRepository extends JpaRepository<SourceCoverageEntryEntity, String> {

    List<SourceCoverageEntryEntity> findByResultIdOrderById(String resultId);
}
