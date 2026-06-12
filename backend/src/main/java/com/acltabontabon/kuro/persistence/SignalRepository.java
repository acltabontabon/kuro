package com.acltabontabon.kuro.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SignalRepository extends JpaRepository<SignalEntity, String> {

    List<SignalEntity> findByResultIdOrderById(String resultId);
}
