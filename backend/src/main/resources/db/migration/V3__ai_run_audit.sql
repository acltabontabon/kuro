-- AI provenance / audit log (issue #16).
--
-- Same portability/convention rules as V1/V2: TEXT/INTEGER only, enum columns as
-- TEXT + CHECK listing the wire values verbatim, application-generated ids,
-- created_at as the row audit time (distinct from the domain timestamps below).

-- One row per model invocation. A result version is produced by one or more AI
-- phases (extraction, then synthesis); each has its own model, prompt version and
-- token usage, so the provenance lives here per phase, not on the single result row.
-- result_id references the specific kuro_result *version* the run contributed to,
-- making any past version reconstructable.
--
-- PII posture (#16): we store the prompt *version*, never the raw prompt text —
-- the prompt carries source PII, the version identifier does not.
CREATE TABLE ai_run (
    id             TEXT    PRIMARY KEY,
    request_id     TEXT    NOT NULL REFERENCES kuro_request (id),
    result_id      TEXT    NOT NULL REFERENCES kuro_result (id),
    phase          TEXT    NOT NULL CHECK (phase IN ('extraction', 'synthesis')),
    model_id       TEXT    NOT NULL,
    prompt_version TEXT    NOT NULL,
    input_tokens   INTEGER,
    output_tokens  INTEGER,
    started_at     TEXT    NOT NULL,
    finished_at    TEXT,
    created_at     TEXT    NOT NULL
);

-- The kuro_result.model_id / prompt_version columns added in V1 were #16
-- placeholders; ai_run supersedes them (per-phase, not per-result). Drop them so
-- there is a single source of truth. SQLite >= 3.35 supports DROP COLUMN; the
-- bundled sqlite-jdbc is well past that.
ALTER TABLE kuro_result DROP COLUMN model_id;
ALTER TABLE kuro_result DROP COLUMN prompt_version;
