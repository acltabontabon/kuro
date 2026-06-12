package com.acltabontabon.kuro.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ThemeSignalRepository extends JpaRepository<ThemeSignalEntity, String> {

    List<ThemeSignalEntity> findByThemeIdInOrderByOrdinal(Collection<String> themeIds);
}
