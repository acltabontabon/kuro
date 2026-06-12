package com.acltabontabon.kuro.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "source_document")
class SourceDocumentEntity extends BaseEntity {

    String resultId;
    String url;
    String platform;
    String author;
    String capturedAt;
    String publishedAt;
    String content;
    String contentHash;
    String context;
}
