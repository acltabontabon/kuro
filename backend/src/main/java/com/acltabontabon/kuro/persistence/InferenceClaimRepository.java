package com.acltabontabon.kuro.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface InferenceClaimRepository extends JpaRepository<InferenceClaimEntity, String> {

    List<InferenceClaimEntity> findByResultIdOrderByOrdinal(String resultId);
}
