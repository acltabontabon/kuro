# AGENTS.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**KURO** (Knowledge from Unified Real-world Opinions) is an early-stage AI product that aggregates public signals from forums, reviews, and online communities to help users form better-informed opinions before major life decisions.

A "KURO" is an informed inference — not a verdict. The product surfaces patterns, themes, sentiment, and supporting evidence from collective public experiences, leaving the final decision to the user.

## Initial Scope

The first version targets two domains:

- **Employment Intelligence** — What employees and candidates say about a workplace (culture, growth, complaints)
- **Rental Intelligence** — What residents say about a building or area (management quality, safety, recurring issues)

## Core Philosophy

KURO does not decide what is true. It identifies patterns from publicly available experiences and presents them transparently. Transparency over certainty; inference over oracle.

This philosophy is codified as a binding product contract in [docs/TRUST_AND_TRANSPARENCY.md](docs/TRUST_AND_TRANSPARENCY.md): ten trust principles, the user-facing wording rules (no verdict-like or directive language), and the map of where each is enforced (schema, the wording lint in `packages/schemas/src/wording.ts`, or as a documented prompt/UI constraint). Any work that touches user-facing output must conform to it.

## Repository Layout

- `packages/schemas` — canonical TypeScript domain schemas (`@kuro/schemas`, pnpm workspace)
- `backend/` — Spring Boot 4.1 service (Java 25, Maven, SQLite + Hibernate 7 + Flyway)
- `docs/` — product contracts and domain definitions

### Backend

Requires JDK 25 (`export JAVA_HOME=$(/usr/libexec/java_home -v 25)`; enforced by Maven Enforcer). Build/test with `mvn verify`, run with `mvn spring-boot:run` from `backend/`. Package boundaries (`api`/`application`/`domain`/`persistence`/`ai`/`extraction`, each documented in its `package-info.java`) are enforced by ArchUnit tests: `domain` stays framework-free, and the layering is `api → application → {domain, persistence}`, `persistence → domain` (nothing depends on `api`; `api` never imports `persistence`). The `ai` package is the vendor-neutral AI seam (`AiProvider.generateStructured`, typed `AiProviderException` hierarchy); `VendorIsolationTest` confines vendor SDK imports (`com.google`, `com.openai`, `com.anthropic`, `dev.langchain4j`) to `ai` so providers stay swappable. Flyway owns the schema (`ddl-auto=validate`) — every new entity lands with a matching migration, written in SQLite/PostgreSQL-portable SQL only. SQLite does not enforce foreign keys by default: datasource URLs must keep `?foreign_keys=true`. See [backend/README.md](backend/README.md).

The `domain` package transcribes `@kuro/schemas`: every enum implements `WireEnum` and carries the schema's exact string literal (`EnumSchemaDriftTest` pins the member sets — update both when the schema changes). Persistence entities use raw-id FK fields (no JPA associations) and store enums via auto-applied wire-string converters; `KuroResultPersistence` is the only public entry point for saving/loading result graphs. The `application` layer (`RequestCommandService`/`RequestQueryService`) owns request workflows and the lifecycle state machine (`domain.RequestLifecycle`); thin `api` controllers delegate to it. Result versions are immutable and insert-only — re-runs go through `KuroResultPersistence.saveNewVersion` (#15). JSON serialization uses Jackson 3 (`tools.jackson`, not `com.fasterxml`); API responses serialize domain objects to the `@kuro/schemas` wire shape via `api.KuroApiJson` (generic `WireEnum` serializer + mix-ins dropping internal confidence fields). The checked-in [backend/openapi.yaml](backend/openapi.yaml) is the reviewable REST contract.

## Status

Early-stage. The backend has the domain model, JPA persistence, the request lifecycle/result-versioning layer, and the REST surface (`/api/requests`); the collection, extraction, and synthesis pipelines that produce results for supported categories are not built yet, so a supported request stays `CREATED` and only the `unsupported_category` refusal runs end-to-end. The current focus is validating the core concept and UX around employment and rental intelligence.
