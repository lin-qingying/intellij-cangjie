# Toolchain 模块设计总结

## 🎯 核心职能

Toolchain 模块是一个**纯粹的 SDK 路径管理模块**，职责明确：

- ✅ SDK 路径的注册与存储
- ✅ SDK 版本信息的检测
- ✅ SDK 有效性验证
- ✅ SDK 元数据管理

**不包含**: 任何工具执行逻辑（编译、运行、格式化等）

---

## 🏗️ 架构设计

### 模块结构

```
toolchain/
├── api/                          # 公共 API (外部可见)
│   ├── CjSdk.kt                 # SDK 信息数据类
│   ├── CjSdkRegistry.kt         # SDK 注册中心接口
│   └── CjSdkDetector.kt         # SDK 检测器接口 (扩展点)
│
├── impl/                         # 内部实现 (external 不可见)
│   ├── CjSdkRegistryImpl.kt     # 注册中心实现 (internal)
│   └── CjSdkDetectorImpl.kt     # 默认检测器实现 (internal)
│
├── utils/
│   └── SdkVersionParser.kt      # 版本解析工具
│
└── CangJieSdkVersion.kt         # SDK 版本数据类
```

### 可见性设计

| 组件 | 可见性 | 访问方式 |
|------|--------|----------|
| `CjSdk` | public | 直接使用 |
| `CjSdkRegistry` | public | `CjSdkRegistry.getInstance()` |
| `CjSdkDetector` | public | `CjSdkDetector.getInstance()` (扩展点) |
| `CjSdkDiscoverer` | public | `CjSdkDiscoverer.getInstance()` (扩展点) |
| `CjSdkRegistryImpl` | **internal** | ❌ 外部不可访问 |
| `CjSdkDetectorImpl` | **internal** | ❌ 外部不可访问 |
| `CjSdkDiscovererImpl` | **internal** | ❌ 外部不可访问 |

---

## 🔌 服务与扩展点

### Application Service

**CjSdkRegistryImpl** - SDK 注册中心服务

```xml
<applicationService serviceImplementation="...CjSdkRegistryImpl"/>
```

访问方式:
```kotlin
val registry = CjSdkRegistry.getInstance()
```

### Extension Point

**CjSdkDetector** - SDK 检测器扩展点

```xml
<extensionPoint name="sdkDetector"
                interface="...CjSdkDetector"/>
```

默认实现:
```xml
<sdkDetector implementation="...CjSdkDetectorImpl"/>
```

访问方式:
```kotlin
// 获取第一个注册的检测器
val detector = CjSdkDetector.getInstance()

// 获取所有检测器
val allDetectors = CjSdkDetector.getAllDetectors()
```

**CjSdkDiscoverer** - SDK 发现器扩展点

```xml
<extensionPoint name="sdkDiscoverer"
                interface="...CjSdkDiscoverer"/>
```

默认实现:
```xml
<sdkDiscoverer implementation="...CjSdkDiscovererImpl"/>
```

访问方式:
```kotlin
// 获取第一个注册的发现器
val discoverer = CjSdkDiscoverer.getInstance()

// 获取所有发现器 (按优先级排序)
val allDiscoverers = CjSdkDiscoverer.getAllDiscoverers()

// 使用所有发现器发现 SDK
val allPaths = CjSdkDiscoverer.discoverAllSdkPaths()
```

---

## 💡 设计原则

### 1. 单一职责
- ✅ 只管理 SDK 路径和元数据
- ❌ 不包含工具执行逻辑
- ❌ 不依赖业务模块（psi, plugin 等）

### 2. 实现隐藏
- 所有 `*Impl` 类标记为 `internal`
- 外部只能通过接口访问
- 通过扩展点/服务获取实例

### 3. 扩展性
- SDK 检测器可自定义（扩展点机制）
- 支持多种检测策略并存

### 4. 持久化
- SDK 信息自动保存到 `cangjie-sdk.xml`
- 重启后自动恢复

---

## 📝 使用示例

### 基本用法

```kotlin
// 1. 获取服务
val registry = CjSdkRegistry.getInstance()
val detector = CjSdkDetector.getInstance() ?: error("No detector")

// 2. 注册 SDK
val sdk = registry.registerSdkPath(Paths.get("/path/to/sdk"))

// 3. 查询 SDK
val allSdks = registry.getAllSdks()
val sdk = registry.getSdk("CangJie-0.53.13")

// 4. 检测 SDK
if (detector.isValidSdk(path)) {
    val version = detector.detectVersion(path)
}

// 5. 发现 SDK
val discoverer = CjSdkDiscoverer.getInstance()
val discoveredPaths = discoverer?.discoverSdkPaths() ?: emptyList()

// 6. 自动发现并注册
CjSdkDiscoverer.discoverAllSdkPaths().forEach { path ->
    registry.registerSdkPath(path)
}
```

### 自定义检测器

```kotlin
class MyDetector : CjSdkDetector {
    // 定义需要的可执行文件
    override val requiredExecutables = listOf("cjc", "cjpm", "custom-tool")

    override fun isValidSdk(homePath: Path): Boolean {
        // 自定义逻辑
    }

    // ... 其他实现
}
```

### 自定义发现器

```kotlin
class MyDiscoverer : CjSdkDiscoverer {
    override val priority = 50
    override val displayName = "My Discoverer"

    override fun discoverSdkPaths(): List<Path> {
        // 自定义发现逻辑
    }

    // ... 其他实现
}
```

```xml
<extensions defaultExtensionNs="org.cangnova.cangjie.toolchain">
    <sdkDetector implementation="com.example.MyDetector"/>
    <sdkDiscoverer implementation="com.example.MyDiscoverer"/>
</extensions>
```
</extensions>
```

---

## 🔗 与其他模块的关系

```
┌─────────────────────┐
│   Plugin 模块        │
│                      │
│ - 使用 SDK 路径      │
│ - 执行工具链命令     │ ← 工具执行逻辑在这里
│ - 编译/运行/格式化   │
└──────────┬──────────┘
           │ 依赖
           │ (调用 CjSdkRegistry.getInstance())
           ↓
┌─────────────────────┐
│  Toolchain 模块      │
│                      │
│ - SDK 路径管理       │ ← 只管理路径和元数据
│ - SDK 版本检测       │
│ - SDK 有效性验证     │
└──────────┬──────────┘
           │ 依赖
           ↓
┌─────────────────────┐
│ Util/Messages       │
│ + IntelliJ Platform │
└─────────────────────┘
```

**职责分离**:
- Toolchain: 管理 SDK 在哪里（路径）+ 是什么（版本）
- Plugin: 使用 SDK 做什么（执行命令）

---

## ✅ 完成清单

- [x] 定义 `CjSdk` 数据类
- [x] 定义 `CjSdkRegistry` 接口
- [x] 定义 `CjSdkDetector` 接口
- [x] 定义 `CjSdkDiscoverer` 接口
- [x] 实现 `CjSdkRegistryImpl` (internal)
- [x] 实现 `CjSdkDetectorImpl` (internal)
- [x] 实现 `CjSdkDiscovererImpl` (internal)
- [x] 创建 `SdkVersionParser` 工具类
- [x] 配置扩展点 (toolchain.xml)
- [x] 通过 `getInstance()` 提供访问
- [x] `CjSdkDetector` 使用扩展点机制
- [x] `CjSdkDiscoverer` 使用扩展点机制
- [x] 编写完整 README 文档

---

## 🎓 关键要点

1. **Toolchain 模块 = SDK 路径管理器**，不是工具链执行器
2. 所有实现都是 `internal` 的，外部只能通过接口
3. `CjSdkRegistry` 通过 Application Service 提供
4. `CjSdkDetector` 通过扩展点提供，支持自定义检测逻辑
5. `CjSdkDiscoverer` 通过扩展点提供，支持自动发现 SDK
6. 持久化到 `cangjie-sdk.xml`，自动加载
7. 不依赖业务模块，保持独立性
