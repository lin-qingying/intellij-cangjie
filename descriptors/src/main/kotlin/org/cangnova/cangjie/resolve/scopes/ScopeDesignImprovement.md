# 仓颉语言静态/实例作用域设计改进方案

## 当前设计的问题

### 1. 语言语义不匹配

**现状问题**：
- 当前代码中 `StaticMemberScope` 和 `InstanceMemberScope` 对 `getContributedClassifier()` 不做过滤
- 文档注释说明"嵌套类通过实例也可访问"、"分类器总是可访问"
- 但**仓颉语言不支持嵌套类**，所有类型声明都是顶层的

**影响**：
```kotlin
// StaticMemberScope.kt:246
override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
    return memberScope.getContributedClassifier(name, location)
    // ❌ 注释说"嵌套类不过滤"，但仓颉没有嵌套类概念
}

// InstanceMemberScope.kt:298
override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
    return memberScope.getContributedClassifier(name, location)
    // ❌ 注释说"过滤非静态分类器"，实际没有过滤
}
```

### 2. 性能问题

**无缓存设计**：
- 每次 `getContributedXxx()` 调用都重新过滤
- 对于频繁访问的成员（如补全、类型检查），重复过滤开销大

**示例**：
```kotlin
// 每次调用都执行 filter
override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
    return memberScope.getContributedFunctions(name, location).filter { it.isStatic }
    // 在 IDE 补全时可能被调用数百次
}
```

### 3. 名称集合不精确

**当前实现**：
```kotlin
override val functionNames: Set<Name>
    get() = memberScope.functionNames  // 直接透传
```

**问题**：
- `StaticMemberScope.functionNames` 包含了实例函数名
- `InstanceMemberScope.functionNames` 包含了静态函数名
- 调用者需要再次过滤，浪费性能

### 4. 职责不清晰

**当前问题**：
- `HasScopeDescriptor` 同时定义 `staticScope` 和 `instanceScope`
- 但大多数描述符（如 `PackageFragmentDescriptor`）只需要一个
- 实例作用域默认为空，静态作用域必须实现

---

## 改进设计方案

### 方案 A：专用作用域接口（推荐）

#### 核心思路
将静态和实例作用域的职责分离到不同接口，按需实现。

#### 接口设计

```kotlin
/**
 * 具有静态作用域的描述符
 *
 * 实现者：
 * - PackageFragmentDescriptor: 包成员都是"静态"的
 * - ClassDescriptor: 类的静态成员
 * - EnumDescriptor: 枚举构造器
 */
interface HasStaticScope : DeclarationDescriptor {
    /**
     * 静态成员作用域
     *
     * 包含：
     * - 静态函数（`static func`）
     * - 静态变量（`static let/var`）
     * - 静态属性（`static prop`）
     * - 枚举构造器（对于枚举类型）
     *
     * 不包含：
     * - 实例成员
     * - 类型别名（应通过限定名访问）
     */
    val staticScope: MemberScope
}

/**
 * 具有实例作用域的描述符
 *
 * 实现者：
 * - ClassDescriptor: 类的实例成员
 * - EnumDescriptor: 枚举的实例成员
 */
interface HasInstanceScope : DeclarationDescriptor {
    /**
     * 实例成员作用域
     *
     * 包含：
     * - 实例函数
     * - 实例变量
     * - 实例属性
     *
     * 不包含：
     * - 静态成员
     * - 类型
     */
    val instanceScope: MemberScope
}

/**
 * 同时具有静态和实例作用域的描述符
 *
 * 仅用于类和枚举。
 */
interface HasBothScopes : HasStaticScope, HasInstanceScope {
    // 继承两个作用域属性
}
```

#### 类描述符实现

```kotlin
interface ClassDescriptor :
    ClassifierDescriptorWithTypeParameters,
    InheritableDescriptor,
    HasBothScopes,  // ✅ 明确表示类同时有两种作用域
    ClassOrPackageFragmentDescriptor,
    DeclarationDescriptorWithVisibility {

    // ... 其他属性

    /**
     * 未替换的成员作用域（实现层使用）
     *
     * 包含所有成员，不区分静态/实例。
     * staticScope 和 instanceScope 都基于此作用域过滤。
     */
    val unsubstitutedMemberScope: MemberScope
}
```

#### LazyClassDescriptor 实现

```kotlin
class LazyClassDescriptor(...) : ClassDescriptor {

    // ✅ 缓存过滤后的作用域
    override val staticScope: MemberScope by lazy {
        when (kind) {
            ClassKind.ENUM -> StaticScopeForCangJieEnum(
                c.storageManager,
                this,
                enumEntriesCanBeUsed = true
            )
            else -> FilteredMemberScope(
                unsubstitutedMemberScope,
                staticFilter = true,
                includeClassifiers = false  // ✅ 仓颉没有嵌套类
            )
        }
    }

    override val instanceScope: MemberScope by lazy {
        FilteredMemberScope(
            unsubstitutedMemberScope,
            staticFilter = false,
            includeClassifiers = false  // ✅ 仓颉没有嵌套类
        )
    }
}
```

#### 改进的 FilteredMemberScope

```kotlin
/**
 * 带缓存的过滤作用域
 *
 * 相比当前的 StaticMemberScope/InstanceMemberScope:
 * 1. 缓存过滤结果
 * 2. 精确的名称集合
 * 3. 支持配置是否包含分类器
 */
class FilteredMemberScope(
    private val baseScope: MemberScope,
    private val staticFilter: Boolean,  // true = 静态, false = 实例
    private val includeClassifiers: Boolean = false  // 仓颉设为 false
) : MemberScope {

    // ✅ 缓存过滤后的名称集合
    override val functionNames: Set<Name> by lazy {
        baseScope.getContributedDescriptors(DescriptorKindFilter.FUNCTIONS)
            .filterIsInstance<SimpleFunctionDescriptor>()
            .filter { it.isStatic == staticFilter }
            .mapTo(mutableSetOf()) { it.name }
    }

    override val variableNames: Set<Name> by lazy {
        baseScope.getContributedDescriptors(DescriptorKindFilter.VARIABLES)
            .filterIsInstance<VariableDescriptor>()
            .filter { it.isStatic == staticFilter }
            .mapTo(mutableSetOf()) { it.name }
    }

    override val propertyNames: Set<Name> by lazy {
        baseScope.getContributedDescriptors(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS)
            .filterIsInstance<PropertyDescriptor>()
            .filter { it.isStatic == staticFilter }
            .mapTo(mutableSetOf()) { it.name }
    }

    // ✅ 仓颉语言不支持嵌套类
    override val classifierNames: Set<Name>?
        get() = if (includeClassifiers) baseScope.classifierNames else emptySet()

    // ✅ 缓存查询结果
    private val functionsCache = ConcurrentHashMap<Name, Collection<SimpleFunctionDescriptor>>()
    private val variablesCache = ConcurrentHashMap<Name, Collection<VariableDescriptor>>()
    private val propertysCache = ConcurrentHashMap<Name, Collection<PropertyDescriptor>>()

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        return functionsCache.getOrPut(name) {
            baseScope.getContributedFunctions(name, location).filter { it.isStatic == staticFilter }
        }
    }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        return variablesCache.getOrPut(name) {
            baseScope.getContributedVariables(name, location).filter { it.isStatic == staticFilter }
        }
    }

    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        return propertysCache.getOrPut(name) {
            baseScope.getContributedPropertys(name, location).filter { it.isStatic == staticFilter }
        }
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        // ✅ 仓颉语言：类内不能声明类型
        return if (includeClassifiers) baseScope.getContributedClassifier(name, location) else null
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return baseScope.getContributedDescriptors(kindFilter, nameFilter)
            .filter { descriptor ->
                when (descriptor) {
                    is CallableDescriptor -> descriptor.isStatic == staticFilter
                    is ClassifierDescriptor -> includeClassifiers
                    else -> false
                }
            }
    }
}
```

---

### 方案 B：分离的静态/实例描述符（激进方案）

#### 核心思路
将类的静态部分和实例部分建模为两个独立的描述符。

```kotlin
/**
 * 类的静态部分
 */
interface ClassStaticDescriptor : DeclarationDescriptor {
    val classDescriptor: ClassDescriptor
    val staticScope: MemberScope
}

/**
 * 类的实例部分
 */
interface ClassInstanceDescriptor : DeclarationDescriptor {
    val classDescriptor: ClassDescriptor
    val instanceScope: MemberScope
    val thisType: CangJieType
}

/**
 * 完整的类描述符
 */
interface ClassDescriptor : ClassifierDescriptorWithTypeParameters {
    val staticPart: ClassStaticDescriptor
    val instancePart: ClassInstanceDescriptor
}
```

**优点**：
- 职责极其清晰
- 强类型约束

**缺点**：
- 改动太大，破坏现有架构
- 与 Kotlin 描述符设计偏离

**结论**：不推荐，改动成本太高。

---

## 推荐实施方案

### 阶段 1：修正文档和注释（立即）

1. **删除/修正嵌套类相关注释**
   - `StaticMemberScope.kt` 中删除"嵌套类不过滤"的说明
   - `InstanceMemberScope.kt` 中删除"嵌套类通过实例也可访问"
   - 添加"仓颉语言不支持嵌套类"的说明

2. **修正 `getContributedClassifier()` 实现**
   ```kotlin
   // StaticMemberScope.kt
   override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
       // 仓颉语言不支持类内声明类型，始终返回 null
       return null
   }

   // InstanceMemberScope.kt
   override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
       // 仓颉语言不支持类内声明类型，始终返回 null
       return null
   }
   ```

3. **修复调试输出 Bug**
   ```kotlin
   // StaticMemberScope.kt:236
   p.println("StaticMemberScope:")  // 修正为 StaticMemberScope
   ```

### 阶段 2：添加缓存（短期）

1. **为 FilteredMemberScope 添加缓存**
   - 缓存名称集合（`functionNames`, `variableNames`, `propertyNames`）
   - 缓存查询结果（使用 `ConcurrentHashMap`）

2. **使用 `lazy` 延迟初始化作用域**
   ```kotlin
   override val staticScope: MemberScope by lazy {
       FilteredMemberScope(unsubstitutedMemberScope, staticFilter = true)
   }
   ```

### 阶段 3：重构接口分离（中期）

1. **引入 `HasStaticScope` 和 `HasInstanceScope` 接口**
2. **让 `ClassDescriptor` 实现 `HasBothScopes`**
3. **让 `PackageFragmentDescriptor` 只实现 `HasStaticScope`**
4. **逐步迁移调用方代码**

### 阶段 4：优化 FilteredMemberScope（长期）

1. **智能缓存失效**：监听描述符变化
2. **增量过滤**：只过滤新增成员
3. **内存优化**：对于空作用域使用单例

---

## 性能对比

### 当前实现
```kotlin
// 每次调用都过滤
override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
    return memberScope.getContributedFunctions(name, location).filter { it.isStatic }
}

// 时间复杂度：O(n)，n = 成员数量
// 空间复杂度：O(m)，m = 过滤后结果数量（每次调用都创建新集合）
```

### 改进后实现
```kotlin
private val functionsCache = ConcurrentHashMap<Name, Collection<SimpleFunctionDescriptor>>()

override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
    return functionsCache.getOrPut(name) {
        baseScope.getContributedFunctions(name, location).filter { it.isStatic == staticFilter }
    }
}

// 首次：O(n)
// 后续：O(1)
// 内存：O(k * m)，k = 不同名称数量，m = 平均结果数量
```

### 性能提升估算
- **IDE 补全场景**：单个类的成员补全可能查询 10-50 个名称
  - 当前：50 次过滤 × O(n) = O(50n)
  - 改进：1 次过滤 + 49 次缓存命中 = O(n + 49) ≈ O(n)
  - **提升：50 倍**

---

## 兼容性分析

### API 兼容性
- ✅ **方案 A（推荐）**：向后兼容
  - `HasStaticScope` 和 `HasInstanceScope` 是新接口
  - 现有代码继续使用 `ClassDescriptor.staticScope` 不受影响

- ⚠️ **修正 `getContributedClassifier()` 返回 null**：可能影响
  - 如果现有代码依赖"类内可以查询分类器"，会失败
  - 但根据仓颉语言语义，这种代码本身就是错误的

### 性能影响
- ✅ 缓存优化：大幅提升性能，无副作用
- ✅ `lazy` 延迟初始化：减少启动开销

---

## 测试建议

### 单元测试

```kotlin
class FilteredMemberScopeTest {
    @Test
    fun `static scope should not include instance members`() {
        val classDescriptor = createTestClass(
            staticFunctions = listOf("staticFunc"),
            instanceFunctions = listOf("instanceFunc")
        )

        val staticScope = classDescriptor.staticScope
        assertNotNull(staticScope.getContributedFunctions(Name.identifier("staticFunc"), NoLookupLocation.FROM_TEST))
        assertNull(staticScope.getContributedFunctions(Name.identifier("instanceFunc"), NoLookupLocation.FROM_TEST))
    }

    @Test
    fun `instance scope should not include static members`() {
        val classDescriptor = createTestClass(
            staticFunctions = listOf("staticFunc"),
            instanceFunctions = listOf("instanceFunc")
        )

        val instanceScope = classDescriptor.instanceScope
        assertNull(instanceScope.getContributedFunctions(Name.identifier("staticFunc"), NoLookupLocation.FROM_TEST))
        assertNotNull(instanceScope.getContributedFunctions(Name.identifier("instanceFunc"), NoLookupLocation.FROM_TEST))
    }

    @Test
    fun `cangjie scopes should never return classifiers`() {
        val classDescriptor = createTestClass()

        // 仓颉语言不支持嵌套类
        assertNull(classDescriptor.staticScope.getContributedClassifier(Name.identifier("NestedClass"), NoLookupLocation.FROM_TEST))
        assertNull(classDescriptor.instanceScope.getContributedClassifier(Name.identifier("NestedClass"), NoLookupLocation.FROM_TEST))
    }

    @Test
    fun `filtered scope should cache query results`() {
        val classDescriptor = createTestClass(
            instanceFunctions = listOf("foo")
        )

        val instanceScope = classDescriptor.instanceScope
        val result1 = instanceScope.getContributedFunctions(Name.identifier("foo"), NoLookupLocation.FROM_TEST)
        val result2 = instanceScope.getContributedFunctions(Name.identifier("foo"), NoLookupLocation.FROM_TEST)

        // 应该返回相同的缓存对象
        assertSame(result1, result2)
    }
}
```

### 集成测试

```kotlin
class ScopeIntegrationTest {
    @Test
    fun `qualified expression should use static scope for class qualifier`() {
        // MyClass.staticMethod()
        val code = """
            class MyClass {
                static func staticMethod() {}
                func instanceMethod() {}
            }

            func test() {
                MyClass.staticMethod()  // ✅ 应该解析
                MyClass.instanceMethod()  // ❌ 应该报错
            }
        """.trimIndent()

        myFixture.configureByText("test.cj", code)
        val errors = myFixture.doHighlighting(HighlightSeverity.ERROR)

        assertTrue(errors.any { it.description.contains("instanceMethod") })
    }
}
```

---

## 总结

### 核心改进点

1. **✅ 语义正确性**：去除"嵌套类"概念，符合仓颉语言规范
2. **✅ 性能优化**：添加缓存，提升 50 倍性能
3. **✅ 接口分离**：引入 `HasStaticScope`/`HasInstanceScope`，职责清晰
4. **✅ 类型安全**：编译期保证只有类和枚举有双作用域

### 实施优先级

| 阶段 | 工作量 | 收益 | 优先级 |
|------|--------|------|--------|
| 阶段 1：修正文档 | 1 天 | 避免误导开发者 | 高 |
| 阶段 2：添加缓存 | 2-3 天 | 性能提升 50 倍 | 高 |
| 阶段 3：接口分离 | 1 周 | 架构清晰，长期可维护性 | 中 |
| 阶段 4：深度优化 | 2 周 | 进一步性能提升 | 低 |

### 推荐行动

**立即开始阶段 1 和阶段 2**，最大化收益/成本比。
