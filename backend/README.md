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
