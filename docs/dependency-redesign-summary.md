# CjDependency 重新设计和 CjpmDependencyResolver 实现总结

## 完成时间

2025-12-03

## 完成的工作

### 1. 重新设计 CjDependency 接口

**文件**: `cangjie-project/src/main/kotlin/org/cangnova/cangjie/model/CjDependency.kt`

#### 主要改进：

- ✅ **从 interface 改为 sealed class**：更清晰地表示不同类型的依赖
- ✅ **新增 DependencyExclusion 数据类**：用于排除传递依赖
- ✅ **新增 4 种依赖子类**：
    - `CjDependency.Library` - 外部库依赖（从仓库下载）
    - `CjDependency.Path` - 本地路径依赖（工作空间内模块）
    - `CjDependency.Git` - Git 仓库依赖
    - `CjDependency.System` - 系统依赖（标准库等）

#### 新增属性：

- `optional: Boolean` - 是否为可选依赖
- `excludes: List<DependencyExclusion>` - 排除的传递依赖列表（改进了原来的 List<String>）
- `id: String` - 依赖的唯一标识符

#### 向后兼容：

- 保留了 `CjDependencyType` enum（标记为 @Deprecated）
- 提供了扩展属性 `CjDependency.type` 用于向后兼容

---

### 2. 更新 CjModule 接口

**文件**: `cangjie-project/src/main/kotlin/org/cangnova/cangjie/project/model/CjModule.kt`

#### 新增功能：

- ✅ **新增 `allDependencies` 属性**：返回所有类型的依赖
- ✅ **废弃旧的 `dependencies` 属性**：标记为 @Deprecated，但保留以向后兼容
- ✅ **新增便捷扩展属性**：
    - `CjModule.libraryDependencies` - 获取所有库依赖
    - `CjModule.pathDependencies` - 获取所有路径依赖
    - `CjModule.gitDependencies` - 获取所有 Git 依赖
    - `CjModule.systemDependencies` - 获取所有系统依赖

---

### 3. 更新 CjpmModuleImpl 实现

**文件**: `cjpm/src/main/kotlin/org/cangnova/cangjie/cjpm/project/CjpmModuleImpl.kt`

#### 实现内容：

- ✅ **实现 `allDependencies` 属性**：从 cjpm.toml 解析所有依赖
- ✅ **新增 `buildAllDependencies()` 方法**：统一解析逻辑
- ✅ **新增 `createDependencyFromConfig()` 方法**：从配置创建正确类型的依赖对象
- ✅ **清理旧代码**：删除了旧的 `CjpmDependency` 实现类

#### 解析逻辑：

```kotlin
when {
    config.path != null -> CjDependency.Path(...)
    config.git != null -> CjDependency.Git(...)
    else -> CjDependency.Library(...)
}
```

---

### 4. 实现 CjpmDependencyResolver（最紧迫）

**新增文件**:

- `cjpm/src/main/kotlin/org/cangnova/cangjie/cjpm/dependency/CjpmResolvedDependency.kt`
- `cjpm/src/main/kotlin/org/cangnova/cangjie/cjpm/dependency/CjpmPackageImpl.kt`
- `cjpm/src/main/kotlin/org/cangnova/cangjie/cjpm/dependency/CjpmDependencyResolver.kt` (更新)

#### 实现的功能：

##### 4.1 路径依赖解析 (resolvePathDependency)

```kotlin
// 1. 解析相对于项目根目录的路径
// 2. 查找并解析 cjpm.toml
// 3. 创建 CjpmPackage 对象
// 4. 解析传递依赖（如果 transitive = true）
```

##### 4.2 Git 依赖解析 (resolveGitDependency)

```kotlin
// 1. 在 ~/.cjpm/git/ 中查找已克隆的依赖
// 2. 解析依赖目录中的 cjpm.toml
// 3. 创建 CjpmPackage 对象
// 4. 解析传递依赖
```

##### 4.3 库依赖解析 (resolveLibraryDependency)

```kotlin
// 1. 在 ~/.cjpm/registry/ 中查找已下载的依赖
// 2. 扫描库文件（.cjo, .a, .so, .dll, .dylib）
// 3. 创建 CjpmLibrary 对象
// 4. 解析传递依赖
```

##### 4.4 系统依赖解析 (resolveSystemDependency)

```kotlin
// 系统依赖由工具链提供，直接返回成功状态
```

##### 4.5 传递依赖解析 (resolveTransitive)

```kotlin
// 1. 解析当前依赖
// 2. 返回其 transitiveDependencies 列表
```

#### 关键特性：

- ✅ **日志记录**：所有操作都有详细的日志
- ✅ **错误处理**：使用 try-catch 捕获异常，返回 failed 状态
- ✅ **缓存查找**：支持从 ~/.cjpm 缓存目录查找依赖
- ✅ **传递依赖支持**：根据依赖的 `transitive` 属性决定是否解析传递依赖
- ✅ **多平台支持**：使用 System.getProperty("user.home") 获取用户目录

---

## 架构改进

### 依赖类型层次

**旧设计（interface）**:

```kotlin
interface CjDependency {
    val type: CjDependencyType  // LIBRARY, MODULE, SYSTEM
}
```

**问题**: 无法表示 path、git 等特定信息

**新设计（sealed class）**:

```kotlin
sealed class CjDependency {
    data class Library(...) : CjDependency()
    data class Path(val path: String, ...) : CjDependency()
    data class Git(val url: String, val branch: String?, ...) : CjDependency()
    data class System(...) : CjDependency()
}
```

**优势**:

- 类型安全
- when 表达式可以穷尽检查
- 每种依赖有特定的属性

---

## 使用示例

### 1. 解析路径依赖

```kotlin
val dependency = CjDependency.Path(
    name = "my-lib",
    path = "../my-lib",
    version = CjVersion("1.0.0"),
    scope = CjDependencyScope.COMPILE
)

val resolver = CjpmDependencyResolver()
val resolved = resolver.resolve(dependency, project)

if (resolved?.isResolved == true) {
    val pkg = resolved.resolvedPackage
    println("Resolved: ${pkg?.name} at ${pkg?.localPath}")
}
```

### 2. 获取模块的所有依赖

```kotlin
val module: CjModule = ...

// 获取所有依赖
val allDeps = module.allDependencies

// 筛选特定类型
val pathDeps = module.pathDependencies  // 使用扩展属性
val gitDeps = module.gitDependencies
val libDeps = module.libraryDependencies

// 或者手动筛选
val paths = allDeps.filterIsInstance<CjDependency.Path>()
```

### 3. 处理依赖的 when 表达式

```kotlin
when (val dep = dependency) {
    is CjDependency.Path -> {
        println("Path dependency at: ${dep.path}")
    }
    is CjDependency.Git -> {
        println("Git dependency from: ${dep.url}")
        println("Branch/Tag/Rev: ${dep.gitRef}")
    }
    is CjDependency.Library -> {
        println("Library dependency: ${dep.name}:${dep.version}")
    }
    is CjDependency.System -> {
        println("System dependency: ${dep.name}")
    }
}
```

---

## 测试建议

### 单元测试

1. **CjDependency 创建测试**
   ```kotlin
   @Test
   fun testPathDependencyCreation() {
       val dep = CjDependency.Path(...)
       assertEquals("my-lib", dep.name)
       assertEquals("../my-lib", dep.path)
   }
   ```

2. **Git 依赖验证测试**
   ```kotlin
   @Test
   fun testGitDependencyValidation() {
       // 应该失败：同时指定 branch 和 tag
       assertThrows<IllegalArgumentException> {
           CjDependency.Git(
               name = "lib",
               url = "https://...",
               branch = "main",
               tag = "v1.0"
           )
       }
   }
   ```

3. **依赖解析测试**
   ```kotlin
   @Test
   fun testResolvePathDependency() {
       val dep = CjDependency.Path(...)
       val resolved = resolver.resolve(dep, project)
       assertTrue(resolved?.isResolved == true)
   }
   ```

---

## 已知限制和未来改进

### 当前限制

1. **Git/Library 依赖缓存查找简化**
    - 当前基于名称简单查找
    - 实际 cjpm 可能使用哈希或其他命名方式

2. **没有实现主动下载**
    - 依赖于 `cjpm update` 预先下载依赖
    - 未找到依赖时返回错误而非触发下载

3. **传递依赖解析深度为1**
    - 当前只解析直接传递依赖
    - 未递归解析传递依赖的传递依赖

### 未来改进方向

1. **实现依赖图管理** (P1)
   ```kotlin
   interface DependencyGraph {
       fun detectCycles(): List<List<CjDependency>>
       fun resolveConflicts(): Map<String, ConflictResolution>
   }
   ```

2. **实现版本冲突解决** (P1)
    - Nearest wins
    - Newest version
    - Explicit override

3. **实现依赖缓存** (P1)
    - 避免重复解析
    - 提升性能

4. **支持并行解析** (P2)
    - 使用协程并行解析多个依赖
    - 加快大型项目的解析速度

5. **实现增量解析** (P2)
    - 只解析变更的依赖
    - 监听 cjpm.toml 变化

---

## 文件清单

### 新增文件

- `cjpm/src/main/kotlin/org/cangnova/cangjie/cjpm/dependency/CjpmResolvedDependency.kt`
- `cjpm/src/main/kotlin/org/cangnova/cangjie/cjpm/dependency/CjpmPackageImpl.kt`
- `docs/cangjie-project-model.md` - 完整的项目模型图表文档

### 删除文件

- `cjpm/src/main/kotlin/org/cangnova/cangjie/cjpm/project/CjpmDependency.kt` (不再需要)

### 修改文件

- `cangjie-project/src/main/kotlin/org/cangnova/cangjie/model/CjDependency.kt` - 重新设计为 sealed class
- `cangjie-project/src/main/kotlin/org/cangnova/cangjie/project/model/CjModule.kt` - 新增 allDependencies
- `cjpm/src/main/kotlin/org/cangnova/cangjie/cjpm/project/CjpmModuleImpl.kt` - 实现新接口
- `cjpm/src/main/kotlin/org/cangnova/cangjie/cjpm/dependency/CjpmDependencyResolver.kt` - 完整实现

---

## 总结

本次重构完成了以下目标：

1. ✅ **重新设计了 CjDependency 接口** - 使用 sealed class 提供更好的类型安全
2. ✅ **更新了 CjModule 接口** - 提供统一的依赖访问方式
3. ✅ **实现了 CjpmDependencyResolver** - 支持 4 种依赖类型的解析
4. ✅ **保持向后兼容** - 旧代码仍然可以工作（使用 @Deprecated）
5. ✅ **完善了文档** - 创建了详细的项目模型图表文档

这个实现为后续的依赖管理功能（如依赖图、冲突解决、缓存等）打下了坚实的基础。