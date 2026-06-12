package com.acltabontabon.kuro.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface RequestSourceRepository extends JpaRepository<RequestSourceEntity, String> {
}
