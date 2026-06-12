-- KURO baseline schema (issue #10).
--
-- Mirrors the canonical Zod model in packages/schemas/src. Portable across
-- SQLite and PostgreSQL: only TEXT/INTEGER/REAL types, no AUTOINCREMENT/SERIAL,
-- no JSON functions, no engine date functions.
--
-- Conventions:
--   * id           TEXT PRIMARY KEY — application-generated (schema Id = non-empty string).
--   * created_at   TEXT NOT NULL — row audit time, ISO-8601 with offset (schema IsoDateTime).
--     Distinct from domain timestamps (generated_at, captured_at, fetched_at, ...).
--   * Enum columns TEXT + CHECK (... IN (...)) listing the Zod enum members verbatim.
--   * *_json       TEXT holding a JSON document for display-only prose/aggregates that
--     need no relational integrity. Shape validity is app-layer.
--   * Join tables  carry ordinal INTEGER to preserve the schema's array order
--     ("position" avoided — reserved word in PostgreSQL).
--
-- SQLite does NOT enforce foreign keys by default: every connection must set
-- PRAGMA foreign_keys=ON. The datasource URLs use ?foreign_keys=true (sqlite-jdbc
-- maps URL params to per-connection pragmas). Without it every FK below is decorative.
--
-- DDL enforces STRUCTURE only. The following rules from the Zod model
-- (result.ts superRefine and friends) stay in the application/validation layer;
-- this list is the canonical registry for #11/#15 validators:
--   1. Per-status field matrix: which nullable kuro_result columns / child rows are
--      required or forbidden for each data_sufficiency (e.g. partial requires >=1
--      evidence_gaps_json entry; unsupported_category forbids the evidence chain).
--   2. Confidence conditionals: insufficient => rating low|unknown; partial =>
--      low|medium; sufficient forbids unknown; high requires >=3 themes rated
--      medium|high.
--   3. partial => inference_limitations_json non-empty.
--   4. unsupported_category: requested_category must NOT be a supported
--      DecisionCategory; supported_categories_json must equal the exact MVP set.
--   5. insufficient: reason kind no_sources_found is inconsistent with existing
--      source_document rows; coverage entries must reference this result's documents.
--   6. Same-result membership: FK targets of signal_evidence, theme_signal,
--      inference_claim_theme and evidence.quality_is_duplicate_of must belong to the
--      same result_id (plain FKs prove existence only).
--   7. sufficient/partial: every source_document has a source_attribution
--      (the FK enforces only the attribution -> document direction).
--   8. Locator variant completeness (charRange => locator_start/locator_end,
--      lineRange => locator_start_line/locator_end_line, anchor => locator_anchor),
--      end >= start, and locator dedup per document unless quality_is_duplicate_of.
--   9. extraction_method synthesized => original_snippet or quality_notes present.
--  10. Attribution: url or accessed_via present; fetched_at not in the future;
--      published_at > fetched_at or trust_tier unknown => trust_rationale required;
--      author_handle must not look like an email or real name; canonical_url has
--      tracking parameters stripped; metadata_json <= 20 keys.
--  11. Min-1 cardinalities: signal_evidence per signal, theme_signal per theme,
--      inference_claim_theme per claim, confidence_reasons_json and
--      suggested_next_sources_json arrays.
--  12. Single is_current=1 result per request; READY results are immutable (#15).

CREATE TABLE subject (
    id           TEXT PRIMARY KEY,
    kind         TEXT NOT NULL CHECK (kind IN ('employer', 'rental', 'product', 'service', 'location', 'role', 'other')),
    display_name TEXT NOT NULL,
    description  TEXT,
    created_at   TEXT NOT NULL
);

-- Lifecycle states per #14 (backend-native enum, not a Zod source).
CREATE TABLE kuro_request (
    id         TEXT PRIMARY KEY,
    status     TEXT NOT NULL CHECK (status IN ('CREATED', 'COLLECTING', 'EXTRACTING', 'SYNTHESIZING', 'READY', 'FAILED')),
    created_at TEXT NOT NULL
);

-- One row per result version (#15). All four KuroResult variants live here;
-- variant-conditional columns are nullable (NULL passes IN-list CHECKs in both
-- engines) and governed by app-layer rule 1 above.
-- confidence_rating is the public value; confidence_support_score and
-- confidence_input_* are @internal diagnostics, never part of the API response.
-- model_id / prompt_version are nullable placeholders for #16 traceability.
CREATE TABLE kuro_result (
    id                                      TEXT    PRIMARY KEY,
    request_id                              TEXT    NOT NULL REFERENCES kuro_request (id),
    version                                 INTEGER NOT NULL,
    is_current                              INTEGER NOT NULL DEFAULT 0 CHECK (is_current IN (0, 1)),
    subject_id                              TEXT    NOT NULL REFERENCES subject (id),
    data_sufficiency                        TEXT    NOT NULL CHECK (data_sufficiency IN ('sufficient', 'partial', 'insufficient', 'unsupported_category')),
    generated_at                            TEXT    NOT NULL,
    category                                TEXT    CHECK (category IN ('employment_intelligence', 'rental_intelligence')),
    summary                                 TEXT,
    final_kuro                              TEXT,
    limitations_json                        TEXT,
    confidence_rating                       TEXT    CHECK (confidence_rating IN ('low', 'medium', 'high', 'unknown')),
    confidence_support_score                REAL,
    confidence_input_source_count           INTEGER,
    confidence_input_source_diversity       REAL,
    confidence_input_source_freshness       REAL,
    confidence_input_signal_consistency     REAL,
    confidence_input_theme_support_aggregate REAL,
    confidence_input_topic_breadth          REAL,
    confidence_reasons_json                 TEXT,
    inference_community_sentiment_summary   TEXT,
    inference_limitations_json              TEXT,
    source_summary_json                     TEXT,
    evidence_gaps_json                      TEXT,
    insufficient_reason_kind                TEXT    CHECK (insufficient_reason_kind IN ('no_sources_found', 'no_usable_evidence', 'subject_unidentifiable', 'out_of_window', 'other')),
    insufficient_reason_explanation         TEXT,
    suggested_next_sources_json             TEXT,
    -- Free string by definition: it names a category OUTSIDE DecisionCategory,
    -- so deliberately no CHECK (app-layer rule 4).
    requested_category                      TEXT,
    supported_categories_json               TEXT,
    refusal_message                         TEXT,
    model_id                                TEXT,
    prompt_version                          TEXT,
    created_at                              TEXT    NOT NULL,
    UNIQUE (request_id, version)
);

CREATE TABLE source_document (
    id           TEXT PRIMARY KEY,
    result_id    TEXT NOT NULL REFERENCES kuro_result (id),
    url          TEXT NOT NULL,
    platform     TEXT NOT NULL,
    -- Nullable by design (PII posture): author is omitted unless safely public.
    author       TEXT,
    captured_at  TEXT NOT NULL,
    published_at TEXT,
    content      TEXT,
    content_hash TEXT,
    context      TEXT,
    created_at   TEXT NOT NULL
);

-- 1:1 with source_document, enforced by the UNIQUE FK.
CREATE TABLE source_attribution (
    id                 TEXT PRIMARY KEY,
    source_document_id TEXT NOT NULL UNIQUE REFERENCES source_document (id),
    source_type        TEXT NOT NULL CHECK (source_type IN ('review_site', 'forum', 'social_media', 'blog', 'news', 'company_site', 'job_board', 'documentation', 'other')),
    url                TEXT,
    canonical_url      TEXT,
    title              TEXT,
    -- Must never be an email address or real-name shape (app-layer rule 10).
    author_handle      TEXT,
    published_at       TEXT,
    fetched_at         TEXT NOT NULL,
    accessed_via       TEXT CHECK (accessed_via IN ('direct_fetch', 'user_paste', 'file_upload', 'api_import', 'other')),
    trust_tier         TEXT NOT NULL CHECK (trust_tier IN ('primary', 'secondary', 'community', 'low_context', 'unknown')),
    trust_rationale    TEXT,
    metadata_json      TEXT,
    created_at         TEXT NOT NULL
);

CREATE TABLE redaction (
    id                    TEXT PRIMARY KEY,
    source_attribution_id TEXT NOT NULL REFERENCES source_attribution (id),
    field                 TEXT NOT NULL,
    category              TEXT NOT NULL CHECK (category IN ('pii', 'private_id', 'email', 'real_name', 'hidden_metadata', 'other')),
    reason                TEXT,
    created_at            TEXT NOT NULL
);

-- Locator/extraction/quality are 1:1 value objects, flattened. locator_kind
-- values keep the schema's exact camelCase.
CREATE TABLE evidence (
    id                     TEXT    PRIMARY KEY,
    result_id              TEXT    NOT NULL REFERENCES kuro_result (id),
    source_document_id     TEXT    NOT NULL REFERENCES source_document (id),
    snippet                TEXT    NOT NULL,
    original_snippet       TEXT,
    locator_kind           TEXT    NOT NULL CHECK (locator_kind IN ('charRange', 'lineRange', 'anchor')),
    locator_start          INTEGER,
    locator_end            INTEGER,
    locator_start_line     INTEGER,
    locator_end_line       INTEGER,
    locator_anchor         TEXT,
    extraction_method      TEXT    NOT NULL CHECK (extraction_method IN ('verbatim', 'normalized', 'synthesized')),
    extracted_at           TEXT    NOT NULL,
    extractor              TEXT    NOT NULL,
    quality_source_trust   TEXT    CHECK (quality_source_trust IN ('low', 'medium', 'high')),
    quality_is_duplicate_of TEXT   REFERENCES evidence (id),
    quality_notes          TEXT,
    created_at             TEXT    NOT NULL
);

CREATE TABLE signal (
    id                                  TEXT PRIMARY KEY,
    result_id                           TEXT NOT NULL REFERENCES kuro_result (id),
    topic                               TEXT NOT NULL,
    sentiment                           TEXT NOT NULL CHECK (sentiment IN ('positive', 'negative', 'neutral', 'mixed')),
    claim                               TEXT NOT NULL,
    confidence_rating                   TEXT NOT NULL CHECK (confidence_rating IN ('low', 'medium', 'high')),
    confidence_support_score            REAL,
    confidence_input_source_count       INTEGER,
    confidence_input_source_diversity   REAL,
    confidence_input_source_freshness   REAL,
    confidence_input_signal_consistency REAL,
    confidence_input_clarity            REAL,
    confidence_input_language_ambiguity REAL,
    confidence_input_directness_of_support REAL,
    confidence_reasons_json             TEXT NOT NULL,
    created_at                          TEXT NOT NULL
);

-- Signal.evidenceIds (ordered, min 1 per signal app-layer).
CREATE TABLE signal_evidence (
    id          TEXT    PRIMARY KEY,
    signal_id   TEXT    NOT NULL REFERENCES signal (id),
    evidence_id TEXT    NOT NULL REFERENCES evidence (id),
    ordinal     INTEGER NOT NULL,
    created_at  TEXT    NOT NULL,
    UNIQUE (signal_id, evidence_id)
);

CREATE TABLE theme (
    id                                  TEXT PRIMARY KEY,
    result_id                           TEXT NOT NULL REFERENCES kuro_result (id),
    topic                               TEXT NOT NULL,
    sentiment                           TEXT NOT NULL CHECK (sentiment IN ('positive', 'negative', 'neutral', 'mixed')),
    confidence_rating                   TEXT NOT NULL CHECK (confidence_rating IN ('low', 'medium', 'high')),
    confidence_support_score            REAL,
    confidence_input_source_count       INTEGER,
    confidence_input_source_diversity   REAL,
    confidence_input_source_freshness   REAL,
    confidence_input_signal_consistency REAL,
    confidence_reasons_json             TEXT NOT NULL,
    may_suggest_json                    TEXT NOT NULL,
    may_not_suggest_json                TEXT NOT NULL,
    limitations_json                    TEXT NOT NULL,
    created_at                          TEXT NOT NULL
);

-- Theme.signalIds (ordered, min 1 per theme app-layer).
CREATE TABLE theme_signal (
    id         TEXT    PRIMARY KEY,
    theme_id   TEXT    NOT NULL REFERENCES theme (id),
    signal_id  TEXT    NOT NULL REFERENCES signal (id),
    ordinal    INTEGER NOT NULL,
    created_at TEXT    NOT NULL,
    UNIQUE (theme_id, signal_id)
);

-- KuroInference is 1:1 with a result: its scalar fields live on kuro_result;
-- the five claim arrays land here. kind maps the schema's object keys
-- (patterns, consensus, disagreements, maySuggest, mayNotSuggest) to snake_case.
CREATE TABLE inference_claim (
    id          TEXT    PRIMARY KEY,
    result_id   TEXT    NOT NULL REFERENCES kuro_result (id),
    kind        TEXT    NOT NULL CHECK (kind IN ('patterns', 'consensus', 'disagreements', 'may_suggest', 'may_not_suggest')),
    description TEXT    NOT NULL,
    ordinal     INTEGER NOT NULL,
    created_at  TEXT    NOT NULL
);

-- InferenceClaim.themeIds (ordered, min 1 per claim app-layer).
CREATE TABLE inference_claim_theme (
    id                 TEXT    PRIMARY KEY,
    inference_claim_id TEXT    NOT NULL REFERENCES inference_claim (id),
    theme_id           TEXT    NOT NULL REFERENCES theme (id),
    ordinal            INTEGER NOT NULL,
    created_at         TEXT    NOT NULL,
    UNIQUE (inference_claim_id, theme_id)
);

-- Per-source assessment on insufficient results (SourceCoverageEntry).
CREATE TABLE source_coverage_entry (
    id                 TEXT PRIMARY KEY,
    result_id          TEXT NOT NULL REFERENCES kuro_result (id),
    source_document_id TEXT NOT NULL REFERENCES source_document (id),
    assessment         TEXT NOT NULL CHECK (assessment IN ('spam', 'duplicate', 'inaccessible', 'unrelated', 'too_vague', 'not_about_subject', 'stale', 'promotional', 'other')),
    note               TEXT,
    created_at         TEXT NOT NULL
);
