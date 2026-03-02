# 为什么在 PSI 层难以获取完整的泛型约束信息

## 问题说明

在 `CjExtend.getExtendId()` 中，我们注释了"暂不包含泛型约束，因为当前 PSI 层不易获取完整的约束信息"。让我通过代码示例说明这个问题。

## 示例：仓颉语言的泛型约束

```cangjie
// 示例 1: 简单泛型约束
extend<T> Array<T> <: Printable where T <: ToString {
    func print() {
        for (item in this) {
            println(item.toString())
        }
    }
}

// 示例 2: 多个泛型约束
extend<K, V> Map<K, V> <: Serializable
    where K <: Comparable<K> & Hashable,
          V <: ToString & Cloneable {
    func serialize(): String { ... }
}

// 示例 3: 复杂嵌套约束
extend<T> Option<T> <: Comparable<Option<T>>
    where T <: Comparable<T> {
    func compareTo(other: Option<T>): Int { ... }
}
```

## PSI 层可以获取的信息

### ✅ 可以获取的（文本层面）

```kotlin
// 在 CjExtend 中可以访问:
class CjExtend : CjTypeStatement {
    // 1. 类型参数列表
    override val typeParameterList: CjTypeParameterList?
        // 可以获取: ["T"] 或 ["K", "V"]

    // 2. 类型约束列表
    override val typeConstraintList: CjTypeConstraintList?
    override val typeConstraints: List<CjTypeConstraint>
        // 可以获取约束的文本形式

    // CjTypeConstraint 的结构:
    val subjectTypeParameterName: CjSimpleNameExpression?
        // 获取被约束的类型参数名，如 "T", "K", "V"

    val boundTypeReference: CjTypeReference?
        // 获取约束类型的引用（单个）

    val boundTypeReferences: List<CjTypeReference>
        // 获取约束类型列表（多个，用 & 连接）
}
```

### ❌ 无法获取的（语义层面）

#### 问题 1: 约束类型的规范化表示（Canonical Form）

```kotlin
// 源码
where T <: Comparable<T>

// PSI 层只能获取文本:
val text = constraint.boundTypeReference?.text
// 结果: "Comparable<T>"

// 但我们需要的是规范化的 mangled 形式:
// "std.ops:Comparable<TypeParam0>"
// 这需要类型解析（Type Resolution）才能获得
```

**为什么需要规范化?**
```cangjie
// 这两个扩展在 PSI 层看起来不同，但语义上相同:

// 扩展 1
import std.ops.Comparable
extend<T> Array<T> <: Printable where T <: Comparable<T> { }

// 扩展 2
import std.ops.Comparable as Cmp
extend<T> Array<T> <: Printable where T <: Cmp<T> { }

// PSI 层:
//   扩展 1 约束文本: "Comparable<T>"
//   扩展 2 约束文本: "Cmp<T>"  ❌ 不同！
// Descriptor 层:
//   两者都解析为: "std.ops:Comparable<TypeParam0>"  ✅ 相同！
```

#### 问题 2: 约束排序和规范化

```kotlin
// 源码（顺序1）
where T <: ToString & Hashable, K <: Comparable<K>

// 源码（顺序2）
where K <: Comparable<K>, T <: Hashable & ToString

// PSI 层:
//   顺序1: "T <: ToString & Hashable, K <: Comparable<K>"
//   顺序2: "K <: Comparable<K>, T <: Hashable & ToString"
//   ❌ 文本不同，生成的 ID 也不同！

// 编译器 Mangling:
//   需要按约束的类型参数名排序
//   需要按约束的 bound 类型排序
//   最终生成相同的 mangled 字符串
```

编译器的约束排序逻辑（来自 `ASTMangler.cpp:452-477`）:
```cpp
void MangleGenericConstraints(const Generic& generic) {
    // 1. 按约束类型（被约束的类型参数）排序
    std::vector<std::pair<Ptr<const GenericConstraint>, std::string>> gcs(generic.genericConstraints.size());
    for (size_t i{0}; i < gcs.size(); ++i) {
        gcs[i] = {generic.genericConstraints[i].get(), MangleType(*generic.genericConstraints[i]->type)};
    }
    std::stable_sort(gcs.begin(), gcs.end(), [](const auto& a, const auto& b) {
        return a.second < b.second;  // 按 mangled 类型排序
    });

    for (const auto& gc : gcs) {
        mangled += MANGLE_GEXTEND_PREFIX;  // 特殊前缀
        mangled += gc.second;               // 被约束的类型

        // 2. 按 upper bounds 排序
        std::vector<std::string> uppers(gc.first->upperBounds.size());
        for (size_t i{0}; i < uppers.size(); ++i) {
            uppers[i] = MangleType(*gc.first->upperBounds[i]);
        }
        std::stable_sort(uppers.begin(), uppers.end());  // 按 mangled 类型排序

        for (const auto& upper : uppers) {
            mangled += ':';
            mangled += upper;
        }
    }
}
```

#### 问题 3: 类型变量的映射

```kotlin
// 源码
extend<T, U> Map<T, U> <: Iterable<(T, U)>
    where T <: Comparable<T>,
          U <: ToString {
    // ...
}

// PSI 层获取的约束:
// constraint[0]: "T <: Comparable<T>"
// constraint[1]: "U <: ToString"

// 但在 Mangling 时需要知道:
// - T 是第 0 个类型参数
// - U 是第 1 个类型参数
// - Comparable<T> 中的 T 引用第 0 个类型参数
//
// 这需要构建类型变量映射，而这需要 TypeResolver
```

#### 问题 4: 约束的传递性和隐含关系

```kotlin
// 源码
interface Printable <: ToString { }

extend<T> Array<T> <: Iterable<T> where T <: Printable {
    // ...
}

// 显式约束: T <: Printable
// 隐含约束: T <: ToString (因为 Printable <: ToString)
//
// 编译器在 Mangling 时需要考虑隐含约束吗？
// 这需要类型系统分析，PSI 层无法完成
```

## 为什么 PSI 层难以处理？

### 1. 缺少类型解析能力

```kotlin
// PSI 层只能获取文本
val constraintText = "T <: Comparable<T>"

// 需要 TypeResolver 才能:
// 1. 解析 Comparable 的完整限定名 (std.ops.Comparable)
// 2. 解析 T 是哪个类型参数（第几个）
// 3. 构建类型参数到类型变量的映射
// 4. 生成 mangled 表示
```

### 2. 需要符号表和作用域

```kotlin
// 约束中可能引用外部类型
where T <: CustomInterface

// 需要:
// 1. 查找 CustomInterface 的定义（作用域查询）
// 2. 获取其完整限定名
// 3. 处理导入别名
//
// 这些都需要 BindingContext 和 ResolutionScope
```

### 3. 需要递归类型处理

```kotlin
where T <: Comparable<Option<T>>
      //              ^^^^^^^^^^^
      //              嵌套泛型类型

// 需要递归 mangle:
// Comparable -> std.ops:Comparable
// Option -> std.option:Option
// T -> TypeParam0
// 最终: "std.ops:Comparable<std.option:Option<TypeParam0>>"
```

## 解决方案：在 Descriptor 层生成完整 ID

### 方案 1: 两阶段 ID 生成（推荐）

```kotlin
// PSI 层: 生成不包含约束的基础 ID
fun CjExtend.getExtendId(): String {
    return "$packageName:$extendedType<:${sortedInterfaces}"
    // 不包含约束
}

// Descriptor 层: 生成包含约束的完整 ID
class LazyExtendDescriptor(...) {
    override val extendId: String by lazy {
        val baseId = cjExtend.getExtendId()  // 从 PSI 获取基础 ID
        val constraintsMangled = mangleConstraints()  // 在 Descriptor 层 mangle 约束
        "$baseId$constraintsMangled"
    }

    private fun mangleConstraints(): String {
        if (declaredTypeParameters.isEmpty()) return ""

        val builder = StringBuilder()

        // 获取所有约束并排序
        val constraints = declaredTypeParameters
            .flatMap { param -> param.upperBounds.map { param to it } }
            .sortedBy { (param, _) -> param.typeConstructor.toString() }

        for ((param, bound) in constraints) {
            builder.append("@G")  // MANGLE_GEXTEND_PREFIX
            builder.append(mangleType(param.defaultType))
            builder.append(":")
            builder.append(mangleType(bound))
        }

        return builder.toString()
    }
}
```

### 方案 2: PSI 层使用简化的文本约束

```kotlin
fun CjExtend.getExtendId(): String {
    val parts = mutableListOf<String>()
    parts.add(packageName)
    parts.add(":")
    parts.add(extendedType)
    parts.add("<:")
    parts.add(sortedInterfaces)

    // 添加简化的约束文本（非规范化）
    val constraintsText = typeConstraints.joinToString(",") { constraint ->
        val param = constraint.subjectTypeParameterName?.text ?: ""
        val bounds = constraint.boundTypeReferences.joinToString("&") { it.text }
        "$param:$bounds"
    }
    if (constraintsText.isNotEmpty()) {
        parts.add("@where")
        parts.add(constraintsText)
    }

    return parts.joinToString("")
}

// 问题: 这种方式生成的 ID 不稳定
// - 导入别名会导致不同 ID
// - 顺序不同会导致不同 ID
// - 无法与编译器的 mangling 对齐
```

## 当前实现的选择

我们选择**不在 PSI 层包含泛型约束**，原因：

1. **稳定性**: PSI 层无法生成稳定的约束表示
2. **正确性**: 无法与编译器的 mangling 策略对齐
3. **简洁性**: 对于大多数扩展（无约束或简单约束），基础 ID 已足够区分

### 实际影响评估

```cangjie
// 场景 1: 无约束的扩展（最常见）
extend Array<T> <: Printable { }
extend String <: Comparable { }
// ✅ 基础 ID 完全足够

// 场景 2: 简单约束的扩展
extend<T> Array<T> <: Sortable where T <: Comparable<T> { }
// ⚠️ 如果同一包有两个相同基础 ID 的扩展，只是约束不同，会冲突
// 但这种情况非常罕见

// 场景 3: 复杂约束
extend<T> Option<T> <: Monad<T>
    where T <: Serializable & Comparable<T> & Hashable { }
// ⚠️ 同样的问题，但发生概率更低
```

### 未来改进方向

如果需要支持约束：

1. **在 `LazyExtendDescriptor` 层生成完整 ID**
   - 可以访问 TypeResolver
   - 可以访问 TypeParameters 的 upper bounds
   - 可以正确 mangle 类型

2. **修改 `ExtendManager.ExtensionDef`**
   ```kotlin
   data class ExtensionDef(
       val id: String,  // 完整 ID（包含约束）
       val baseId: String,  // 基础 ID（不含约束）
       // ...
   )
   ```

3. **在 Stub 中缓存完整 ID**
   - 避免每次都重新计算
   - 序列化到索引中

## 总结

PSI 层难以获取完整泛型约束的根本原因：

| 需求 | PSI 层能力 | Descriptor 层能力 |
|------|-----------|------------------|
| 类型文本 | ✅ 可获取 | ✅ 可获取 |
| 类型解析 | ❌ 无法完成 | ✅ 有 TypeResolver |
| 符号查找 | ❌ 需要触发解析 | ✅ 有 BindingContext |
| 类型规范化 | ❌ 只有文本 | ✅ 有 Canonical Type |
| Type Mangling | ❌ 缺少工具 | ✅ 可实现 |
| 约束排序 | ⚠️ 只能按文本 | ✅ 可按语义 |

因此，完整的 extend ID 生成（包含约束）应该在 **Descriptor 层**完成，而不是 PSI 层。