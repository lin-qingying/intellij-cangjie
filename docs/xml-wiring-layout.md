# XML Wiring Layout

## Main entry

`plugin.xml` -> `META-INF/cangjie-all.xml`

## Wiring domains

- `cangjie-product.xml`: startup, configurables, and host product services
- `cangjie-language-shell.xml`: parser definition and language shell
- `cangjie-editor.xml`: editor handlers and editor shell
- `cangjie-ide-features.xml`: highlighting and other IDE-facing integrations
- `cangjie-extensionPoints.xml`: host extension points
- `cangjie-project.xml` / `cangjie-dependency.xml` / `cangjie-toolchain.xml`: project-side integrations
- `cangjie-run.xml`: run integration
- `cangjie-debugger.xml`: debugger integration
- `cangjie-analysis-entry.xml`: upstream analysis include only

## Rule

Each XML should describe one responsibility domain.
No historical “catch-all” implementation registration should remain.
