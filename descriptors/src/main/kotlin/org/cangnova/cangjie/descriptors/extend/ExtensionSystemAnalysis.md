# 仓颉语言扩展系统实现分析

## 概述

仓颉语言的扩展系统允许为现有类型添加新功能而不修改原始类型定义。扩展分为两类：
1. **直接扩展** - 不实现接口，仅添加成员
2. **接口扩展** - 为类型实现新接口并添加成员

## 核心架构

### 1. PSI 层 - 语法表示

#### CjExtend (psi/src/main/kotlin/org/cangnova/cangjie/psi/CjExtend.kt)

扩展声明的 PSI 元素，继承自 `CjTypeStatement`。

**关键属性**：
- `receiverTypeReceiver: CjTypeReference?` - 被扩展的类型引用
- `superTypeListEntries` - 扩展实现的接口列表（继承自 CjTypeStatement）
- `name: String?` - 扩展名称（基于被扩展类型）

**扩展标识生成**：
```kotlin
fun getExtendId(): String {
    // 通过组合以下信息生成唯一ID：
    // - 被扩展类型名称
    // - 实现的接口列表
    // - 包名（fqName）
    // - 源码位置（textOffset, textRange, text）
}
```

**名称解析逻辑**：
```kotlin
private fun getExtendName(): String? {
    return when (val type = receiverTypeReceiver?.typeElement) {
        is CjUserType -> type.referencedName
        is CjOptionType -> "Option"
        is CjBasicType -> type.name
        else -> null
    }
}
```

#### 语法解析 (psi/src/main/kotlin/org/cangnova/cangjie/parsing/CangJieParsing.kt)

**扩展声明语法**：
```cangjie
extend [<T1, T2, ...>] Type [<: Interface1 & Interface2>] [where constraints] {
    // 成员声明
}
```

**解析流程** (parseClass 函数)：
```kotlin
// Line 2801-2806: 扩展特殊处理
if (token == EXTEND_KEYWORD) {
    // 1. 解析泛型参数 <T>
    if (at(LT)) {
        parseTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET)
        typeParametersDeclared = true
    }
    // 2. 解析被扩展类型
    parseTypeRef()
}
```

**PSI 节点类型**：
- Line 2843: `EXTEND_KEYWORD_Id -> EXTEND` (节点类型映射)
- Line 2179: 注册 `ClassParser()` 处理 extend 关键字

### 2. 描述符层 - 语义表示

#### ExtendDescriptor (descriptors/src/main/kotlin/org/cangnova/cangjie/descriptors/extend/ExtendDescriptor.kt)

扩展的语义描述符接口，核心设计：

**继承关系**：
```kotlin
interface ExtendDescriptor : InheritableDescriptor, HasScopeDescriptor
```

**设计原则**：
- **不是 ClassifierDescriptor**: 扩展不产生新类型
- **是 InheritableDescriptor**: 可以实现接口
- **是 HasScopeDescriptor**: 包含成员作用域

**核心属性**：
```kotlin
interface ExtendDescriptor {
    val extendId: String                        // 扩展唯一标识
    val declaredTypeParameters: List<...>       // 泛型参数列表
    val extendTypeConstructor: TypeConstructor  // 被扩展类型构造器
    val extendType: CangJieType                 // 被扩展类型
    val superTypes: Collection<CangJieType>     // 实现的接口列表
    val unsubstitutedMemberScope: MemberScope   // 原始成员作用域
    val staticScope: MemberScope                // 静态成员作用域
    val kind: ClassKind get() = ClassKind.EXTEND // 固定为 EXTEND
    val modality: Modality get() = Modality.FINAL // 固定为 FINAL
}
```

**作用域计算**：
```kotlin
// 支持泛型实例化后的作用域
fun getMemberScope(typeArguments: List<TypeProjection>): MemberScope
fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope

val memberScope: MemberScope
    get() {
        if (declaredTypeParameters.isEmpty()) return unsubstitutedMemberScope
        // 使用默认类型参数实例化
        return getMemberScope(declaredTypeParameters.map {
            TypeProjectionImpl(it.defaultType)
        })
    }
```

#### AbstractExtendDescriptor (descriptors/src/main/kotlin/org/cangnova/cangjie/descriptors/extend/AbstractExtendDescriptor.kt)

抽象基类实现，提供通用逻辑：

**类型替换实现**：
```kotlin
override fun getMemberScope(typeArguments: List<TypeProjection>): MemberScope {
    // 1. 验证类型参数数量
    assert(typeArguments.size == declaredTypeParameters.size)

    // 2. 无泛型参数时直接返回原始作用域
    if (typeArguments.isEmpty()) return unsubstitutedMemberScope

    // 3. 构建替换器并创建替换作用域
    val substitutor = TypeConstructorSubstitution
        .create(extendType.constructor, typeArguments)
        .buildSubstitutor()
    return SubstitutingScope(unsubstitutedMemberScope, substitutor)
}
```

**默认属性**：
```kotlin
override val staticScope: MemberScope = MemberScope.Empty
override val visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC
override val original: ExtendDescriptor = this
```

#### DeserializedExtendDescriptor (descriptors/deserialization/...)

从编译后元数据反序列化的扩展描述符。

**内部作用域** (DeserializedExtendMemberScope)：
继承自 `DeserializedMemberScope`，实现：
1. 从元数据加载成员（函数、变量、属性）
2. 从接口继承成员（fake overrides）
3. 成员可见性过滤

**关键方法**：
```kotlin
// 计算非声明函数（从接口继承）
override fun computeNonDeclaredFunctions(name: Name, functions: MutableList<...>) {
    // 1. 收集所有父接口的函数
    val fromSupertypes = ArrayList<SimpleFunctionDescriptor>()
    for (supertype in refinedSupertypes()) {
        fromSupertypes.addAll(supertype.memberScope.getContributedFunctions(name, ...))
    }

    // 2. 生成 fake overrides
    generateFakeOverrides(name, fromSupertypes, functions)
}

// 生成 fake override
private fun <D : CallableMemberDescriptor> generateFakeOverrides(
    name: Name,
    fromSupertypes: Collection<D>,
    result: MutableList<D>
) {
    c.components.cangjieTypeChecker.overridingUtil.generateOverridesInFunctionGroup(
        name, fromSupertypes, fromCurrent, extendDescriptor,
        object : NonReportingOverrideStrategy() {
            override fun addFakeOverride(fakeOverride: CallableMemberDescriptor) {
                OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, null)
                result.add(fakeOverride as D)
            }
        }
    )
}
```

### 3. 扩展管理器 - ExtendManager

#### ExtendManager 接口 (descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/extend/ExtendManager.kt)

模块级别的扩展定义管理器，作为 `ModuleCapability` 提供。

**核心数据结构**：
```kotlin
data class ExtensionDef(
    val id: String,                           // 扩展唯一标识
    val extendedConstructor: TypeConstructor, // 被扩展类型构造器
    val typeParameters: List<TypeParameterDescriptor>, // 泛型参数
    val interfaces: List<CangJieType>,        // 实现的接口列表
)
```

**核心功能**：
```kotlin
// 获取某个类型构造器的扩展接口
fun getExtendSupertypes(
    forConstructor: TypeConstructor,    // 被扩展的类型构造器
    forTypeArgs: List<CangJieType> = emptyList(), // 类型参数
    excludeExtendId: String? = null,    // 排除特定扩展（避免循环）
): Collection<CangJieType>

// 重建扩展定义表
fun rebuild(defs: Collection<ExtensionDef>)

// 使缓存失效
fun invalidate()
```

#### ExtendManagerImpl (descriptors/src/main/kotlin/org/cangnova/cangjie/resolve/extend/ExtendManagerImpl.kt)

**实现策略**：

1. **扩展定义存储**：
```kotlin
// 按类型构造器索引扩展定义
private val defsByCtor = mutableMapOf<TypeConstructor, MutableSet<ExtensionDef>>()
```

2. **缓存机制**：
```kotlin
private data class Key(
    val ctor: TypeConstructor,
    val argsKey: List<CangJieType>,
    val excludeId: String?,
)
private val cache = mutableMapOf<Key, Collection<CangJieType>>()
```

3. **类型实参替换**：
```kotlin
override fun getExtendSupertypes(
    forConstructor: TypeConstructor,
    forTypeArgs: List<CangJieType>,
    excludeExtendId: String?,
): Collection<CangJieType> {
    // 1. 检查缓存
    val key = Key(forConstructor, forTypeArgs, excludeExtendId)
    cache[key]?.let { return it }

    // 2. 查找该类型的所有扩展
    val defs = defsByCtor[forConstructor].orEmpty()
    if (defs.isEmpty()) return emptyList()

    // 3. 为每个扩展应用类型参数替换
    val result = buildList {
        defs.forEach { def ->
            if (excludeExtendId != null && def.id == excludeExtendId) return@forEach

            // 构建替换器：扩展的类型参数 -> 实际类型参数
            val substitutor = buildSubstitutor(def.typeParameters, forTypeArgs)

            // 替换接口中的类型参数
            def.interfaces.forEach { iface ->
                val applied = substitutor.substitute(iface, Variance.INVARIANT)
                if (applied != null) add(applied)
            }
        }
    }.distinct()

    // 4. 缓存结果
    cache[key] = result
    return result
}
```

### 4. 扩展解析器 - ExtendDescriptorResolver

#### ExtendDescriptorResolver (analysis/src/main/kotlin/org/cangnova/cangjie/resolve/ExtendDescriptorResolver.kt)

**当前状态**: 仅有框架代码，核心方法未实现

```kotlin
class ExtendDescriptorResolver(...) {
    fun getExtendDescriptor(cjExtend: CjExtend): ClassDescriptorWithResolutionScopes {
        TODO() // 未实现
    }
}
```

**预期职责**：
1. 从 `CjExtend` PSI 创建 `ExtendDescriptor`
2. 解析被扩展类型
3. 解析实现的接口列表
4. 解析泛型约束
5. 创建成员作用域
6. 注册到 `ExtendManager`

## 实现状态分析

### 已实现功能

#### ✅ PSI 层
- [x] `CjExtend` PSI 元素定义
- [x] 扩展声明语法解析
- [x] Stub 索引支持（`CangJieExtendStub`）
- [x] 扩展 ID 生成逻辑
- [x] 被扩展类型和接口列表解析

#### ✅ 描述符层
- [x] `ExtendDescriptor` 接口设计
- [x] `AbstractExtendDescriptor` 抽象实现
- [x] `DeserializedExtendDescriptor` 反序列化支持
- [x] 成员作用域类型替换
- [x] Fake overrides 生成

#### ✅ 扩展管理
- [x] `ExtendManager` 接口定义
- [x] `ExtendManagerImpl` 实现
- [x] 扩展定义索引
- [x] 类型参数替换缓存

### 未实现功能

#### ❌ 源码扩展解析
- [ ] `ExtendDescriptorResolver.getExtendDescriptor()` 实现
- [ ] PSI -> Descriptor 转换逻辑
- [ ] 扩展成员解析
- [ ] 扩展定义注册到 ExtendManager

#### ❌ 扩展可见性规则
- [ ] 孤儿规则检查（orphan rule）
- [ ] 导出规则检查
- [ ] 访问修饰符验证

#### ❌ 扩展成员解析
- [ ] 扩展成员在类型作用域中的可见性
- [ ] 扩展函数调用解析
- [ ] this/super 上下文处理
- [ ] private 成员访问检查

#### ❌ 泛型约束
- [ ] where 子句解析
- [ ] 额外约束检查
- [ ] 约束可见性规则

## 与语言规范的对比

### 完全符合规范的部分

1. **扩展不是分类器**
   - `ExtendDescriptor` 未实现 `ClassifierDescriptor`
   - 扩展不产生新类型 ✅

2. **扩展支持泛型**
   - `declaredTypeParameters` 属性
   - 类型替换机制完整 ✅

3. **扩展实现接口**
   - `superTypes` 属性
   - fake overrides 生成 ✅

4. **扩展具有作用域**
   - `unsubstitutedMemberScope` 属性
   - 成员解析机制 ✅

### 需要完善的部分

1. **孤儿规则 (Orphan Rule)**
   - 规范：扩展必须与被扩展类型或接口在同一包
   - 实现：❌ 未找到检查代码

2. **导出规则**
   - 规范：直接扩展导出取决于被扩展类型和泛型约束
   - 规范：接口扩展导出取决于接口和约束
   - 实现：❌ 未找到完整实现

3. **访问规则**
   - 规范：扩展不能访问 private 成员
   - 规范：private 扩展成员仅在扩展内可见
   - 实现：❌ 未找到检查代码

4. **扩展成员遮盖检查**
   - 规范：扩展不能遮盖类型的任何成员
   - 规范：扩展不能遮盖其他扩展的成员
   - 实现：❌ 未找到检查代码

5. **扩展可见性**
   - 规范：泛型约束影响扩展间的可见性
   - 规范：约束更宽松的扩展对约束更严格的扩展可见
   - 实现：❌ 未找到实现

## 关键问题

### 1. ExtendDescriptorResolver 未实现

**问题**: `getExtendDescriptor()` 返回 `TODO()`，无法从源码创建扩展描述符。

**影响**：
- 无法解析用户编写的扩展声明
- 扩展成员无法在 IDE 中使用
- 代码补全不支持扩展成员

**建议实现**：
```kotlin
fun getExtendDescriptor(cjExtend: CjExtend): ClassDescriptorWithResolutionScopes {
    // 1. 解析被扩展类型
    val extendedType = typeResolver.resolveType(
        cjExtend.receiverTypeReceiver,
        scope,
        trace,
        checkBounds = true
    )

    // 2. 解析泛型参数
    val typeParameters = cjExtend.typeParameters?.let {
        descriptorResolver.resolveTypeParametersForCallable(...)
    } ?: emptyList()

    // 3. 解析实现的接口
    val superTypes = cjExtend.superTypeListEntries.map {
        typeResolver.resolveType(it.typeReference, ...)
    }

    // 4. 创建扩展描述符
    val descriptor = LazyExtendDescriptor(
        extendType = extendedType,
        superTypes = superTypes,
        declaredTypeParameters = typeParameters,
        extendId = cjExtend.getExtendId(),
        containingDeclaration = getContainingDeclaration(cjExtend),
        storageManager = storageManager,
        declarationProvider = PsiBasedExtendMemberDeclarationProvider(cjExtend)
    )

    // 5. 注册到 ExtendManager
    module.getCapability(ExtendManager.CAPABILITY)?.let { manager ->
        manager.rebuild(listOf(
            ExtendManager.ExtensionDef(
                id = descriptor.extendId,
                extendedConstructor = extendedType.constructor,
                typeParameters = typeParameters,
                interfaces = superTypes
            )
        ))
    }

    return descriptor
}
```

### 2. 扩展成员作用域缺失

**问题**: `ExtendDescriptorImpl.unsubstitutedMemberScope` 返回 `TODO()`。

**影响**：
- 扩展成员无法被查找
- 无法实现代码补全
- 无法进行类型检查

**建议**: 创建 `LazyExtendMemberScope`，类似 `LazyClassMemberScope`。

### 3. ExtendManager 未集成到类型系统

**问题**: 类型构造器未查询扩展接口。

**影响**：
- 扩展的接口不会被类型检查器识别
- 无法将扩展类型赋值给接口类型
- instanceof 检查失败

**建议**: 在 `AbstractTypeConstructor.getSupertypes()` 中查询 ExtendManager：
```kotlin
override fun computeSupertypes(): Collection<CangJieType> {
    val declared = declaringDescriptor.superTypes
    val fromExtends = module.getCapability(ExtendManager.CAPABILITY)
        ?.getExtendSupertypes(this, emptyList()) ?: emptyList()
    return declared + fromExtends
}
```

### 4. 缺少诊断错误

需要添加以下错误检查：
- `ORPHAN_EXTEND` - 违反孤儿规则
- `EXTEND_SHADOWS_MEMBER` - 扩展遮盖成员
- `EXTEND_ACCESSES_PRIVATE` - 扩展访问 private 成员
- `CONFLICTING_EXTEND_DEFINITIONS` - 冲突的扩展定义
- `EXTEND_MODALITY_ERROR` - 扩展使用了 open/override/redef

## 实现路线图

### 阶段 1: 基础扩展解析（核心功能）

1. **实现 LazyExtendDescriptor**
   - 创建类似 `LazyClassDescriptor` 的实现
   - 惰性解析成员
   - 支持泛型参数

2. **实现 ExtendDescriptorResolver**
   - PSI -> Descriptor 转换
   - 类型解析
   - 成员作用域创建

3. **集成到类型系统**
   - 修改 `AbstractTypeConstructor`
   - 类型检查支持扩展接口
   - 实例化检查

### 阶段 2: 扩展成员解析

1. **ExtendMemberScope 实现**
   - 成员查找
   - 名称解析
   - 可见性过滤

2. **扩展函数调用解析**
   - 成员访问表达式
   - this 上下文
   - 重载解析

3. **代码补全支持**
   - 扩展成员补全
   - 类型推导

### 阶段 3: 可见性和规则检查

1. **孤儿规则检查**
   - 包级别检查
   - 错误报告

2. **导出规则检查**
   - 直接扩展导出
   - 接口扩展导出
   - 泛型约束可见性

3. **访问规则检查**
   - private 成员访问
   - 扩展成员遮盖
   - 修饰符验证

### 阶段 4: 高级特性

1. **泛型约束可见性**
   - 约束包含关系计算
   - 扩展间可见性

2. **接口继承冲突**
   - 检查顺序计算
   - 默认实现覆盖

3. **性能优化**
   - ExtendManager 缓存优化
   - 延迟解析优化

## 总结

仓颉语言扩展系统的架构设计**非常清晰且符合语言规范**：

**优点**：
1. ✅ 扩展不是分类器的设计正确
2. ✅ ExtendManager 管理全局扩展定义的架构合理
3. ✅ 类型替换和泛型支持完整
4. ✅ 反序列化机制完善

**主要缺陷**：
1. ❌ **ExtendDescriptorResolver 未实现** - 阻塞源码扩展解析
2. ❌ **ExtendManager 未集成到类型系统** - 扩展接口不生效
3. ❌ **缺少可见性和规则检查** - 无法保证语言规范
4. ❌ **扩展成员作用域不完整** - 影响 IDE 功能

**优先级排序**：
1. **P0**: 实现 ExtendDescriptorResolver（解锁基础功能）
2. **P0**: 集成 ExtendManager 到类型系统（类型检查生效）
3. **P1**: 实现 ExtendMemberScope（成员解析）
4. **P2**: 添加规则检查（孤儿规则、导出规则）
5. **P3**: 高级特性（约束可见性、冲突检测）

当前的实现可以称为**"架构完整但功能未完成"的状态**，需要补充核心解析逻辑才能真正支持扩展系统。
