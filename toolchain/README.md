# Toolchain 模块

## 概述

toolchain 模块是仓颉 IntelliJ 插件的 **SDK 路径管理模块**,负责:
- ✅ SDK 的注册与存储
- ✅ SDK 路径的管理
- ✅ SDK 信息(版本)的获取
- ✅ SDK 有效性检测

## 架构设计

```
toolchain/
├── api/                      # 公共接口
│   ├── CjSdk.kt             # SDK 信息数据类
│   ├── CjSdkRegistry.kt     # SDK 注册中心接口
│   └── CjSdkDetector.kt     # SDK 检测器接口
├── impl/                     # 实现类
│   ├── CjSdkRegistryImpl.kt # SDK 注册中心实现
│   └── CjSdkDetectorImpl.kt # SDK 检测器实现
└── utils/                    # 工具类
    └── SdkVersionParser.kt  # 版本解析工具
```

## 核心 API

### 1. CjSdk - SDK 信息

```kotlin
data class CjSdk(
    val id: String,              // SDK 唯一标识
    val name: String,            // SDK 显示名称
    val homePath: Path,          // SDK 根目录
    val version: CangJieSdkVersion?, // 版本信息
    val isValid: Boolean         // 是否有效
)
```

### 2. CjSdkRegistry - SDK 注册中心

负责管理所有已注册的 SDK:

```kotlin
interface CjSdkRegistry {
    // 获取所有 SDK
    fun getAllSdks(): List<CjSdk>

    // 根据 ID 获取 SDK
    fun getSdk(id: String): CjSdk?

    // 根据路径获取 SDK
    fun getSdkByPath(homePath: Path): CjSdk?

    // 注册 SDK
    fun registerSdk(sdk: CjSdk): Boolean
    fun registerSdkPath(homePath: Path, customName: String? = null): CjSdk?

    // 取消注册
    fun unregisterSdk(id: String): Boolean

    // 检查是否已注册
    fun isSdkRegistered(id: String): Boolean
    fun isSdkPathRegistered(homePath: Path): Boolean

    // 刷新 SDK 信息
    fun refreshAllSdks()
    fun refreshSdk(id: String): CjSdk?
}
```

### 3. CjSdkDetector - SDK 检测器

负责检测和验证 SDK (通过扩展点机制提供):

```kotlin
interface CjSdkDetector {
    // 必需的可执行文件列表 (每个检测器自己定义)
    val requiredExecutables: List<String>

    // 检测是否为有效的 SDK
    fun isValidSdk(homePath: Path): Boolean

    // 检测版本信息
    fun detectVersion(homePath: Path): CangJieSdkVersion?

    // 创建 SDK 实例
    fun createSdk(homePath: Path, customName: String? = null): CjSdk?

    // 检查必需的可执行文件
    fun checkRequiredExecutables(homePath: Path): List<String>

    companion object {
        // 获取默认检测器 (从扩展点)
        fun getInstance(): CjSdkDetector?

        // 获取所有注册的检测器
        fun getAllDetectors(): List<CjSdkDetector>
    }
}
```

**默认实现** (`CjSdkDetectorImpl`):
- `requiredExecutables = listOf("cjc", "cjpm")`

### 4. CjSdkDiscoverer - SDK 发现器

负责在系统中自动发现 SDK (通过扩展点机制提供):

```kotlin
interface CjSdkDiscoverer {
    // 发现器优先级 (数值越小优先级越高)
    val priority: Int

    // 发现器显示名称
    val displayName: String

    // 发现系统中所有可能的 SDK 路径
    fun discoverSdkPaths(): List<Path>

    // 在指定路径下搜索 SDK
    fun searchSdkPaths(searchPath: Path, recursive: Boolean = true, maxDepth: Int = 3): List<Path>

    companion object {
        // 获取默认发现器
        fun getInstance(): CjSdkDiscoverer?

        // 获取所有发现器 (按优先级排序)
        fun getAllDiscoverers(): List<CjSdkDiscoverer>

        // 使用所有发现器发现 SDK
        fun discoverAllSdkPaths(): List<Path>
    }
}
```

**默认实现** (`CjSdkDiscovererImpl`):
- 搜索环境变量: `CANGJIE_HOME`, `CANGJIE_SDK`, `PATH`
- Windows: `C:\Program Files\CangJie`, `C:\CangJie`, `%LOCALAPPDATA%\CangJie`
- macOS: `/usr/local/cangjie`, `/opt/cangjie`, `/Applications/CangJie`
- Linux: `/usr/local/cangjie`, `/opt/cangjie`, `/usr/cangjie`
- 用户目录: `~/.cangjie`, `~/cangjie`, `~/.local/cangjie`

## 使用示例

### 获取服务实例

```kotlin
// 获取 SDK 注册中心 (Application Service)
val registry = CjSdkRegistry.getInstance()

// 获取 SDK 检测器 (从扩展点)
val detector = CjSdkDetector.getInstance()
    ?: error("No SDK detector available")
```

### 注册新的 SDK

```kotlin
val registry = CjSdkRegistry.getInstance()

// 方式 1: 直接注册路径(自动检测)
val sdk = registry.registerSdkPath(
    homePath = Paths.get("/path/to/cangjie-sdk"),
    customName = "My CangJie SDK"
)

// 方式 2: 先创建 SDK 实例再注册
val detector = CjSdkDetector.getInstance()
val sdk = detector.createSdk(Paths.get("/path/to/cangjie-sdk"))
if (sdk != null) {
    registry.registerSdk(sdk)
}
```

### 获取已注册的 SDK

```kotlin
val registry = CjSdkRegistry.getInstance()

// 获取所有 SDK
val allSdks = registry.getAllSdks()

// 根据 ID 获取
val sdk = registry.getSdk("CangJie-0.53.13")

// 根据路径获取
val sdk = registry.getSdkByPath(Paths.get("/path/to/sdk"))
```

### 检测 SDK

```kotlin
val detector = CjSdkDetector.getInstance()
    ?: error("No SDK detector available")

// 检查是否为有效的 SDK
if (detector.isValidSdk(sdkPath)) {
    // 获取版本信息
    val version = detector.detectVersion(sdkPath)
    println("SDK Version: $version")

    // 检查缺失的可执行文件
    val missing = detector.checkRequiredExecutables(sdkPath)
    if (missing.isEmpty()) {
        println("SDK is complete")
    } else {
        println("Missing executables: $missing")
    }
}
```

### 发现 SDK

```kotlin
val discoverer = CjSdkDiscoverer.getInstance()
    ?: error("No SDK discoverer available")

// 自动发现系统中的所有 SDK
val discoveredPaths = discoverer.discoverSdkPaths()
println("Found ${discoveredPaths.size} SDKs")

// 在指定路径下搜索
val customPaths = discoverer.searchSdkPaths(
    searchPath = Paths.get("/custom/path"),
    recursive = true,
    maxDepth = 3
)

// 使用所有发现器发现 SDK
val allPaths = CjSdkDiscoverer.discoverAllSdkPaths()

// 自动注册发现的 SDK
val registry = CjSdkRegistry.getInstance()
allPaths.forEach { path ->
    registry.registerSdkPath(path)
}
```

### 刷新 SDK 信息

```kotlin
val registry = CjSdkRegistry.getInstance()

// 刷新所有 SDK
registry.refreshAllSdks()

// 刷新特定 SDK
registry.refreshSdk("CangJie-0.53.13")
```

## 持久化

SDK 注册信息会自动持久化到 IntelliJ 配置文件 `cangjie-sdk.xml` 中,包括:
- SDK 路径映射
- 自定义名称

## 扩展点

toolchain 模块提供了 SDK 检测器和发现器扩展点,允许自定义逻辑:

### 1. 自定义 SDK 检测器

```kotlin
class MyCustomSdkDetector : CjSdkDetector {
    // 定义自己需要的可执行文件
    override val requiredExecutables = listOf("cjc", "cjpm", "cjfmt")

    override fun isValidSdk(homePath: Path): Boolean {
        // 自定义检测逻辑
    }

    override fun detectVersion(homePath: Path): CangJieSdkVersion? {
        // 自定义版本检测
    }

    override fun createSdk(homePath: Path, customName: String?): CjSdk? {
        // 自定义 SDK 创建
    }

    override fun checkRequiredExecutables(homePath: Path): List<String> {
        // 检查自定义的可执行文件列表
    }
}
```

### 2. 自定义 SDK 发现器

```kotlin
class MyCustomSdkDiscoverer : CjSdkDiscoverer {
    // 设置优先级 (数值越小优先级越高)
    override val priority: Int = 50

    override val displayName: String = "My Custom Discoverer"

    override fun discoverSdkPaths(): List<Path> {
        // 在自定义位置搜索 SDK
        return listOf(
            Paths.get("/my/custom/path1"),
            Paths.get("/my/custom/path2")
        )
    }

    override fun searchSdkPaths(searchPath: Path, recursive: Boolean, maxDepth: Int): List<Path> {
        // 自定义搜索逻辑
    }
}
```

### 注册扩展

在 `plugin.xml` 中注册:

```xml
<extensions defaultExtensionNs="org.cangnova.cangjie.toolchain">
    <!-- 注册检测器 -->
    <sdkDetector implementation="com.example.MyCustomSdkDetector"/>

    <!-- 注册发现器 -->
    <sdkDiscoverer implementation="com.example.MyCustomSdkDiscoverer"/>
</extensions>
```

### 使用扩展

```kotlin
// 获取默认检测器 (第一个注册的)
val detector = CjSdkDetector.getInstance()

// 获取所有检测器
val allDetectors = CjSdkDetector.getAllDetectors()

// 获取所有发现器 (按优先级排序)
val discoverers = CjSdkDiscoverer.getAllDiscoverers()

// 使用所有发现器发现 SDK
val allSdkPaths = CjSdkDiscoverer.discoverAllSdkPaths()
```

## 依赖关系

toolchain 模块只依赖:
- ✅ `util` 模块 - 通用工具类
- ✅ `messages` 模块 - 国际化消息
- ✅ IntelliJ Platform API - 最小化依赖

**不依赖**:
- ❌ `plugin` 模块
- ❌ `psi` 模块
- ❌ 其他业务模块

这确保了 toolchain 作为基础设施模块的独立性。

## 与其他模块的关系

```
┌─────────────────────┐
│   Plugin 模块        │
│  (使用 SDK 信息)     │
└──────────┬──────────┘
           │ 依赖
           ↓
┌─────────────────────┐
│  Toolchain 模块      │
│  (管理 SDK 路径)     │
└──────────┬──────────┘
           │ 依赖
           ↓
┌─────────────────────┐
│  Util/Messages      │
└─────────────────────┘
```

- **Plugin 模块**调用 toolchain 获取 SDK 路径和版本信息
- **Plugin 模块**负责工具链的执行逻辑(编译、运行等)
- **Toolchain 模块**只负责 SDK 的路径管理,不涉及执行

## 注意事项

1. **轻量化设计**: toolchain 模块不包含任何工具执行逻辑,只管理路径和元数据
2. **版本检测**: 通过执行 `cjc --version` 命令获取版本信息(带超时保护)
3. **并发安全**: SDK 注册中心使用 `ConcurrentHashMap` 保证线程安全
4. **平台兼容**: 自动处理 Windows(.exe) 和 Unix 系统的可执行文件差异
5. **实现隐藏**: 所有实现类(`*Impl`)都是 `internal` 的,外部只能通过接口访问
6. **扩展点机制**: `CjSdkDetector` 通过扩展点提供,支持自定义检测逻辑
