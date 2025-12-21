# 仓颉 CJO 服务模块设计文档

## 1. 概述

本文档描述仓颉 IntelliJ 插件中 CJO（编译后元数据文件）服务模块的统一设计方案。该模块负责：

1. CJO 文件的统一加载和解析
2. 包元数据的缓存管理
3. 跨包类型引用的解析
4. 为 Stub 构建层和描述符层提供统一的数据访问接口

## 2. 当前架构问题

### 2.1 现有架构的分散性

当前存在两套独立的元数据访问机制：

```
┌──────────────────────────────────────────────────────────────────────┐
│                          Stub 构建层                                  │
│  (analysis/decompiler-to-psi)                                        │
├──────────────────────────────────────────────────────────────────────┤
│  PackageMetadataRegistry ──── FullIdResolver ──── TypeClsStubBuilder │
│  (简单缓存，未完整实现)                                                │
└────────────────────────────────┬─────────────────────────────────────┘
                                 │ 重复加载
                                 ▼
┌──────────────────────────────────────────────────────────────────────┐
│                          描述符层                                     │
│  (descriptors/deserialization)                                       │
├──────────────────────────────────────────────────────────────────────┤
│  DeserializationComponents ── FullIdFinder ── TypeDeserializer       │
│  (通过 ModuleDescriptor 访问)                                         │
└──────────────────────────────────────────────────────────────────────┘
```

### 2.2 主要问题

1. **重复加载**: 同一个 CJO 文件可能被多次解析
2. **缓存不统一**: Stub 层和描述符层各自维护缓存
3. **生命周期管理不完善**: 未利用 IntelliJ 缓存系统
4. **跨包引用解析不完整**: `PackageMetadataRegistry.getPackage()` 未实现按需加载

### 2.3 废弃的 CJD 文件

`CangJieDeclarationsFileType` (.cjd) 已被标记为废弃，应当删除相关代码。当前仅使用：
- `.cjo` - 编译后的包元数据文件（CangJie Object）
- `.cjb` - 内置库元数据文件（使用相同的格式，仅文件扩展名不同）

## 3. 统一架构设计

### 3.1 分层架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           应用层                                         │
│  Stub 构建 │ 描述符反序列化 │ 代码补全 │ 导航 │ 重构                        │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       CJO 服务层 (新增)                                   │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                     CjoPackageService                              │ │
│  │  - 统一的包元数据访问接口                                            │ │
│  │  - 项目级服务 (@Service)                                            │ │
│  │  - IntelliJ 缓存集成                                                │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                │                                         │
│  ┌─────────────────────────────┼─────────────────────────────────────┐  │
│  │                             ▼                                     │  │
│  │  ┌───────────────────┐  ┌───────────────────┐  ┌────────────────┐ │  │
│  │  │ CjoFileRegistry   │  │ CjoPackageCache   │  │ CjoFileLoader  │ │  │
│  │  │ (文件位置注册)     │  │ (包装器缓存)       │  │ (文件加载)     │ │  │
│  │  └───────────────────┘  └───────────────────┘  └────────────────┘ │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       元数据解析层                                        │
│  moduleParser.kt ──── FbPackage ──── PackageWrapper                     │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.2 核心组件

#### 3.2.1 CjoPackageService (项目级服务)

统一的包元数据服务，为整个项目提供 CJO 文件的访问和缓存管理。

```kotlin
/**
 * CJO 包元数据服务
 *
 * 项目级服务，提供统一的包元数据访问接口。
 * 支持：
 * 1. 按需加载 - 只在需要时解析 CJO 文件
 * 2. 自动缓存 - 使用 IntelliJ 缓存系统
 * 3. 缓存失效 - 文件修改时自动清除
 * 4. 跨包查找 - 支持完整的 FullId 解析
 */
@Service(Service.Level.PROJECT)
class CjoPackageService(private val project: Project) : Disposable {

    /**
     * 获取包元数据
     *
     * @param packageFqName 包的完全限定名
     * @return 包装器，如果找不到返回 null
     */
    fun getPackage(packageFqName: FqName): PackageWrapper?

    /**
     * 通过虚拟文件获取包元数据
     *
     * @param virtualFile CJO 文件
     * @return 包装器
     */
    fun getPackageFromFile(virtualFile: VirtualFile): PackageWrapper?

    /**
     * 注册包文件位置（用于延迟加载）
     */
    fun registerPackageFile(packageFqName: FqName, virtualFile: VirtualFile)

    /**
     * 获取所有已注册的包名
     */
    fun getAllPackageNames(): Set<FqName>

    /**
     * 检查包是否存在
     */
    fun hasPackage(packageFqName: FqName): Boolean

    /**
     * 使缓存失效
     */
    fun invalidateCache(packageFqName: FqName)

    companion object {
        fun getInstance(project: Project): CjoPackageService
    }
}
```

#### 3.2.2 CjoFileLoader (文件加载器)

负责 CJO 文件的实际加载和解析。

```kotlin
/**
 * CJO 文件加载器
 *
 * 负责从文件系统加载和解析 CJO 文件。
 * 支持 .cjo 和 .cjb 两种文件格式。
 */
object CjoFileLoader {

    /**
     * 从 VirtualFile 加载包元数据
     */
    fun loadFromFile(virtualFile: VirtualFile): PackageWrapper?

    /**
     * 从字节数组加载包元数据
     */
    fun loadFromBytes(bytes: ByteArray): PackageWrapper?

    /**
     * 从 InputStream 加载包元数据
     */
    fun loadFromStream(inputStream: InputStream): PackageWrapper?
}
```

#### 3.2.3 CjoFullIdResolver (跨包类型解析器)

统一的 FullId 解析接口，供 Stub 层和描述符层使用。

```kotlin
/**
 * FullId 解析器
 *
 * 统一的跨包类型引用解析接口。
 * 支持当前包和导入包的声明查找。
 */
interface CjoFullIdResolver {

    /**
     * 解析 FullId 获取声明信息
     */
    fun resolve(fullId: FbFullId): ResolveResult

    sealed class ResolveResult {
        data class Success(
            val fqName: FqName,
            val name: Name,
            val packageFqName: FqName,
            val needsQualification: Boolean,
            val declIndex: Int
        ) : ResolveResult()

        object Failed : ResolveResult()
    }
}

/**
 * 基于 CjoPackageService 的 FullId 解析器实现
 */
class CjoFullIdResolverImpl(
    private val packageService: CjoPackageService,
    private val currentPackage: PackageWrapper
) : CjoFullIdResolver
```

### 3.3 与现有模块的集成

#### 3.3.1 Stub 构建层集成

```kotlin
// ClsStubBuilderComponents 使用 CjoPackageService
class ClsStubBuilderComponents(
    val project: Project,
    val packageService: CjoPackageService,  // 替换原来的 registry
    val currentPackage: PackageWrapper,
    // ...
) {
    val fullIdResolver = CjoFullIdResolverImpl(packageService, currentPackage)
}

// CangJieMetadataStubBuilder 使用 CjoPackageService
private fun buildCompatibleFileStub(
    file: FileWithMetadata.Compatible,
    virtualFile: VirtualFile,
    project: Project
): PsiFileStub<*> {
    val packageService = CjoPackageService.getInstance(project)
    // ...
}
```

#### 3.3.2 描述符层集成

```kotlin
// DeserializationComponents 可选使用 CjoPackageService
class DeserializationComponents(
    // ...
    val packageService: CjoPackageService? = null,  // 可选注入
    // ...
) {
    // 当 packageService 可用时，优先使用它进行跨包查找
}

// FullIdFinderImpl 可复用 CjoFullIdResolver
class FullIdFinderImpl(
    val moduleDescriptor: ModuleDescriptor,
    val `package`: PackageWrapper,
    val context: DeserializationContext,
    val packageService: CjoPackageService? = null  // 可选注入
) : FullIdFinder
```

## 4. 数据流程

### 4.1 CJO 文件加载流程

```
IDE 索引/用户请求
    │
    ▼
CjoPackageService.getPackage(packageFqName)
    │
    ├─ 检查缓存 ──→ 命中 ──→ 返回 PackageWrapper
    │
    └─ 未命中
       │
       ▼
    查找文件位置 (fileRegistry)
       │
       ├─ 未注册 ──→ 扫描项目依赖查找 .cjo 文件
       │
       └─ 已注册
          │
          ▼
    CjoFileLoader.loadFromFile(virtualFile)
       │
       ▼
    ByteBuffer.toFbPackage()
       │
       ▼
    FbPackage.packageWrapper
       │
       ▼
    存入缓存 (CachedValuesManager)
       │
       ▼
    返回 PackageWrapper
```

### 4.2 跨包类型解析流程

```
FullId { pkgId: 3, decl: "ArrayList", index: 42 }
    │
    ▼
CjoFullIdResolver.resolve(fullId)
    │
    ├─ pkgId = -1 (INVALID) ──→ Failed
    │
    ├─ pkgId = -2 (CURRENT) ──→ 在当前包 declTable 查找
    │
    └─ pkgId >= 0 (导入包)
       │
       ▼
    currentPackage.importPackageFqNames[pkgId]
       │
       ▼
    CjoPackageService.getPackage(importedPackageFqName)
       │
       ├─ 找到包 ──→ 在包的 declTable 查找 index 或 exportId
       │
       └─ 未找到 ──→ Failed
```

## 5. 缓存策略

### 5.1 使用 IntelliJ CachedValuesManager

```kotlin
private fun loadAndCachePackage(
    packageFqName: FqName,
    virtualFile: VirtualFile
): PackageWrapper? {
    return CachedValuesManager.getManager(project).getCachedValue(
        virtualFile,
        CjoPackageCacheKey,
        {
            val wrapper = CjoFileLoader.loadFromFile(virtualFile)
            CachedValueProvider.Result.create(
                wrapper,
                virtualFile,  // 文件变化时失效
                ProjectRootModificationTracker.getInstance(project)  // 项目结构变化时失效
            )
        },
        false
    )
}
```

### 5.2 缓存失效条件

| 条件 | 触发 |
|------|------|
| CJO 文件内容修改 | VirtualFile 依赖 |
| 项目依赖变化 | ProjectRootModificationTracker |
| 显式失效调用 | invalidateCache() |
| 项目关闭 | Disposable |

## 6. 废弃代码清理

### 6.1 待删除的 CJD 相关代码

| 文件 | 说明 |
|------|------|
| `CangJieDeclarationsFileType` | 废弃的 .cjd 文件类型 |
| 相关的 FileTypeFactory | 如果有 |
| plugin.xml 中的注册 | 文件类型注册 |

### 6.2 待重构的代码

| 文件 | 变更 |
|------|------|
| `PackageMetadataRegistry` | 重构为使用 `CjoPackageService` 或删除 |
| `FullIdResolver` | 重构为实现 `CjoFullIdResolver` 接口 |
| `ClsStubBuilderComponents` | 使用 `CjoPackageService` |
| `CangJieMetadataStubBuilder` | 使用 `CjoPackageService` |

## 7. 模块结构

### 7.1 新模块: `metadata-service`

建议创建一个新的 Gradle 模块来承载 CJO 服务：

```
metadata-service/
├── src/main/kotlin/org/cangnova/cangjie/metadata/service/
│   ├── CjoPackageService.kt         # 核心服务
│   ├── CjoFileLoader.kt             # 文件加载
│   ├── CjoFullIdResolver.kt         # FullId 解析接口
│   ├── impl/
│   │   ├── CjoPackageServiceImpl.kt
│   │   └── CjoFullIdResolverImpl.kt
│   └── cache/
│       └── CjoPackageCache.kt       # 缓存管理
└── build.gradle.kts
```

### 7.2 模块依赖

```
metadata-service
    ├── depends on: metadata (FbPackage, PackageWrapper)
    ├── depends on: common (Name, FqName)
    └── depends on: intellij-platform (Service, CachedValuesManager)

decompiler-to-psi
    └── depends on: metadata-service

descriptors/deserialization
    └── depends on: metadata-service (可选)
```

## 8. 服务注册

### 8.1 plugin.xml 配置

```xml
<idea-plugin>
    <extensions defaultExtensionNs="com.intellij">
        <!-- CJO 包服务 -->
        <projectService
            serviceInterface="org.cangnova.cangjie.metadata.service.CjoPackageService"
            serviceImplementation="org.cangnova.cangjie.metadata.service.impl.CjoPackageServiceImpl"/>
    </extensions>
</idea-plugin>
```

## 9. 实现计划

### 阶段 1: 核心服务实现
- [ ] 创建 `metadata-service` 模块（或在现有模块中实现）
- [ ] 实现 `CjoPackageService` 接口和实现类
- [ ] 实现 `CjoFileLoader`
- [ ] 实现基于 IntelliJ 缓存的存储

### 阶段 2: 集成现有代码
- [ ] 重构 `PackageMetadataRegistry` 为使用 `CjoPackageService`
- [ ] 重构 `FullIdResolver` 实现 `CjoFullIdResolver` 接口
- [ ] 更新 `ClsStubBuilderComponents` 使用新服务
- [ ] 更新 `CangJieMetadataStubBuilder`

### 阶段 3: 清理废弃代码
- [ ] 删除 `CangJieDeclarationsFileType` (.cjd)
- [ ] 清理相关的文件类型注册
- [ ] 删除不再使用的代码

### 阶段 4: 测试和优化
- [ ] 编写单元测试
- [ ] 性能测试
- [ ] 内存使用优化

## 10. 相关文件

### 现有核心文件

| 文件 | 职责 |
|------|------|
| `moduleParser.kt` | Flatbuffers 解析 |
| `PackageWrapper.kt` | 包装器定义 |
| `readPackageFragment.kt` | 内置库读取 |
| `CangJieBuiltInDecompiler.kt` | 内置库反编译 |
| `CangJieMetadataDecompiler.kt` | 通用元数据反编译 |

### 待创建文件

| 文件 | 职责 |
|------|------|
| `CjoPackageService.kt` | 核心服务接口 |
| `CjoPackageServiceImpl.kt` | 服务实现 |
| `CjoFileLoader.kt` | 文件加载器 |
| `CjoFullIdResolver.kt` | FullId 解析接口 |

## 11. 设计考量

### 11.1 性能优化

1. **延迟加载**: 只在首次访问时加载包元数据
2. **缓存复用**: 同一包的多次访问复用缓存
3. **依赖跟踪**: 精确的缓存失效条件

### 11.2 内存管理

1. **软引用**: 大型包可考虑使用软引用存储
2. **项目隔离**: 每个项目独立的服务实例
3. **及时释放**: 项目关闭时释放所有资源

### 11.3 线程安全

1. **并发访问**: 使用 ConcurrentHashMap
2. **原子操作**: 加载时的竞态条件处理
3. **读写分离**: 读操作不阻塞

## 12. 参考资料

- [IntelliJ Platform SDK - Project-level Services](https://plugins.jetbrains.com/docs/intellij/plugin-services.html)
- [IntelliJ Platform SDK - Caching](https://plugins.jetbrains.com/docs/intellij/caching.html)
- [Flatbuffers 官方文档](https://google.github.io/flatbuffers/)
