package com.acltabontabon.kuro.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SignalEvidenceRepository extends JpaRepository<SignalEvidenceEntity, String> {

    List<SignalEvidenceEntity> findBySignalIdInOrderByOrdinal(Collection<String> signalIds);
}
