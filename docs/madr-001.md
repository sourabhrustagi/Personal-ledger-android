# MADR-001 — Android client choices

## Context

LedgerOne ships one Android app. Later releases add modules. They do not start a new application.

## Decision

- UI is Jetpack Compose. XML layouts are not the screen toolkit.
- Dependency injection is Hilt. Koin is not used.
- Ledger screens and their Hilt view models live in `:feature:ledger`. Wallet screens live in `:feature:wallet`. Room, sync, and the offline repository live in `:core:database`. Retrofit HTTP clients live in `:core:network`. The app module supplies the flavor config and navigation host.
- The `mock` flavor is the default and runs without a backend. `dev`, `staging`, and `prod` bind the network repository, each with its own API host and application id.
- Room is the on-device source of truth for ledger rows. The UI does not read the network directly.
- Money is a `Long` in minor units.

## Consequences

A new screen goes in its feature module and uses the shared domain types. The mock catalog in `:core:mock` is what local runs and tests seed from.
