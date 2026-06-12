# KURO Backend

Spring Boot 4.1 service on Java 25, backed by SQLite (Hibernate ORM 7 via the
community SQLite dialect) with Flyway-owned schema.

## Requirements

- JDK 25 (enforced by Maven Enforcer — the build fails on any other major version)
- Maven 3.9+

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)   # macOS
```

## Run

```sh
mvn spring-boot:run
```

Boots against a file-backed SQLite database at `data/kuro.db`, created
automatically on first start — no manual setup. Local SQLite files
(`*.db`, `*.db-shm`, `*.db-wal`, `data/`) are git-ignored.

Health check: `GET http://localhost:8080/actuator/health` → `{"status":"UP"}`.
Actuator exposes only `health` and `info`.

## Test

```sh
mvn verify
```

Tests run under the `test` profile against a throwaway SQLite database in
`target/` (wiped by `mvn clean`). The `@SpringBootTest` smoke test doubles as
the SQLite + Hibernate 7 + Flyway viability check.

## Package boundaries

Base package `com.acltabontabon.kuro`, one Maven module, boundaries are
packages (each documented in its `package-info.java`):

| Package       | Role                                  | May depend on |
|---------------|---------------------------------------|---------------|
| `domain`      | Framework-free core model             | nothing internal, no frameworks |
| `api`         | HTTP controllers, DTOs                | `domain`      |
| `persistence` | JPA entities, repositories            | `domain`      |
| `ai`          | AI provider adapters                  | `domain`      |
| `extraction`  | Signal extraction                     | `domain`      |

The `domain` rule (no Spring, no JPA/Jakarta persistence, no Hibernate, no
Flyway, no adapter packages) is enforced by the ArchUnit test
`DomainBoundaryTest` — it fails the build on violation.

## Schema ownership

**Flyway owns the schema.** Migrations live in
`src/main/resources/db/migration`; `spring.jpa.hibernate.ddl-auto=validate`
means Hibernate never creates or alters tables. Every entity added later must
land in the same PR as its matching Flyway migration.

`V1__schema.sql` is the baseline: it mirrors the canonical Zod model in
`packages/schemas/src` (tables, FK chain, 1:1 attribution, enum CHECKs). DDL
enforces structure only — the conditional rules from `result.ts`'s
`superRefine` stay app-layer and are listed in the migration's header comment.

### Foreign keys (SQLite)

SQLite does **not** enforce foreign keys by default. Both datasource URLs carry
`?foreign_keys=true` (sqlite-jdbc maps URL parameters to per-connection
pragmas, so it survives connection pooling). Without it, every FK in the schema
is decorative. `SchemaMigrationTest` asserts the pragma is on, so removing the
parameter fails the build.

### PostgreSQL portability

The migration uses only SQL portable between SQLite and PostgreSQL:
`TEXT`/`INTEGER`/`REAL` columns, application-generated TEXT ids (no
`AUTOINCREMENT`/`SERIAL`), ISO-8601 TEXT timestamps (no engine date functions),
and plain `CHECK`/`UNIQUE`/`REFERENCES` constraints. To prove it applies to
Postgres unchanged (one-off manual check, not CI):

```sh
docker run --rm -d --name kuro-pg -e POSTGRES_PASSWORD=x -p 5432:5432 postgres:17
docker exec -i kuro-pg psql -U postgres -v ON_ERROR_STOP=1 \
  < src/main/resources/db/migration/V1__schema.sql
docker rm -f kuro-pg
```
