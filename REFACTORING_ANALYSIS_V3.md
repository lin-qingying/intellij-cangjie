# 仓颉语言模块系统重构分析报告 V3

## 更新说明

V3 版本基于以下新理解：
1. **LibraryModuleDescriptorImpl** 参考 Java/Kotlin 的 Library 机制（classes + sources 映射）
2. **LibraryModuleDescriptorImpl** 用于所有外部依赖（stdlib, binary, library 等），不只是 stdlib
3. **不需要** cjoFiles, sourceFiles, sdk 等参数直接传入构造函数
4. **BuiltInsLoader.createPackageFragmentProvider** 用于普通二进制库引入，不是为了兼容性
5. **不需要** LibraryLoadMode 枚举

---

## 1. 当前实现分析

### 1.1 ModuleDescriptor 当前结构

```kotlin
interface ModuleDescriptor : DeclarationDescriptor {
    val projectDescriptor: ProjectDescriptor
    val isValid: Boolean
    fun getPackage(fqName: FqName): PackageViewDescriptor
    val builtIns: CangJieBuiltIns
    // ... 其他方法
}

class ModuleDescriptorImpl(
    override val projectDescriptor: ProjectDescriptor,
    moduleName: Name,  // ← 问题：这个name既是模块名称也是显示名称
    private val storageManager: StorageManager,
    //...
) : DeclarationDescriptorImpl(Annotations.EMPTY, moduleName), ModuleDescriptor
```

**当前问题**:
- `moduleName` 继承自 `DeclarationDescriptorImpl` 的 `name` 属性
- 这个 `name` 既用作模块的**唯一标识**，又用作**显示名称**
- 对于内置模块，使用特殊名称 `Name.special("<built-ins module>")`
- 无法区分模块名称和描述性显示名称

### 1.2 CangJieBuiltIns 当前结构

```kotlin
open class CangJieBuiltIns(
    val projectDescriptor: ProjectDescriptor,
    val storageManager: StorageManager,
) {
    // 单一的内置模块，包含所有内容
    private val builtInsModuleProvider: NotNullLazyValue<ModuleDescriptorImpl> =
        storageManager.createLazyValue {
            val sdk = CjProjectSdkConfig.getInstance(projectDescriptor.project).getProjectSdk()
            createBuiltInsModuleInternal(isFallback = sdk == null)
        }

    val builtInsModule: ModuleDescriptorImpl
        get() = builtInsModuleProvider()

    // 从 builtInsModule 的不同包获取类型
    val STD_SYNC_SCOPE get() = builtInsModule.getPackage(sync).memberScope
    val STD_CORE_SCOPE get() = builtInsModule.getPackage(core).memberScope
    val STD_AST_SCOPE get() = builtInsModule.getPackage(ast).memberScope
    val BASIC_SCOPE get() = builtInsModule.getPackage(BASIC_PACKAGE_FQ_NAME).memberScope
}
```

**当前的包结构**:
```
builtInsModule (Name.special("<built-ins module>"))
├── cangjie (BASIC_PACKAGE_FQ_NAME)
│   ├── Int8, Int16, Int32, Int64, IntNative
│   ├── UInt8, UInt16, UInt32, UInt64, UIntNative
│   ├── Float16, Float32, Float64
│   ├── Bool, Rune, Unit, Nothing
│   └── (基本类型)
├── std.core (core = FqName("std.core"))
│   ├── Any, Object
│   ├── Array
│   ├── CType, CPointer, CFunc
│   ├── Throwable
│   ├── Range, Resource
│   ├── Comparable, Equatable, Countable
│   └── (核心标准库类型)
├── std.sync (sync = FqName("std.sync"))
│   ├── ReentrantMutex
│   ├── Future
│   └── (同步相关类型)
└── std.ast (ast = FqName("std.ast"))
    ├── Tokens
    └── (AST相关类型)
```

### 1.3 BuiltInsLoader 当前结构

```kotlin
interface BuiltInsLoader {
    fun createPackageFragmentProvider(
        storageManager: StorageManager,
        builtInsModule: ModuleDescriptor,
        isFallback: Boolean,
        sdk: CjSdk?
    ): PackageFragmentProvider
}

class BuiltInsLoaderImpl : BuiltInsLoader {
    override fun createPackageFragmentProvider(...): PackageFragmentProvider {
        // 1. 基本类型提供者
        val basicTypesProvider = BasicTypesPackageFragmentProvider(storageManager, builtInsModule)

        // 2. 从 .cjo 文件反序列化其他包（std.core, std.sync, std.ast）
        val builtProvider = createBuiltInPackageFragmentProvider(
            storageManager, builtInsModule, StandardNames.ALL_NAMES, isFallback,
            resourceLoader::loadResource, sdk
        )

        // 3. 组合
        return CompositePackageFragmentProvider(listOf(basicTypesProvider, builtProvider))
    }
}
```

### 1.4 CjDependency 依赖类型

```kotlin
sealed class CjDependency {
    // 1. 标准库依赖
    data class Stdlib(
        override val name: String,
        override val versionReq: VersionRequirement,
        override val scope: CjDependencyScope = CjDependencyScope.PROVIDED
    ) : CjDependency()

    // 2. 二进制依赖（.cjo + 可选的库文件）
    data class Binary(
        override val name: String,
        val cjoPath: java.nio.file.Path,
        val libPath: java.nio.file.Path? = null,
        override val target: String
    ) : CjDependency()

    // 3. 本地路径依赖（源文件）
    data class Path(
        override val name: String,
        val path: java.nio.file.Path
    ) : CjDependency()

    // 4. 外部库依赖（从远程下载，可能包含 classes + sources）
    data class Library(
        override val name: String,
        override val group: String? = null,
        override val versionReq: VersionRequirement,
        val registry: String? = null
    ) : CjDependency()
}
```

---

## 2. 重构目标

### 2.1 目标模块和包结构

根据仓颉语言的导入语法 `import std.core`，应该实现以下结构：

```
builtInsModule (由 CangJieBuiltIns 创建和管理)
├── 模块名称: Name.special("<built-ins>")
├── 描述名称: "<built-ins>"
└── 包含内容: 仅基本类型
    └── cangjie (BASIC_PACKAGE_FQ_NAME)
        ├── Int8, Int16, Int32, Int64, IntNative
        ├── UInt8, UInt16, UInt32, UInt64, UIntNative
        ├── Float16, Float32, Float64
        └── Bool, Rune, Unit, Nothing

stdlibModule (LibraryModuleDescriptorImpl，外部创建，CangJieBuiltIns 只持有引用)
├── 模块名称: Name.identifier("std")
├── 描述名称: "std"
└── 包含内容: 标准库类型 (通过 Library 机制加载)
    ├── std.core (核心包 - PackageViewDescriptor)
    │   ├── Any, Object
    │   ├── Array
    │   ├── CType, CPointer, CFunc
    │   ├── Throwable
    │   ├── Range, Resource
    │   └── Comparable, Equatable, Countable
    ├── std.sync (同步包 - PackageViewDescriptor)
    │   ├── ReentrantMutex
    │   └── Future
    └── std.ast (AST包 - PackageViewDescriptor)
        └── Tokens

otherLibraryModules (LibraryModuleDescriptorImpl，用于所有外部依赖)
├── 对应 CjDependency.Binary
├── 对应 CjDependency.Library
└── 对应 CjDependency.Path (如果解析为库依赖)
```

**语义映射**:
- `import std.core` → `std` 是模块（ModuleDescriptor），`core` 是包（PackageViewDescriptor）
- 用户看到的导入语句：`import std.core`
- 底层解析：`stdlibModule.getPackage(FqName("std.core"))` 或 `stdlibModule.getPackage(FqName("core"))`

### 2.2 LibraryModuleDescriptorImpl 设计

**设计原则**：参考 Java/Kotlin 的 Library 机制，自动根据上下文类型选择加载方式

在 IntelliJ 中，Library 的概念是：
- **Classes**: 编译后的二进制文件（.jar, .class）
- **Sources**: 对应的源代码文件（用于导航、查看实现）
- **Library** 本身不直接持有文件路径，而是通过 IntelliJ 的 Library 系统管理

`LibraryModuleDescriptorImpl` 的加载策略：
1. **判断模块类型**：根据 AnalysisContext 的 `isSourceContext` 属性
2. **二进制模块** (isSourceContext = false)：
   - 优先使用 `BuiltInsLoader` 加载 .cjo 文件
   - 如果加载失败，回退到 `DelegatingPackageFragmentProvider`（源码方式）
3. **源码模块** (isSourceContext = true)：
   - 直接使用 `DelegatingPackageFragmentProvider`

```kotlin
/**
 * 库模块描述符实现
 *
 * 用于表示所有外部依赖库（stdlib, binary libraries, external libraries 等）
 * 参考 Java/Kotlin 的 Library 机制：
 * - classes (编译后的 .cjo 文件) - 用于类型解析
 * - sources (源文件) - 用于代码导航和查看实现
 *
 * ## 使用场景
 *
 * 1. **标准库 (CjDependency.Stdlib)**:
 *    - 从 SDK 加载 .cjo 文件（二进制模式）
 *
 * 2. **二进制依赖 (CjDependency.Binary)**:
 *    - 从指定的 .cjo 文件加载（二进制模式）
 *
 * 3. **外部库 (CjDependency.Library)**:
 *    - 优先从 .cjo 文件加载（如果存在）
 *    - 回退到源码加载（如果 .cjo 不存在）
 *
 * 4. **路径依赖 (CjDependency.Path)**:
 *    - 通常作为源码模块处理
 *
 * ## 自动加载策略
 *
 * 模块根据 AnalysisContext.isSourceContext 自动选择加载方式：
 * - **isSourceContext = false（二进制）**:
 *   1. 尝试使用 BuiltInsLoader 加载 .cjo 文件
 *   2. 失败则回退到 DelegatingPackageFragmentProvider
 * - **isSourceContext = true（源码）**:
 *   直接使用 DelegatingPackageFragmentProvider
 *
 * @see CjDependency
 * @see BuiltInsLoader
 * @see DelegatingPackageFragmentProvider
 */
class LibraryModuleDescriptorImpl(
    projectDescriptor: ProjectDescriptor,
    moduleName: Name,
    displayName: String,
    storageManager: StorageManager,
    capabilities: Map<ModuleCapability<*>, Any?> = emptyMap(),
    stableName: Name? = null
) : ModuleDescriptorImpl(
    projectDescriptor,
    moduleName,
    displayName,
    storageManager,
    capabilities,
    stableName
) {
    /**
     * 智能初始化方法
     *
     * 根据模块的上下文类型自动选择加载方式：
     * 1. 如果是二进制模块（isSourceContext = false），尝试用 BuiltInsLoader 加载
     * 2. 如果 BuiltInsLoader 加载失败，或者是源码模块，使用 DelegatingPackageFragmentProvider
     *
     * @param resolver 项目解析器，用于创建 DelegatingPackageFragmentProvider
     * @param content 模块内容
     * @param packageOracle 包预言，用于优化包查找
     */
    fun <M : AnalysisContext> initializeWithSmartLoading(
        resolver: AbstractResolverForProject<M>,
        content: ModuleContent<M>,
        packageOracle: PackageOracle
    ) {
        val context = getCapability(AnalysisContextCapability) as? M
        val isSourceContext = context?.isSourceContext ?: true

        val provider = if (!isSourceContext) {
            // 二进制模块：尝试用 BuiltInsLoader 加载
            try {
                BuiltInsLoader.Instance.createPackageFragmentProvider(
                    storageManager,
                    this,
                    emptyList() // cjoFiles will be resolved internally
                )
            } catch (e: Exception) {
                // 加载失败，回退到源码方式
                DelegatingPackageFragmentProvider(
                    resolver, this, content, packageOracle
                )
            }
        } else {
            // 源码模块：直接使用 DelegatingPackageFragmentProvider
            DelegatingPackageFragmentProvider(
                resolver, this, content, packageOracle
            )
        }

        initialize(provider)
    }
}
```

**关键设计点**：
1. **自动判断加载方式**：根据 `isSourceContext` 属性自动选择
2. **优雅降级**：二进制加载失败时自动回退到源码方式
3. **统一接口**：所有库模块使用相同的初始化方法
4. **灵活性**：保留了 `initialize()` 方法，允许外部直接传入 Provider

---

## 3. BuiltInsLoader 重构

### 3.1 重构后的接口

```kotlin
interface BuiltInsLoader {
    /**
     * 创建基本类型的包片段提供者
     *
     * 仅包含 cangjie 包下的基本类型（Int8, Bool, Unit 等）
     * 这些类型是硬编码的，不从文件加载
     *
     * @param storageManager 存储管理器
     * @param builtInsModule 内置模块描述符
     * @return 基本类型的 PackageFragmentProvider
     */
    fun createBasicTypesPackageFragmentProvider(
        storageManager: StorageManager,
        builtInsModule: ModuleDescriptor
    ): PackageFragmentProvider

    /**
     * 创建标准库的包片段提供者
     *
     * 包含 std.core, std.sync, std.ast 等包
     * 从 SDK 的 .cjo 文件反序列化获取
     *
     * @param storageManager 存储管理器
     * @param stdlibModule 标准库模块描述符
     * @param isFallback 是否为回退模式（SDK 不可用时）
     * @param sdk 仓颉 SDK
     * @return 标准库的 PackageFragmentProvider
     */
    fun createStdLibPackageFragmentProvider(
        storageManager: StorageManager,
        stdlibModule: ModuleDescriptor,
        isFallback: Boolean,
        sdk: CjSdk?
    ): PackageFragmentProvider

    /**
     * 创建普通二进制库的包片段提供者
     *
     * 用于加载其他二进制库（非 stdlib）
     * 从指定的 .cjo 文件反序列化获取
     *
     * **注意**：此方法暂不实现，预留接口
     *
     * @param storageManager 存储管理器
     * @param libraryModule 库模块描述符
     * @param cjoFiles .cjo 文件列表
     * @return 二进制库的 PackageFragmentProvider
     */
    fun createPackageFragmentProvider(
        storageManager: StorageManager,
        libraryModule: ModuleDescriptor,
        cjoFiles: List<VirtualFile>
    ): PackageFragmentProvider

    companion object {
        val Instance: BuiltInsLoader by lazy(LazyThreadSafetyMode.PUBLICATION) {
            val implementations = ServiceLoader.load(
                BuiltInsLoader::class.java,
                BuiltInsLoader::class.java.classLoader
            )
            implementations.firstOrNull()
                ?: throw IllegalStateException("No BuiltInsLoader implementation found")
        }
    }
}
```

### 3.2 BuiltInsLoaderImpl 实现

```kotlin
class BuiltInsLoaderImpl : BuiltInsLoader {

    override fun createBasicTypesPackageFragmentProvider(
        storageManager: StorageManager,
        builtInsModule: ModuleDescriptor
    ): PackageFragmentProvider {
        // 仅返回基本类型提供者
        return BasicTypesPackageFragmentProvider(storageManager, builtInsModule)
    }

    override fun createStdLibPackageFragmentProvider(
        storageManager: StorageManager,
        stdlibModule: ModuleDescriptor,
        isFallback: Boolean,
        sdk: CjSdk?
    ): PackageFragmentProvider {
        // 从 SDK 加载标准库的 .cjo 文件
        val stdPackages = listOf(
            FqName("std.core"),
            FqName("std.sync"),
            FqName("std.ast"),
            // ... 其他标准库包
        )

        return createBuiltInPackageFragmentProvider(
            storageManager,
            stdlibModule,
            stdPackages,
            isFallback,
            resourceLoader::loadResource,
            sdk
        )
    }

    override fun createPackageFragmentProvider(
        storageManager: StorageManager,
        libraryModule: ModuleDescriptor,
        cjoFiles: List<VirtualFile>
    ): PackageFragmentProvider {
        // 暂不实现，预留接口
        TODO("Not yet implemented - reserved for future binary library loading")
    }

    private fun createBuiltInPackageFragmentProvider(
        storageManager: StorageManager,
        module: ModuleDescriptor,
        packageFqNames: List<FqName>,
        isFallback: Boolean,
        loadResource: (String) -> InputStream?,
        sdk: CjSdk?
    ): PackageFragmentProvider {
        val packageFragments = packageFqNames.mapNotNull { fqName ->
            val resourcePath = BuiltInSerializerFlatbuffers.getBuiltInsFilePath(fqName, sdk)
            if (resourcePath != null) {
                val inputStream = loadResource(resourcePath)
                BuiltInsPackageFragmentImpl.create(
                    fqName,
                    storageManager,
                    module,
                    inputStream,
                    isFallback
                )
            } else null
        }
        return PackageFragmentProviderImpl(packageFragments)
    }
}
```

---

## 4. CangJieBuiltIns 重构

### 4.1 重构后的结构

```kotlin
open class CangJieBuiltIns(
    val projectDescriptor: ProjectDescriptor,
    val storageManager: StorageManager,
) {
    // ========== 1. 内置模块（仅基本类型）==========

    private val builtInsModuleProvider: NotNullLazyValue<ModuleDescriptorImpl> =
        storageManager.createLazyValue {
            createBuiltInsModuleInternal()
        }

    val builtInsModule: ModuleDescriptorImpl
        get() = builtInsModuleProvider()

    private fun createBuiltInsModuleInternal(): ModuleDescriptorImpl {
        val module = ModuleDescriptorImpl(
            projectDescriptor,
            Name.special("<built-ins>"),  // 模块名称
            "<built-ins>",                 // 显示名称
            storageManager
        )
        // 仅初始化基本类型的 PackageFragmentProvider
        module.initialize(
            BuiltInsLoader.Instance.createBasicTypesPackageFragmentProvider(
                storageManager,
                module
            )
        )
        module.setDependencies(module)  // 自依赖
        return module
    }

    val BASIC_SCOPE get() = builtInsModule.getPackage(BASIC_PACKAGE_FQ_NAME).memberScope

    // 基本类型访问器
    val int8Type get() = getBasicClassByName(StandardNames.INT8).defaultType
    val boolType get() = getBasicClassByName(StandardNames.BOOL).defaultType
    // ... 其他基本类型

    private fun getBasicClassByName(simpleName: Name): ClassDescriptor {
        val classifier = BASIC_SCOPE.getContributedClassifier(simpleName, NoLookupLocation.FROM_BASIC)
        require(classifier is ClassDescriptor) {
            "Basic class ${BASIC_PACKAGE_FQ_NAME.child(simpleName)} not found or not a class"
        }
        return classifier
    }

    // ========== 2. 标准库模块引用（不由 CangJieBuiltIns 创建）==========

    /**
     * 标准库模块引用
     *
     * 注意：此模块不由 CangJieBuiltIns 创建，而是由外部创建并设置。
     * 通常在项目初始化时，根据 SDK 和依赖配置创建。
     */
    private var _stdlibModule: LibraryModuleDescriptorImpl? = null

    val stdlibModule: LibraryModuleDescriptorImpl
        get() = _stdlibModule
            ?: error("Stdlib module has not been set. Call setStdlibModule() first.")

    /**
     * 设置标准库模块
     *
     * 应该在项目初始化时调用，传入已配置好的标准库模块描述符。
     *
     * @param module 标准库模块描述符
     */
    fun setStdlibModule(module: LibraryModuleDescriptorImpl) {
        require(_stdlibModule == null) { "Stdlib module has already been set" }
        _stdlibModule = module
    }

    // 从 stdlibModule 获取标准库包的作用域
    val STD_CORE_SCOPE get() = stdlibModule.getPackage(FqName("std.core")).memberScope
    val STD_SYNC_SCOPE get() = stdlibModule.getPackage(FqName("std.sync")).memberScope
    val STD_AST_SCOPE get() = stdlibModule.getPackage(FqName("std.ast")).memberScope

    // 标准库类型访问器
    val any: ClassDescriptor get() = getStdCoreClassByName("Any")
    val array: ClassDescriptor get() = getStdCoreClassByName("Array")
    val reentrantMutex: ClassDescriptor get() = getStdSyncClassByName("ReentrantMutex")
    // ... 其他标准库类型

    private fun getStdCoreClassByName(simpleName: String): ClassDescriptor {
        val classifier = STD_CORE_SCOPE.getContributedClassifier(
            Name.identifier(simpleName),
            NoLookupLocation.FROM_BUILTINS
        )
        require(classifier is ClassDescriptor) {
            "Stdlib class std.core.$simpleName not found or not a class"
        }
        return classifier
    }

    private fun getStdSyncClassByName(simpleName: String): ClassDescriptor {
        val classifier = STD_SYNC_SCOPE.getContributedClassifier(
            Name.identifier(simpleName),
            NoLookupLocation.FROM_BUILTINS
        )
        require(classifier is ClassDescriptor) {
            "Stdlib class std.sync.$simpleName not found or not a class"
        }
        return classifier
    }
}
```

---

## 5. 模块初始化逻辑

### 5.1 标准库模块的创建（在项目初始化时）

```kotlin
// 在 AbstractResolverForProject 或类似的地方
private fun createStdlibModuleDescriptor(
    dependency: CjDependency.Stdlib,
    sdk: CjSdk
): LibraryModuleDescriptorImpl {
    val stdlibModule = LibraryModuleDescriptorImpl(
        projectDescriptor,
        Name.identifier("std"),
        "std",
        storageManager
    )

    // 初始化标准库模块
    val provider = BuiltInsLoader.Instance.createStdLibPackageFragmentProvider(
        storageManager,
        stdlibModule,
        isFallback = sdk == null,
        sdk = sdk
    )
    stdlibModule.initialize(provider)

    // stdlib 模块依赖 builtInsModule（需要访问基本类型）
    stdlibModule.setDependencies(stdlibModule, projectDescriptor.builtIns.builtInsModule)

    // 设置到 BuiltIns
    projectDescriptor.builtIns.setStdlibModule(stdlibModule)

    return stdlibModule
}
```

### 5.2 处理其他库依赖

```kotlin
// 处理 CjDependency.Binary（只有 cjo）
private fun createBinaryLibraryModule(
    dependency: CjDependency.Binary
): LibraryModuleDescriptorImpl {
    val module = LibraryModuleDescriptorImpl(
        projectDescriptor,
        Name.identifier(dependency.name),
        dependency.name,
        storageManager
    )

    // 从 .cjo 文件创建 provider
    // 这里可以使用 BuiltInsLoader.createPackageFragmentProvider (未来实现)
    // 或者直接创建专门的 CjoLibraryPackageFragmentProvider
    val cjoFile = VfsUtil.findFile(dependency.cjoPath, true)
    val provider = createCjoLibraryProvider(listOf(cjoFile!!), module)

    module.initialize(provider)

    // 设置依赖：自身 + builtIns + stdlib
    module.setDependencies(module, builtInsModule, stdlibModule)

    return module
}

// 处理 CjDependency.Library（可能有 cjo 和 source）
private fun createExternalLibraryModule(
    dependency: CjDependency.Library,
    resolvedPath: Path  // 解析后的库路径
): LibraryModuleDescriptorImpl {
    val module = LibraryModuleDescriptorImpl(
        projectDescriptor,
        Name.identifier(dependency.name),
        dependency.name,
        storageManager
    )

    // 查找 classes 和 sources
    val classesRoot = findClassesRoot(resolvedPath)  // 查找 .cjo 文件目录
    val sourcesRoot = findSourcesRoot(resolvedPath)  // 查找源码目录

    val provider = when {
        classesRoot != null && sourcesRoot != null -> {
            // 混合模式：优先使用 classes，sources 用于导航
            val cjoProvider = createCjoLibraryProvider(classesRoot.children.toList(), module)
            val sourceProvider = createSourceLibraryProvider(sourcesRoot.children.toList(), module)
            MixedLibraryPackageFragmentProvider(cjoProvider, sourceProvider)
        }
        classesRoot != null -> {
            // 只有 classes
            createCjoLibraryProvider(classesRoot.children.toList(), module)
        }
        sourcesRoot != null -> {
            // 只有 sources
            createSourceLibraryProvider(sourcesRoot.children.toList(), module)
        }
        else -> error("Library ${dependency.name} has no classes or sources")
    }

    module.initialize(provider)
    module.setDependencies(module, builtInsModule, stdlibModule)

    return module
}

// 辅助方法：创建 cjo library provider
private fun createCjoLibraryProvider(
    cjoFiles: List<VirtualFile>,
    module: ModuleDescriptor
): PackageFragmentProvider {
    // 解析 .cjo 文件并创建 PackageFragments
    // 可以重用 BuiltInsPackageFragmentImpl 的反序列化逻辑
    TODO("Implement cjo library provider")
}

// 辅助方法：创建 source library provider
private fun createSourceLibraryProvider(
    sourceFiles: List<VirtualFile>,
    module: ModuleDescriptor
): PackageFragmentProvider {
    // 解析源文件并创建 PackageFragments
    // 可能需要类似 SourceModuleDescriptor 的逻辑
    TODO("Implement source library provider")
}
```

---

## 6. ModuleDescriptor displayName 重构

### 6.1 接口修改

```kotlin
interface ModuleDescriptor : DeclarationDescriptor {
    val projectDescriptor: ProjectDescriptor

    /**
     * 模块名称：用于内部唯一标识
     * 继承自 DeclarationDescriptor.name
     * 例如: Name.special("<built-ins>"), Name.identifier("std")
     */
    // name 属性已从 DeclarationDescriptor 继承

    /**
     * 描述名称：用于显示和文档
     * 例如: "<built-ins>", "std", "myModule"
     */
    val displayName: String

    // ... 其他方法保持不变
}
```

### 6.2 实现修改

```kotlin
class ModuleDescriptorImpl(
    override val projectDescriptor: ProjectDescriptor,
    moduleName: Name,           // 模块的唯一标识名称
    displayName: String? = null, // 模块的显示名称（可选，默认使用 moduleName.asString()）
    private val storageManager: StorageManager,
    private val capabilities: Map<ModuleCapability<*>, Any?> = emptyMap(),
    override val stableName: Name? = null,
) : DeclarationDescriptorImpl(Annotations.EMPTY, moduleName), ModuleDescriptor {

    override val displayName: String = displayName ?: moduleName.asString()

    // ... 其他实现保持不变
}
```

---

## 7. 实施步骤

### 阶段 1: 添加 displayName（向后兼容）

1. ✅ 在 `ModuleDescriptor` 接口添加 `displayName` 属性
2. ✅ 在 `ModuleDescriptorImpl` 添加 `displayName` 参数（带默认值）
3. ✅ 更新所有创建模块的位置，显式传入 `displayName`

### 阶段 2: 扩展 BuiltInsLoader 接口

1. ✅ 添加 `createBasicTypesPackageFragmentProvider` 方法
2. ✅ 添加 `createStdLibPackageFragmentProvider` 方法
3. ✅ 添加 `createPackageFragmentProvider(libraryModule, cjoFiles)` 方法（暂不实现）
4. ✅ 在 `BuiltInsLoaderImpl` 实现新方法
5. ✅ 移除原来的 `createPackageFragmentProvider(builtInsModule)` 方法（或标记为废弃）

### 阶段 3: 创建 LibraryModuleDescriptorImpl

1. ✅ 创建新类 `LibraryModuleDescriptorImpl`
2. ✅ 继承自 `ModuleDescriptorImpl`，不添加额外字段
3. ✅ 作为标记类，表示这是一个库模块

### 阶段 4: 重构 CangJieBuiltIns

1. ✅ 保留 `builtInsModule`（只包含基本类型）
2. ✅ 添加 `_stdlibModule` 引用和 `setStdlibModule()` 方法
3. ✅ 更新 `STD_CORE_SCOPE`, `STD_SYNC_SCOPE`, `STD_AST_SCOPE` 使用 `stdlibModule`
4. ✅ 更新所有 `myStdXxxBuiltInClassesByName` 使用新的 SCOPE

### 阶段 5: 更新项目初始化逻辑

1. ✅ 在 `AbstractResolverForProject` 或相关位置添加 stdlib 模块创建逻辑
2. ✅ 根据 `CjDependency` 类型创建对应的 `LibraryModuleDescriptorImpl`：
   - `CjDependency.Stdlib` → 创建 stdlib 模块
   - `CjDependency.Binary` → 创建二进制库模块
   - `CjDependency.Library` → 创建外部库模块
   - `CjDependency.Path` → 根据情况处理
3. ✅ 调用 `builtIns.setStdlibModule()` 设置 stdlib 引用

### 阶段 6: 测试和验证

1. ✅ 单元测试：验证 `builtInsModule` 只包含基本类型
2. ✅ 单元测试：验证 `stdlibModule` 包含标准库类型
3. ✅ 集成测试：验证 `import std.core` 正确解析
4. ✅ 集成测试：验证类型查找（如 `std.core.Any`）正确工作
5. ✅ 集成测试：验证模块依赖关系正确

---

## 8. 风险评估

### 8.1 高风险区域

1. **模块依赖解析**
   - **风险**: stdlib 模块的依赖配置不正确，可能导致无法查找基本类型
   - **缓解**: 确保 `stdlibModule.setDependencies(self, builtInsModule)` 正确配置

2. **初始化顺序**
   - **风险**: stdlib 模块在 `builtIns.setStdlibModule()` 之前被访问
   - **缓解**: 在项目初始化早期设置 stdlib 模块，添加断言检查

3. **包片段提供者**
   - **风险**: 如果包片段提供者没有正确拆分，可能导致类型重复或缺失
   - **缓解**: 仔细审查每个提供者的包范围，确保不重叠且完整覆盖

### 8.2 中风险区域

1. **Library 机制集成**
   - **风险**: 如何正确集成 IntelliJ 的 Library 系统
   - **缓解**: 参考 Kotlin 插件的实现，使用 VirtualFile 而非直接的 Path

2. **现有代码兼容性**
   - **风险**: 大量现有代码可能依赖 `builtInsModule` 包含所有类型
   - **缓解**: 提供 `displayName` 默认值，逐步迁移

---

## 9. 总结

### 9.1 核心变更

1. **ModuleDescriptor**:
   - 增加 `displayName: String` 属性用于显示
   - 保持 `name: Name` 用于内部唯一标识

2. **CangJieBuiltIns**:
   - `builtInsModule` 仅包含基本类型（cangjie 包）
   - 新增 `stdlibModule` 引用（LibraryModuleDescriptorImpl，外部创建）
   - 所有 `myStdXxxBuiltInClassesByName` 从 `stdlibModule` 获取

3. **BuiltInsLoader**:
   - 新增 `createBasicTypesPackageFragmentProvider`
   - 新增 `createStdLibPackageFragmentProvider`
   - 新增 `createPackageFragmentProvider(libraryModule, cjoFiles)` (暂不实现)
   - 移除或废弃原 `createPackageFragmentProvider(builtInsModule)`

4. **LibraryModuleDescriptorImpl**:
   - 新类，用于所有外部依赖
   - 参考 Java/Kotlin Library 机制（classes + sources）
   - 简单的标记类，不持有文件路径
   - 内容通过 PackageFragmentProvider 管理

### 9.2 预期收益

1. **语义清晰**: `import std.core` 明确表示 `std` 是模块，`core` 是包
2. **职责分离**: 基本类型和标准库类型分属不同模块
3. **灵活性**: 支持多种库加载方式（cjo, 源文件, 混合）
4. **可扩展性**: 未来可以轻松添加更多模块
5. **符合语言设计**: 与仓颉语言的模块系统设计一致
6. **参考成熟实践**: 借鉴 Java/Kotlin 的 Library 机制

### 9.3 架构优势

1. **解耦**: CangJieBuiltIns 不再负责创建 stdlib 模块
2. **统一**: 所有库依赖使用相同的 LibraryModuleDescriptorImpl
3. **灵活**: 支持源码调试和二进制分发两种场景
4. **简化**: LibraryModuleDescriptorImpl 不持有文件路径，委托给 PackageFragmentProvider
5. **可扩展**: 易于添加新的 PackageFragmentProvider 实现
