# QualifiedExpressionResolver 重构方案

## 概述

本文档分析了 `QualifiedExpressionResolver` 类的现状问题，并提出了符合仓颉语言语义的重构方案。

## 一、现状问题分析

### 1.1 职责过多（God Class）

当前 `QualifiedExpressionResolver` 承担了过多职责：

| 职责 | 方法 | 行数 |
|------|------|------|
| 包声明解析 | `resolvePackageHeader()` | ~20 |
| 导入处理 | `processImportReference()`, `doProcessImportReference()` | ~150 |
| 类型解析 | `resolveDescriptorForType()`, `resolveQualifierPartListForType()` | ~100 |
| 表达式限定符解析 | `resolveQualifierInExpressionAndUnroll()`, `resolveClassOrPackageInQualifiedExpression()` | ~80 |
| 包/类前缀解析 | `resolveToPackageOrClassPrefix()`, `resolveToPackageOrClassPrefixByImport()` | ~200 |
| 结果存储与可见性检查 | `storeResult()` (多个重载) | ~150 |
| 限定符接收器创建 | `storeQualifier()` | ~40 |

**总计**: 1600+ 行代码在单个类中

### 1.2 重复代码

`resolveToPackageOrClassPrefix()` 和 `resolveToPackageOrClassPrefixByImport()` 两个方法有大量重复逻辑：

```kotlin
// 两个方法几乎相同的代码结构
private fun resolveToPackageOrClassPrefix(...): Pair<DeclarationDescriptor?, Int> {
    if (resolveInIDEMode(path)) { /* 相同逻辑 */ }
    if (path.isEmpty()) { /* 相同逻辑 */ }
    if (position == QualifierPosition.EXPRESSION) { /* 相同逻辑 */ }
    val classifierDescriptor = scopeForFirstPart?.findClassifier(...)
    // ... 后续遍历逻辑也相似
}
```

### 1.3 混合关注点

可见性检查、错误报告、结果存储逻辑与解析逻辑耦合：

```kotlin
private fun storeResult(...): QualifierReceiver? {
    // 1. 记录绑定 (核心功能)
    trace.record(BindingContext.REFERENCE_TARGET, ...)

    // 2. 可见性检查 (应该分离)
    if (!isVisible(descriptor, fromToCheck, position, ...)) {
        trace.report(INVISIBLE_REFERENCE.on(...))
    }

    // 3. 位置特定处理 (应该用策略模式)
    when (position) {
        QualifierPosition.PACKAGE_HEADER -> { ... }
        QualifierPosition.IMPORT -> { ... }
        // ...
    }

    // 4. 限定符创建 (应该分离)
    return if (isQualifier) storeQualifier(...) else null
}
```

### 1.4 与 Kotlin 设计的耦合

当前设计很大程度上借鉴了 Kotlin 编译器，但仓颉语言有自己的特点：

| 特性 | Kotlin | 仓颉 |
|------|--------|------|
| 模块概念 | 弱（主要是包） | 强（模块 > 包 > 声明） |
| 导入语义 | `import pkg.*` 导入成员 | `import pkg` 可作为命名空间使用 |
| 重导出 | 无 | 支持 `public import` |
| 类型限定 | 允许 `kotlin.String` | 不允许 `std.core.String`（模块名不能用于类型限定） |

---

## 二、仓颉编译器设计参考

通过分析仓颉编译器源码，发现以下关键设计模式：

### 2.1 ImportContent 结构

```cpp
// external/cangjie_compiler/src/Parse/ParseImports.cpp
struct ImportContent {
    std::vector<PathPart> prefixPaths;  // 前缀路径 [std, core]
    Identifier identifier;               // 导入的标识符 String
    bool isWildcard;                      // 是否是通配导入
};
```

### 2.2 MemberAccess 嵌套结构

```cpp
// 对于 a.b.c.d 表达式
MemberAccess {
    receiver: MemberAccess {
        receiver: MemberAccess {
            receiver: RefExpr("a"),
            member: "b"
        },
        member: "c"
    },
    member: "d"
}
```

### 2.3 解析与语义分析分离

- 解析阶段：只构建 AST，不做符号解析
- 语义分析阶段：专门的 Pass 处理导入解析、类型检查等

---

## 三、重构方案

### 3.1 新的目录结构

```
resolve/qualified/
├── QualifiedExpressionResolver.kt      # 统一入口（精简版）
├── context/
│   ├── ResolutionContext.kt            # 解析上下文
│   └── ResolutionPosition.kt           # 解析位置枚举
├── resolvers/
│   ├── PackageHeaderResolver.kt        # 包声明解析
│   ├── ImportResolver.kt               # 导入解析
│   ├── TypeReferenceResolver.kt        # 类型引用解析
│   └── ExpressionQualifierResolver.kt  # 表达式限定符解析
├── lookup/
│   ├── QualifierLookupStrategy.kt      # 查找策略接口
│   ├── ImportScopeLookup.kt            # 从导入作用域查找
│   ├── PackageHierarchyLookup.kt       # 从包层次查找
│   └── ClassMemberLookup.kt            # 从类成员查找
├── validation/
│   ├── VisibilityChecker.kt            # 可见性检查
│   ├── ModuleNameValidator.kt          # 模块名使用验证
│   └── ReexportValidator.kt            # 重导出验证
└── result/
    ├── ResolutionResult.kt             # 解析结果
    └── QualifierReceiver.kt            # 限定符接收器（已存在）
```

### 3.2 核心类设计

#### 3.2.1 ResolutionContext - 解析上下文

```kotlin
/**
 * 解析上下文 - 封装解析过程中的所有必要信息
 *
 * 遵循不可变设计原则，通过 copy/with 方法创建新实例
 */
data class ResolutionContext(
    val trace: BindingTrace,
    val moduleDescriptor: ModuleDescriptor,
    val position: ResolutionPosition,
    val scopeForFirstPart: LexicalScope?,
    val shouldBeVisibleFrom: DeclarationDescriptor?,
    val languageVersionSettings: LanguageVersionSettings
) {
    /**
     * 创建子上下文（用于递归解析）
     */
    fun withPosition(newPosition: ResolutionPosition): ResolutionContext =
        copy(position = newPosition)

    fun withScope(newScope: LexicalScope): ResolutionContext =
        copy(scopeForFirstPart = newScope)

    /**
     * 检查是否在 IDE 模式（处理 _root_ide_package_ 前缀）
     */
    val isIDEMode: Boolean
        get() = scopeForFirstPart?.ownerDescriptor?.containingDeclaration == null
}

/**
 * 解析位置 - 决定解析行为的枚举
 */
enum class ResolutionPosition {
    /** 包声明: package com.example */
    PACKAGE_HEADER,

    /** 导入语句: import std.core.String */
    IMPORT,

    /** 类型位置: var x: String */
    TYPE,

    /** 表达式位置: foo.bar() */
    EXPRESSION;

    /** 是否允许模块名作为限定符 */
    val allowsModuleName: Boolean
        get() = this == IMPORT

    /** 是否值优先于类型 */
    val prefersValue: Boolean
        get() = this == EXPRESSION
}
```

#### 3.2.2 QualifierLookupStrategy - 查找策略接口

```kotlin
/**
 * 限定符查找策略接口
 *
 * 使用策略模式，允许不同位置使用不同的查找逻辑
 */
interface QualifierLookupStrategy {
    /**
     * 查找指定名称的描述符
     *
     * @param name 要查找的名称
     * @param context 解析上下文
     * @return 查找结果，包含描述符和是否继续查找的标志
     */
    fun lookup(name: Name, context: ResolutionContext): LookupResult

    /**
     * 策略优先级（数字越小优先级越高）
     */
    val priority: Int
}

/**
 * 查找结果
 */
sealed class LookupResult {
    /** 找到描述符 */
    data class Found(
        val descriptor: DeclarationDescriptor,
        val continueSearch: Boolean = false
    ) : LookupResult()

    /** 未找到，继续尝试下一个策略 */
    object NotFound : LookupResult()

    /** 存在歧义 */
    data class Ambiguous(
        val candidates: List<DeclarationDescriptor>
    ) : LookupResult()
}

/**
 * 从导入作用域查找包
 */
class ImportedPackageLookup : QualifierLookupStrategy {
    override val priority: Int = 10

    override fun lookup(name: Name, context: ResolutionContext): LookupResult {
        val scope = context.scopeForFirstPart ?: return LookupResult.NotFound
        val packageView = scope.findPackage(name)

        return if (packageView != null && !packageView.isEmpty()) {
            LookupResult.Found(packageView)
        } else {
            LookupResult.NotFound
        }
    }
}

/**
 * 从当前作用域查找分类器（类/接口/类型别名）
 */
class ClassifierLookup : QualifierLookupStrategy {
    override val priority: Int = 20

    override fun lookup(name: Name, context: ResolutionContext): LookupResult {
        val scope = context.scopeForFirstPart ?: return LookupResult.NotFound
        val classifier = scope.findClassifier(name, NoLookupLocation.FOR_QUALIFIER)

        return if (classifier != null) {
            LookupResult.Found(classifier)
        } else {
            LookupResult.NotFound
        }
    }
}

/**
 * 从模块的包层次结构查找
 */
class PackageHierarchyLookup : QualifierLookupStrategy {
    override val priority: Int = 30

    override fun lookup(name: Name, context: ResolutionContext): LookupResult {
        // 只在导入位置允许直接使用模块包
        if (!context.position.allowsModuleName) {
            return LookupResult.NotFound
        }

        val packageView = context.moduleDescriptor.getPackage(FqName(name.asString()))
        return if (!packageView.isEmpty()) {
            LookupResult.Found(packageView)
        } else {
            LookupResult.NotFound
        }
    }
}
```

#### 3.2.3 TypeReferenceResolver - 类型引用解析器

```kotlin
/**
 * 类型引用解析器
 *
 * 专门处理类型位置的限定名称解析，如 `com.example.MyClass<T>`
 */
class TypeReferenceResolver(
    private val lookupStrategies: List<QualifierLookupStrategy>,
    private val visibilityChecker: VisibilityChecker,
    private val moduleNameValidator: ModuleNameValidator
) {
    /**
     * 解析用户类型
     *
     * @param userType PSI 类型节点
     * @param context 解析上下文
     * @return 解析结果
     */
    fun resolve(
        userType: CjUserType,
        context: ResolutionContext
    ): TypeResolutionResult {
        // 1. 无限定名：直接在当前作用域查找
        if (userType.qualifier == null) {
            return resolveSimpleType(userType, context)
        }

        // 2. 有限定名：逐级解析
        val qualifierParts = userType.asQualifierPartList()
        return resolveQualifiedType(qualifierParts, context)
    }

    private fun resolveSimpleType(
        userType: CjUserType,
        context: ResolutionContext
    ): TypeResolutionResult {
        val name = userType.referenceExpression?.referencedNameAsName
            ?: return TypeResolutionResult.Error("Missing type name")

        // 使用策略链查找
        for (strategy in lookupStrategies.sortedBy { it.priority }) {
            when (val result = strategy.lookup(name, context)) {
                is LookupResult.Found -> {
                    val descriptor = result.descriptor
                    if (descriptor is ClassifierDescriptor) {
                        // 检查可见性
                        visibilityChecker.check(descriptor, context)?.let { error ->
                            return TypeResolutionResult.Error(error)
                        }
                        return TypeResolutionResult.Success(descriptor)
                    }
                }
                is LookupResult.Ambiguous -> {
                    return TypeResolutionResult.Ambiguous(result.candidates)
                }
                LookupResult.NotFound -> continue
            }
        }

        return TypeResolutionResult.NotFound(name)
    }

    private fun resolveQualifiedType(
        parts: List<QualifierPart>,
        context: ResolutionContext
    ): TypeResolutionResult {
        // 解析限定符前缀
        val (qualifier, nextIndex) = resolveQualifierPrefix(
            parts.dropLast(1),
            context
        )

        if (qualifier == null) {
            return TypeResolutionResult.NotFound(parts.first().name)
        }

        // 验证是否使用了模块名作为限定符
        moduleNameValidator.validateForType(parts, qualifier, context)?.let { error ->
            return TypeResolutionResult.Error(error)
        }

        // 在限定符作用域中查找类型
        val lastName = parts.last().name
        val classifier = when (qualifier) {
            is PackageViewDescriptor ->
                qualifier.memberScope.getContributedClassifier(lastName, NoLookupLocation.FOR_TYPE)
            is ClassDescriptor ->
                qualifier.unsubstitutedMemberScope.getContributedClassifier(lastName, NoLookupLocation.FOR_TYPE)
            else -> null
        }

        return if (classifier != null) {
            TypeResolutionResult.Success(classifier)
        } else {
            TypeResolutionResult.NotFound(lastName)
        }
    }
}

/**
 * 类型解析结果
 */
sealed class TypeResolutionResult {
    data class Success(val descriptor: ClassifierDescriptor) : TypeResolutionResult()
    data class NotFound(val name: Name) : TypeResolutionResult()
    data class Ambiguous(val candidates: List<DeclarationDescriptor>) : TypeResolutionResult()
    data class Error(val message: String) : TypeResolutionResult()
}
```

#### 3.2.4 ImportResolver - 导入解析器

```kotlin
/**
 * 导入解析器
 *
 * 处理 import 语句的解析，创建导入作用域
 */
class ImportResolver(
    private val lookupStrategies: List<QualifierLookupStrategy>,
    private val visibilityChecker: VisibilityChecker,
    private val reexportValidator: ReexportValidator
) {
    /**
     * 处理单个导入指令
     *
     * @param importDirective 导入指令
     * @param context 解析上下文
     * @return 导入作用域，失败返回 null
     */
    fun processImport(
        importDirective: CjImportInfo,
        context: ResolutionContext
    ): ImportingScope? {
        val path = importDirective.importContent?.asQualifierPartList()
            ?: return null

        return if (importDirective.isAllUnder) {
            processAllUnderImport(path, importDirective, context)
        } else {
            processSingleImport(path, importDirective, context)
        }
    }

    private fun processAllUnderImport(
        path: List<QualifierPart>,
        directive: CjImportInfo,
        context: ResolutionContext
    ): ImportingScope? {
        // 解析包或类
        val target = resolveToPackageOrClass(path, context) ?: return null

        // 不能从类进行全导入
        if (target is ClassDescriptor) {
            context.trace.report(
                CANNOT_ALL_UNDER_IMPORT_FROM_ENUM.on(
                    path.last().expression ?: return null,
                    target
                )
            )
            return null
        }

        // 创建重导出作用域（如果需要）
        val reexportScope = createReexportScopeIfNeeded(target, directive, context)

        return AllUnderImportScope.create(target, emptyList(), reexportScope)
    }

    private fun processSingleImport(
        path: List<QualifierPart>,
        directive: CjImportInfo,
        context: ResolutionContext
    ): ImportingScope? {
        val aliasName = directive.importedName ?: return null

        // 解析限定符前缀
        val qualifier = resolveToPackageOrClass(
            path.dropLast(1),
            context
        ) ?: return null

        val lastName = path.last().name

        // 验证重导出
        if (directive is CjImportDirectiveItem && directive.isReexport) {
            reexportValidator.validate(qualifier, lastName, directive, context)
        }

        // 创建重导出作用域
        val reexportScope = createReexportScopeIfNeeded(qualifier, directive, context)

        return LazyExplicitImportScope(
            context.languageVersionSettings,
            qualifier,
            context.shouldBeVisibleFrom as? PackageFragmentDescriptor,
            lastName,
            aliasName,
            CallOnceFunction(Unit) { /* 延迟验证 */ },
            reexportScope
        )
    }

    private fun createReexportScopeIfNeeded(
        target: DeclarationDescriptor,
        directive: CjImportInfo,
        context: ResolutionContext
    ): PackageReexportScope? {
        if (target !is PackageViewDescriptor) return null
        if (directive !is CjImportDirectiveItem) return null

        return PackageReexportScope(
            packageFqName = target.fqName,
            project = directive.project,
            moduleDescriptor = context.moduleDescriptor,
            fromPackage = context.shouldBeVisibleFrom as? PackageFragmentDescriptor
        )
    }
}
```

#### 3.2.5 重构后的 QualifiedExpressionResolver

```kotlin
/**
 * 限定表达式解析器（重构版）
 *
 * 作为统一入口，委托给专门的解析器处理不同类型的解析请求
 */
class QualifiedExpressionResolver(
    val languageVersionSettings: LanguageVersionSettings
) {
    // 依赖注入
    @set:Inject
    private lateinit var typeResolver: TypeResolver

    // 查找策略（按优先级排序）
    private val lookupStrategies: List<QualifierLookupStrategy> = listOf(
        ImportedPackageLookup(),
        ClassifierLookup(),
        PackageHierarchyLookup()
    )

    // 验证器
    private val visibilityChecker = VisibilityChecker(languageVersionSettings)
    private val moduleNameValidator = ModuleNameValidator()
    private val reexportValidator = ReexportValidator()

    // 专门的解析器
    private val typeReferenceResolver = TypeReferenceResolver(
        lookupStrategies, visibilityChecker, moduleNameValidator
    )
    private val importResolver = ImportResolver(
        lookupStrategies, visibilityChecker, reexportValidator
    )
    private val expressionResolver = ExpressionQualifierResolver(
        lookupStrategies, visibilityChecker
    )
    private val packageHeaderResolver = PackageHeaderResolver()

    // ============ 公共 API ============

    /**
     * 解析包声明
     */
    fun resolvePackageHeader(
        packageDirective: CjPackageDirective,
        module: ModuleDescriptor,
        trace: BindingTrace
    ) {
        val context = createContext(trace, module, ResolutionPosition.PACKAGE_HEADER)
        packageHeaderResolver.resolve(packageDirective, context)
    }

    /**
     * 解析类型引用
     */
    fun resolveDescriptorForType(
        userType: CjUserType,
        scope: LexicalScope,
        trace: BindingTrace,
        isDebuggerContext: Boolean = false
    ): TypeQualifierResolutionResult {
        val context = createContext(trace, scope, ResolutionPosition.TYPE, isDebuggerContext)
        return typeReferenceResolver.resolve(userType, context).toQualifierResult()
    }

    /**
     * 处理导入
     */
    fun processImportReference(
        importDirective: CjImportInfo,
        moduleDescriptor: ModuleDescriptor,
        trace: BindingTrace,
        excludedImportNames: Collection<FqName>,
        packageFragmentForVisibilityCheck: PackageFragmentDescriptor?
    ): ImportingScope? {
        val context = createContext(
            trace, moduleDescriptor, ResolutionPosition.IMPORT,
            visibleFrom = packageFragmentForVisibilityCheck
        )
        return importResolver.processImport(importDirective, context)
    }

    /**
     * 解析表达式中的限定符
     */
    fun resolveQualifierInExpressionAndUnroll(
        expression: CjQualifiedExpression,
        context: ExpressionTypingContext,
        isValue: (CjSimpleNameExpression) -> Boolean
    ): List<CallExpressionElement> {
        val resolutionContext = createContext(
            context.trace,
            context.scope,
            ResolutionPosition.EXPRESSION
        )
        return expressionResolver.resolveAndUnroll(expression, resolutionContext, isValue)
    }

    // ============ 内部方法 ============

    private fun createContext(
        trace: BindingTrace,
        module: ModuleDescriptor,
        position: ResolutionPosition,
        visibleFrom: DeclarationDescriptor? = null
    ): ResolutionContext {
        return ResolutionContext(
            trace = trace,
            moduleDescriptor = module,
            position = position,
            scopeForFirstPart = null,
            shouldBeVisibleFrom = visibleFrom,
            languageVersionSettings = languageVersionSettings
        )
    }

    private fun createContext(
        trace: BindingTrace,
        scope: LexicalScope,
        position: ResolutionPosition,
        isDebuggerContext: Boolean = false
    ): ResolutionContext {
        return ResolutionContext(
            trace = trace,
            moduleDescriptor = scope.ownerDescriptor.module,
            position = position,
            scopeForFirstPart = scope,
            shouldBeVisibleFrom = if (isDebuggerContext) null else scope.ownerDescriptor,
            languageVersionSettings = languageVersionSettings
        )
    }
}
```

---

## 四、迁移策略

### 4.1 Phase 1: 基础设施（1-2 周）

1. 创建 `ResolutionContext` 和 `ResolutionPosition`
2. 实现 `QualifierLookupStrategy` 接口和基础实现
3. 实现 `VisibilityChecker`
4. 单元测试覆盖

### 4.2 Phase 2: 验证器（1 周）

1. 实现 `ModuleNameValidator`
2. 实现 `ReexportValidator`
3. 集成测试

### 4.3 Phase 3: 专门解析器（2-3 周）

1. 实现 `PackageHeaderResolver`
2. 实现 `TypeReferenceResolver`
3. 实现 `ImportResolver`
4. 实现 `ExpressionQualifierResolver`
5. 逐步替换原有方法

### 4.4 Phase 4: 整合与清理（1 周）

1. 重构 `QualifiedExpressionResolver` 为门面类
2. 删除废弃代码
3. 更新文档
4. 性能测试

---

## 五、关键改进对比

| 方面 | 重构前 | 重构后 |
|------|--------|--------|
| **类大小** | 1600+ 行 | ~200 行（门面）+ 多个小类 |
| **职责分离** | 单一类处理所有 | 专门解析器各司其职 |
| **可测试性** | 难以单元测试 | 每个组件可独立测试 |
| **扩展性** | 修改影响全局 | 添加新策略即可扩展 |
| **仓颉语义** | 借鉴 Kotlin | 专门适配仓颉 |
| **代码复用** | 大量重复 | 策略模式复用 |
| **可维护性** | 困难 | 清晰的边界 |

---

## 六、测试策略

### 6.1 单元测试

```kotlin
class ImportedPackageLookupTest {
    @Test
    fun `should find imported package by name`() {
        // 准备: 创建包含 `import std.core` 的作用域
        val scope = createScopeWithImport("std.core")
        val context = createContext(scope, ResolutionPosition.EXPRESSION)

        // 执行
        val lookup = ImportedPackageLookup()
        val result = lookup.lookup(Name.identifier("core"), context)

        // 验证
        assertIs<LookupResult.Found>(result)
        assertEquals("std.core", (result.descriptor as PackageViewDescriptor).fqName.asString())
    }

    @Test
    fun `should return NotFound for non-imported package`() {
        val scope = createEmptyScope()
        val context = createContext(scope, ResolutionPosition.EXPRESSION)

        val lookup = ImportedPackageLookup()
        val result = lookup.lookup(Name.identifier("unknown"), context)

        assertIs<LookupResult.NotFound>(result)
    }
}
```

### 6.2 集成测试

```kotlin
class TypeReferenceResolverIntegrationTest {
    @Test
    fun `should resolve simple type from import`() {
        // import std.core.String
        // var x: String
        val file = createFile("""
            import std.core.String

            func test() {
                var x: String = "hello"
            }
        """)

        val typeRef = file.findTypeReference("String")
        val result = resolver.resolve(typeRef, context)

        assertIs<TypeResolutionResult.Success>(result)
        assertEquals("std.core.String", result.descriptor.fqName.asString())
    }

    @Test
    fun `should reject module name in type position`() {
        // var x: std.core.String  // 错误：不能使用模块名限定类型
        val file = createFile("""
            func test() {
                var x: std.core.String = "hello"
            }
        """)

        val typeRef = file.findTypeReference("std.core.String")
        val result = resolver.resolve(typeRef, context)

        assertIs<TypeResolutionResult.Error>(result)
        assertTrue(result.message.contains("module"))
    }
}
```

### 6.3 场景测试

```kotlin
class CangjieImportSemanticsTest {
    @Test
    fun `import package allows using it as namespace`() {
        // import std.core
        // var x: core.String
        val file = createFile("""
            import std.core

            func test() {
                var x: core.String = "hello"
            }
        """)

        assertNoErrors(file)
    }

    @Test
    fun `reexport package should fail`() {
        // public import std.core  // 错误：包不能被重导出
        val file = createFile("""
            public import std.core
        """)

        val errors = collectErrors(file)
        assertTrue(errors.any { it.code == "PACKAGE_CANNOT_BE_REEXPORTED" })
    }
}
```

---

## 七、风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 行为变更导致兼容性问题 | 高 | 大量回归测试，逐步替换 |
| 性能下降 | 中 | 保持缓存机制，性能基准测试 |
| 依赖注入复杂化 | 低 | 使用已有的 DI 框架 |
| 开发周期延长 | 中 | 分阶段实施，保持可发布状态 |

---

## 八、总结

本重构方案通过：

1. **分离职责**：将 1600+ 行的巨型类拆分为多个专门的解析器
2. **策略模式**：使用 `QualifierLookupStrategy` 统一查找逻辑
3. **上下文对象**：`ResolutionContext` 简化参数传递
4. **适配仓颉语义**：专门处理模块/包层次、重导出等特性

将使代码更加清晰、可测试、可维护，并更好地符合仓颉语言的设计理念。
