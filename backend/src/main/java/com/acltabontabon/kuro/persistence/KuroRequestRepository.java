package com.acltabontabon.kuro.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface KuroRequestRepository extends JpaRepository<KuroRequestEntity, String> {
}
