# Host / Upstream Responsibility Matrix

| Area | Host | Upstream |
| --- | --- | --- |
| Plugin assembly | Yes | No |
| IntelliJ extension orchestration | Yes | No |
| Project integration | Yes | No |
| Debugger / LSP integration | Yes | No |
| Formatter / highlighting integration shell | Yes | No |
| PSI implementation | No | Yes |
| Parser implementation | No | Yes |
| Analysis implementation | No | Yes |
| Stub / decompiled implementation | No | Yes |

## Constraint

If a capability already belongs to upstream, the host project may only wire it, not reimplement it.
