package com.acltabontabon.kuro.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ThemeRepository extends JpaRepository<ThemeEntity, String> {

    List<ThemeEntity> findByResultIdOrderById(String resultId);
}
