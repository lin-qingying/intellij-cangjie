# CangJie Host Plugin Architecture

## Goal

This repository is the IntelliJ host plugin for CangJie.
It is not a local compiler/analysis implementation repository anymore.

## Layering

1. Product layer:
`plugin`, packaging, product assembly, IntelliJ plugin metadata.

2. IDE shell layer:
language registration, editor interaction, actions, navigation, run entrypoints.

3. Project integration layer:
project model, dependency sync, workspace integration, toolchain.

4. Feature integration layer:
debugger, LSP, formatter, highlighting.

5. Support layer:
messages, notifications, telemetry, icons.

## Upstream boundary

The following capabilities come from upstream runtime dependencies:

- PSI
- parser definitions
- analysis API
- stubs / decompiled / light declarations

The host project may declare IntelliJ extension wiring for these capabilities, but it must not keep a local copy of their implementation.

## Forbidden direction

Do not reintroduce local `analysis/`, `psi/`, `descriptors/`, `common/`, `util/`, `metadata/`, or `macro/` implementation modules into the host architecture.
