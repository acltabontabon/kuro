package com.acltabontabon.kuro.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface InferenceClaimThemeRepository extends JpaRepository<InferenceClaimThemeEntity, String> {

    List<InferenceClaimThemeEntity> findByInferenceClaimIdInOrderByOrdinal(Collection<String> inferenceClaimIds);
}
