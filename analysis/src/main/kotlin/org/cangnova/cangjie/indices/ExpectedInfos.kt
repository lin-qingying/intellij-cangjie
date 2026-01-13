/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.indices

import org.cangnova.cangjie.descriptors.*

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.cangnova.cangjie.resolve.ResolutionFacade
import org.cangnova.cangjie.resolve.calls.CallTransformer
import org.cangnova.cangjie.resolve.calls.components.hasDefaultValue
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.util.*
import org.cangnova.cangjie.resolve.ideService
import org.cangnova.cangjie.resolve.scopes.getResolutionScope
import org.cangnova.cangjie.types.*
import com.intellij.openapi.util.text.StringUtil
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.getTargetFunctionDescriptor
import java.util.ArrayList


/**
 * 按类型过滤器接口
 *
 * 用于代码补全中，根据期望类型过滤候选项。
 * 此接口定义了如何判断一个描述符的类型是否匹配期望类型。
 *
 * **核心方法**：
 * - [matchingSubstitutor] - 判断类型是否匹配，并返回类型替换器
 *
 * **实现类**：
 * - [All] - 接受所有类型（不进行过滤）
 * - [None] - 拒绝所有类型（完全过滤）
 * - [ByExpectedTypeFilter] - 按具体的期望类型过滤（最常用）
 * - 其他自定义过滤器（如 NullableTypesFilter）
 *
 * **使用场景**：
 * - 函数参数补全：只显示参数类型匹配的候选项
 * - 赋值语句补全：只显示与左值类型兼容的候选项
 * - 返回值补全：只显示与函数返回类型匹配的候选项
 */
interface ByTypeFilter {
    /**
     * 判断描述符类型是否匹配期望类型
     *
     * 返回 null 表示不匹配，返回 [ComposableTypeSubstitutor] 表示匹配（可能需要类型替换）。
     *
     * **类型替换器的作用**：
     * 在泛型场景中，候选项的类型可能包含类型参数，需要替换为具体类型。
     * 例如：期望类型为 `ArrayList<Int>`，候选项为 `List<T>`，
     * 需要返回替换器将 `T` 替换为 `Int`。
     *
     * @param descriptorType 候选项的模糊类型（FuzzyType，包含类型参数信息）
     * @return ComposableTypeSubstitutor? 类型替换器（匹配时），或 null（不匹配时）
     */
    fun matchingSubstitutor(descriptorType: FuzzyType): ComposableTypeSubstitutor?

    /**
     * 单个模糊类型
     *
     * 用于简化访问，大多数过滤器只有一个期望类型。
     * 返回 null 表示没有期望类型（如 [All]、[None]）。
     */
    val fuzzyType: FuzzyType?
        get() = null

    /**
     * 多个模糊类型
     *
     * 某些场景下可能有多个期望类型（如重载函数的不同签名）。
     * 默认实现返回 [fuzzyType] 的单元素列表（如果非 null）。
     */
    val multipleFuzzyTypes: Collection<FuzzyType>
        get() = listOfNotNull(fuzzyType)

    /**
     * 接受所有类型的过滤器
     *
     * 不进行任何类型检查，所有候选项都匹配。
     *
     * **使用场景**：
     * - 无法推断期望类型时的回退策略
     * - 显式要求不进行类型过滤
     */
    object All : ByTypeFilter {
        override fun matchingSubstitutor(descriptorType: FuzzyType) = ComposableTypeSubstitutor.EMPTY
    }

    /**
     * 拒绝所有类型的过滤器
     *
     * 所有候选项都不匹配（完全过滤）。
     *
     * **使用场景**：
     * - 仅期望命名参数，不期望表达式值
     * - 明确禁止某些位置的补全
     */
    object None : ByTypeFilter {
        override fun matchingSubstitutor(descriptorType: FuzzyType) = null
    }
}

/**
 * 补全项选项
 *
 * 控制代码补全项的显示选项。
 *
 * **选项说明**：
 * - `starPrefix` - 是否需要在补全项前添加 `*` 前缀（用于 vararg 参数展开）
 *
 * **使用场景**：
 * - 在 vararg 参数位置，数组需要用 `*` 展开：`foo(*array)`
 * - 在 vararg 参数位置，单个元素不需要 `*`：`foo(element)`
 *
 * @property starPrefix 是否需要 `*` 前缀
 */
data class ItemOptions(val starPrefix: Boolean) {
    companion object {
        /**
         * 默认选项（无特殊前缀）
         */
        val DEFAULT = ItemOptions(false)

        /**
         * 需要 `*` 前缀的选项（vararg 展开）
         */
        val STAR_PREFIX = ItemOptions(true)
    }
}

/**
 * 补全后缀类型
 *
 * 表示代码补全后自动插入的后缀符号。
 * 用于提升补全体验，减少用户手动输入。
 *
 * **后缀类型**：
 * - [COMMA] - 逗号，用于参数列表中间位置
 * - [RPARENTH] - 右括号，用于参数列表结束位置
 * - [RBRACKET] - 右方括号，用于数组访问结束位置
 * - [ELSE] - else 关键字，用于 if 表达式的 then 分支后
 * - [RBRACE] - 右花括号，用于 lambda 表达式结束位置
 *
 * **使用示例**：
 * ```kotlin
 * foo(1, |)      // 补全位置在逗号后，tail = COMMA
 * foo(1|)        // 补全位置在最后参数，tail = RPARENTH
 * array[0|]      // 数组访问，tail = RBRACKET
 * if (true) x| else y  // if-else，tail = ELSE
 * list.map { it| }     // lambda，tail = RBRACE
 * ```
 */
enum class Tail {
    COMMA,
    RPARENTH,
    RBRACKET,
    ELSE,
    RBRACE
}

/**
 * 按期望类型过滤器
 *
 * 最常用的类型过滤器实现，根据具体的期望类型进行过滤。
 * 使用子类型检查判断候选项类型是否与期望类型兼容。
 *
 * **工作原理**：
 * - 使用 [FuzzyType.checkIsSubtypeOf] 判断候选项类型是否为期望类型的子类型
 * - 如果是子类型，返回类型替换器（可能包含泛型参数替换信息）
 * - 如果不是子类型，返回 null（过滤掉）
 *
 * **使用示例**：
 * ```kotlin
 * val expectedType = FuzzyType(Int类型)
 * val filter = ByExpectedTypeFilter(expectedType)
 *
 * filter.matchingSubstitutor(Int类型) != null  // true（完全匹配）
 * filter.matchingSubstitutor(Number类型) != null  // true（子类型）
 * filter.matchingSubstitutor(String类型) == null  // true（不兼容）
 * ```
 *
 * @property fuzzyType 期望的模糊类型
 */
class ByExpectedTypeFilter(override val fuzzyType: FuzzyType) : ByTypeFilter {
    override fun matchingSubstitutor(descriptorType: FuzzyType) = descriptorType.checkIsSubtypeOf(fuzzyType)

    override fun equals(other: Any?) = other is ByExpectedTypeFilter && fuzzyType == other.fuzzyType

    override fun hashCode() = fuzzyType.hashCode()
}

/**
 * 参数位置数据
 *
 * 记录补全位置在函数调用中的参数信息。
 * 这是 [ExpectedInfo.AdditionalData] 的主要实现，用于携带参数相关的上下文信息。
 *
 * **核心信息**：
 * - [function] - 被调用的函数描述符
 * - [callType] - 调用类型（普通调用、数组访问等）
 *
 * **子类**：
 * - [Positional] - 位置参数（按顺序传递）
 * - [Named] - 命名参数（通过名称传递）
 *
 * **使用场景**：
 * - 智能补全：根据参数类型提示候选项
 * - 参数提示：显示参数名称和类型
 * - 快速修复：建议添加命名参数
 *
 * @property function 被调用的函数
 * @property callType 调用类型
 */
sealed class ArgumentPositionData(val function: FunctionDescriptor, val callType: Call.CallType) : ExpectedInfo.AdditionalData {
    /**
     * 位置参数数据
     *
     * 表示按位置传递的参数（非命名参数）。
     *
     * @property argumentIndex 参数索引（从 0 开始）
     * @property isFunctionLiteralArgument 是否为函数字面量参数（lambda）
     * @property namedArgumentCandidates 可用的命名参数候选（用于建议转换为命名参数）
     */
    class Positional(
        function: FunctionDescriptor,
        callType: Call.CallType,
        val argumentIndex: Int,
        val isFunctionLiteralArgument: Boolean,
        val namedArgumentCandidates: Collection<ParameterDescriptor>
    ) : ArgumentPositionData(function, callType)

    /**
     * 命名参数数据
     *
     * 表示通过参数名称传递的参数。
     *
     * @property argumentName 参数名称
     */
    class Named(function: FunctionDescriptor, callType: Call.CallType, val argumentName: Name) : ArgumentPositionData(function, callType)
}

/**
 * ExpectedInfo 的模糊类型扩展属性
 *
 * 便捷地从 [ExpectedInfo] 中获取期望的模糊类型。
 * 直接访问 [ExpectedInfo.filter] 的 [ByTypeFilter.fuzzyType] 属性。
 */
val ExpectedInfo.fuzzyType: FuzzyType?
    get() = filter.fuzzyType

/**
 * 期望信息
 *
 * 代码补全系统的核心数据类，封装了补全位置的所有期望信息。
 * 包括类型期望、命名期望、补全后缀、选项和附加数据。
 *
 * **核心字段**：
 * - [filter] - 类型过滤器，判断候选项类型是否匹配
 * - [expectedName] - 期望的符号名称（用于排序和优先级）
 * - [tail] - 补全后自动插入的后缀（如逗号、括号）
 * - [itemOptions] - 补全项选项（如是否需要 `*` 前缀）
 * - [additionalData] - 附加数据（如参数位置、返回值信息）
 *
 * **使用场景**：
 * - **函数参数补全**：
 *   ```kotlin
 *   fun foo(x: Int, y: String) { }
 *   foo(1, |)  // ExpectedInfo(类型=String, 期望名="y", tail=RPARENTH)
 *   ```
 * - **赋值补全**：
 *   ```kotlin
 *   val name: String = |  // ExpectedInfo(类型=String, 期望名="name", tail=null)
 *   ```
 * - **返回值补全**：
 *   ```kotlin
 *   func foo(): Int { return | }  // ExpectedInfo(类型=Int, tail=null)
 *   ```
 *
 * **多构造函数支持**：
 * - 主构造函数：接受 [ByTypeFilter] 过滤器（最灵活）
 * - 便捷构造函数 1：接受 [FuzzyType]（含类型参数）
 * - 便捷构造函数 2：接受 [CangJieType]（普通类型）
 *
 * **工厂方法**：
 * - [createForArgument] - 创建函数参数的期望信息
 * - [createForNamedArgumentExpected] - 创建命名参数期望信息
 * - [createForReturnValue] - 创建返回值的期望信息
 *
 * @property filter 类型过滤器
 * @property expectedName 期望的符号名称（可为 null）
 * @property tail 补全后缀类型（可为 null，表示无自动插入）
 * @property itemOptions 补全项选项
 * @property additionalData 附加数据（可为 null）
 */
data /* for copy() */
class ExpectedInfo(
    val filter: ByTypeFilter,
    val expectedName: String?,
    val tail: Tail?,
    val itemOptions: ItemOptions = ItemOptions.DEFAULT,
    val additionalData: AdditionalData? = null
) {
    /**
     * 附加数据接口
     *
     * 标记接口，用于扩展 [ExpectedInfo] 的上下文信息。
     *
     * **实现类**：
     * - [ArgumentPositionData] - 参数位置数据
     * - [ReturnValueAdditionalData] - 返回值数据
     * - [ComparisonOperandAdditionalData] - 比较运算符数据
     * - [IfConditionAdditionalData] - if 条件数据
     */
    interface AdditionalData {}

    /**
     * 便捷构造函数：接受 FuzzyType
     *
     * 使用模糊类型创建期望信息，自动包装为 [ByExpectedTypeFilter]。
     *
     * @param fuzzyType 模糊类型（包含类型参数）
     */
    constructor(
        fuzzyType: FuzzyType,
        expectedName: String?,
        tail: Tail?,
        itemOptions: ItemOptions = ItemOptions.DEFAULT,
        additionalData: AdditionalData? = null
    ) : this(ByExpectedTypeFilter(fuzzyType), expectedName, tail, itemOptions, additionalData)

    /**
     * 便捷构造函数：接受 CangJieType
     *
     * 使用普通类型创建期望信息，自动转换为模糊类型。
     *
     * @param type 仓颉类型
     */
    constructor(
        type: CangJieType,
        expectedName: String?,
        tail: Tail?,
        itemOptions: ItemOptions = ItemOptions.DEFAULT,
        additionalData: AdditionalData? = null
    ) : this(type.toFuzzyType(emptyList()), expectedName, tail, itemOptions, additionalData)

    /**
     * 判断类型是否匹配（接受 FuzzyType）
     *
     * @param descriptorType 描述符的模糊类型
     * @return DefaultTypeSubstitutor? 匹配时返回类型替换器，不匹配时返回 null
     */
    fun matchingSubstitutor(descriptorType: FuzzyType): ComposableTypeSubstitutor? = filter.matchingSubstitutor(descriptorType)

    /**
     * 判断类型是否匹配（接受 CangJieType）
     *
     * 便捷方法，自动将类型转换为模糊类型后调用 [matchingSubstitutor]。
     *
     * @param descriptorType 描述符的类型
     * @return DefaultTypeSubstitutor? 匹配时返回类型替换器，不匹配时返回 null
     */
    fun matchingSubstitutor(descriptorType: CangJieType): ComposableTypeSubstitutor? = matchingSubstitutor(descriptorType.toFuzzyType(emptyList()))

    companion object {
        /**
         * 为函数参数创建期望信息
         *
         * 工厂方法，专门用于创建函数参数位置的期望信息。
         * 使用函数的类型参数创建模糊类型，支持泛型参数推断。
         *
         * @param type 参数类型
         * @param expectedName 期望的参数名
         * @param tail 补全后缀
         * @param argumentData 参数位置数据
         * @param itemOptions 补全项选项
         * @return ExpectedInfo 参数的期望信息
         */
        fun createForArgument(
            type: CangJieType,
            expectedName: String?,
            tail: Tail?,
            argumentData: ArgumentPositionData,
            itemOptions: ItemOptions = ItemOptions.DEFAULT
        ): ExpectedInfo {
            return ExpectedInfo(type.toFuzzyType(argumentData.function.typeParameters), expectedName, tail, itemOptions, argumentData)
        }

        /**
         * 创建命名参数期望信息
         *
         * 当期望用户输入命名参数时使用（不期望表达式值）。
         * 使用 [ByTypeFilter.None] 过滤器，表示不接受任何类型的值。
         *
         * @param argumentData 参数位置数据
         * @return ExpectedInfo 命名参数的期望信息
         */
        fun createForNamedArgumentExpected(argumentData: ArgumentPositionData): ExpectedInfo {
            return ExpectedInfo(ByTypeFilter.None, null, null/*TODO?*/, ItemOptions.DEFAULT, argumentData)
        }

        /**
         * 为返回值创建期望信息
         *
         * 用于 return 语句或函数体表达式的补全。
         *
         * @param type 返回类型（null 表示无类型要求）
         * @param callable 可调用描述符（函数或属性 getter）
         * @return ExpectedInfo 返回值的期望信息
         */
        fun createForReturnValue(type: CangJieType?, callable: CallableDescriptor): ExpectedInfo {
            val filter = if (type != null) ByExpectedTypeFilter(type.toFuzzyType(emptyList())) else ByTypeFilter.All
            return ExpectedInfo(filter, callable.name.asString(), null, additionalData = ReturnValueAdditionalData(callable))
        }
    }
}

/**
 * 返回值附加数据
 *
 * 携带返回值相关的上下文信息。
 *
 * @property callable 返回值所属的可调用描述符（函数或属性）
 */
class ReturnValueAdditionalData(val callable: CallableDescriptor) : ExpectedInfo.AdditionalData


/**
 * 期望信息计算器
 *
 * 代码补全系统的核心组件，负责根据补全位置的上下文计算期望信息。
 * 通过分析表达式的父节点和周围上下文，推断出该位置期望的类型、名称和其他信息。
 *
 * **工作原理**：
 * 1. 识别表达式的上下文（参数位置、赋值、if/else、return 等）
 * 2. 根据不同上下文调用相应的计算方法
 * 3. 返回一个或多个 [ExpectedInfo] 对象
 *
 * **支持的上下文**：
 * - 函数参数：`foo(|)`
 * - lambda 参数：`list.map { | }`
 * - 数组索引：`array[|]`
 * - 赋值和比较：`x = |`, `x == |`
 * - if-else：`if (|) ...`, `if (c) | else ...`
 * - 代码块：`{ ... | }`
 * - 感叹号运算符：`!|`
 * - 变量初始化：`val x: Int = |`
 * - 函数体：`func foo(): Int = |`
 * - return 语句：`return |`
 * - for 循环范围：`for (x in |) ...`
 * - in 运算符：`x in |`
 * - elvis 运算符：`x ?? |`
 *
 * **性能优化**：
 * - 使用绑定上下文缓存已解析的类型信息
 * - 支持启发式签名匹配（[useHeuristicSignatures]）
 * - 支持外部调用的期望类型传播（[useOuterCallsExpectedTypeCount]）
 *
 * **使用示例**：
 * ```kotlin
 * val calculator = ExpectedInfos(bindingContext, resolutionFacade, indicesHelper)
 * val expectedInfos = calculator.calculate(expression)
 * // 使用 expectedInfos 过滤代码补全候选项
 * ```
 *
 * @property bindingContext 绑定上下文（包含已解析的类型和引用信息）
 * @property resolutionFacade 解析门面（提供类型解析服务）
 * @property indicesHelper 索引辅助器（用于查找可迭代类型、contains 运算符等）
 * @property useHeuristicSignatures 是否使用启发式签名匹配（默认 true）
 * @property useOuterCallsExpectedTypeCount 向外传播期望类型的层数（默认 0，不传播）
 */
class ExpectedInfos(
    private val bindingContext: BindingContext,
    private val resolutionFacade: ResolutionFacade,
    private val indicesHelper: CangJieIndicesHelper?,
    private val useHeuristicSignatures: Boolean = true,
    private val useOuterCallsExpectedTypeCount: Int = 0
) {
    /**
     * 计算表达式的期望信息
     *
     * 主入口方法，根据表达式的上下文计算所有期望信息。
     * 按优先级尝试不同的上下文识别方法，返回第一个匹配的结果。
     *
     * **计算流程**：
     * 1. 尝试各种上下文计算方法（按优先级排序）
     * 2. 如果所有方法都不匹配，从绑定上下文中获取类型
     * 3. 过滤掉包含错误类型的期望信息
     *
     * **优先级顺序**（从高到低）：
     * - Elvis 运算符（`??`）
     * - 函数参数
     * - Lambda 参数
     * - 数组索引参数
     * - 相等和赋值
     * - if 表达式
     * - 代码块表达式
     * - 感叹号运算符
     * - 变量初始化
     * - 函数体表达式
     * - return 语句
     * - for 循环范围
     * - in 运算符参数
     * - 绑定上下文中的类型（兜底）
     *
     * @param expressionWithType 要计算期望信息的表达式
     * @return Collection<ExpectedInfo> 期望信息列表（可能为空）
     */
    fun calculate(expressionWithType: CjExpression): Collection<ExpectedInfo> {
        val expectedInfos = calculateForElvis(expressionWithType)
            ?: calculateForArgument(expressionWithType)
            ?: calculateForFunctionLiteralArgument(expressionWithType)
            ?: calculateForIndexingArgument(expressionWithType)
            ?: calculateForEqAndAssignment(expressionWithType)
            ?: calculateForIf(expressionWithType)
            ?: calculateForBlockExpression(expressionWithType)
//            ?: calculateForMatchEntryValue(expressionWithType)
            ?: calculateForExclOperand(expressionWithType)
            ?: calculateForInitializer(expressionWithType)
            ?: calculateForExpressionBody(expressionWithType)
            ?: calculateForReturn(expressionWithType)
            ?: calculateForLoopRange(expressionWithType)
            ?: calculateForInOperatorArgument(expressionWithType)

            ?: getFromBindingContext(expressionWithType)
            ?: return emptyList()
        return expectedInfos.filterNot { it.fuzzyType?.type?.isError ?: false }
    }

    /**
     * 计算函数参数位置的期望信息（从表达式上下文推断）
     *
     * 检测表达式是否位于函数调用的参数位置，如果是则计算该参数的期望类型。
     *
     * **工作流程**：
     * 1. 向上遍历 PSI 树，找到 [CjValueArgument] 节点
     * 2. 继续向上找到 [CjValueArgumentList] 和 [CjCallElement]
     * 3. 委托给 [calculateForArgument(CjCallElement, ValueArgument)] 计算期望信息
     *
     * **容错处理**：
     * - 如果 parent 不是 [CjValueArgument]，尝试 parent.parent（避免解析错误）
     * - 这是为了处理某些解析异常场景（如 KTIJ-18231）
     *
     * **返回 null 的情况**：
     * - 表达式不在函数参数位置
     * - PSI 树结构不完整
     *
     * @param expressionWithType 当前表达式
     * @return Collection<ExpectedInfo>? 期望信息列表，如果不在参数位置则返回 null
     */
    private fun calculateForArgument(expressionWithType: CjExpression): Collection<ExpectedInfo>? {
        var valueArgumentCandidate = expressionWithType.parent
        if (valueArgumentCandidate !is CjValueArgument) { // Avoid parsing errors like KTIJ-18231
            valueArgumentCandidate = valueArgumentCandidate.parent
        }
        val argument = valueArgumentCandidate as? CjValueArgument ?: return null
        val argumentList = argument.parent as? CjValueArgumentList ?: return null
        val callElement = argumentList.parent as? CjCallElement ?: return null
        return calculateForArgument(callElement, argument)
    }

    /**
     * 计算 Lambda 表达式参数的期望信息
     *
     * 检测表达式是否位于 lambda 函数字面量参数位置（尾随 lambda 语法）。
     *
     * **尾随 Lambda 语法**：
     * 仓颉语言允许将最后一个函数类型参数写在括号外：
     * ```kotlin
     * list.map { it * 2 }  // lambda 在括号外
     * // 等价于：list.map({ it * 2 })
     * ```
     *
     * **工作流程**：
     * 1. 检查表达式的 parent 是否为 [CjLambdaArgument]
     * 2. 检查 lambda 的 parent 是否为 [CjCallExpression]
     * 3. 获取调用表达式的第一个 lambda 参数
     * 4. 验证该 lambda 的表达式是否为当前表达式
     * 5. 委托给 [calculateForArgument(Call, ValueArgument)] 计算期望信息
     *
     * **返回 null 的情况**：
     * - 表达式不在 lambda 参数位置
     * - lambda 的内部表达式不是当前表达式
     *
     * @param expressionWithType 当前表达式
     * @return Collection<ExpectedInfo>? 期望信息列表，如果不在 lambda 参数位置则返回 null
     */
    private fun calculateForFunctionLiteralArgument(expressionWithType: CjExpression): Collection<ExpectedInfo>? {
        val functionLiteralArgument = expressionWithType.parent as? CjLambdaArgument
        val callExpression = functionLiteralArgument?.parent as? CjCallExpression ?: return null
        val literalArgument = callExpression.lambdaArguments.firstOrNull() ?: return null
        if (literalArgument.getArgumentExpression() != expressionWithType) return null
        return calculateForArgument(callExpression, literalArgument)
    }

    /**
     * 计算数组索引参数的期望信息
     *
     * 检测表达式是否位于数组访问的索引位置，如果是则计算索引的期望类型。
     *
     * **数组访问语法**：
     * ```kotlin
     * array[index]      // index 位置需要期望类型
     * matrix[i, j]      // 多维索引，i 和 j 都需要期望类型
     * map["key"]        // 支持任意类型的索引（取决于 get 运算符定义）
     * ```
     *
     * **工作流程**：
     * 1. 检查表达式的 parent 是否为 [CjContainerNode]（索引容器节点）
     * 2. 检查容器节点的 parent 是否为 [CjArrayAccessExpression]
     * 3. 验证容器节点确实是数组访问的索引节点
     * 4. 获取数组访问的 Call 对象（可能调用 get 运算符）
     * 5. 从 Call 的参数列表中找到当前表达式对应的参数
     * 6. 委托给 [calculateForArgument(Call, ValueArgument)] 计算期望信息
     *
     * **运算符重载**：
     * 数组访问实际上是调用 `get` 运算符：
     * - `array[i]` → `array.get(i)`
     * - `array[i, j]` → `array.get(i, j)`
     *
     * **返回 null 的情况**：
     * - 表达式不在数组索引位置
     * - 无法解析 Call 对象
     * - 在参数列表中找不到当前表达式
     *
     * @param expressionWithType 当前表达式
     * @return Collection<ExpectedInfo>? 期望信息列表，如果不在索引位置则返回 null
     */
    private fun calculateForIndexingArgument(expressionWithType: CjExpression): Collection<ExpectedInfo>? {
        val containerNode = expressionWithType.parent as? CjContainerNode ?: return null
        val arrayAccessExpression = containerNode.parent as? CjArrayAccessExpression ?: return null
        if (containerNode != arrayAccessExpression.indicesNode) return null
        val call = arrayAccessExpression.getCall(bindingContext) ?: return null
        val argument = call.valueArguments.firstOrNull { it.getArgumentExpression() == expressionWithType } ?: return null
        return calculateForArgument(call, argument)
    }

    /**
     * 计算函数参数的期望信息（从 CallElement 和 ValueArgument）
     *
     * 根据调用元素和参数，计算该参数位置的期望信息。
     * 这是 calculateForArgument 系列方法的中间层，负责从 CallElement 获取 Call 对象。
     *
     * **工作流程**：
     * 1. 从 [CjCallElement] 获取 [Call] 对象（通过绑定上下文）
     * 2. 验证 Call 对象的 callElement 与传入的 callElement 一致
     * 3. 委托给 [calculateForArgument(Call, ValueArgument)] 进行实际计算
     *
     * **错误处理**：
     * - 某些情况下可能获取到错误的 Call 对象（见 testEA70945）
     * - 通过比较 callElement 来检测这种错误，避免错误的类型推断
     *
     * **返回 null 的情况**：
     * - 无法从绑定上下文获取 Call 对象
     * - Call 对象的 callElement 与传入的不一致（错误的 Call）
     *
     * @param callElement 调用元素（如 CjCallExpression, CjArrayAccessExpression）
     * @param argument 参数对象
     * @return Collection<ExpectedInfo>? 期望信息列表，如果无法计算则返回 null
     */
    private fun calculateForArgument(callElement: CjCallElement, argument: ValueArgument): Collection<ExpectedInfo>? {
        val call = callElement.getCall(bindingContext) ?: return null
        // sometimes we get wrong call (see testEA70945) TODO: refactor resolve so that it does not happen
        if (call.callElement != callElement) return null
        return calculateForArgument(call, argument)
    }

    /**
     * 计算函数参数的期望信息（公共接口）
     *
     * **核心方法**：这是计算参数期望类型的主入口之一，供外部调用。
     * 根据调用对象和参数，推断该参数位置期望的类型、名称和其他补全信息。
     *
     * **工作流程**：
     * 1. 调用私有方法 [calculateForArgument(Call, CangJieType, ValueArgument)]（期望类型为 NO_EXPECTED_TYPE）
     * 2. 如果启用了外部调用期望类型传播（[useOuterCallsExpectedTypeCount] > 0）：
     *    - 检查结果中是否有自由类型参数（泛型未确定）
     *    - 如果函数返回类型也有自由类型参数
     *    - 则向外传播：从调用表达式的上下文获取期望类型
     *    - 使用外部期望类型重新计算参数的期望信息
     *
     * **外部期望类型传播示例**：
     * ```kotlin
     * val list: List<Int> = buildList { add(|) }
     * // buildList 的类型参数 T 未确定
     * // 但外部期望类型是 List<Int>，所以 T = Int
     * // 因此 add 的参数期望类型是 Int
     * ```
     *
     * **性能优化**：
     * - makesSenseToUseOuterCallExpectedType 函数检查是否需要向外传播
     * - 仅在有自由类型参数且函数返回类型也有自由参数时才传播
     * - 避免不必要的递归计算
     *
     * @param call 调用对象（包含被调用的函数和所有参数）
     * @param argument 当前参数对象
     * @return Collection<ExpectedInfo> 期望信息列表（非空，最坏情况返回空列表）
     */
    fun calculateForArgument(call: Call, argument: ValueArgument): Collection<ExpectedInfo> {
        val results = calculateForArgument(call, TypeUtils.NO_EXPECTED_TYPE, argument)

        fun makesSenseToUseOuterCallExpectedType(info: ExpectedInfo): Boolean {
            val data = info.additionalData as ArgumentPositionData
            return info.fuzzyType != null
                    && info.fuzzyType!!.freeParameters.isNotEmpty()
                    && data.function.fuzzyReturnType()?.freeParameters?.isNotEmpty() ?: false
        }

        if (useOuterCallsExpectedTypeCount > 0 && results.any(::makesSenseToUseOuterCallExpectedType)) {
            val callExpression = (call.callElement as? CjExpression)?.getQualifiedExpressionForSelectorOrThis() ?: return results
            val expectedFuzzyTypes =
                ExpectedInfos(bindingContext, resolutionFacade, indicesHelper, useHeuristicSignatures, useOuterCallsExpectedTypeCount - 1)
                    .calculate(callExpression)
                    .mapNotNull { it.fuzzyType }
            if (expectedFuzzyTypes.isEmpty() || expectedFuzzyTypes.any { it.freeParameters.isNotEmpty() }) return results

            return expectedFuzzyTypes
                .map { it.type }
                .toSet()
                .flatMap { calculateForArgument(call, it, argument) }
        }

        return results
    }

    /**
     * 计算函数参数的期望信息（核心实现）
     *
     * **最核心的实现方法**：执行参数期望类型推断的完整逻辑。
     * 这是一个复杂的方法，处理函数重载解析、参数匹配、vararg 参数等多种情况。
     *
     * **主要步骤**：
     * 1. **处理隐式调用**：如果是隐式 invoke 调用，递归处理外层调用
     * 2. **创建截断调用**：只包含当前参数之前的参数（用于重载解析）
     * 3. **解析候选函数**：找出所有可能匹配的函数重载
     * 4. **为每个候选生成期望信息**：
     *    - 检查前面的参数是否都匹配
     *    - 确定当前参数对应哪个形参
     *    - 处理 vararg 参数
     *    - 处理命名参数
     *    - 确定补全后缀（逗号、右括号等）
     *
     * **截断调用的目的**：
     * 在补全当前参数时，后面的参数可能不完整或错误。
     * 通过截断调用（只包含前面的参数），可以更准确地推断当前参数的类型。
     *
     * **隐式 invoke 示例**：
     * ```kotlin
     * val f: (Int) -> String = ...
     * f(|)  // 这是隐式调用 f.invoke(|)
     * ```
     *
     * @param call 调用对象
     * @param callExpectedType 调用的期望返回类型（用于泛型参数推断）
     * @param argument 当前参数
     * @return Collection<ExpectedInfo> 期望信息列表
     */
    private fun calculateForArgument(call: Call, callExpectedType: CangJieType, argument: ValueArgument): Collection<ExpectedInfo> {

        if (call is CallTransformer.CallForImplicitInvoke)
            return calculateForArgument(call.outerCall, callExpectedType, argument)

        val argumentIndex = call.valueArguments.indexOf(argument)
        assert(argumentIndex >= 0) {
            "Could not find argument '$argument(${argument.asElement()
                .text})' among arguments of call: $call. Call element text: '${call.callElement.text}'"
        }

        // leave only arguments before the current one
        val truncatedCall = object : DelegatingCall(call) {
            val arguments = call.valueArguments.subList(0, argumentIndex)
            override val valueArgumentList: CjValueArgumentList? = null
            override val valueArguments  = arguments
            override val functionLiteralArguments  = emptyList<LambdaArgument>()

        }

        val candidates = truncatedCall.resolveCandidates(bindingContext, resolutionFacade, callExpectedType)

        val expectedInfos = ArrayList<ExpectedInfo>()

        for (candidate in candidates) {
            expectedInfos.addExpectedInfoForCandidate(candidate, call, argument, argumentIndex, checkPrevArgumentsMatched = true)
        }

        if (expectedInfos.isEmpty()) { // if no candidates have previous arguments matched, try with no type checking for them
            for (candidate in candidates) {
                expectedInfos.addExpectedInfoForCandidate(candidate, call, argument, argumentIndex, checkPrevArgumentsMatched = false)
            }
        }

        return expectedInfos
    }

    /**
     * 为候选函数添加期望信息
     *
     * **复杂的核心方法**：为解析出的候选函数生成期望信息，添加到集合中。
     * 这是参数期望类型推断的最细致部分，处理各种边界情况。
     *
     * **主要逻辑步骤**：
     * 1. **验证候选有效性**：
     *    - 检查所有参数都有映射到形参
     *    - 检查前面的参数类型都匹配（可选）
     * 2. **确定参数对应的形参**：
     *    - 通过参数到形参的映射找到对应的形参
     *    - 处理类型推断失败的情况（回退到原始类型）
     * 3. **处理特殊调用类型**：
     *    - 数组访问：最后一个参数（set 的值）不需要期望信息
     * 4. **生成命名参数候选**：
     *    - 如果是位置参数且有未使用的命名参数，建议使用命名参数
     * 5. **处理 vararg 参数**：
     *    - vararg 参数可以接受单个元素或数组
     *    - 数组需要用 `*` 展开
     * 6. **确定补全后缀**：
     *    - 最后一个参数：右括号/右方括号
     *    - 中间参数：逗号（如果后面有必需参数）
     *    - 其他情况：无后缀
     *
     * **类型推断失败处理**：
     * 在代码补全时，类型推断可能失败（因为参数不完整）。
     * 此时回退到原始类型（未替换泛型参数的类型）。
     *
     * **命名参数建议**：
     * 如果候选的命名参数列表不为空，会创建一个特殊的期望信息，
     * 建议用户使用命名参数（不期望表达式值）。
     *
     * @param candidate 候选的已解析调用
     * @param call 原始调用对象
     * @param argument 当前参数
     * @param argumentIndex 参数索引
     * @param checkPrevArgumentsMatched 是否检查前面的参数类型匹配
     */
    private fun MutableCollection<ExpectedInfo>.addExpectedInfoForCandidate(
        candidate: ResolvedCall< out FunctionDescriptor>,
        call: Call,
        argument: ValueArgument,
        argumentIndex: Int,
        checkPrevArgumentsMatched: Boolean
    ) {
        // check that all arguments before the current has mappings to parameters
        if (!candidate.allArgumentsMapped()) return

        // check that all arguments before the current one matched
        if (checkPrevArgumentsMatched && !candidate.allArgumentsMatched()) return

        var descriptor = candidate.resultingDescriptor
        if (descriptor.valueParameters.isEmpty()) return

        val argumentToParameter = call.mapArgumentsToParameters(descriptor)
        var parameter = argumentToParameter[argument]
        var parameterType = parameter?.type

        if (parameterType != null && parameterType.containsError()) {
            // Type inference for parameter is failed (because we are in the middle
            // of completing some argument and not yet all arguments may be passed)
            val originalParameter = descriptor.original.valueParameters[parameter!!.index]
            parameter = originalParameter
            descriptor = descriptor.original
            parameterType = fixSubstitutedType(parameterType, originalParameter.type)
        }

        val argumentName = argument.getArgumentName()?.asName
        val isFunctionLiteralArgument = argument is LambdaArgument

        val callType = call.callType
        val isArrayAccess = callType == Call.CallType.ARRAY_GET_METHOD || callType == Call.CallType.ARRAY_SET_METHOD
        val rparenthTail = if (isArrayAccess) Tail.RBRACKET else Tail.RPARENTH

        val argumentPositionData = if (argumentName != null) {
            ArgumentPositionData.Named(descriptor, callType, argumentName)
        } else {
            val namedArgumentCandidates = if (!isFunctionLiteralArgument && !isArrayAccess && descriptor.hasStableParameterNames()) {
                val alreadyPassedParameters =
                    // Suggest only parameter names which are not used yet
                    call.valueArguments.mapNotNullTo(mutableSetOf()) { it.getArgumentName()?.asName } +
                            // plus not parameter names which are already successfully mapped
                            // (everything that goes after argumentIndex may be incorrectly parsed)
                            argumentToParameter
                                .filter { (arg, _) ->
                                    call.valueArguments.indexOf(arg).takeIf { it != -1 }?.let { it < argumentIndex } == true
                                }
                                .map { (_, param) -> param.name }
                descriptor.valueParameters.filter { it.name !in alreadyPassedParameters }
            } else {
                emptyList()
            }
            ArgumentPositionData.Positional(descriptor, callType, argumentIndex, isFunctionLiteralArgument, namedArgumentCandidates)
        }

        var parameters = descriptor.valueParameters
        if (callType == Call.CallType.ARRAY_SET_METHOD) { // last parameter in set is used for value assigned
            if (parameter == parameters.last()) {
                parameter = null
                parameterType = null
            }
            parameters = parameters.dropLast(1)
        }

        if (parameter == null) {
            if (argumentPositionData is ArgumentPositionData.Positional && argumentPositionData.namedArgumentCandidates.isNotEmpty()) {
                add(ExpectedInfo.createForNamedArgumentExpected(argumentPositionData))
            }
            return
        }
        parameterType!!

        val expectedName = if (descriptor.hasSynthesizedParameterNames()) null else parameter.name.asString()

        fun needCommaForParameter(parameter: ValueParameterDescriptor): Boolean {
            if (parameter.hasDefaultValue()) return false // parameter is optional
            if (parameter.varargElementType != null) return false // vararg arguments list can be empty
            // last parameter of functional type can be placed outside parenthesis:
            if (!isArrayAccess && parameter == parameters.last() && parameter.type.isFunctionType) return false
            return true
        }

        val tail = if (argumentName == null) {
            when {
                parameter == parameters.last() -> rparenthTail
                parameters.dropWhile { it != parameter }.drop(1).any(::needCommaForParameter) -> Tail.COMMA
                else -> null
            }
        } else {
            namedArgumentTail(argumentToParameter, argumentName, descriptor)
        }

        val alreadyHasStar = argument.getSpreadElement() != null

        val varargElementType = parameter.varargElementType
        if (varargElementType != null) {
            if (isFunctionLiteralArgument) return

            val varargTail = if (argumentName == null && tail == rparenthTail)
                null /* even if it's the last parameter, there can be more arguments for the same parameter */
            else
                tail

            if (!alreadyHasStar) {
                add(ExpectedInfo.createForArgument(varargElementType, expectedName?.unpluralize(), varargTail, argumentPositionData))
            }

            val starOptions = if (!alreadyHasStar) ItemOptions.STAR_PREFIX else ItemOptions.DEFAULT
            add(ExpectedInfo.createForArgument(parameterType, expectedName, varargTail, argumentPositionData, starOptions))
        } else {
            if (alreadyHasStar) return

            if (isFunctionLiteralArgument) {
                if (parameterType.isFunctionType) {
                    add(ExpectedInfo.createForArgument(parameterType, expectedName, null, argumentPositionData))
                }
            } else {
                add(ExpectedInfo.createForArgument(parameterType, expectedName, tail, argumentPositionData))
            }
        }
    }

    /**
     * 修复替换后的类型
     *
     * 在类型推断失败时（通常在代码补全中），尝试修复替换后的类型。
     * 如果替换后的类型包含错误，用原始类型的对应部分替换。
     *
     * **使用场景**：
     * 在代码补全时，参数可能不完整，导致类型推断失败。
     * 此方法将错误的类型参数替换为原始类型的参数。
     *
     * **修复策略**：
     * - 如果整个类型是错误类型，返回原始类型
     * - 如果类型参数数量不匹配，返回原始类型
     * - 对每个类型参数，如果包含错误，使用原始类型的对应参数
     *
     * **示例**：
     * ```kotlin
     * // 原始类型：List<T>
     * // 替换后的类型：List<Error>（因为参数不完整）
     * // 修复后的类型：List<T>
     * ```
     *
     * @param substitutedType 替换后的类型（可能包含错误）
     * @param originalType 原始类型（未替换泛型参数）
     * @return CangJieType 修复后的类型
     */
    private fun fixSubstitutedType(substitutedType: CangJieType, originalType: CangJieType): CangJieType {
        if (substitutedType.isError) return originalType
        if (substitutedType.arguments.size != originalType.arguments.size) return originalType
        val newTypeArguments = substitutedType.arguments.zip(originalType.arguments).map { (argument, originalArgument) ->
            if (argument.type.containsError()) originalArgument else argument
        }
        return substitutedType.replace(newTypeArguments)
    }

    /**
     * 检查已解析调用的所有参数是否都匹配
     *
     * 判断调用中的所有参数是否都成功映射到形参，且类型匹配。
     * 忽略本身就有错误类型的参数（如未完成的表达式）。
     *
     * @receiver ResolvedCall<D> 已解析的调用
     * @return Boolean true 表示所有参数都匹配
     */
    private fun <D : CallableDescriptor> ResolvedCall<D>.allArgumentsMatched() = call.valueArguments
        .none { argument -> getArgumentMapping(argument).isError() && !argument.hasError() /* ignore arguments that has error type */ }

    /**
     * 检查参数是否有错误类型
     *
     * 判断参数的表达式是否有错误类型，或者无法获取类型。
     *
     * @receiver ValueArgument 参数对象
     * @return Boolean true 表示参数有错误类型或无法获取类型
     */
    private fun ValueArgument.hasError() = getArgumentExpression()?.let { bindingContext.getType(it) }?.isError ?: true

    /**
     * 计算命名参数的补全后缀
     *
     * 根据命名参数的使用情况，确定补全后应该插入的后缀。
     *
     * **后缀规则**：
     * - 如果所有参数都已使用（或都有默认值），返回右括号
     * - 如果剩余未使用参数都有默认值，返回 null（可选）
     * - 否则返回逗号（还有必需参数）
     *
     * **注意**：命名参数不支持数组访问（[]），所以总是返回 RPARENTH 而不是 RBRACKET
     *
     * @param argumentToParameter 参数到形参的映射
     * @param argumentName 当前命名参数的名称
     * @param descriptor 函数描述符
     * @return Tail? 补全后缀，null 表示无后缀
     */
    private fun namedArgumentTail(
        argumentToParameter: Map<ValueArgument, ValueParameterDescriptor>,
        argumentName: Name,
        descriptor: FunctionDescriptor
    ): Tail? {
        val usedParameterNames = (argumentToParameter.values.map { it.name } + listOf(argumentName)).toSet()
        val notUsedParameters = descriptor.valueParameters.filter { it.name !in usedParameterNames }
        return when {
            notUsedParameters.isEmpty() -> Tail.RPARENTH // named arguments no supported for []
            notUsedParameters.all { it.hasDefaultValue() } -> null
            else -> Tail.COMMA
        }
    }

    /**
     * 计算相等比较和赋值表达式的期望信息
     *
     * 检测表达式是否位于相等比较（==, !=）或赋值（=）的一侧。
     * 如果是，则另一侧的类型就是当前表达式的期望类型。
     *
     * **支持的运算符**：
     * - `=` - 赋值运算符
     * - `==` - 相等比较运算符
     * - `!=` - 不等比较运算符
     *
     * **工作流程**：
     * 1. 检查表达式的 parent 是否为 [CjBinaryExpression]
     * 2. 检查运算符是否为 EQ 或 COMPARISON_TOKENS
     * 3. 获取另一侧操作数的类型作为期望类型
     * 4. 从操作数推断期望的名称
     * 5. 对于比较运算符，特殊处理可空类型
     *
     * **特殊处理**：
     * - **Nothing 类型**：如果另一侧是 null，使用 [NullableTypesFilter]（只接受可空类型）
     * - **比较运算符**：
     *   - 将期望类型转换为可空类型（允许可空类型的候选项）
     *   - 添加 [ComparisonOperandAdditionalData]（用于抑制 null 字面量的显示）
     *
     * **示例**：
     * ```kotlin
     * val x: Int = |        // 期望类型 Int
     * if (y == |) ...       // 期望类型 y 的类型（可空）
     * if (| == null) ...    // 只接受可空类型
     * ```
     *
     * @param expressionWithType 当前表达式
     * @return Collection<ExpectedInfo>? 期望信息列表，如果不在相等/赋值位置则返回 null
     */
    private fun calculateForEqAndAssignment(expressionWithType: CjExpression): Collection<ExpectedInfo>? {
        val binaryExpression = expressionWithType.parent as? CjBinaryExpression
        if (binaryExpression != null) {
            val operationToken = binaryExpression.operationToken
            if (operationToken == CjTokens.EQ || operationToken in COMPARISON_TOKENS) {
                val otherOperand = if (expressionWithType == binaryExpression.right) binaryExpression.left else binaryExpression.right
                if (otherOperand != null) {
                    var expectedType = bindingContext.getType(otherOperand) ?: return null

                    val expectedName = expectedNameFromExpression(otherOperand)

                    if (expectedType.isNothing()) { // other operand is 'null'
                        return listOf(ExpectedInfo(NullableTypesFilter, expectedName, null))
                    }

                    var additionalData: ExpectedInfo.AdditionalData? = null
                    if (operationToken in COMPARISON_TOKENS) {
                        // if we complete argument of == or !=, make types in expected info's nullable to allow items of nullable type too
                        additionalData =
                            ComparisonOperandAdditionalData(suppressNullLiteral = expectedType.optionality() == TypeOptionality.NOT_OPTION)
                        expectedType = expectedType.makeOption()
                    }

                    return listOf(ExpectedInfo(expectedType, expectedName, null, additionalData = additionalData))
                }
            }
        }
        return null
    }

    /**
     * 可空类型过滤器
     *
     * 只接受可空类型的过滤器。
     * 用于 `== null` 或 `!= null` 等比较中，只显示可空类型的候选项。
     *
     * **过滤规则**：
     * - 如果描述符类型是可空类型（Option类型），接受
     * - 如果描述符类型是非可空类型（NOT_OPTION），拒绝
     */
    private object NullableTypesFilter : ByTypeFilter {
        override fun matchingSubstitutor(descriptorType: FuzzyType) =
            if (descriptorType.type.optionality() != TypeOptionality.NOT_OPTION) ComposableTypeSubstitutor.EMPTY else null
    }

    /**
     * 计算 if 表达式的期望信息
     *
     * 检测表达式是否位于 if 表达式的条件、then 分支或 else 分支。
     * 根据不同位置计算不同的期望信息。
     *
     * **三种位置的处理**：
     * 1. **条件位置** (`if (|) ...`）：
     *    - 期望类型：Bool
     *    - 补全后缀：右括号
     *    - 附加数据：IfConditionAdditionalData
     *
     * 2. **then 分支** (`if (c) | else ...`）：
     *    - 继承 if 表达式自身的期望信息
     *    - 补全后缀：else 关键字
     *
     * 3. **else 分支** (`if (c) x else |`）：
     *    - 优先使用 if 表达式自身的期望信息
     *    - 过滤：只保留与 then 分支类型兼容的期望信息
     *    - 如果没有 if 期望信息，使用 then 分支的类型
     *    - 移除附加数据（避免干扰）
     *
     * **类型推断逻辑**：
     * if 表达式是一个表达式（有返回值），then 和 else 的类型应该兼容。
     * 因此，else 分支的期望类型会考虑 then 分支的类型。
     *
     * @param expressionWithType 当前表达式
     * @return Collection<ExpectedInfo>? 期望信息列表，如果不在 if 表达式中则返回 null
     */
    private fun calculateForIf(expressionWithType: CjExpression): Collection<ExpectedInfo>? {
        val ifExpression = (expressionWithType.parent as? CjContainerNode)?.parent as? CjIfExpression ?: return null
        when (expressionWithType) {
            ifExpression.condition -> return listOf(
                ExpectedInfo(
                    resolutionFacade.moduleDescriptor.builtIns.boolType,
                    null,
                    Tail.RPARENTH,
                    additionalData = IfConditionAdditionalData
                )
            )

            ifExpression.then -> return calculate(ifExpression).map { ExpectedInfo(it.filter, it.expectedName, Tail.ELSE) }

            ifExpression.`else` -> {
                val ifExpectedInfos = calculate(ifExpression)
                val thenType = ifExpression.then?.let { bindingContext.getType(it) }

                if (ifExpectedInfos.any { it.fuzzyType != null }) {
                    val filteredInfo = if (thenType != null && !thenType.isError)
                        ifExpectedInfos.filter { it.matchingSubstitutor(thenType) != null }
                    else
                        ifExpectedInfos
                    return filteredInfo.copyWithNoAdditionalData()
                } else if (thenType != null) {
                    return listOf(ExpectedInfo(thenType, null, null))
                }
            }
        }

        return null
    }

    /**
     * 计算 Elvis 运算符的期望信息
     *
     * Elvis 运算符（`??`）用于提供空值的默认值。
     * 检测表达式是否位于 Elvis 运算符的右侧。
     *
     * **语法**：`leftExpr ?? rightExpr`
     * - 如果 leftExpr 非 null，返回 leftExpr
     * - 如果 leftExpr 为 null，返回 rightExpr
     *
     * **期望类型计算**：
     * 1. 获取左侧表达式的类型（可空类型）
     * 2. 转换为非可空类型（makeNonOption）
     * 3. 使用非可空类型作为右侧的期望类型
     * 4. 同时考虑整个 Elvis 表达式的外部期望类型
     * 5. 过滤出与左侧非可空类型兼容的期望信息
     *
     * **类型推断示例**：
     * ```kotlin
     * val x: Int? = getValue()
     * val y: Int = x ?? |   // 右侧期望类型为 Int（x 的非可空类型）
     * ```
     *
     * @param expressionWithType 当前表达式
     * @return Collection<ExpectedInfo>? 期望信息列表，如果不在 Elvis 右侧则返回 null
     */
    private fun calculateForElvis(expressionWithType: CjExpression): Collection<ExpectedInfo>? {
        val binaryExpression = expressionWithType.parent as? CjBinaryExpression
        if (binaryExpression != null) {
            val operationToken = binaryExpression.operationToken
            if (operationToken == CjTokens.COALESCING && expressionWithType == binaryExpression.right) {
                val leftExpression = binaryExpression.left ?: return null
                val leftType = bindingContext.getType(leftExpression)
                val leftTypeNotNullable = leftType?.makeNonOption()
                val expectedInfos = calculate(binaryExpression)
                if (expectedInfos.any { it.fuzzyType != null }) {
                    val filteredInfo = if (leftTypeNotNullable != null)
                        expectedInfos.filter { it.matchingSubstitutor(leftTypeNotNullable) != null }
                    else
                        expectedInfos
                    return filteredInfo.copyWithNoAdditionalData()
                } else if (leftTypeNotNullable != null) {
                    return listOf(ExpectedInfo(leftTypeNotNullable, null, null))
                }
            }
        }
        return null
    }

    /**
     * 计算代码块表达式的期望信息
     *
     * 检测表达式是否位于代码块的最后一个语句位置。
     * 代码块的最后一个表达式的值是整个代码块的值。
     *
     * **两种代码块**：
     * 1. **Lambda 函数体**：
     *    - 计算 lambda 表达式的期望类型（函数类型）
     *    - 提取函数类型的返回类型
     *    - 使用返回类型作为最后一个表达式的期望类型
     *    - 补全后缀：右花括号
     *
     * 2. **普通代码块**：
     *    - 继承代码块自身的期望信息
     *    - 移除补全后缀（代码块内部不需要后缀）
     *
     * **Lambda 函数示例**：
     * ```kotlin
     * val f: (Int) -> String = { x ->
     *     val y = x * 2
     *     |   // 期望类型为 String（lambda 返回类型）
     * }
     * ```
     *
     * **普通代码块示例**：
     * ```kotlin
     * fun foo(): Int {
     *     val x = 10
     *     |   // 期望类型为 Int（函数返回类型）
     * }
     * ```
     *
     * @param expressionWithType 当前表达式
     * @return Collection<ExpectedInfo>? 期望信息列表，如果不在代码块最后则返回 null
     */
    private fun calculateForBlockExpression(expressionWithType: CjExpression): Collection<ExpectedInfo>? {
        val block = expressionWithType.parent as? CjBlockExpression ?: return null
        if (expressionWithType != block.statements.last()) return null

        val functionLiteral = block.parent as? CjFunctionLiteral
        return if (functionLiteral != null) {
            val literalExpression = functionLiteral.parent as CjLambdaExpression
            calculate(literalExpression)
                .mapNotNull { it.fuzzyType }
                .filter { it.type.isFunctionType }
                .map {
                    val returnType = it.type.getReturnTypeFromFunctionType()
                    ExpectedInfo(returnType.toFuzzyType(it.freeParameters), null, Tail.RBRACE)
                }
        } else {
            calculate(block).map { ExpectedInfo(it.filter, it.expectedName, null) }
        }
    }
//
//    private fun calculateForMatchEntryValue(expressionWithType: CjExpression): Collection<ExpectedInfo>? {
//        val condition = expressionWithType.parent as? CjMatchConditionWithExpression ?: return null
//        val entry = condition.parent as CjMatchEntry
//        val whenExpression = entry.parent as CjMatchExpression
//        val subject = whenExpression.subjectExpression
//        if (subject != null) {
//            val subjectType = bindingContext.getType(subject) ?: return null
//            return listOf(ExpectedInfo(subjectType, null, null, additionalData = MatchEntryAdditionalData(whenWithSubject = true)))
//        } else {
//            return listOf(
//                ExpectedInfo(
//                    resolutionFacade.moduleDescriptor.builtIns.boolType,
//                    null,
//                    null,
//                    additionalData = MatchEntryAdditionalData(whenWithSubject = false)
//                )
//            )
//        }
//    }

    private fun calculateForExclOperand(expressionWithType: CjExpression): Collection<ExpectedInfo>? {
        val prefixExpression = expressionWithType.parent as? CjPrefixExpression ?: return null
        if (prefixExpression.operationToken != CjTokens.EXCL) return null
        return listOf(ExpectedInfo(resolutionFacade.moduleDescriptor.builtIns.boolType, null, null))
    }

    private fun calculateForInitializer(expressionWithType: CjExpression): Collection<ExpectedInfo>? {
        val property = expressionWithType.parent as? CjVariable<*> ?: return null
        if (expressionWithType != property.initializer) return null
        val propertyDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, property] as? VariableDescriptor
            ?: return null
        val expectedName = propertyDescriptor.name.asString()
        val returnTypeToUse = returnTypeToUse(propertyDescriptor, hasExplicitReturnType = property.typeReference != null)
        val expectedInfo = if (returnTypeToUse != null)
            ExpectedInfo(returnTypeToUse, expectedName, null)
        else
            ExpectedInfo(ByTypeFilter.All, expectedName, null) // no explicit type or type from base - only expected name known
        return listOf(expectedInfo)
    }

    private fun calculateForExpressionBody(expressionWithType: CjExpression): Collection<ExpectedInfo>? {
        val declaration = expressionWithType.parent as? CjDeclarationWithBody ?: return null
        if (expressionWithType != declaration.bodyExpression || declaration.hasBlockBody()) return null
        val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] as? FunctionDescriptor ?: return null
        return listOfNotNull(functionReturnValueExpectedInfo(descriptor, hasExplicitReturnType = declaration.hasDeclaredReturnType()))
    }

    private fun calculateForReturn(expressionWithType: CjExpression): Collection<ExpectedInfo>? {
        val returnExpression = expressionWithType.parent as? CjReturnExpression ?: return null
        val descriptor = returnExpression.getTargetFunctionDescriptor(bindingContext) ?: return null
        return listOfNotNull(functionReturnValueExpectedInfo(descriptor, hasExplicitReturnType = true))
    }

    private fun functionReturnValueExpectedInfo(descriptor: FunctionDescriptor, hasExplicitReturnType: Boolean): ExpectedInfo? {
        return when (descriptor) {
            is SimpleFunctionDescriptor -> {
                ExpectedInfo.createForReturnValue(returnTypeToUse(descriptor, hasExplicitReturnType), descriptor)
            }

            is PropertyGetterDescriptor -> {
                val property = descriptor.correspondingProperty
                ExpectedInfo.createForReturnValue(returnTypeToUse(property, hasExplicitReturnType), property)
            }

            else -> null
        }
    }

    private fun returnTypeToUse(descriptor: CallableDescriptor, hasExplicitReturnType: Boolean): CangJieType? {
        return if (hasExplicitReturnType)
            descriptor.returnType
        else
            descriptor.overriddenDescriptors.singleOrNull()?.returnType
    }

    private fun calculateForLoopRange(expressionWithType: CjExpression): Collection<ExpectedInfo>? {
        val forExpression = (expressionWithType.parent as? CjContainerNode)?.parent as? CjForExpression ?: return null
        if (expressionWithType != forExpression.loopRange) return null

        val loopVar = forExpression.pattern
        val loopVarType = if (loopVar  != null)
            (bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, loopVar] as VariableDescriptor).type.takeUnless { it.isError }
        else
            null

        val scope = expressionWithType.getResolutionScope(bindingContext, resolutionFacade)
        val iterableDetector = resolutionFacade.ideService<IterableTypesDetection>().createDetector(scope)

        val byTypeFilter = object : ByTypeFilter {
            override fun matchingSubstitutor(descriptorType: FuzzyType): ComposableTypeSubstitutor? {
                return if (iterableDetector.isIterable(descriptorType, loopVarType)) ComposableTypeSubstitutor.EMPTY else null
            }
        }
        return listOf(ExpectedInfo(byTypeFilter, null, Tail.RPARENTH))
    }

    private fun calculateForInOperatorArgument(expressionWithType: CjExpression): Collection<ExpectedInfo>? {
        val binaryExpression = expressionWithType.parent as? CjBinaryExpression ?: return null
        val operationToken = binaryExpression.operationToken
        if (operationToken != CjTokens.IN_KEYWORD   || expressionWithType != binaryExpression.right) return null

        val leftOperandType = binaryExpression.left?.let { bindingContext.getType(it) } ?: return null
        val scope = expressionWithType.getResolutionScope(bindingContext, resolutionFacade)
        val detector = TypesWithContainsDetector(scope, indicesHelper, leftOperandType)

        val byTypeFilter = object : ByTypeFilter {
            override fun matchingSubstitutor(descriptorType: FuzzyType): ComposableTypeSubstitutor? {
                val operatorPair = detector.findOperator(descriptorType) ?: return null
                return operatorPair.second as? ComposableTypeSubstitutor
            }
        }
        return listOf(ExpectedInfo(byTypeFilter, null, null))
    }


    private fun getFromBindingContext(expressionWithType: CjExpression): Collection<ExpectedInfo>? {
        val expectedType = bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, expressionWithType] ?: return null
        return listOf(ExpectedInfo(expectedType, null, null))
    }

    private fun expectedNameFromExpression(expression: CjExpression?): String? {
        return when (expression) {
            is CjSimpleNameExpression -> expression.referencedName
            is CjQualifiedExpression -> expectedNameFromExpression(expression.selectorExpression)
            is CjCallExpression -> expectedNameFromExpression(expression.calleeExpression)
            is CjArrayAccessExpression -> expectedNameFromExpression(expression.arrayExpression)?.unpluralize()
            else -> null
        }
    }

    private fun String.unpluralize() = StringUtil.unpluralize(this)

    private fun Collection<ExpectedInfo>.copyWithNoAdditionalData() = map {
        it.copy(additionalData = null, itemOptions = ItemOptions.DEFAULT)
    }
}
val COMPARISON_TOKENS = setOf(CjTokens.EQEQ, CjTokens.EXCLEQ )
class ComparisonOperandAdditionalData(val suppressNullLiteral: Boolean) : ExpectedInfo.AdditionalData
object IfConditionAdditionalData : ExpectedInfo.AdditionalData
val ExpectedInfo.multipleFuzzyTypes: Collection<FuzzyType>
    get() = filter.multipleFuzzyTypes
