# CangJieCompletionContributor 补全流程分析

## 概述

CangJieCompletionContributor 是仓颉语言插件的代码补全核心组件，采用了多阶段管道式架构，支持基础补全（Basic Completion）和智能补全（Smart Completion）两种模式。

## 架构层次

```
CangJieCompletionContributor (用户界面层)
    ↓
CangJieKindExecutingCompletionContributor (执行器层)
    ↓
SmartPipelineRunner (管道层)
    ↓
ImmediateExecutor / SuggestionGeneratorExecutor (生成器执行层)
    ↓
CompletionSession (会话层)
    ├── BasicCompletionSession (基础补全)
    └── SmartCompletionSession (智能补全)
```

## 完整补全流程

### 阶段 1: 预处理 (beforeCompletion)

**入口**: [CangJieCompletionContributor.kt:207](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\CangJieCompletionContributor.kt#L207)

```kotlin
override fun beforeCompletion(context: CompletionInitializationContext)
```

**主要职责**:

1. **调整替换偏移量**
   - 标记 `replacementOffset` 为已修改，防止后续被改变
   - 根据智能补全模式调整表达式边界

2. **确定虚拟标识符 (Dummy Identifier)**
   ```kotlin
   context.dummyIdentifier = when {
       context.completionType == CompletionType.SMART -> DEFAULT_DUMMY_IDENTIFIER
       PackageDirectiveCompletion.ACTIVATION_PATTERN.accepts(tokenBefore) -> PackageDirectiveCompletion.DUMMY_IDENTIFIER
       else -> CompletionDummyIdentifierProviderService.getInstance().provideDummyIdentifier(context)
   }
   ```
   - `DEFAULT_DUMMY_IDENTIFIER = "IntellijIdeaRulezzz$"` (包含 `$` 忽略光标后内容)

3. **处理特殊上下文**
   - 字符串模板内补全位置校正
   - 参数位置校正
   - 表达式替换偏移量计算

4. **智能补全特殊处理**
   - 查找要替换的完整表达式（向上遍历到非首个子节点）
   - 保存旧参数替换偏移量 (`OLD_ARGUMENTS_REPLACEMENT_OFFSET`)
   - 保存多参数替换偏移量 (`MULTIPLE_ARGUMENTS_REPLACEMENT_OFFSET`)

### 阶段 2: 触发条件判断 (shouldBeCalled)

**位置**: [CangJieCompletionContributor.kt:127](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\CangJieCompletionContributor.kt#L127)

```kotlin
override fun shouldBeCalled(parameters: CompletionParameters): Boolean
```

**判断条件**:
- 当前文件必须是 `CjFile` 类型
- 原始文件也必须是 `CjFile` 类型

### 阶段 3: 管道启动 (fillCompletionVariants)

**位置**: [CangJieCompletionContributor.kt:61](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\CangJieCompletionContributor.kt#L61)

**流程**:
```
fillCompletionVariants
    ↓
SmartPipelineRunner.runPipeline()
    ↓
ImmediatePipelineRunner (默认实现)
    ↓
kindCollector.collectKinds()
```

**SmartPipelineRunner 机制** ([SmartPipelineRunner.kt:12](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\turboComplete\SmartPipelineRunner.kt#L12)):
- 支持扩展点机制 (`org.cangnova.cangjie.completion.turboComplete.smartPipelineRunner`)
- 默认使用 `ImmediatePipelineRunner` 立即执行模式
- 创建 `PolicyController` 控制补全策略

### 阶段 4: 补全类型收集 (collectKinds)

**位置**: [CangJieCompletionContributor.kt:95](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\CangJieCompletionContributor.kt#L95)

#### 4.1 字符串模板补全优先处理

```kotlin
StringTemplateCompletion.correctParametersForInStringTemplateCompletion(parameters)?.let {
    generateCompletionKinds(correctedParameters, generatorExecutor, result, ::wrapLookupElementForStringTemplateAfterDotCompletion)
    return
}
```

#### 4.2 正常补全流程

进入 `generateCompletionKinds()` - [CangJieCompletionContributor.kt:281](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\CangJieCompletionContributor.kt#L281)

**抑制条件检查** (`shouldSuppressCompletion`) - [行145](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\CangJieCompletionContributor.kt#L145):
- 数字字面量内部 (`AFTER_NUMBER_LITERAL`)
- 整数字面量后紧跟小数点 (`AFTER_INTEGER_LITERAL_AND_DOT`)
- 表达式内部自动补全禁用 (Registry 配置)

**包指令补全优先**:
```kotlin
if (PackageDirectiveCompletion.perform(parameters, result)) {
    result.stopHere()
    return
}
```

### 阶段 5: 会话创建与执行

#### 5.1 基础补全模式 (CompletionType.BASIC)

**创建会话** - [行315](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\CangJieCompletionContributor.kt#L315):

```kotlin
val session = BasicCompletionSession(configuration, parameters, resultPolicyController, suggestionGeneratorExecutor)
```

**配置参数** ([CompletionSession.kt:80](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\CompletionSession.kt#L80)):
- `useBetterPrefixMatcherForNonImportedClasses`: 调用次数 < 2
- `nonAccessibleDeclarations`: 调用次数 >= 2
- `staticMembers`: 调用次数 >= 2

**自动弹出检查** - [行319](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\CangJieCompletionContributor.kt#L319):
```kotlin
if (parameters.isAutoPopup && session.shouldDisableAutoPopup()) {
    result.stopHere()
    return
}
```

#### 5.2 智能补全模式 (CompletionType.SMART)

**创建会话** - [行346](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\CangJieCompletionContributor.kt#L346):

```kotlin
val session = SmartCompletionSession(configuration, parameters, result)
```

### 阶段 6: BasicCompletionSession 详细流程

**入口**: [BasicCompletionSession.kt:932](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\BasicCompletionSession.kt#L932)

#### 6.1 检测补全类别

**方法**: `detectCompletionCategory()` - [行113](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\BasicCompletionSession.kt#L113)

**补全类别优先级**:

1. **DECLARATION_NAME** (声明名称)
   - 条件: `nameExpression == null` 且父节点为 `CjNamedDeclaration`
   - 场景: 变量名、函数名、类名等声明位置

2. **OPERATOR_NAME** (操作符名称)
   - 条件: `OPERATOR_NAME.isApplicable()`
   - 场景: `operator` 关键字修饰的函数名

3. **NAMED_ARGUMENTS_ONLY** (命名参数)
   - 条件: `NamedArgumentCompletion.isOnlyNamedArgumentExpected()`
   - 场景: 函数调用中的命名参数位置

4. **SUPER_QUALIFIER** (父类限定符)
   - 条件: `nameExpression.getStrictParentOfType<CjSuperExpression>() != null`
   - 场景: `super.` 后的成员访问

5. **ALL** (全量补全)
   - 默认类别，执行最全面的补全

#### 6.2 ALL 类别补全生成

**核心方法**: `ALL.generateCategories()` - [行154](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\BasicCompletionSession.kt#L154)

##### 6.2.1 关键字补全

```kotlin
KEYWORDS_ONLY.generateCategories()  // 优先执行
```

**关键字处理** ([行814](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\BasicCompletionSession.kt#L814)):
- 关键字值处理 (`KeywordValues.process`)
- 特殊关键字增强:
  - `this` → 生成 `this@label` 项
  - `return` → 生成 `return@label` 项
  - `override` → 触发覆盖补全

##### 6.2.2 引用变体收集 (Reference Variants)

**描述符过滤策略** ([行263-284](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\BasicCompletionSession.kt#L263-L284)):

根据前缀首字母区分:
- **空前缀或有接收者**: 单一过滤器
- **小写字母开头**:
  1. `USUALLY_START_LOWER_CASE` (callables, packages)
  2. `USUALLY_START_UPPER_CASE` (classifiers, constructors)
- **大写字母开头**: 顺序相反

**生成器创建** ([行169-192](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\BasicCompletionSession.kt#L169-L192)):
```kotlin
makeReferenceSuggestionGenerators(descriptors, lookupElementFactory)
```

产生两个生成器:
1. **REFERENCE_BASIC**: 基础引用变体
2. **REFERENCE_EXTENSION**: 扩展函数变体

##### 6.2.3 包名补全

**条件**: 无接收者 + 描述符过滤器包含 `PACKAGES_MASK`

**实现** ([行290-308](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\BasicCompletionSession.kt#L290-L308)):
```kotlin
addKind(CangJieCompletionKindName.PACKAGE_NAME) {
    val packageNames = CangJiePackageIndexUtils.getSubPackageFqNames(
        FqName.ROOT,
        GlobalSearchScope.allScope(project),
        prefixMatcher.asNameFilter()
    )
    packageNames.forEach { collector.addElement(createLookupElementForPackage(it)) }
}
```

##### 6.2.4 命名参数补全

```kotlin
addKind(CangJieCompletionKindName.NAMED_ARGUMENT) {
    NamedArgumentCompletion.complete(collector, expectedInfos, callTypeAndReceiver.callType)
}
```

##### 6.2.5 扩展函数类型值补全

**条件**: `receiverTypes != null`

**场景**: 函数类型变量作为扩展函数调用 ([行314-330](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\BasicCompletionSession.kt#L314-L330))

##### 6.2.6 智能补全增强项

```kotlin
addKind(CangJieCompletionKindName.SMART_ADDITIONAL_ITEM) {
    completeWithSmartCompletion(lookupElementFactory)
}
```

通过 `SmartCompletion.additionalItems()` 获取类型匹配的智能建议

##### 6.2.7 上下文变量补全

两个变体:
- **CONTEXT_VARIABLE_TYPE_SC**: 智能补全相关
- **CONTEXT_VARIABLE_TYPE_REFERENCE**: 引用相关

##### 6.2.8 静态成员补全

三个层级 ([行348-428](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\BasicCompletionSession.kt#L348-L428)):

1. **STATIC_MEMBER_FROM_IMPORTS**: 从导入语句
2. **STATIC_MEMBER_OBJECT_MEMBER**: 对象成员扩展
3. **STATIC_MEMBER_EXPLICIT_INHERITED**: 显式继承成员扩展
4. **STATIC_MEMBER_INACCESSIBLE**: 不可访问成员 (invocationCount >= 2)

##### 6.2.9 非导入项补全

**方法**: `completeNonImported()` - [行433](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\BasicCompletionSession.kt#L433)

**包含内容**:
1. **顶层可调用项** (从索引):
   ```kotlin
   processTopLevelCallables { collector.addDescriptorElements(it, lookupElementFactory, notImported = true) }
   ```

2. **非导入类**:
   - 使用 `BetterPrefixMatcher` (首次调用)
   - 通过 `AllClassesCompletion` 从索引收集
   - 根据 `ClassKind` 过滤

3. **特殊处理: 枚举成员补全**:
   - 检测限定表达式前缀匹配枚举类名
   - 创建新的导入作用域
   - 重新分析上下文并收集成员

#### 6.3 二次补全

**触发条件** ([行327](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\CangJieCompletionContributor.kt#L327)):
```kotlin
if (session.isNothingAddedToResult && parameters.invocationCount < 2)
```

**配置调整** ([行329-336](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\CangJieCompletionContributor.kt#L329-L336)):
```kotlin
val newConfiguration = CompletionSessionConfiguration(
    useBetterPrefixMatcherForNonImportedClasses = false,
    nonAccessibleDeclarations = false,
    staticMembers = parameters.invocationCount > 0,
)
```

放宽限制重新尝试补全

### 阶段 7: SmartCompletionSession 流程

**特点**: 轻量级，主要依赖 `SmartCompletion` 组件

**描述符过滤器** ([SmartCompletionSession.kt:52](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\SmartCompletionSession.kt#L52)):
```kotlin
DescriptorKindFilter.VALUES exclude SamConstructorDescriptorKindExclude
```

**扩展类引用支持**: 当期望函数类型时，包含 `CLASSES_MASK`

**实现**: `doComplete()` 为空，核心逻辑在 `SmartCompletion` 中

### 阶段 8: 结果收集与排序

#### 8.1 LookupElementsCollector

**职责**:
- 收集 `LookupElement` 对象
- 应用后处理器链
- 去重和过滤
- 刷新到 `CompletionResultSet`

#### 8.2 排序权重

**基础排序器** ([CompletionSession.kt:499](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\CompletionSession.kt#L499)):

```kotlin
CompletionSorter.defaultSorter(parameters, prefixMatcher)
    .weighAfter("lift.shorter", RealPrefixMatchingWeigher())
```

**可扩展权重** (已注释的高级权重):
- `DeprecatedWeigher`: 废弃项降权
- `PriorityWeigher`: 优先级权重
- `NotImportedWeigher`: 未导入项权重
- `KindWeigher`: 描述符类型权重
- `CallableWeigher`: 可调用项权重

#### 8.3 LookupElement 增强

**自动填充抑制** ([行936-945](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\BasicCompletionSession.kt#L936-L945)):
```kotlin
if (parameters.isAutoPopup) {
    collector.addLookupElementPostProcessor { lookupElement ->
        lookupElement.putUserData(LookupCancelService.AUTO_POPUP_AT, position.startOffset)
        lookupElement
    }
}
```

**Lambda 字面量处理**:
```kotlin
if (isAtFunctionLiteralStart(position)) {
    lookupElement.apply { suppressItemSelectionByCharsOnTyping = true }
}
```

### 阶段 9: PolicyController 策略控制

**位置**: [PolicyController.kt](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\addingPolicy\PolicyController.kt)

**功能**:
- 控制何时将结果添加到 ResultSet
- 支持延迟添加和批量刷新
- 防止重复添加

**策略类型**:
- `NoneKindPolicy`: 生成阶段策略
- 其他自定义策略 (通过扩展点)

## 关键数据结构

### CompletionSessionConfiguration

**位置**: [CompletionSession.kt:80](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\CompletionSession.kt#L80)

```kotlin
class CompletionSessionConfiguration(
    val useBetterPrefixMatcherForNonImportedClasses: Boolean,
    val nonAccessibleDeclarations: Boolean,
    val staticMembers: Boolean,
)
```

### CallTypeAndReceiver

**作用**: 识别补全位置的调用类型

**定义位置**: [CallType.kt:124](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\resolve\calls\util\CallType.kt#L124)

**类型**:
- `DEFAULT`: 默认位置
- `DOT`: 点访问 (`receiver.`)
- `SAFE`: 安全调用 (`receiver?.`)
- `OPERATOR`: 操作符调用
- `CALLABLE_REFERENCE`: 可调用引用 (`::`)
- `IMPORT_DIRECTIVE`: **导入语句** (import 补全)
- `PACKAGE_DIRECTIVE`: 包指令 (package 补全)
- `TYPE`: 类型位置
- `DELEGATE`: 委托表达式
- `ANNOTATION`: 注解
- `SUPER_MEMBERS`: super 成员访问

**检测逻辑** ([CallType.kt:160](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\resolve\calls\util\CallType.kt#L160)):
```kotlin
fun detect(expression: CjSimpleNameExpression): CallTypeAndReceiver<*, *> {
    // 检测可调用引用
    if (parent is CjCallableReferenceExpression) {
        return CALLABLE_REFERENCE(...)
    }

    // 检测 import 语句
    if (expression.isImportDirectiveExpression()) {
        return IMPORT_DIRECTIVE(receiverExpression)
    }

    // 检测 package 语句
    if (expression.isPackageDirectiveExpression()) {
        return PACKAGE_DIRECTIVE(receiverExpression)
    }

    // 其他类型检测...
}
```

### DescriptorKindFilter

**作用**: 过滤描述符类型

**掩码**:
- `CALLABLES_MASK`: 函数、属性、变量
- `CLASSIFIERS_MASK`: 类、接口、类型别名
- `PACKAGES_MASK`: 包
- `FUNCTIONS_MASK`: 仅函数
- `VALUES_MASK`: 仅值（变量、参数）

### ReceiverType

**结构**:
```kotlin
data class ReceiverType(
    val type: CangJieType,       // 接收者类型
    val receiverIndex: Int       // 接收者优先级索引
)
```

## 补全种类 (Completion Kinds)

### CangJieCompletionKindName 枚举

**位置**: [CangJieKindVariety.kt](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\CangJieKindVariety.kt)

**完整列表**:

| 种类 | 说明 |
|------|------|
| `KEYWORD_ONLY` | 关键字补全 |
| `REFERENCE_BASIC` | 基础引用变体 |
| `REFERENCE_EXTENSION` | 扩展函数变体 |
| `PACKAGE_NAME` | 包名补全 |
| `NAMED_ARGUMENT` | 命名参数 |
| `EXTENSION_FUNCTION_TYPE_VALUE` | 扩展函数类型值 |
| `SMART_ADDITIONAL_ITEM` | 智能补全增强项 |
| `CONTEXT_VARIABLE_TYPE_SC` | 上下文变量(智能) |
| `CONTEXT_VARIABLE_TYPE_REFERENCE` | 上下文变量(引用) |
| `STATIC_MEMBER_FROM_IMPORTS` | 导入的静态成员 |
| `STATIC_MEMBER_OBJECT_MEMBER` | 对象成员静态扩展 |
| `STATIC_MEMBER_EXPLICIT_INHERITED` | 显式继承静态扩展 |
| `STATIC_MEMBER_INACCESSIBLE` | 不可访问静态成员 |
| `NON_IMPORTED` | 非导入项 |
| `DEBUGGER_VARIANTS` | 调试器变体 |
| `DECLARATION_NAME` | 声明名称 |
| `DECLARATION_NAME_FROM_UNRESOLVED_OVERRIDE` | 未解析覆盖声明 |
| `TOP_LEVEL_CLASS_NAME` | 顶层类名 |
| `PARAMETER_OR_VAR_NAME_AND_TYPE` | 参数/变量名和类型 |
| `OPERATOR_NAME` | 操作符名 |
| `SUPER_QUALIFIER` | Super 限定符 |

## 性能优化机制

### 1. 延迟计算

大量使用 `lazy` 委托:
```kotlin
private val smartCompletion by lazy { ... }
private val completionKind by lazy { detectCompletionCategory() }
protected val collector by lazy { LookupElementsCollector(...) }
```

### 2. 索引加速

使用 Stub 索引避免完整 PSI 解析:
- `CangJieClassShortNameIndex`
- `CangJieFunctionShortNameIndex`
- `CangJieTopLevelFunctionByPackageIndex`

### 3. 分批处理

通过 `SuggestionGenerator` 机制异步生成:
```kotlin
suggestionGeneratorConsumer.pass(generator)
// 稍后执行
suggestionGeneratorExecutor.executeAll()
```

### 4. 早期退出

多处使用提前返回优化:
```kotlin
if (shouldSuppressCompletion(...)) {
    result.stopHere()
    return
}
```

### 5. 智能过滤

- `ShadowedDeclarationsFilter`: 过滤被覆盖的声明
- `BetterPrefixMatcher`: 改进的前缀匹配
- 描述符去重和合并

## 调试要点

### 启用日志

在 `Help → Diagnostic Tools → Debug Log Settings` 添加:
```
#org.cangnova.cangjie.completion
```

### 断点位置

1. **入口断点**: `CangJieCompletionContributor.fillCompletionVariants:61`
2. **预处理**: `beforeCompletion:207`
3. **分类检测**: `BasicCompletionSession.detectCompletionCategory:113`
4. **结果收集**: `LookupElementsCollector.addElement`

### 常见问题排查

| 问题 | 排查位置 |
|------|----------|
| 补全未触发 | `shouldBeCalled()` + `shouldSuppressCompletion()` |
| 结果为空 | `session.isNothingAddedToResult` + 检查描述符过滤器 |
| 顺序错误 | `createSorter()` + 各种 Weigher |
| 导入失败 | `ImportableFqNameClassifier` + `ImportInsertHelper` |
| 性能问题 | `CompletionBenchmarkSink` + 索引查询 |

## Import 语句补全详解

### 触发机制

**检测位置**: [CallType.kt:169](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\resolve\calls\util\CallType.kt#L169)

```kotlin
if (expression.isImportDirectiveExpression()) {
    return IMPORT_DIRECTIVE(receiverExpression)
}
```

### 核心处理逻辑

#### 1. 扩展函数排除

**位置**: [ReferenceVariantsCollector.kt:278](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\ReferenceVariantsCollector.kt#L278)

```kotlin
private fun configuration(descriptorKindFilter: DescriptorKindFilter): FilterConfiguration {
    val completeExtensionsFromIndices = descriptorKindFilter.kindMask.and(DescriptorKindFilter.CALLABLES_MASK) != 0
            && DescriptorKindExclude.Extensions !in descriptorKindFilter.excludes
            && callTypeAndReceiver !is CallTypeAndReceiver.IMPORT_DIRECTIVE  // ← 关键判断
    // ...
}
```

**效果**: import 语句中**不会**从索引补全扩展函数

#### 2. 顶层可调用项排除

**位置**: [CompletionSession.kt:367](d:\code\intellij\intellij-cangjie\analysis\src\main\kotlin\org\cangnova\cangjie\completion\CompletionSession.kt#L367)

```kotlin
protected open fun shouldCompleteTopLevelCallablesFromIndex(): Boolean {
    if (nameExpression == null) return false
    if ((descriptorKindFilter?.kindMask ?: 0).and(DescriptorKindFilter.CALLABLES_MASK) == 0) return false
    if (callTypeAndReceiver is CallTypeAndReceiver.IMPORT_DIRECTIVE) return false  // ← import 中返回 false
    return callTypeAndReceiver.receiver == null
}
```

**效果**: import 语句中**不会**从索引补全非导入的顶层可调用项

#### 3. Import 补全的描述符类型

Import 语句使用 `CallType.IMPORT_DIRECTIVE` 的描述符过滤器：

```kotlin
data object IMPORT_DIRECTIVE : CallType<CjExpression?>(DescriptorKindFilter.ALL)
```

允许补全**所有类型**的描述符（类、函数、变量、包等）

### Import 补全流程总结

```
用户在 import 语句输入
    ↓
CallTypeAndReceiver.detect() 识别为 IMPORT_DIRECTIVE
    ↓
进入 BasicCompletionSession.ALL 类别
    ↓
引用变体收集 (ReferenceVariantsCollector)
    ├─ 从当前作用域收集基础变体
    ├─ ✗ 跳过扩展函数索引查询 (行278)
    └─ ✗ 跳过顶层可调用项索引查询 (行370)
    ↓
包名补全 (如果适用)
    ↓
非导入类补全 (从索引)
    ├─ AllClassesCompletion
    ├─ 包括类、接口、类型别名
    └─ 支持跨模块导入
    ↓
结果收集与展示
```

### Import 补全包含的内容

✅ **包含**:
- 包名 (`PACKAGE_NAME` kind)
- 类和接口
- 类型别名
- 对象声明
- 顶层函数和属性（从当前作用域）

❌ **不包含**:
- 未导入的扩展函数（通过索引）
- 未导入的顶层可调用项（通过索引）
- 局部变量和函数
- 成员属性和方法

### 特殊场景

#### 限定导入 (Qualified Import)

```kotlin
import com.example.module.↓
```

此时 `receiverExpression` 不为空，补全会：
1. 解析限定符路径 (`com.example.module`)
2. 获取该包/类的成员
3. 仅显示该范围内的可导入项

#### 别名导入

```kotlin
import com.example.LongClassName as ↓
```

别名位置的处理由 PSI 结构决定，不走标准补全流程。

### 性能优化要点

1. **索引排除**: 通过早期返回避免大量索引查询
2. **作用域限制**: 仅搜索可导入的顶层声明
3. **包名缓存**: 利用 `CangJiePackageIndexUtils` 的缓存机制

## 扩展点

### 自定义补全贡献者

实现 `CompletionContributor` 并注册:
```xml
<completion.contributor
    language="CangJie"
    implementationClass="your.package.YourContributor"/>
```

### 自定义管道运行器

实现 `SmartPipelineRunner` 并注册:
```xml
<org.cangnova.cangjie.completion.turboComplete.smartPipelineRunner
    implementation="your.package.YourRunner"/>
```

### 自定义建议生成器

实现 `SuggestionGeneratorExecutorProvider`:
```kotlin
class CustomGeneratorProvider : SuggestionGeneratorExecutorProvider {
    override fun shouldBeCalled(parameters: CompletionParameters): Boolean
    override fun createExecutor(parameters: CompletionParameters): SuggestionGeneratorExecutor
}
```

注册扩展点:
```xml
<org.cangnova.cangjie.turboComplete.suggestionGeneratorExecutorProvider
    implementation="your.package.CustomGeneratorProvider"/>
```

## 总结

CangJieCompletionContributor 通过以下设计实现高效、灵活的代码补全:

1. **分层架构**: 清晰分离触发、执行、生成、收集各阶段
2. **策略模式**: 通过 `CompletionCategory` 和 `PolicyController` 灵活控制流程
3. **管道机制**: 支持异步和批量处理提升性能
4. **扩展点丰富**: 支持自定义各个环节的行为
5. **智能过滤**: 多层次过滤和排序确保结果质量

整个流程从用户输入到结果展示,经历了约 10+ 个主要阶段和 20+ 种补全类型,充分体现了现代 IDE 补全系统的复杂性和强大功能。
