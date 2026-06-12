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

Requires JDK 25 (`export JAVA_HOME=$(/usr/libexec/java_home -v 25)`; enforced by Maven Enforcer). Build/test with `mvn verify`, run with `mvn spring-boot:run` from `backend/`. Package boundaries (`api`/`domain`/`persistence`/`ai`/`extraction`, each documented in its `package-info.java`) are enforced by an ArchUnit test: `domain` stays framework-free. Flyway owns the schema (`ddl-auto=validate`) — every new entity lands with a matching migration. See [backend/README.md](backend/README.md).

## Status

Early-stage. The backend is a skeleton only (no entities or endpoints yet) — the current focus is validating the core concept and UX around employment and rental intelligence.
