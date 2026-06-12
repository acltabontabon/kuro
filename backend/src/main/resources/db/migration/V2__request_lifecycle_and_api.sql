-- Request lifecycle, source staging, and result-versioning guards (issues #13/#14/#15).
--
-- Same portability/convention rules as V1__schema.sql: TEXT/INTEGER only, enum
-- columns as TEXT + CHECK listing the wire values verbatim, application-generated ids.

-- #14: append-only record of every request lifecycle transition.
CREATE TABLE request_status_transition (
    id          TEXT PRIMARY KEY,
    request_id  TEXT NOT NULL REFERENCES kuro_request (id),
    from_status TEXT NOT NULL CHECK (from_status IN ('CREATED', 'COLLECTING', 'EXTRACTING', 'SYNTHESIZING', 'READY', 'FAILED')),
    to_status   TEXT NOT NULL CHECK (to_status   IN ('CREATED', 'COLLECTING', 'EXTRACTING', 'SYNTHESIZING', 'READY', 'FAILED')),
    at          TEXT NOT NULL,
    note        TEXT,
    created_at  TEXT NOT NULL
);

-- #13: user-attached sources, staged on the request before any result exists.
CREATE TABLE request_source (
    id         TEXT PRIMARY KEY,
    request_id TEXT NOT NULL REFERENCES kuro_request (id),
    kind       TEXT NOT NULL CHECK (kind IN ('url', 'text')),
    value      TEXT NOT NULL,
    created_at TEXT NOT NULL
);

-- #13: a request remembers what it is about even before a result is produced.
ALTER TABLE kuro_request ADD COLUMN category   TEXT CHECK (category IN ('employment_intelligence', 'rental_intelligence'));
ALTER TABLE kuro_request ADD COLUMN subject_id TEXT REFERENCES subject (id);

-- #15: at most one current result per request (DB-level guarantee, defence in
-- depth behind the insert-only persistence path). (request_id, version) is
-- already UNIQUE in V1.
CREATE UNIQUE INDEX ux_kuro_result_current ON kuro_result (request_id) WHERE is_current = 1;
