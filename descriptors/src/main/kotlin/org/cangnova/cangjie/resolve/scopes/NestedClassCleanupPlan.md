# 嵌套类概念清理计划

## 背景

仓颉语言不支持嵌套类（nested class）和内部类（inner class）概念。所有类型声明都必须是顶层的。
当前代码库中存在大量从 Kotlin 编译器移植过来的嵌套类相关代码，需要系统性清理。

## 已完成的清理

### 1. 作用域过滤器 ✅
- `StaticMemberScope.kt` - 修正 `getContributedClassifier()` 返回 null
- `InstanceMemberScope.kt` - 修正 `getContributedClassifier()` 返回 null
- 删除所有"嵌套类总是可访问"的注释

### 2. 类描述符接口 ✅
- `ClassDescriptor.kt` - 删除关于 inner 类和捕获类型参数的注释

## 待处理项

### A. 需要删除的诊断错误（高优先级）

#### 1. NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE

**位置**:
- `analysis/src/main/kotlin/org/cangnova/cangjie/diagnostics/infos/errors/MiscErrors.kt:458`

**使用处**:
- `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/CallExpressionResolver.kt:581`
- `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/calls/tasks/AbstractTracingStrategy.kt:241` (已注释)

**问题**:
这个错误检查类似这样的代码:
```kotlin
// 在其他语言中:
// obj.NestedClass // 错误：通过实例访问嵌套类
```

在仓颉语言中，由于不存在嵌套类，这个检查永远不会触发（ClassifierQualifier 不会是嵌套类）。

**处理方案**:
- 删除 `NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE` 错误定义
- 删除 `CallExpressionResolver.kt:575-583` 中的检查逻辑
- 删除消息文件中的相关条目

#### 2. INACCESSIBLE_OUTER_CLASS_EXPRESSION

**位置**:
- `analysis/src/main/kotlin/org/cangnova/cangjie/diagnostics/infos/errors/MiscErrors.kt:710`

**使用处**:
- `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/DescriptorResolver.kt:1401`

**问题**:
这个错误检查从嵌套类访问外部类表达式的情况。仓颉没有嵌套类，所以永远不会触发。

关键函数 `isStaticNestedClass()` 检查:
```kotlin
fun isStaticNestedClass(descriptor: DeclarationDescriptor): Boolean {
    val containing = descriptor.containingDeclaration
    return descriptor is ClassDescriptor &&
            containing is ClassDescriptor && !isEnumConstructor(descriptor)
}
```

在仓颉中，类的 `containingDeclaration` 应该是 `PackageFragmentDescriptor`，而不是 `ClassDescriptor`，
所以这个函数永远返回 `false`。

**处理方案**:
- 删除 `INACCESSIBLE_OUTER_CLASS_EXPRESSION` 错误定义
- 简化 `DescriptorResolver.kt` 中的逻辑（移除 `isStaticNestedClass` 检查）
- 删除消息文件中的相关条目

### B. 需要重命名/重构的类（中优先级）

#### 1. InnerClassesScopeWrapper

**位置**: `analysis/src/main/kotlin/org/cangnova/cangjie/resolve/scopes/InnerClassesScopeWrapper.kt`

**当前用途**:
这个类实际上是一个"仅分类器作用域包装器"，过滤掉所有非分类器成员。
名字暗示"内部类"但实际上只是过滤分类器。

**使用处**:
- `descriptors/src/main/kotlin/org/cangnova/cangjie/descriptors/impl/AbstractClassDescriptor.kt:87` (已注释)
- `descriptors/README.md:325` (文档引用)

**处理方案**:
- 重命名为 `ClassifierOnlyScope` 或 `ClassifierFilterScope`
- 更新文档注释说明其真实用途
- 更新 README 中的引用

#### 2. CangJieInnerClassInheritorsSearcher

**位置**: `analysis/src/main/kotlin/org/cangnova/cangjie/search/CangJieInnerClassInheritorsSearcher.kt`

**当前用途**:
这个类在局部作用域内搜索类的继承者。名字暗示"内部类继承者搜索"，但实际上是在局部作用域搜索继承者。

**处理方案**:
- 重命名为 `LocalScopeInheritorsSearcher` 或 `CangJieLocalInheritorsSearcher`
- 更新类文档注释

### C. 需要清理的消息和文档（低优先级）

#### 1. CangJieBundle.properties

**需要删除/更新的条目**:
- `formatter.checkbox.text.insert.imports.for.nested.classes` (行 450)
- `button.text.move.nested.class.0.to.upper.level` (行 537)
- `button.text.move.nested.class.0.to.another.class` (行 538)
- `text.0.already.contains.nested.class.1` (行 653)
- `text.cannot.move.inner.class.0.into.itself` (行 679)
- `text.inner.class.0.cannot.be.moved.to.interface` (行 721)
- `text.move.declaration.supports.only.top.levels.and.nested.classes` (行 736)
- `text.moving.multiple.nested.classes.to.top.level.not.supported` (行 749)
- `text.nested.classes.to.upper.level` (行 751)
- `title.move.nested.classes.to.upper.level` (行 827)
- `inspection.redundant.inner.class.modifier.descriptor` (行 2082)
- `inspection.redundant.inner.class.modifier.display.name` (行 2083)

**处理方案**:
- 检查这些消息键是否在代码中使用
- 如果未使用，直接删除
- 如果使用，需要同时删除使用代码

#### 2. CangJieBundle_zh_CN.properties

**需要删除/更新的条目**:
- `formatter.checkbox.text.insert.imports.for.nested.classes` (行 111)

### D. 可能需要审查的其他文件

以下文件包含 "nested" 或 "inner" 关键词，需要人工审查确定是否需要修改：

1. 格式化相关 (可能是从 Kotlin 插件复制的配置，需要检查):
   - `formatter/src/main/kotlin/org/cangnova/cangjie/formatter/CangJieLanguageCodeStyleSettingsProvider.kt`
   - `formatter/src/main/kotlin/org/cangnova/cangjie/formatter/ImportSettingsPanel.kt`
   - `formatter/src/main/kotlin/org/cangnova/cangjie/formatter/CangJieCodeStyleSettings.kt`

2. 反编译器相关 (处理外部库，可能确实遇到其他语言的嵌套类):
   - `analysis/decompiler-to-psi/src/main/kotlin/org/cangnova/cangjie/decompiler/**/*.kt`

3. 类型系统核心 (需要仔细审查，确保不破坏类型系统):
   - `descriptors/src/main/kotlin/org/cangnova/cangjie/types/*.kt`
   - `descriptors/src/main/kotlin/org/cangnova/cangjie/descriptors/impl/*.kt`

## 实施步骤

### 第一阶段：删除无用诊断错误 (立即执行)

1. 注释掉 `NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE` 和 `INACCESSIBLE_OUTER_CLASS_EXPRESSION`
2. 注释掉所有使用这些错误的代码
3. 运行测试验证没有破坏现有功能
4. 确认无误后，完全删除这些代码

### 第二阶段：重命名误导性的类名 (1-2 天后)

1. 使用 IDE 的重构功能重命名 `InnerClassesScopeWrapper` 和 `CangJieInnerClassInheritorsSearcher`
2. 更新相关文档
3. 运行测试验证

### 第三阶段：清理消息文件 (1 周后)

1. 搜索每个消息键的使用处
2. 删除未使用的消息键
3. 对于仍在使用的，需要同步删除使用代码

### 第四阶段：审查其他文件 (持续进行)

1. 逐个审查标记的文件
2. 根据上下文决定是否需要修改

## 风险评估

### 低风险操作
- 删除未使用的消息键
- 更新注释和文档
- 重命名类（使用 IDE 重构）

### 中风险操作
- 删除诊断错误定义
- 简化 `DescriptorResolver` 逻辑

### 高风险操作
- 修改类型系统核心代码
- 修改反编译器代码（可能需要处理其他语言的嵌套类）

## 测试策略

每次修改后应该运行：
1. `./gradlew :analysis:test` - 语义分析测试
2. `./gradlew :psi:test` - PSI 测试
3. `./gradlew test` - 所有测试
4. 手动测试 IDE 功能（补全、导航、重构等）

## 备注

- 反编译器相关代码可能需要保留对嵌套类的支持，因为可能需要反编译 Java/Kotlin 库
- 某些"inner"关键词可能指的是"内层"而非"内部类"，需要根据上下文判断
- 格式化器配置中的嵌套类选项如果来自 Kotlin，可以直接删除
