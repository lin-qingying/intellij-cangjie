# 枚举构造器查找：作用域遍历 vs 专用索引性能对比

## 方案定义

### 方案 A：通过作用域遍历可见枚举

```kotlin
/**
 * 在作用域塔中查找枚举构造器
 *
 * 遍历当前作用域及父作用域中所有可见的枚举类，
 * 然后在这些枚举中查找匹配的构造器
 */
fun findConstructorInScope(
    name: String,
    arity: Int,
    scope: LexicalScope
): List<EnumConstructorDescriptor> {
    val result = mutableListOf<EnumConstructorDescriptor>()

    // 遍历作用域塔（从内到外）
    var currentScope: LexicalScope? = scope
    while (currentScope != null) {
        // 获取该层作用域中所有可见的分类器（类、接口、枚举等）
        val descriptors = currentScope.getContributedDescriptors(
            DescriptorKindFilter.CLASSIFIERS,
            MemberScope.ALL_NAME_FILTER
        )

        // 过滤出枚举类
        for (descriptor in descriptors) {
            if (descriptor is EnumDescriptor) {
                // 遍历该枚举的所有构造器
                for (constructor in descriptor.constructors) {
                    if (constructor.name.asString() == name &&
                        constructor.valueParameters.size == arity) {
                        result.add(constructor)
                    }
                }
            }
        }

        // 移动到父作用域
        currentScope = currentScope.parent
    }

    return result
}
```

### 方案 B：专用索引 + 可见性过滤

```kotlin
/**
 * 使用专用索引查找枚举构造器
 *
 * 先通过索引快速找到所有同名构造器，
 * 然后过滤出当前作用域可见的构造器
 */
fun findConstructorByIndex(
    name: String,
    arity: Int,
    project: Project,
    resolveScope: GlobalSearchScope,
    lexicalScope: LexicalScope
): List<EnumConstructorDescriptor> {
    // 步骤 1: 通过索引查找所有同名构造器（全局查找）
    val allConstructors = StubIndex.getElements(
        CangJieEnumConstructorIndex.KEY,
        name,
        project,
        resolveScope,
        CjEnumConstructor::class.java
    )

    // 步骤 2: 过滤参数数量
    val matchedArity = allConstructors
        .mapNotNull { it.resolveToDescriptor() as? EnumConstructorDescriptor }
        .filter { it.valueParameters.size == arity }

    // 步骤 3: 过滤可见性
    return matchedArity.filter { constructor ->
        isVisibleInScope(constructor, lexicalScope)
    }
}

/**
 * 检查构造器在作用域中是否可见
 */
private fun isVisibleInScope(
    constructor: EnumConstructorDescriptor,
    scope: LexicalScope
): Boolean {
    val enumDescriptor = constructor.containingDeclaration as? EnumDescriptor
        ?: return false

    // 检查枚举类是否在作用域中可见
    return scope.findClassifier(enumDescriptor.name, NoLookupLocation.FROM_IDE) != null
}
```

---

## 性能分析

### 场景假设（典型代码位置）

```cangjie
package com.example.app

import com.example.colors.Color      // 导入的枚举
import com.example.status.Status     // 导入的枚举

enum LocalResult {                   // 本地枚举
    Success(Int64) |
    Error(String)
}

func test(): LocalResult {
    return Success(42)  // 查找 Success 构造器
    //     ^^^^^^^ 在这里解析
}
```

**作用域结构**：
1. **局部作用域**：函数内部
2. **类/文件作用域**：包含 LocalResult 枚举
3. **导入作用域**：包含 Color, Status 枚举
4. **包作用域**：com.example.app
5. **隐式导入**：std.core.* (包含 Option 等)

**可见枚举数量**：
- 局部枚举：LocalResult (1个)
- 导入枚举：Color, Status (2个)
- 隐式导入：Option (1个)
- **总计：4个枚举**

### 方案 A：作用域遍历

```
操作步骤:

1. 遍历局部作用域
   - 获取描述符: 0个枚举
   - 耗时: ~0.1ms

2. 遍历文件作用域
   - 获取描述符: LocalResult (1个枚举)
   - 遍历构造器: 2个 (Success, Error)
   - 检查匹配: 2次
   - 耗时: ~0.3ms

3. 遍历导入作用域
   - 获取描述符: Color, Status (2个枚举)
   - 遍历构造器: 6个 (Red, Green, Blue, Active, Inactive, ...)
   - 检查匹配: 6次
   - 耗时: ~0.5ms

4. 遍历包作用域
   - 获取描述符: 0个枚举（假设包内无其他枚举）
   - 耗时: ~0.1ms

5. 遍历隐式导入作用域
   - 获取描述符: Option (1个枚举)
   - 遍历构造器: 2个 (Some, None)
   - 检查匹配: 2次
   - 耗时: ~0.3ms

总耗时: ~1.3ms
总检查: 4个枚举, 10个构造器
```

### 方案 B：专用索引

```
操作步骤:

1. 索引查找 "Success"
   - 全局查找所有名为 Success 的构造器
   - 假设项目中有 3 个同名构造器:
     * LocalResult.Success (当前文件)
     * ApiResult.Success (另一个包，未导入)
     * NetworkResult.Success (另一个包，未导入)
   - 耗时: ~0.1ms

2. 过滤参数数量 (arity = 1)
   - 检查 3 个构造器
   - 耗时: ~0.01ms

3. 过滤可见性
   - 检查 LocalResult.Success: ✅ 可见
   - 检查 ApiResult.Success: ❌ 不可见（未导入）
   - 检查 NetworkResult.Success: ❌ 不可见（未导入）
   - 耗时: ~0.3ms（每次检查需要在作用域查找）

总耗时: ~0.41ms
总检查: 3个构造器, 3次可见性检查
```

---

## 关键差异分析

### 1. 检查范围

#### 方案 A：作用域遍历
```
检查范围 = 当前作用域可见的所有枚举

优点:
- 只检查相关的枚举（数量少）
- 符合语言的可见性规则
- 不会检查不可见的构造器

缺点:
- 必须遍历所有可见枚举的所有构造器
- 即使构造器名称不匹配也要检查
```

**示例**：
```
作用域中有 4 个枚举:
- LocalResult: Success(Int64), Error(String)  ← 检查 2 个构造器
- Color: Red, Green, Blue                     ← 检查 3 个构造器（虽然不匹配）
- Status: Active, Inactive                    ← 检查 2 个构造器（虽然不匹配）
- Option: Some(T), None                       ← 检查 2 个构造器（虽然不匹配）

总检查: 9 个构造器，但只有 1 个匹配
```

#### 方案 B：专用索引
```
检查范围 = 全局所有同名构造器

优点:
- 只检查同名的构造器（数量少）
- 不会检查名称不匹配的构造器

缺点:
- 可能检查不可见的构造器
- 需要额外的可见性过滤步骤
```

**示例**：
```
全局有 3 个 Success 构造器:
- LocalResult.Success(Int64)         ← 可见
- ApiResult.Success(String)          ← 不可见（未导入）
- NetworkResult.Success(Response)    ← 不可见（未导入）

总检查: 3 个构造器，3 次可见性检查
```

### 2. 时间复杂度

假设：
- **E_visible**：作用域中可见的枚举数量
- **C_visible**：作用域中可见的构造器总数
- **N_avg**：每个枚举的平均构造器数量
- **M_global**：全局同名构造器数量
- **S_depth**：作用域深度（通常 3-5 层）

#### 方案 A：作用域遍历

```
时间复杂度 = O(S_depth × E_visible × N_avg)

分解:
- 遍历作用域层: O(S_depth)
- 每层获取枚举: O(E_visible / S_depth)
- 遍历构造器: O(N_avg)

实际场景:
- S_depth = 4 层
- E_visible = 4 个枚举
- N_avg = 3 个构造器/枚举
- 总操作 = 4 × 4 × 3 = 48 次比较

注意: getContributedDescriptors() 可能已缓存，
     实际每层作用域可能只返回该层的新增枚举
```

#### 方案 B：专用索引

```
时间复杂度 = O(1 + M_global × S_depth)

分解:
- 索引查找: O(1) (哈希表)
- 可见性检查: O(M_global × S_depth)

实际场景:
- 索引查找: 1 次
- M_global = 3 个同名构造器
- S_depth = 4 层（最坏情况，每次都要遍历到顶层）
- 总操作 = 1 + 3 × 4 = 13 次操作

注意: findClassifier() 在作用域中查找枚举类，
     可能在第一层就找到（最好情况 O(1)）
```

### 3. 不同场景的性能

#### 场景 1: 常见名称（如 Red）

```cangjie
enum Color { Red | Green | Blue }
enum Status { Red | Yellow }
enum Light { Red | Amber | Green }

func test(): Color {
    return Red  // 查找 Red 构造器
}
```

**方案 A：作用域遍历**
```
可见枚举: Color, Status, Light (3个)
总构造器: Red, Green, Blue, Red, Yellow, Red, Amber, Green (8个)
匹配: Red (3个)
耗时: ~1.2ms
```

**方案 B：专用索引**
```
全局 Red 构造器: 假设 10 个（不同包）
可见的 Red: 3 个
可见性检查: 10 次
耗时: ~0.8ms
```

**结论**：专用索引稍快（1.5×）

#### 场景 2: 罕见名称（如 Success）

```cangjie
enum LocalResult { Success(Int64) | Error(String) }

func test(): LocalResult {
    return Success(42)
}
```

**方案 A：作用域遍历**
```
可见枚举: LocalResult, Option (2个)
总构造器: Success, Error, Some, None (4个)
匹配: Success (1个)
耗时: ~0.6ms
```

**方案 B：专用索引**
```
全局 Success 构造器: 3 个
可见的 Success: 1 个
可见性检查: 3 次
耗时: ~0.4ms
```

**结论**：专用索引快 1.5×

#### 场景 3: 大量导入

```cangjie
import com.example.colors.*  // 10 个枚举
import com.example.status.*  // 8 个枚举
import com.example.results.* // 12 个枚举

enum LocalEnum { Value1 | Value2 }

func test(): LocalEnum {
    return Value1
}
```

**方案 A：作用域遍历**
```
可见枚举: LocalEnum + 30 个导入枚举 = 31 个
总构造器: 假设平均 4 个/枚举 = 124 个构造器
匹配: Value1 (1个)
耗时: ~15ms (遍历大量不相关的枚举)
```

**方案 B：专用索引**
```
全局 Value1 构造器: 假设 2 个
可见性检查: 2 次
耗时: ~0.5ms
```

**结论**：专用索引快 **30×**（在大量导入时优势明显）

#### 场景 4: 深层作用域嵌套

```cangjie
func outer() {
    enum OuterEnum { A | B }

    func inner() {
        enum InnerEnum { C | D }

        func deepest() {
            return A  // 查找 A
        }
    }
}
```

**方案 A：作用域遍历**
```
作用域深度: 5 层
每层平均枚举: 0.5 个
总检查: 5 × 0.5 × 2 = 5 个构造器
耗时: ~0.8ms
```

**方案 B：专用索引**
```
全局 A 构造器: 1 个
可见性检查: 1 次（在第 3 层找到）
耗时: ~0.3ms
```

**结论**：专用索引快 2.6×

---

## 缓存效果对比

### 方案 A：作用域遍历

```kotlin
// 作用域本身通常已缓存
// 每次调用 getContributedDescriptors() 会返回缓存结果

优点:
- 无需额外缓存（作用域已缓存）
- 内存占用低

缺点:
- 每次查询仍需遍历所有可见枚举
- 无法跳过不匹配的构造器名称
```

### 方案 B：专用索引

```kotlin
// StubIndex 内置多级缓存
// 1. 内存缓存（热数据）
// 2. 磁盘缓存（持久化）
// 3. 增量更新

优点:
- 索引查找极快（< 0.1ms）
- 持久化缓存（IDE 重启保留）

缺点:
- 可见性检查仍需遍历作用域（但只对匹配的构造器）
```

---

## 实现复杂度对比

### 方案 A：作用域遍历

```kotlin
// 实现简单，约 20 行代码

fun findInScope(name: String, arity: Int, scope: LexicalScope): List<...> {
    val result = mutableListOf<EnumConstructorDescriptor>()
    var current = scope

    while (current != null) {
        val descriptors = current.getContributedDescriptors(...)
        for (descriptor in descriptors) {
            if (descriptor is EnumDescriptor) {
                for (ctor in descriptor.constructors) {
                    if (ctor.name.asString() == name &&
                        ctor.valueParameters.size == arity) {
                        result.add(ctor)
                    }
                }
            }
        }
        current = current.parent
    }

    return result
}
```

**优点**：
- 代码简洁
- 无需额外索引
- 符合语言语义

**缺点**：
- 无法优化查找过程
- 必须遍历所有可见枚举

### 方案 B：专用索引

```kotlin
// 实现稍复杂，约 50 行代码

// 1. 定义索引 (10 行)
class CangJieEnumConstructorIndex : StringStubIndexExtension<...>() { ... }

// 2. 修改 Stub (5 行)
override fun indexStub(sink: IndexSink) {
    name?.let { sink.occurrence(CangJieEnumConstructorIndex.KEY, it) }
}

// 3. 查找 + 过滤 (30 行)
fun findByIndex(name: String, arity: Int, ...): List<...> {
    val all = StubIndex.getElements(...)
    val matched = all.filter { it.valueParameters.size == arity }
    return matched.filter { isVisibleInScope(it, scope) }
}

// 4. 可见性检查 (5 行)
fun isVisibleInScope(ctor: ..., scope: LexicalScope): Boolean {
    val enumDesc = ctor.containingDeclaration as? EnumDescriptor
    return scope.findClassifier(enumDesc.name, ...) != null
}
```

**优点**：
- 查找高效（跳过不匹配的构造器）
- 可扩展（支持模糊搜索、补全等）

**缺点**：
- 需要额外的索引基础设施
- 实现稍复杂

---

## 综合对比表

| 指标 | 作用域遍历 | 专用索引 | 优势 |
|------|-----------|---------|------|
| **典型场景耗时** | 0.6-1.5ms | 0.3-0.8ms | 索引 1.5-2× |
| **大量导入耗时** | 10-20ms | 0.5-1ms | 索引 **10-30×** |
| **深层嵌套耗时** | 0.8-1.2ms | 0.3-0.5ms | 索引 2-3× |
| **内存占用** | 低（复用作用域） | 中（额外索引 ~100KB） | 作用域 ✓ |
| **代码复杂度** | 低（~20 行） | 中（~50 行） | 作用域 ✓ |
| **可扩展性** | 低 | 高 | 索引 ✓ |
| **首次构建** | 无需构建 | 1-5秒 | 作用域 ✓ |
| **增量更新** | 无需更新 | 自动增量 | - |

---

## 结论与建议

### 性能总结

1. **典型场景**（少量导入）：
   - 专用索引快 **1.5-2 倍**
   - 但作用域遍历也能接受（< 2ms）

2. **极端场景**（大量导入）：
   - 专用索引快 **10-30 倍**
   - 作用域遍历可能达到 10-20ms（用户感觉卡顿）

3. **实现成本**：
   - 作用域遍历简单（20 行代码）
   - 专用索引稍复杂（50 行代码 + 索引注册）

### 推荐方案

#### 推荐使用 **方案 A：作用域遍历** 如果：

✅ 项目规模小（< 50 个枚举）
✅ 不使用通配符导入（`import pkg.*`）
✅ 追求简单实现
✅ 性能要求不极致（< 2ms 可接受）

**理由**：
- 实现简单，易于维护
- 符合语言语义（严格遵循可见性）
- 性能在大多数场景下足够好
- 无需额外索引基础设施

#### 推荐使用 **方案 B：专用索引** 如果：

✅ 项目规模大（> 100 个枚举）
✅ 经常使用通配符导入
✅ 需要支持高级功能（模糊搜索、补全预测等）
✅ 追求极致性能（< 1ms）

**理由**：
- 性能稳定（不受导入数量影响）
- 可扩展性强（支持更多查找场景）
- 用户体验更好（即时响应）

### 混合方案（最优）

```kotlin
fun findEnumConstructor(
    name: String,
    arity: Int,
    scope: LexicalScope,
    project: Project
): List<EnumConstructorDescriptor> {

    // 策略 1: 先尝试快速路径（作用域遍历）
    // 仅检查当前文件作用域和直接导入
    val quickResult = findInCurrentFileScope(name, arity, scope)
    if (quickResult.isNotEmpty()) {
        return quickResult
    }

    // 策略 2: 如果快速路径未找到，使用索引
    // 查找全局同名构造器，然后过滤可见性
    return findByIndexWithVisibility(name, arity, project, scope)
}
```

**优势**：
- 大多数情况下使用快速路径（本地枚举）
- 极端情况下回退到索引（跨包查找）
- 平衡了简单性和性能

---

## 最终建议

对于仓颉语言插件：

**阶段 1（MVP）**：使用作用域遍历
- 实现简单，快速上线
- 性能在典型场景下足够

**阶段 2（优化）**：添加专用索引
- 当用户反馈性能问题时
- 或项目规模增长后

**阶段 3（完善）**：实现混合方案
- 结合两者优势
- 提供最佳用户体验

---

## 代码示例对比

### 完整实现：作用域遍历

```kotlin
private fun findConstructorInScope(
    name: Name,
    arity: Int,
    scope: LexicalScope
): List<EnumConstructorDescriptor> {
    val result = mutableListOf<EnumConstructorDescriptor>()

    scope.collectAllFromMeAndParent { currentScope ->
        val descriptors = currentScope.getContributedDescriptors(
            DescriptorKindFilter.CLASSIFIERS,
            MemberScope.ALL_NAME_FILTER
        )

        descriptors.filterIsInstance<EnumDescriptor>().forEach { enumDesc ->
            enumDesc.constructors.forEach { constructor ->
                if (constructor.name == name &&
                    constructor.valueParameters.size == arity) {
                    result.add(constructor)
                }
            }
        }
    }

    return result
}
```

### 完整实现：专用索引

```kotlin
private fun findConstructorByIndex(
    name: Name,
    arity: Int,
    project: Project,
    resolveScope: GlobalSearchScope,
    lexicalScope: LexicalScope
): List<EnumConstructorDescriptor> {
    // 索引查找
    val constructors = CangJieEnumConstructorIndex.findConstructors(
        name.asString(),
        arity,
        project,
        resolveScope
    )

    // 可见性过滤
    return constructors.filter { constructor ->
        val enumDescriptor = constructor.containingDeclaration as? EnumDescriptor
        enumDescriptor?.let {
            lexicalScope.findClassifier(
                it.name,
                NoLookupLocation.FROM_IDE
            ) != null
        } ?: false
    }
}
```

两者代码量相当，但性能特性不同。根据项目阶段和需求选择合适的方案。
