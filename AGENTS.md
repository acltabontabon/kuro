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

## Status

Early-stage. No application code exists yet — the current focus is validating the core concept and UX around employment and rental intelligence.
