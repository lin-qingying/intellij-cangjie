# 架构重构方案：CangJieProject、ModuleDescriptor 与 CangJieBuiltIns

## 当前设计的问题

### 1. 循环依赖问题

当前代码存在以下循环依赖：

```
ModuleDescriptor
  ├─> builtIns: CangJieBuiltIns
  └─> cangJieProject: CangJieProject

CangJieBuiltIns
  └─> builtInsModule: ModuleDescriptorImpl (内部创建)
```

### 2. SDK 依赖不明确

- `CangJieBuiltIns` 需要从 SDK 加载标准库（见 `BuiltInsLoaderImpl.kt:84`）
- 但 `CangJieBuiltIns` 没有明确持有 SDK 引用
- `BuiltInSerializerFlatbuffers` 需要通过 `CjProjectSdkConfig` 获取 SDK

### 3. 层次结构混乱

- `ModuleDescriptor.containingDeclaration` 应该返回 `CangJieProject`
- 但 `CangJieProject` 不是 `DeclarationDescriptor`
- `ModuleDescriptor` 既是项目的子对象，又是声明描述符

## 正确的架构设计

### 层次结构

```
IntelliJ Project
  │
  ├─> CjProjectSdkConfig (项目级SDK配置)
  │     └─> CjSdk (SDK实例)
  │
  └─> CangJieProject (仓颉项目描述符 - 项目级服务)
        │
        ├─> project: Project (关联的IntelliJ项目)
        ├─> sdk: CjSdk (使用的SDK)
        ├─> builtIns: CangJieBuiltIns (全局唯一，基于SDK创建)
        │     └─> builtInsModule: ModuleDescriptorImpl (内部模块)
        │
        └─> modules: List<ModuleDescriptor> (用户模块列表)
              └─> 每个模块通过 cangJieProject.builtIns 访问内置类型
```

### 关键设计点

#### 1. CangJieProject 应该是项目级服务

```kotlin
@Service(Service.Level.PROJECT)
class CangJieProjectService(private val project: Project) {

    private val cangJieProject: CangJieProject by lazy {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()
        CangJieProjectImpl(
            projectName = Name.identifier(project.name),
            project = project,
            sdk = sdk,
            storageManager = LockBasedStorageManager(project, "CangJieProject")
        )
    }

    fun getProject(): CangJieProject = cangJieProject

    companion object {
        fun getInstance(project: Project): CangJieProjectService {
            return project.getService(CangJieProjectService::class.java)
        }
    }
}
```

#### 2. CangJieProject 管理 BuiltIns

```kotlin
interface CangJieProject {
    val name: Name
    val project: Project
    val sdk: CjSdk?

    /**
     * 项目的 BuiltIns，基于 SDK 创建
     * 整个项目共享同一个 BuiltIns 实例
     */
    val builtIns: CangJieBuiltIns

    /**
     * 用户定义的模块列表
     */
    val modules: List<ModuleDescriptor>

    fun getModule(moduleName: Name): ModuleDescriptor?
}

class CangJieProjectImpl(
    override val name: Name,
    override val project: Project,
    override val sdk: CjSdk?,
    private val storageManager: StorageManager
) : CangJieProject {

    /**
     * 全局唯一的 BuiltIns 实例，基于 SDK 创建
     */
    override val builtIns: CangJieBuiltIns by lazy {
        CangJieBuiltIns(storageManager, sdk).apply {
            // 初始化 builtInsModule
            createBuiltInsModule(isFallback = sdk == null)
        }
    }

    private val moduleMap = ConcurrentHashMap<Name, ModuleDescriptor>()

    override val modules: List<ModuleDescriptor>
        get() = moduleMap.values.toList()

    // ... 其他实现
}
```

#### 3. CangJieBuiltIns 需要 SDK

```kotlin
open class CangJieBuiltIns(
    val storageManager: StorageManager,
    val sdk: CjSdk?  // 添加 SDK 引用
) {
    // ... 现有代码

    protected fun createBuiltInsModule(isFallback: Boolean) {
        myBuiltInsModule = ModuleDescriptorImpl(
            BUILTINS_MODULE_NAME,
            storageManager,
            this,
            cangJieProject = CangJieProject.ERROR,  // BuiltIns 模块不属于用户项目
            isBuiltInsModule = true
        )
        builtInsModule.initialize(
            BuiltInsLoader.Instance.createPackageFragmentProvider(
                storageManager,
                builtInsModule,
                isFallback,
                sdk  // 传递 SDK
            )
        )
        builtInsModule.setDependencies(builtInsModule)
    }
}
```

#### 4. ModuleDescriptor 简化

```kotlin
interface ModuleDescriptor : DeclarationDescriptor {

    /**
     * 所属的仓颉项目
     * 通过项目可以访问 builtIns 和其他模块
     */
    val cangJieProject: CangJieProject

    /**
     * 便捷访问 BuiltIns
     */
    val builtIns: CangJieBuiltIns
        get() = cangJieProject.builtIns

    // containingDeclaration 不再返回 CangJieProject
    // 因为 CangJieProject 不是 DeclarationDescriptor
    override val containingDeclaration: DeclarationDescriptor?
        get() = null

    // ... 其他成员
}
```

#### 5. BuiltInsLoader 接收 SDK

```kotlin
class BuiltInsLoaderImpl : BuiltInsLoader {
    override fun createPackageFragmentProvider(
        storageManager: StorageManager,
        builtInsModule: ModuleDescriptor,
        isFallback: Boolean,
        sdk: CjSdk?  // 添加 SDK 参数
    ): PackageFragmentProvider {
        return createBuiltInPackageFragmentProvider(
            storageManager,
            builtInsModule,
            StandardNames.ALL_NAMES,
            isFallback,
            sdk,
            resourceLoader::loadResource,
        )
    }

    fun createBuiltInPackageFragmentProvider(
        storageManager: StorageManager,
        module: ModuleDescriptor,
        packageFqNames: Set<FqName>,
        isFallback: Boolean,
        sdk: CjSdk?,  // 使用 SDK 而不是 project
        loadResource: (String) -> InputStream?,
    ): PackageFragmentProvider {
        // 使用 SDK 获取 stdlib 路径
        val resourcePath = sdk?.let {
            BuiltInSerializerFlatbuffers.getBuiltInsFilePath(fqName, it)
        }
        // ...
    }
}
```

## 实施步骤

1. **修改 CangJieBuiltIns**
   - 添加 `sdk: CjSdk?` 参数
   - 修改 `createBuiltInsModule` 传递 `CangJieProject.ERROR`

2. **修改 CangJieProject**
   - 添加 `sdk: CjSdk?` 属性
   - 添加 `builtIns: CangJieBuiltIns` 属性（懒加载）
   - 在创建 BuiltIns 时传递 SDK

3. **修改 ModuleDescriptor**
   - 保持 `cangJieProject: CangJieProject` 非空
   - 添加便捷属性 `builtIns` 委托到 `cangJieProject.builtIns`
   - 移除 `builtIns` 作为构造参数

4. **修改 ModuleDescriptorImpl**
   - 移除 `builtIns` 构造参数
   - 通过 `cangJieProject.builtIns` 访问

5. **修改 BuiltInsLoader**
   - 添加 `sdk: CjSdk?` 参数
   - 使用 SDK 获取标准库路径

6. **创建 CangJieProjectService**
   - 作为项目级服务
   - 管理整个项目的 CangJieProject 实例

7. **更新所有创建 ModuleDescriptorImpl 的地方**
   - 传递正确的 `cangJieProject`
   - 移除 `builtIns` 参数

## 优点

1. **清晰的依赖关系**：SDK → CangJieProject → BuiltIns → Modules
2. **单一职责**：CangJieProject 管理项目级资源（SDK、BuiltIns、Modules）
3. **避免循环依赖**：BuiltIns 不需要持有完整的 CangJieProject
4. **资源共享**：整个项目共享一个 BuiltIns 实例
5. **易于测试**：可以创建独立的 CangJieProject 用于测试

## 特殊情况处理

### BuiltIns Module

BuiltIns 内部的 `builtInsModule` 比较特殊，它：
- 不属于任何用户项目
- 使用 `CangJieProject.ERROR` 作为其 `cangJieProject`
- `builtIns` 属性返回自身所在的 CangJieBuiltIns 实例

### 测试和独立场景

对于测试或不需要完整项目的场景：
- 可以创建临时的 `CangJieProject` 实例
- 传递 `sdk = null` 表示使用 fallback
- 使用 `ProjectManager.getInstance().defaultProject`
