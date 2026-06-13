package com.acltabontabon.kuro.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface AiRunRepository extends JpaRepository<AiRunEntity, String> {

    List<AiRunEntity> findByResultIdOrderByStartedAt(String resultId);
}
