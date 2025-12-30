# ResolutionScope 继承体系详解

> **文档版本**: 1.0
> **生成日期**: 2025-12-29
 

## 目录

1. [概述](#概述)
2. [顶层接口架构](#顶层接口架构)
3. [MemberScope 分支详解](#memberscope-分支详解)
4. [HierarchicalScope 分支详解](#hierarchicalscope-分支详解)
5. [完整继承树图](#完整继承树图)
6. [作用域层次与使用场景](#作用域层次与使用场景)
7. [设计模式分析](#设计模式分析)
8. [性能优化策略](#性能优化策略)

---

## 概述

`ResolutionScope` 是仓颉语言插件符号解析系统的核心抽象,定义了所有作用域类型的基础接口。作用域系统负责在编译过程中解析标识符引用,支持类型检查、代码补全、导航等 IDE 功能。

### 核心设计理念

- **层次化查找**: 支持从内向外的多层作用域链查找
- **惰性计算**: 延迟构建作用域内容,提高性能
- **组合优先**: 通过组合而非继承构建复杂作用域
- **错误恢复**: 提供错误作用域支持 IDE 容错

---

## 顶层接口架构

```
ResolutionScope (顶层接口)
├── MemberScope (成员作用域 - 添加名称集合)
└── HierarchicalScope (层次化作用域 - 添加父子关系)
    ├── LexicalScope (词法作用域 - 代码块级)
    └── ImportingScope (导入作用域 - 包导入)
```

### ResolutionScope (org.cangnova.cangjie.resolve.scopes)

**定义位置**: `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/ResolutionScope.kt`

**核心职责**:
- 根据名称查找各类符号描述符(分类器、函数、变量、属性、宏等)
- 支持包视图和完全限定名查找
- 提供基于索引和导出 ID 的反序列化查找
- 区分普通符号和弃用符号的查找
- 支持增量编译的查找位置记录

**关键方法**:
```kotlin
// 单一符号查找
fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor?
fun getContributedPackageView(name: Name, location: LookupLocation): PackageViewDescriptor?

// 多符号收集(支持重载)
fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor>
fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor>
fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor>
fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor>

// 批量查找
fun getContributedDescriptors(
    kindFilter: DescriptorKindFilter = DescriptorKindFilter.ALL,
    nameFilter: (Name) -> Boolean = MemberScope.ALL_NAME_FILTER
): Collection<DeclarationDescriptor>

// 优化方法
fun definitelyDoesNotContainName(name: Name): Boolean
fun recordLookup(name: Name, location: LookupLocation)

// 反序列化支持
fun getContributedClassifierByIndex(index: Int, location: LookupLocation): ClassifierDescriptor?
fun getContributedClassifierByExportId(exportId: String, location: LookupLocation): ClassifierDescriptor?
```

**设计特点**:
- 所有查找方法默认返回空集合或 null
- 支持弃用符号的包含/排除查找
- 通过 `LookupLocation` 支持增量编译依赖追踪

---

## MemberScope 分支详解

### MemberScope (org.cangnova.cangjie.resolve.scopes)

**定义位置**: `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/MemberScope.kt`

**与 ResolutionScope 的区别**:
- 添加了名称集合属性: `functionNames`, `variableNames`, `classifierNames`, `propertyNames`
- 名称集合可以是超集(包含不存在的名称),用于性能优化
- 提供 `printScopeStructure()` 用于调试

**核心属性**:
```kotlin
val functionNames: Set<Name>        // 所有函数名称
val variableNames: Set<Name>        // 所有变量名称
val classifierNames: Set<Name>?     // 所有分类器名称(可为 null)
val propertyNames: Set<Name>        // 所有属性名称
```

**特殊实现 - MemberScope.Empty**:
- 单例空作用域
- 所有查找返回空结果
- `definitelyDoesNotContainName()` 总是返回 true
- 用于优化空作用域的创建

### MemberScopeImpl (org.cangnova.cangjie.resolve.scopes)

**定义位置**: `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/MemberScopeImpl.kt`

**作用**: MemberScope 的抽象基类,提供默认实现

**默认行为**:
- 所有查找方法返回空集合
- 名称集合通过 `getContributedDescriptors()` 计算(效率较低)
- 子类只需覆盖实际支持的方法

**继承层次**:

```
MemberScopeImpl (抽象基类)
├── GivenFunctionsMemberScope (提供预定义函数集合的作用域)
│   ├── TupleClassScope (元组类的合成作用域)
│   ├── FunctionClassScope (函数类型的合成作用域)
│   └── VArrayClassScope (可变数组的合成作用域)
│
├── MemberScope.Empty (空作用域单例)
│
├── SubpackagesScope (子包作用域 - descriptors 模块)
│   └── SubpackagesImportingScope (支持子包导入的作用域)
│
├── DeserializedMemberScope (反序列化成员作用域 - 从编译产物加载)
│   ├── DeserializedEnumMemberScope (枚举类反序列化作用域)
│   ├── DeserializedClassMemberScope (类反序列化作用域)
│   ├── DeserializedExtendMemberScope (扩展反序列化作用域)
│   └── DeserializedPackageMemberScope (包反序列化作用域)
│
├── PackageReexportScope (包重导出作用域)
│
├── AbstractLazyMemberScope (惰性成员作用域基类 - analysis 模块)
│   ├── LazyClassMemberScope (类成员惰性加载作用域)
│   ├── LazyExtendMemberScope (扩展成员惰性加载作用域)
│   └── LazyPackageMemberScope (包成员惰性加载作用域)
│
├── StaticScopeForCangJieEnum (仓颉枚举的静态作用域)
│
├── InnerClassesScopeWrapper (内部类包装作用域)
│
├── EnumEntriesScope (枚举条目作用域 - TypeAliasQualifier 内部类)
│
├── DynamicCallableDescriptors.createDynamicDescriptorScope (动态描述符作用域)
│
├── BasicTypesMemberScope (基础类型成员作用域 - builtins.basic 包)
│
├── BuiltinsTypesMemberScope (内置类型成员作用域 - builtins.builtinstype 包)
│
├── CDocLinkResolutionService.GlobalSyntheticPackageViewDescriptor.memberScope (CDoc 链接解析的合成包作用域)
│
├── DeprecatedMemberScope (弃用成员作用域)
│
└── ExtensionsScope (CDoc 引用的扩展作用域)
```

### 重要的直接 MemberScope 实现

#### 1. SubstitutingScope (类型替换作用域)

**定义位置**: `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/SubstitutingScope.kt`

**用途**: 对底层作用域的所有符号应用类型替换(装饰器模式)

**核心功能**:
```kotlin
class SubstitutingScope(
    private val workerScope: MemberScope,
    givenSubstitutor: TypeSubstitutor
) : MemberScope
```

**使用场景**:
- 泛型类实例化: `List<T>` → `List<Int>`
- 继承关系中的类型替换: 子类继承泛型父类
- 扩展函数的接收者类型替换

**特点**:
- 透传名称集合属性(无需重新计算)
- 使用捕获替换器处理通配符类型
- 缓存替换结果避免重复计算

#### 2. InstanceMemberScope (实例成员作用域)

**定义位置**: `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/InstanceMemberScope.kt`

**用途**: 表示类实例的成员作用域,排除静态成员

**特点**:
- 过滤掉类的静态成员
- 仅返回实例方法、实例属性等
- 用于表达式 `obj.member` 的解析

#### 3. StaticMemberScope (静态成员作用域)

**定义位置**: `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/StaticMemberScope.kt`

**用途**: 表示类的静态成员作用域

**特点**:
- 仅包含静态方法、静态属性、嵌套类等
- 用于类名限定访问: `MyClass.staticMember`

#### 4. PrimitiveMemberScope (原生类型成员作用域)

**定义位置**: `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/PrimitiveMemberScope.kt`

**用途**: 为原生类型(Int, Bool, Float 等)提供成员作用域

**特点**:
- 包含原生类型的方法(如 `Int.toString()`)
- 通常通过内置库定义

#### 5. ErrorScope (错误恢复作用域)

**定义位置**: `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/ErrorScope.kt`

**用途**: 错误恢复,在符号解析失败时返回错误描述符

**特点**:
- 所有查找返回错误描述符
- 允许编译器继续分析,避免级联错误
- IDE 友好,在不完整代码中保持功能可用

**子类**:
```
ErrorScope
└── ThrowingScope (抛出异常的错误作用域,用于不应被访问的场景)
```

#### 6. BuiltInsMemberScope (内置类型成员作用域)

**定义位置**: `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/BuiltInsMemberScope.kt`

**用途**: 为内置类型和包提供成员作用域

**特点**:
- 包含语言内置的类型和函数
- 通常在编译器初始化时构建

#### 7. ChainedMemberScope (链式成员作用域)

**定义位置**: `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/ChainedMemberScope.kt`

**用途**: 组合多个 MemberScope 形成统一查找视图(组合模式)

**核心功能**:
```kotlin
class ChainedMemberScope private constructor(
    private val debugName: String,
    private val scopes: Array<out MemberScope>
) : MemberScope {
    companion object {
        fun create(debugName: String, vararg scopes: MemberScope): MemberScope
        fun create(debugName: String, scopes: Iterable<MemberScope>): MemberScope
    }
}
```

**特点**:
- 自动扁平化嵌套的 ChainedMemberScope
- 过滤空作用域优化性能
- 单一符号查找使用短路求值
- 多符号收集合并所有结果

**使用场景**:
- 组合类自身和父类的成员作用域
- 组合多个导入语句引入的符号
- 实现从内向外的符号查找

#### 8. AbstractScopeAdapter (作用域适配器)

**定义位置**: `descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/AbstractScopeAdapter.kt`

**用途**: 适配器基类,用于包装和转换作用域

**子类**:
```
AbstractScopeAdapter
├── TypeIntersectionScope (类型交集作用域)
└── LazyScopeAdapter (惰性作用域适配器)
```

---

## HierarchicalScope 分支详解

### HierarchicalScope (org.cangnova.cangjie.resolve.scopes)

**定义位置**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/Scopes.kt`

**核心特征**:
- 添加 `parent: HierarchicalScope?` 属性,支持父子链
- 提供 `printStructure(p: Printer)` 用于调试
- 用于实现从内向外的作用域链查找

**辅助方法**:
```kotlin
// 遍历作用域链
inline fun HierarchicalScope.processForMeAndParent(process: (HierarchicalScope) -> Unit)

// 从作用域链收集列表
inline fun <T : Any> HierarchicalScope.getListFromMeAndParent(
    fetch: (HierarchicalScope) -> List<T>?
): List<T>

// 从作用域链查找第一个匹配项
inline fun <T : Any> HierarchicalScope.findFirstFromMeAndParent(
    fetch: (HierarchicalScope) -> T?
): T?
```

### BaseHierarchicalScope (org.cangnova.cangjie.resolve.scopes)

**用途**: HierarchicalScope 的抽象基类,提供默认空实现

**继承层次**:

```
BaseHierarchicalScope
├── LexicalScope.Base (基础词法作用域)
└── BaseImportingScope (导入作用域基类)
```

### LexicalScope (词法作用域)

**定义位置**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/Scopes.kt`

**核心特征**:
```kotlin
interface LexicalScope : HierarchicalScope {
    override val parent: HierarchicalScope  // 非 null,必有父作用域
    val ownerDescriptor: DeclarationDescriptor  // 作用域所有者
    val isOwnerDescriptorAccessibleByLabel: Boolean  // 是否可通过标签访问
    val implicitReceiver: ReceiverParameterDescriptor?  // 隐式接收者(this)
    val kind: LexicalScopeKind  // 作用域类型

    fun addVariableDescriptor(variableDescriptor: VariableDescriptor)  // 添加局部变量
}
```

**LexicalScopeKind (词法作用域类型枚举)**:
```kotlin
enum class LexicalScopeKind(val withLocalDescriptors: Boolean) {
    // 类相关
    CLASS_HEADER(false),              // 类头部作用域
    CLASS_INHERITANCE(false),         // 类继承声明作用域
    CLASS_STATIC_SCOPE(false),        // 类静态作用域
    CLASS_MEMBER_SCOPE(false),        // 类成员作用域
    CONSTRUCTOR_HEADER(false),        // 构造器头部作用域

    // 函数相关
    FUNCTION_HEADER(false),           // 函数头部作用域
    FUNCTION_HEADER_FOR_DESTRUCTURING(false),  // 解构用函数头部
    FUNCTION_INNER_SCOPE(true),       // 函数内部作用域

    // 属性相关
    PROPERTY_HEADER(false),           // 属性头部作用域
    PROPERTY_ACCESSOR_BODY(true),     // 属性访问器作用域
    PROPERTY_DELEGATE_METHOD(false),  // 属性委托方法作用域
    VARIABLE_INITIALIZER_OR_DELEGATE(true),  // 变量初始化器作用域

    // 扩展相关
    EXTEND_HEADER(false),             // extend 头部作用域

    // 控制流相关
    CODE_BLOCK(true),                 // 代码块作用域
    IF(true),                         // if 语句作用域
    THEN(true),                       // then 分支作用域
    ELSE(true),                       // else 分支作用域
    WHILE(true),                      // while 循环作用域
    WHILE_BODY(true),                 // while 循环主体作用域
    DO_WHILE_BODY(true),              // do-while 主体作用域
    FOR(true),                        // for 循环作用域
    MATCH(true),                      // match 表达式作用域
    MATCH_CASE(true),                 // match 分支作用域
    TRY(true),                        // try 块作用域
    CATCH(true),                      // catch 块作用域

    // 表达式相关
    LEFT_BOOLEAN_EXPRESSION(true),    // 布尔表达式左侧
    RIGHT_BOOLEAN_EXPRESSION(true),   // 布尔表达式右侧

    // 其他
    TYPE_ALIAS_HEADER(false),         // 类型别名头部作用域
    DEFAULT_VALUE(true),              // 默认值作用域
    CALLABLE_REFERENCE(false),        // 可调用引用作用域
    SYNTHETIC(false),                 // 合成作用域
    EMPTY(false),                     // 空作用域
    THROWING(false)                   // 抛出语句作用域
}
```

**LexicalScope 实现**:

```
LexicalScope
├── LexicalScope.Base (基础实现 - BaseHierarchicalScope 的子类)
│
├── LexicalChainedScope (链式词法作用域)
│
├── LexicalScopeStorage (作用域存储实现)
│
├── LexicalWritableScope.Snapshot (可写作用域的快照)
│
├── LexicalScopeImpl (标准词法作用域实现)
│
├── DeprecatedLexicalScope (弃用的词法作用域)
│
├── LexicalScopeWrapper (词法作用域包装器 - scopeUtil.kt)
│
└── ErrorLexicalScope (错误词法作用域)
```

### ImportingScope (导入作用域)

**定义位置**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/Scopes.kt`

**核心特征**:
```kotlin
interface ImportingScope : HierarchicalScope {
    override val parent: ImportingScope?  // 父导入作用域

    fun getContributedPackage(name: Name): PackageViewDescriptor?  // 获取导入的包

    fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter = DescriptorKindFilter.ALL,
        nameFilter: (Name) -> Boolean = MemberScope.ALL_NAME_FILTER,
        changeNamesForAliased: Boolean  // 是否为别名更改名称
    ): Collection<DeclarationDescriptor>

    fun computeImportedNames(): Set<Name>?  // 计算导入的名称集合
}
```

**特殊实现 - ImportingScope.Empty**:
- 单例空导入作用域
- 作为导入作用域链的终点
- `definitelyDoesNotContainName()` 总是返回 true

**ImportingScope 实现**:

```
ImportingScope
├── ImportingScope.Empty (空导入作用域单例)
│
├── BaseImportingScope (导入作用域基类)
│   └── SubpackagesImportingScope (子包导入作用域)
│
├── LazyImportScope (惰性导入作用域)
│
├── FileScopeFactory.FilesScopesBuilder.lazyImportingScope (文件惰性导入作用域)
│
├── FileScopeFactory.FilesScopesBuilder.CurrentFileScope (当前文件作用域)
│
├── FileScopeFactory.CurrentPackageScope (当前包作用域)
│
├── CompositePrioritizedImportingScope (复合优先级导入作用域)
│
├── MemberScopeToImportingScopeAdapter (成员作用域到导入作用域的适配器)
│
└── scopeUtil.kt.withParent (带父作用域的扩展方法)
```

#### BaseImportingScope (导入作用域基类)

**定义位置**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/Scopes.kt`

**用途**: ImportingScope 的抽象基类,继承自 BaseHierarchicalScope

**特点**:
- 提供默认空实现
- 子类只需覆盖实际支持的方法

#### CompositePrioritizedImportingScope (复合优先级导入作用域)

**定义位置**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/Scopes.kt`

**用途**: 组合两个导入作用域,按优先级查找

**核心功能**:
```kotlin
class CompositePrioritizedImportingScope(
    private val primaryScope: ImportingScope,    // 主作用域(高优先级)
    private val secondaryScope: ImportingScope   // 次级作用域(低优先级)
) : ImportingScope
```

**查找策略**:
- 单一符号: 先在 primary 中查找,未找到则在 secondary 中查找
- 多符号: 合并两个作用域的结果

**使用场景**:
- 文件导入覆盖包导入
- 显式导入优先于通配符导入

---

## 完整继承树图

### 层次结构总览

```
ResolutionScope (顶层接口)
├─────────────────────────────────────────────────────────────┐
│                                                               │
MemberScope (成员作用域)                          HierarchicalScope (层次化作用域)
│                                                               │
├── MemberScopeImpl (抽象基类)                                 ├── BaseHierarchicalScope
│   ├── GivenFunctionsMemberScope                              │   ├── LexicalScope.Base
│   │   ├── TupleClassScope                                    │   └── BaseImportingScope
│   │   ├── FunctionClassScope                                 │       └── SubpackagesImportingScope
│   │   └── VArrayClassScope                                   │
│   ├── Empty                                                  ├── LexicalScope (词法作用域)
│   ├── SubpackagesScope                                       │   ├── Base
│   │   └── SubpackagesImportingScope                          │   ├── LexicalChainedScope
│   ├── DeserializedMemberScope                                │   ├── LexicalScopeStorage
│   │   ├── DeserializedEnumMemberScope                        │   ├── Snapshot
│   │   ├── DeserializedClassMemberScope                       │   ├── LexicalScopeImpl
│   │   ├── DeserializedExtendMemberScope                      │   ├── DeprecatedLexicalScope
│   │   └── DeserializedPackageMemberScope                     │   ├── LexicalScopeWrapper
│   ├── PackageReexportScope                                   │   └── ErrorLexicalScope
│   ├── AbstractLazyMemberScope                                │
│   ├── StaticScopeForCangJieEnum                              └── ImportingScope (导入作用域)
│   ├── InnerClassesScopeWrapper                                   ├── Empty
│   ├── EnumEntriesScope                                           ├── BaseImportingScope
│   ├── DynamicDescriptorScope                                     ├── LazyImportScope
│   ├── BasicTypesMemberScope                                      ├── lazyImportingScope
│   ├── BuiltinsTypesMemberScope                                   ├── CurrentFileScope
│   ├── GlobalSyntheticPackageViewScope                            ├── CurrentPackageScope
│   ├── DeprecatedMemberScope                                      ├── SubpackagesImportingScope
│   └── ExtensionsScope                                            ├── CompositePrioritizedImportingScope
│                                                                  ├── MemberScopeToImportingScopeAdapter
├── SubstitutingScope (装饰器)                                     └── withParent
├── InstanceMemberScope
├── StaticMemberScope
├── PrimitiveMemberScope
├── ErrorScope
│   └── ThrowingScope
├── BuiltInsMemberScope
├── AbstractScopeAdapter
│   ├── TypeIntersectionScope
│   └── LazyScopeAdapter
├── ChainedMemberScope (组合)
├── DeprecatedMemberScope
└── (其他实现)
```

---

## 作用域层次与使用场景

### 1. 代码块级别 (LexicalScope)

**作用域链示例**:
```
function test() {
    let x = 1         // FUNCTION_INNER_SCOPE
    {
        let y = 2     // CODE_BLOCK
        {
            let z = 3 // CODE_BLOCK (嵌套)
        }
    }
}
```

**作用域链**:
```
CODE_BLOCK (z)
  ↓ parent
CODE_BLOCK (y)
  ↓ parent
FUNCTION_INNER_SCOPE (x, 函数参数)
  ↓ parent
FUNCTION_HEADER (类型参数)
  ↓ parent
CLASS_MEMBER_SCOPE (类成员)
  ↓ parent
...
```

### 2. 类成员级别 (MemberScope)

**示例**:
```cangjie
class Parent<T> {
    func parentMethod(item: T): T { ... }
}

class Child : Parent<String> {
    func childMethod() { ... }
}
```

**Child 的成员作用域**:
```
ChainedMemberScope("Child with Parent") {
    ├── InstanceMemberScope(Child 的直接成员)
    └── SubstitutingScope(
            Parent 的成员作用域,
            substitutor: T → String
        )
}
```

### 3. 包和导入级别 (ImportingScope)

**示例**:
```cangjie
package com.example.myapp

import std.collection.*
import std.math.{sin, cos}
import mylib.Helper

// 代码...
```

**导入作用域链**:
```
CompositePrioritizedImportingScope {
    primary: LazyImportScope(显式导入: sin, cos, Helper)
    secondary: LazyImportScope(通配符导入: collection.*)
}
  ↓ parent
CurrentPackageScope(com.example.myapp)
  ↓ parent
RootPackageScope
  ↓ parent
BuiltInsImportingScope(内置符号)
```

### 4. 类型系统级别

**泛型实例化**:
```cangjie
class Box<T> {
    var value: T
    func get(): T = value
}

let box: Box<Int> = Box(42)
```

**box 的类型作用域**:
```
SubstitutingScope(
    workerScope: Box 的原始成员作用域,
    substitutor: T → Int
)

// box.get() 返回 Int 而非 T
// box.value 的类型是 Int 而非 T
```

### 5. 错误恢复级别

**示例**:
```cangjie
let x = unknownFunction()  // 错误: 未定义
let y = x.someMethod()     // 应该继续分析,不报级联错误
```

**错误恢复流程**:
```
1. unknownFunction 查找失败
   ↓
2. 创建 ErrorType,其 memberScope 是 ErrorScope
   ↓
3. x.someMethod() 在 ErrorScope 中查找
   ↓
4. 返回 ErrorFunctionDescriptor
   ↓
5. 不报告 y 的类型错误(因为 x 已经是错误)
```

---

## 设计模式分析

### 1. 模板方法模式 (Template Method)

**应用**: `MemberScopeImpl`

```kotlin
abstract class MemberScopeImpl : MemberScope {
    // 提供默认实现
    override fun getContributedFunctions(...) = emptyList()
    override fun getContributedVariables(...) = emptyList()

    // 子类选择性覆盖
    // override fun getContributedFunctions(...) = actualFunctions
}
```

**优点**:
- 简化子类实现
- 提供一致的默认行为
- 只需覆盖必要的方法

### 2. 装饰器模式 (Decorator)

**应用**: `SubstitutingScope`

```kotlin
class SubstitutingScope(
    private val workerScope: MemberScope,
    givenSubstitutor: TypeSubstitutor
) : MemberScope {
    // 透传查找,但对结果应用类型替换
    override fun getContributedFunctions(name, location) =
        substitute(workerScope.getContributedFunctions(name, location))
}
```

**优点**:
- 不修改原作用域
- 可以链式装饰
- 职责单一

### 3. 组合模式 (Composite)

**应用**: `ChainedMemberScope`

```kotlin
class ChainedMemberScope(
    private val debugName: String,
    private val scopes: Array<out MemberScope>
) : MemberScope {
    // 聚合多个作用域的查找结果
    override fun getContributedFunctions(name, location) =
        scopes.flatMap { it.getContributedFunctions(name, location) }
}
```

**优点**:
- 统一处理单个和多个作用域
- 自动扁平化优化
- 支持任意层次组合

### 4. 单例模式 (Singleton)

**应用**: `MemberScope.Empty`, `ImportingScope.Empty`

```kotlin
object Empty : MemberScopeImpl() {
    override fun definitelyDoesNotContainName(name: Name) = true
    // 所有查找返回空结果
}
```

**优点**:
- 避免重复创建空对象
- 全局唯一实例
- 快速比较(===)

### 5. 适配器模式 (Adapter)

**应用**: `MemberScopeToImportingScopeAdapter`

```kotlin
// 将 MemberScope 适配为 ImportingScope
class MemberScopeToImportingScopeAdapter(
    private val memberScope: MemberScope
) : ImportingScope {
    // 桥接两种接口
}
```

**优点**:
- 接口转换
- 重用现有实现
- 降低耦合

### 6. 策略模式 (Strategy)

**应用**: 作用域查找的 `DescriptorKindFilter` 和 `nameFilter`

```kotlin
fun getContributedDescriptors(
    kindFilter: DescriptorKindFilter = DescriptorKindFilter.ALL,
    nameFilter: (Name) -> Boolean = MemberScope.ALL_NAME_FILTER
): Collection<DeclarationDescriptor>

// 不同的过滤策略
val functions = scope.getContributedDescriptors(
    kindFilter = DescriptorKindFilter.FUNCTIONS,
    nameFilter = { it.asString().startsWith("test") }
)
```

**优点**:
- 灵活的过滤逻辑
- 运行时切换策略
- 代码复用

---

## 性能优化策略

### 1. 惰性计算 (Lazy Evaluation)

**实现**:
```kotlin
// AbstractLazyMemberScope
abstract class AbstractLazyMemberScope : MemberScopeImpl() {
    private val _functions by lazy { computeFunctions() }

    override fun getContributedFunctions(...) = _functions.filter { ... }
}
```

**优点**:
- 延迟构建作用域内容
- 仅在实际访问时计算
- 自动缓存结果

### 2. 名称集合的超集语义

**实现**:
```kotlin
override val functionNames: Set<Name>
    get() = stubIndex.getAllFunctionNames() // 可能包含不存在的名称

override fun getContributedFunctions(name: Name, location: LookupLocation) =
    stubIndex.getFunctions(name).filter { ... } // 实际验证
```

**优点**:
- 避免完整扫描作用域
- 使用廉价的近似计算
- 实际验证延迟到查找时

### 3. 快速失败优化

**实现**:
```kotlin
override fun definitelyDoesNotContainName(name: Name): Boolean {
    // 使用 Bloom Filter 或名称集合快速判断
    return !possibleNames.contains(name)
}
```

**优点**:
- 快速排除明显不存在的名称
- 避免不必要的深度查找
- O(1) 时间复杂度

### 4. 作用域扁平化

**实现**:
```kotlin
fun create(debugName: String, scopes: Iterable<MemberScope>): MemberScope {
    val flattenedScopes = SmartList<MemberScope>()
    for (scope in scopes) {
        when {
            scope === MemberScope.Empty -> {} // 过滤空作用域
            scope is ChainedMemberScope -> {
                // 展开嵌套的链式作用域
                flattenedScopes.addAll(scope.scopes)
            }
            else -> flattenedScopes.add(scope)
        }
    }
    return when (flattenedScopes.size) {
        0 -> MemberScope.Empty
        1 -> flattenedScopes[0]
        else -> ChainedMemberScope(debugName, flattenedScopes)
    }
}
```

**优点**:
- 减少嵌套层级
- 提升查找效率
- 避免冗余包装

### 5. 缓存机制

**实现**:
```kotlin
class SubstitutingScope(...) : MemberScope {
    private var substitutedDescriptors: MutableMap<...>? = null

    private fun <D> substitute(descriptor: D): D {
        if (capturingSubstitutor.isEmpty) return descriptor

        if (substitutedDescriptors == null) {
            substitutedDescriptors = HashMap()
        }

        return substitutedDescriptors!!.getOrPut(descriptor) {
            descriptor.substitute(capturingSubstitutor)
        } as D
    }
}
```

**优点**:
- 避免重复替换同一描述符
- 降低计算开销
- 惰性初始化缓存

### 6. Stub 索引集成

**实现**:
```kotlin
// 使用 Stub 索引加速包级声明查找
override fun getContributedFunctions(name: Name, location: LookupLocation) =
    CangJieFunctionShortNameIndex.getInstance()
        .get(name.asString(), project, packageScope)
        .map { it.resolveToDescriptor() }
```

**优点**:
- 避免完整解析大文件
- 使用持久化索引
- O(1) 平均查找时间

---

## 总结

### 核心设计原则

1. **接口隔离**: ResolutionScope, MemberScope, HierarchicalScope 各司其职
2. **组合优先**: ChainedMemberScope, CompositePrioritizedImportingScope 体现组合思想
3. **装饰增强**: SubstitutingScope, AbstractScopeAdapter 使用装饰器模式
4. **惰性优化**: 延迟构建作用域内容,提高性能
5. **错误恢复**: ErrorScope 保证编译器和 IDE 在错误情况下的健壮性

### 扩展建议

**添加新的作用域实现时**:

1. 确定继承基类:
   - 普通成员作用域 → 继承 `MemberScopeImpl`
   - 词法作用域 → 实现 `LexicalScope`
   - 导入作用域 → 继承 `BaseImportingScope`

2. 覆盖必要方法:
   - 至少实现 `printScopeStructure()` 用于调试
   - 覆盖实际支持的查找方法
   - 优化名称集合属性以提高性能

3. 考虑性能优化:
   - 实现 `definitelyDoesNotContainName()` 快速排除
   - 使用惰性计算和缓存
   - 集成 Stub 索引(如果适用)

4. 测试要点:
   - 单元测试各类符号查找
   - 性能测试大型作用域
   - 错误恢复测试

### 相关文档

- [DescriptorKindFilter.kt](../descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/DescriptorKindFilter.kt) - 描述符类型过滤器
- [LookupLocation.kt](../descriptors/src/main/kotlin/org/cangnova/cangjie/incremental/components/LookupLocation.kt) - 查找位置(增量编译支持)
- [TypeSubstitutor.kt](../descriptors/src/main/kotlin/org/cangnova/cangjie/types/TypeSubstitutor.kt) - 类型替换器

---

 
