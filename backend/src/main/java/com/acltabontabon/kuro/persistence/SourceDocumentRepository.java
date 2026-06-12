package com.acltabontabon.kuro.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Raw source documents are write-once: rows are inserted at capture and
 * never updated afterwards. This module deliberately exposes no update
 * path — extraction and synthesis only read them. Result immutability on
 * top of this is layered by #15.
 */
interface SourceDocumentRepository extends JpaRepository<SourceDocumentEntity, String> {

    List<SourceDocumentEntity> findByResultIdOrderById(String resultId);
}
