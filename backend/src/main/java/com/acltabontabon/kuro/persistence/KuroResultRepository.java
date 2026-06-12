package com.acltabontabon.kuro.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface KuroResultRepository extends JpaRepository<KuroResultEntity, String> {

    @Query("select max(r.version) from KuroResultEntity r where r.requestId = :requestId")
    Optional<Integer> findMaxVersion(@Param("requestId") String requestId);

    Optional<KuroResultEntity> findByRequestIdAndIsCurrentTrue(String requestId);
}
