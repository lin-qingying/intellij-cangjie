# 仓颉二进制 Stub 构建系统设计文档

## 1. 概述

本文档描述仓颉 IntelliJ 插件中二进制元数据文件（.cjo/.cjb）的反序列化和 Stub 构建系统的设计。该系统负责：

1. 读取和解析 Flatbuffers 格式的二进制元数据
2. 构建轻量级的 PSI Stub 索引
3. 支持跨包类型引用的解析
4. 为 IDE 提供代码导航、补全等功能

## 2. 架构概览

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           IDE 入口点                                      │
├─────────────────────────────────────────────────────────────────────────┤
│  CangJieBuiltInDecompiler (.cjb)  │  CangJieMetadataDecompiler (.cjo)   │
└───────────────┬───────────────────┴─────────────────┬───────────────────┘
                │                                     │
                ▼                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    CangJieMetadataStubBuilder                            │
│                         (Stub 构建入口)                                   │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       Flatbuffers 反序列化层                              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌────────────────────────┐  │
│  │  moduleParser   │→ │   FbPackage     │→ │   PackageWrapper       │  │
│  │  (ByteBuffer →) │  │   (数据模型)     │  │   (包装器)             │  │
│  └─────────────────┘  └─────────────────┘  └────────────────────────┘  │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         Stub 构建器组件                                   │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                   CjoStubBuilderContext                            │ │
│  │  - Project                                                         │ │
│  │  - PackageMetadataRegistry (跨包查找)                               │ │
│  │  - ClassDataFinder                                                 │ │
│  │  - DeclTable / TypeTable                                          │ │
│  │  - PackageWrapper                                                  │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                    │                                     │
│                                    ▼                                     │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐  │
│  │ ClassCjoStub    │  │ CallableCjoStub │  │   TypeCjoStubBuilder    │  │
│  │ Builder         │  │ Builder         │  │   (类型引用构建)         │  │
│  └────────┬────────┘  └────────┬────────┘  └───────────┬─────────────┘  │
│           │                    │                       │                 │
│           └────────────────────┼───────────────────────┘                 │
│                                │                                         │
│                                ▼                                         │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                       FullIdResolver                               │ │
│  │               (跨包类型引用解析)                                     │ │
│  │  - 当前包引用: 通过 index 查找 DeclTable                            │ │
│  │  - 跨包引用: 通过 PackageMetadataRegistry 查找                      │ │
│  └────────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         PSI Stub 输出                                    │
│  CangJieFileStubImpl → ClassStub / FunctionStub / VariableStub etc.    │
└─────────────────────────────────────────────────────────────────────────┘
```

## 3. 核心组件详解

### 3.1 反序列化入口

#### 3.1.1 CangJieMetadataDecompiler

抽象基类，负责将二进制元数据文件反编译为 PSI 结构。

```kotlin
abstract class CangJieMetadataDecompiler<out V : BinaryVersion>(
    private val fileType: FileType,
    private val serializerFlatbuffers: () -> SerializerExtensionFlatbuffers,
    private val expectedBinaryVersion: () -> V,
    private val invalidBinaryVersion: () -> V,
    stubVersion: Int
) : CjoFileDecompilers.Full()
```

**核心职责**：
- 文件类型识别 (`accepts`)
- 安全读取文件内容 (`readFileSafely`)
- 提供 Stub 构建器 (`stubBuilder`)
- 创建文件视图提供者 (`createFileViewProvider`)

#### 3.1.2 CangJieBuiltInDecompiler

内置库反编译器，处理 `.cjb` 文件：

```kotlin
internal class CangJieBuiltInDecompiler : CangJieMetadataDecompiler<BuiltInsBinaryVersion>(
    CangJieBuiltInFileType,
    { BuiltInSerializerFlatbuffers },
    { BuiltInsBinaryVersion.INSTANCE },
    { BuiltInsBinaryVersion.INVALID_VERSION },
    stubVersionForStubBuilderAndDecompiler
)
```

### 3.2 Flatbuffers 反序列化

#### 3.2.1 数据流

```
.cjo/.cjb 文件 (二进制)
    │
    ▼
ByteBuffer / InputStream
    │
    ▼ toFbPackage()
    │
FbPackage (Kotlin 数据类)
    │
    ▼ FbPackage.packageWrapper
    │
PackageWrapper (包装器，提供便捷访问)
```

#### 3.2.2 核心解析函数 (moduleParser.kt)

```kotlin
// 从 ByteBuffer 解析
fun ByteBuffer.toFbPackage(): FbPackage {
    val packageFb = Package.getRootAsPackage(this)
    return packageFb.parser()
}

// 从 InputStream 解析
fun InputStream.toFbPackage(): FbPackage {
    return ByteBuffer.wrap(this.readBytes()).toFbPackage()
}
```

#### 3.2.3 FbPackage 数据结构

```kotlin
data class FbPackage(
    val cjcVersion: String,           // 编译器版本
    val cjoVersion: BuiltInsBinaryVersion,  // 二进制版本
    val fullPkgName: String,          // 完整包名
    val pkgDepInfo: String,           // 包依赖信息
    val imports: List<String>,        // 导入的包列表
    val files: List<String>,          // 源文件列表
    val fileImports: List<FbImports>, // 文件级导入
    val types: List<FbSemaTy>,        // 类型表
    val decls: List<FbDecl>,          // 声明表
    val exprs: List<FbExpr>,          // 表达式表
    val values: List<FbCompositeValue>, // 复合值表
    val moduleName: String,           // 模块名称
    val kind: FbPackageKind,          // 包类型
    val access: FbAccessLevel         // 访问级别
)
```

#### 3.2.4 PackageWrapper 包装器

提供对 FbPackage 的便捷访问：

```kotlin
class PackageWrapper(val original: FbPackage) : DeclarationWrapper {
    val declTable = DeclTable(original.decls)      // 声明表
    val typeTable = TypeTable(original.types)      // 类型表
    val cjoVersion: BuiltInsBinaryVersion          // 版本
    val packageName: FqName                        // 包名
    val importPackageFqNames: List<FqName>         // 导入包列表

    // 预过滤的顶层声明
    val allClassDecls: List<ClassDeclWrapper>
    val functions: List<FunctionWrapper>
    val variables: List<VariableWrapper>
    val extends: List<ExtendWrapper>
    val typeAliass: List<TypeAliasWrapper>
}
```

### 3.3 FullId 解析机制

#### 3.3.1 FullId 结构

```kotlin
data class FbFullId(
    val pkgId: Int,      // 包索引
    val decl: String,    // 导出ID (exportId)
    val index: Int       // 声明索引 (1-based)
)
```

**pkgId 含义**：
- `-1` (INVALID_PACKAGE_INDEX): 无效引用
- `-2` (CURRENT_PKG_INDEX): 当前包
- `>=0`: 导入包的索引（在 `importPackageFqNames` 中的位置）

#### 3.3.2 解析策略

```kotlin
class FullIdResolver(
    private val project: Project,
    private val currentPackage: PackageWrapper,
    private val registry: PackageMetadataRegistry
) {
    fun resolveFullId(fullId: FbFullId): ResolveResult {
        return when (val pkgIndex = PackageIndex.fromValue(fullId.pkgId)) {
            PackageIndex.INVALID_PACKAGE_INDEX -> ResolveResult.Failed
            PackageIndex.CURRENT_PKG_INDEX -> resolveInCurrentPackage(fullId.index)
            null -> resolveInImportedPackage(fullId.pkgId, fullId.index, fullId.decl)
            else -> ResolveResult.Failed
        }
    }
}
```

**解析优先级**：
1. 优先使用 `index` 查找
2. 若 `index <= 0`，使用 `exportId` 查找

### 3.4 PackageMetadataRegistry 设计

#### 3.4.1 当前问题

当前实现存在以下问题：
1. 使用简单的 `ConcurrentHashMap` 缓存，未利用 IntelliJ 缓存系统
2. `getPackage` 方法的延迟加载未实现
3. 缓存未与 IDE 生命周期绑定

#### 3.4.2 重构设计

```kotlin
/**
 * 包元数据注册表 (重构版)
 *
 * 使用 IntelliJ 的缓存系统管理包元数据，支持：
 * 1. 按需加载 - 只在需要时解析 cjo 文件
 * 2. 自动失效 - 文件修改时自动清除缓存
 * 3. 项目级生命周期 - 随项目关闭自动释放
 */
@Service(Service.Level.PROJECT)
class PackageMetadataRegistry(private val project: Project) : Disposable {

    /**
     * 包元数据缓存
     * 使用 CachedValuesManager 实现自动失效
     */
    private val packageCache = ConcurrentHashMap<FqName, CachedValue<PackageWrapper?>>()

    /**
     * 包名到 cjo 文件的映射
     * 用于按需加载
     */
    private val packageFileMap = ConcurrentHashMap<FqName, VirtualFile>()

    /**
     * 获取包元数据
     *
     * 实现按需加载：
     * 1. 检查缓存
     * 2. 若未缓存，从 VirtualFile 加载
     * 3. 解析并缓存结果
     */
    fun getPackage(packageFqName: FqName): PackageWrapper? {
        // 检查是否有预加载的包
        val cachedValue = packageCache[packageFqName]
        if (cachedValue != null) {
            return cachedValue.value
        }

        // 尝试按需加载
        val virtualFile = packageFileMap[packageFqName] ?: return null
        return loadAndCachePackage(packageFqName, virtualFile)
    }

    /**
     * 从 VirtualFile 加载包并缓存
     */
    private fun loadAndCachePackage(
        packageFqName: FqName,
        virtualFile: VirtualFile
    ): PackageWrapper? {
        val cached = CachedValuesManager.getManager(project).createCachedValue {
            val wrapper = loadPackageFromFile(virtualFile)
            CachedValueProvider.Result.create(
                wrapper,
                virtualFile,  // 依赖文件变化
                ModificationTracker.NEVER_CHANGED  // 或使用更细粒度的跟踪
            )
        }
        packageCache[packageFqName] = cached
        return cached.value
    }

    /**
     * 从文件加载 PackageWrapper
     */
    private fun loadPackageFromFile(virtualFile: VirtualFile): PackageWrapper? {
        if (!virtualFile.isValid) return null

        return try {
            val bytes = virtualFile.contentsToByteArray(false)
            val stream = ByteArrayInputStream(bytes)
            stream.toFbPackage().packageWrapper
        } catch (e: Exception) {
            LOG.warn("Failed to load package from ${virtualFile.path}", e)
            null
        }
    }

    /**
     * 注册包文件位置
     */
    fun registerPackageFile(packageFqName: FqName, virtualFile: VirtualFile) {
        packageFileMap[packageFqName] = virtualFile
    }

    /**
     * 注册已加载的包
     */
    fun registerPackage(packageWrapper: PackageWrapper) {
        val packageFqName = packageWrapper.packageName
        val cached = CachedValuesManager.getManager(project).createCachedValue {
            CachedValueProvider.Result.create(
                packageWrapper,
                ModificationTracker.NEVER_CHANGED
            )
        }
        packageCache[packageFqName] = cached
    }

    override fun dispose() {
        packageCache.clear()
        packageFileMap.clear()
    }

    companion object {
        private val LOG = Logger.getInstance(PackageMetadataRegistry::class.java)

        fun getInstance(project: Project): PackageMetadataRegistry {
            return project.service<PackageMetadataRegistry>()
        }
    }
}
```

### 3.5 类型引用构建

#### 3.5.1 TypeCjoStubBuilder

负责创建类型引用的 Stub：

```kotlin
class TypeCjoStubBuilder(
    private val parentStub: StubElement<out PsiElement>,
    private val context: CjoStubBuilderContext
) {
    /**
     * 创建用户类型 Stub
     *
     * 对于跨包引用，创建完整限定名的嵌套 UserType 结构
     */
    private fun createUserTypeStub(parent: StubElement<out PsiElement>, typeWrapper: TypeWrapper) {
        when (val result = extractTypeInfo(typeWrapper, context)) {
            is FullIdResolver.ResolveResult.Success -> {
                if (result.needsQualification) {
                    // 跨包引用：创建 std.collection.ArrayList 形式
                    createQualifiedUserTypeStub(parent, result.packageFqName, result.name, typeWrapper)
                } else {
                    // 当前包：创建简单名称引用
                    createSimpleUserTypeStub(parent, result.name, typeWrapper)
                }
            }
            is FullIdResolver.ResolveResult.Failed -> {
                createAnyTypeStub(parent)  // 回退到 Any
            }
        }
    }
}
```

#### 3.5.2 限定名类型结构

对于 `std.collection.ArrayList<T>`，生成的 Stub 结构：

```
CjUserType (最外层)
├── CjUserType (qualifier: std.collection)
│   ├── CjUserType (qualifier: std)
│   │   └── CjNameReferenceExpression (std)
│   └── CjNameReferenceExpression (collection)
├── CjNameReferenceExpression (ArrayList)
└── CjTypeArgumentList
    └── CjTypeProjection
        └── CjTypeReference
            └── CjUserType
                └── CjNameReferenceExpression (T)
```

## 4. 数据流程

### 4.1 Stub 构建完整流程

```
1. IDE 触发 Stub 构建 (文件索引)
   │
   ▼
2. CjoFileDecompilers.find(virtualFile, CjoFileDecompilers.Full::class.java)
   │ 匹配 CangJieBuiltInDecompiler 或 CangJieMetadataDecompiler
   │
   ▼
3. CangJieMetadataStubBuilder.buildFileStub(fileContent)
   │
   ├─ 读取文件内容
   │  readFile(bytes, virtualFile) → FileWithMetadata.Compatible
   │
   ├─ 版本检查
   │  如果不兼容 → FileWithMetadata.Incompatible → 生成错误 Stub
   │
   └─ 构建 Stub
      │
      ▼
4. buildCompatibleFileStub(file, virtualFile, project)
   │
   ├─ 获取 PackageMetadataRegistry
   │  val registry = PackageMetadataRegistry.getInstance(project)
   │
   ├─ 注册当前包
   │  registry.registerPackage(packageWrapper)
   │
   ├─ 创建组件
   │  CjoStubBuilderContext(packageFqName, ...)
   │
   └─ 创建声明 Stubs
      │
      ├─ 顶层函数/变量: createPackageDeclarationsStubs()
      ├─ 类声明: ClassCjoStubBuilder.build()
      ├─ 扩展声明: ExtendCjoStubBuilder.build()
      └─ 类型别名: TypeAliasCjoStubBuilder.build()
      │
      ▼
5. 类型引用解析 (TypeCjoStubBuilder)
   │
   ├─ extractTypeInfo(typeWrapper, context)
   │  │
   │  └─ FullIdResolver.resolveFullId(declPtr)
   │     │
   │     ├─ 当前包: declTable[index] → ResolveResult.Success
   │     │
   │     └─ 跨包: registry.getPackage(fqName) → 查找或加载
   │
   └─ 创建类型 Stub
      ├─ 简单类型: CjUserType + CjNameReferenceExpression
      └─ 限定类型: 嵌套 CjUserType 结构
      │
      ▼
6. 输出 CangJieFileStubImpl
```

### 4.2 跨包引用解析流程

```
FullId { pkgId: 3, decl: "ArrayList", index: 42 }
   │
   ▼
FullIdResolver.resolveFullId()
   │
   ├─ pkgId = 3 → 导入包索引
   │
   └─ importPackageFqNames[3] → "std.collection"
      │
      ▼
resolveInImportedPackage("std.collection", 42, "ArrayList")
   │
   ├─ 优先尝试 index 查找
   │  registry.getPackage("std.collection")
   │  │
   │  ├─ 缓存命中 → packageWrapper.declTable[42]
   │  │
   │  └─ 缓存未命中 → loadPackageFromFile()
   │     │
   │     └─ packageFileMap["std.collection"] → VirtualFile
   │        │
   │        └─ toFbPackage() → PackageWrapper
   │
   └─ index 失败时使用 exportId
      findDeclByExportId("ArrayList")
      │
      ▼
ResolveResult.Success(
    fqName = "std.collection.ArrayList",
    name = "ArrayList",
    needsQualification = true,
    packageFqName = "std.collection"
)
```

## 5. 设计考量

### 5.1 性能优化

1. **延迟加载**：包元数据只在需要时才从文件加载
2. **缓存机制**：使用 `CachedValuesManager` 自动管理缓存生命周期
3. **预过滤**：`PackageWrapper` 预先过滤顶层声明，避免重复遍历

### 5.2 内存管理

1. **项目级作用域**：`PackageMetadataRegistry` 是项目级服务，随项目关闭释放
2. **弱引用**：可考虑对大型包使用软引用
3. **缓存失效**：文件修改时自动清除相关缓存

### 5.3 错误处理

1. **版本不兼容**：生成包含提示信息的特殊 Stub
2. **文件读取失败**：返回 null，跳过该文件
3. **解析错误**：记录日志，使用回退类型（Any）

### 5.4 扩展性

1. **多种文件类型**：通过继承 `CangJieMetadataDecompiler` 支持不同格式
2. **自定义序列化器**：通过 `SerializerExtensionFlatbuffers` 接口扩展
3. **版本演进**：`BinaryVersion` 机制支持向后兼容

## 6. 实现检查清单

- [x] 创建 `PackageMetadataRegistry` 基础实现
- [x] 创建 `FullIdResolver` 跨包解析
- [x] 更新 `CjoStubBuilderContext` 添加必要参数
- [x] 更新 `TypeCjoStubBuilder.extractTypeInfo` 返回完整解析结果
- [x] 实现 `createUserTypeStub` 支持限定名
- [x] 更新 `CangJieMetadataStubBuilder` 传递参数
- [ ] 重构 `PackageMetadataRegistry` 使用 IntelliJ 缓存系统
- [ ] 实现 `loadPackageFromFile` 方法
- [ ] 注册为项目级服务
- [ ] 添加单元测试
- [ ] 性能测试和优化

## 7. 相关文件

### 核心文件

| 文件 | 职责 |
|------|------|
| `CangJieMetadataDecompiler.kt` | 反编译器抽象基类 |
| `CangJieBuiltInDecompiler.kt` | 内置库反编译器 |
| `CangJieMetadataStubBuilder.kt` | Stub 构建入口 |
| `moduleParser.kt` | Flatbuffers 解析 |
| `PackageWrapper.kt` | 包装器定义 |
| `PackageMetadataRegistry.kt` | 包元数据注册表 |
| `FullIdResolver.kt` | FullId 解析器 |
| `TypeCjoStubBuilder.kt` | 类型 Stub 构建 |
| `CjoStubBuilderContext.kt` | 构建上下文组件 |

### 辅助文件

| 文件 | 职责 |
|------|------|
| `ClassCjoStubBuilder.kt` | 类声明 Stub 构建 |
| `CallableCjoStubBuilder.kt` | 函数/变量 Stub 构建 |
| `cjoStubBuilding.kt` | 通用 Stub 构建工具 |
| `CjDecompiledFile.kt` | 反编译文件 PSI |
| `CangJieDecompiledFileViewProvider.kt` | 文件视图提供者 |

## 8. 参考资料

- [Flatbuffers 官方文档](https://google.github.io/flatbuffers/)
- [IntelliJ Platform SDK - Stub Indexes](https://plugins.jetbrains.com/docs/intellij/stub-indexes.html)
- [IntelliJ Platform SDK - Caching](https://plugins.jetbrains.com/docs/intellij/caching.html)
