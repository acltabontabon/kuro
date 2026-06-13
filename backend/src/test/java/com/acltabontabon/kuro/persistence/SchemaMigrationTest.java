package com.acltabontabon.kuro.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Proves the V1 schema enforces what it claims: foreign keys are actually on
 * (SQLite defaults them off — guards the {@code ?foreign_keys=true} URL wiring),
 * FK and CHECK violations are rejected, and source attribution is 1:1.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class SchemaMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void foreignKeyEnforcementIsOn() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA foreign_keys")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).as("PRAGMA foreign_keys").isEqualTo(1);
        }
    }

    @Test
    void fkViolationIsRejected() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            assertThatThrownBy(() -> stmt.executeUpdate("""
                    INSERT INTO evidence (id, result_id, source_document_id, snippet,
                        locator_kind, locator_anchor, extraction_method, extracted_at,
                        extractor, created_at)
                    VALUES ('ev-orphan', 'no-such-result', 'no-such-doc', 'snippet',
                        'anchor', 'p1', 'verbatim', '2026-06-12T00:00:00Z',
                        'test', '2026-06-12T00:00:00Z')"""))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("FOREIGN KEY");
        }
    }

    @Test
    void enumCheckViolationIsRejected() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            assertThatThrownBy(() -> stmt.executeUpdate("""
                    INSERT INTO subject (id, kind, display_name, created_at)
                    VALUES ('sub-bad-kind', 'banana', 'Acme', '2026-06-12T00:00:00Z')"""))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("CHECK");
        }
    }

    @Test
    void aiRunForeignKeyToResultIsEnforced() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            assertThatThrownBy(() -> stmt.executeUpdate("""
                    INSERT INTO ai_run (id, request_id, result_id, phase, model_id,
                        prompt_version, started_at, created_at)
                    VALUES ('run-orphan', 'no-such-request', 'no-such-result', 'extraction',
                        'claude', 'v1', '2026-06-12T00:00:00Z', '2026-06-12T00:00:00Z')"""))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("FOREIGN KEY");
        }
    }

    @Test
    void aiRunPhaseCheckRejectsUnknownPhase() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                seedResult(stmt);
                assertThatThrownBy(() -> stmt.executeUpdate("""
                        INSERT INTO ai_run (id, request_id, result_id, phase, model_id,
                            prompt_version, started_at, created_at)
                        VALUES ('run-bad', 'req-1', 'res-1', 'daydreaming',
                            'claude', 'v1', '2026-06-12T00:00:00Z', '2026-06-12T00:00:00Z')"""))
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("CHECK");
            } finally {
                conn.rollback();
            }
        }
    }

    /** Minimal subject → request → result chain so ai_run/attribution FKs resolve. */
    private static void seedResult(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
                INSERT INTO subject (id, kind, display_name, created_at)
                VALUES ('sub-1', 'employer', 'Acme', '2026-06-12T00:00:00Z')""");
        stmt.executeUpdate("""
                INSERT INTO kuro_request (id, status, created_at)
                VALUES ('req-1', 'READY', '2026-06-12T00:00:00Z')""");
        stmt.executeUpdate("""
                INSERT INTO kuro_result (id, request_id, version, subject_id,
                    data_sufficiency, generated_at, created_at)
                VALUES ('res-1', 'req-1', 1, 'sub-1',
                    'insufficient', '2026-06-12T00:00:00Z', '2026-06-12T00:00:00Z')""");
    }

    @Test
    void attributionIsUniquePerSourceDocument() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                seedResult(stmt);
                stmt.executeUpdate("""
                        INSERT INTO source_document (id, result_id, url, platform,
                            captured_at, created_at)
                        VALUES ('doc-1', 'res-1', 'https://example.com/review', 'example',
                            '2026-06-12T00:00:00Z', '2026-06-12T00:00:00Z')""");
                stmt.executeUpdate("""
                        INSERT INTO source_attribution (id, source_document_id, source_type,
                            fetched_at, trust_tier, created_at)
                        VALUES ('attr-1', 'doc-1', 'review_site',
                            '2026-06-12T00:00:00Z', 'community', '2026-06-12T00:00:00Z')""");
                assertThatThrownBy(() -> stmt.executeUpdate("""
                        INSERT INTO source_attribution (id, source_document_id, source_type,
                            fetched_at, trust_tier, created_at)
                        VALUES ('attr-2', 'doc-1', 'review_site',
                            '2026-06-12T00:00:00Z', 'community', '2026-06-12T00:00:00Z')"""))
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("UNIQUE");
            } finally {
                conn.rollback();
            }
        }
    }
}
