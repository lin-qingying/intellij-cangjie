/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.calls.components

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.cangnova.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import org.cangnova.cangjie.resolve.calls.inference.NewConstraintSystem
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintInjector
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.NewTypeVariable
import org.cangnova.cangjie.resolve.calls.inference.model.TypeVariableTypeConstructor
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.results.SimpleConstraintSystem
import org.cangnova.cangjie.resolve.calls.tower.CandidateFactoryProviderForInvoke
import org.cangnova.cangjie.resolve.calls.tower.ImplicitScopeTower
import org.cangnova.cangjie.resolve.constants.IntegerValueTypeConstant
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.StubTypeForBuilderInference
import org.cangnova.cangjie.types.UnwrappedType

/**
 * 仓颉解析无状态回调接口 (CangJie Resolution Stateless Callbacks)
 *
 * 这个接口提供了一组无状态的工具方法,用于在调用解析过程中查询各种属性和创建辅助对象。
 * 与 [CangJieResolutionCallbacks] 不同,这个接口中的所有方法都是**无状态**的,
 * 即它们不依赖于解析追踪(trace)或会话状态,可以安全地在任何上下文中调用。
 *
 * ## 无状态设计的优势
 * - **线程安全**: 可以在多线程环境中并发调用
 * - **可重用**: 同一个实例可以被多个解析过程共享
 * - **易测试**: 不需要设置复杂的状态即可测试
 * - **无副作用**: 调用不会影响其他解析过程
 *
 * ## 主要职责
 * 1. **调用特征检测**: 判断调用是否为中缀、操作符、构造器等特殊形式
 * 2. **可见性检查**: 判断描述符在解析中是否应该被隐藏
 * 3. **作用域提供**: 为可调用引用参数提供作用域塔
 * 4. **invoke 处理**: 获取 invoke 调用对应的变量候选
 * 5. **构建器推导**: 检测构建器推导相关的调用
 * 6. **约束系统创建**: 为重载解析创建约束系统
 *
 * ## 使用场景
 * ```kotlin
 * // 检查是否为操作符调用
 * if (statelessCallbacks.isOperatorCall(call)) {
 *     // 应用操作符特定的解析规则
 * }
 *
 * // 检查描述符是否在解析中隐藏
 * if (statelessCallbacks.isHiddenInResolution(descriptor, call, resolutionCallbacks)) {
 *     // 跳过此候选
 * }
 * ```
 *
 * @see CangJieResolutionCallbacks 有状态的解析回调接口
 * @see ResolutionCandidate 使用这些回调的解析候选
 */
interface CangJieResolutionStatelessCallbacks {
    /**
     * 判断描述符是否来自源代码
     *
     * 检查给定的可调用描述符是否定义在用户源代码中,而非来自库或编译器生成。
     * 这个信息用于重载解析中的优先级判断:源代码中的定义通常优先于库中的定义。
     *
     * @param descriptor 要检查的可调用描述符
     * @return 如果描述符来自源代码则返回 true
     */
    fun isDescriptorFromSource(descriptor: CallableDescriptor): Boolean

    /**
     * 判断是否为中缀调用
     *
     * 检查调用是否使用中缀语法(如 `a foo b`),这会影响解析规则和优先级。
     * 中缀调用只对标记为 `infix` 的函数有效。
     *
     * @param cangjieCall 要检查的调用
     * @return 如果是中缀调用则返回 true
     */
    fun isInfixCall(cangjieCall: CangJieCall): Boolean

    /**
     * 判断是否为操作符调用
     *
     * 检查调用是否使用操作符语法(如 `a + b`、`a[i]`)。
     * 操作符调用只对标记为 `operator` 的函数有效。
     *
     * @param cangjieCall 要检查的调用
     * @return 如果是操作符调用则返回 true
     */
    fun isOperatorCall(cangjieCall: CangJieCall): Boolean

    /**
     * 判断是否为 super 或委托构造器调用
     *
     * 检查调用是否为构造器中的 super 调用或委托构造器调用。
     * 这类调用有特殊的解析规则和限制。
     *
     * @param cangjieCall 要检查的调用
     * @return 如果是 super 或委托构造器调用则返回 true
     */
    fun isSuperOrDelegatingConstructorCall(cangjieCall: CangJieCall): Boolean

    /**
     * 判断描述符在解析中是否隐藏(基于参数)
     *
     * 检查给定的描述符在解析特定参数时是否应该被隐藏。
     * 某些描述符可能因为注解、可见性或其他原因在特定上下文中不可见。
     *
     * @param descriptor 要检查的声明描述符
     * @param cangjieCallArgument 相关的调用参数
     * @param resolutionCallbacks 解析回调(用于访问状态相关信息)
     * @return 如果描述符应该被隐藏则返回 true
     */
    fun isHiddenInResolution(
        descriptor: DeclarationDescriptor,
        cangjieCallArgument: CangJieCallArgument,
        resolutionCallbacks: CangJieResolutionCallbacks
    ): Boolean

    /**
     * 判断描述符在解析中是否隐藏(基于调用)
     *
     * 检查给定的描述符在解析特定调用时是否应该被隐藏。
     * 这是 [isHiddenInResolution] 的重载版本,基于整个调用而非单个参数。
     *
     * @param descriptor 要检查的声明描述符
     * @param cangjieCall 相关的调用
     * @param resolutionCallbacks 解析回调(用于访问状态相关信息)
     * @return 如果描述符应该被隐藏则返回 true
     */
    fun isHiddenInResolution(
        descriptor: DeclarationDescriptor, cangjieCall: CangJieCall, resolutionCallbacks: CangJieResolutionCallbacks
    ): Boolean

    /**
     * 判断是否为 super 表达式
     *
     * 检查接收者是否为 super 表达式。super 表达式用于访问父类成员,
     * 有特殊的解析规则(如跳过虚函数分发)。
     *
     * @param receiver 要检查的接收者参数
     * @return 如果是 super 表达式则返回 true
     */
    fun isSuperExpression(receiver: SimpleCangJieCallArgument?): Boolean

    /**
     * 获取可调用引用参数的作用域塔
     *
     * 为可调用引用参数(如 `::foo`)创建合适的作用域塔。
     * 作用域塔用于在解析可调用引用时查找候选函数或属性。
     *
     * @param argument 可调用引用参数
     * @return 用于解析该引用的作用域塔
     */
    fun getScopeTowerForCallableReferenceArgument(argument: CallableReferenceCangJieCallArgument): ImplicitScopeTower

    /**
     * 获取 invoke 调用对应的变量候选
     *
     * 当调用形如 `obj()` 时,首先需要将 `obj` 解析为变量,
     * 然后在该变量的类型上查找 `invoke` 操作符。
     * 此方法返回第一步解析出的变量候选。
     *
     * 示例:
     * ```kotlin
     * val lambda: (Int) -> String = { it.toString() }
     * lambda(42)  // 这是 invoke 调用,lambda 是变量候选
     * ```
     *
     * @param functionCall 函数调用
     * @return 变量候选,如果不是 invoke 调用则返回 null
     */
    fun getVariableCandidateIfInvoke(functionCall: CangJieCall): ResolutionCandidate?

    /**
     * 判断是否为构建器推导调用
     *
     * 检查参数和参数是否涉及构建器推导(Builder Inference)。
     * 构建器推导是一种特殊的类型推导模式,用于推导构建器 lambda 的类型参数。
     *
     * 示例:
     * ```kotlin
     * buildList {  // 这里使用构建器推导
     *     add(1)
     *     add(2)
     * }
     * ```
     *
     * @param argument 调用参数
     * @param parameter 对应的值参数描述符
     * @return 如果是构建器推导调用则返回 true
     */
    fun isBuilderInferenceCall(argument: CangJieCallArgument, parameter: ValueParameterDescriptor): Boolean

    /**
     * 判断类型集合的旧交集是否为空
     *
     * 使用旧的类型系统规则判断给定类型集合的交集是否为空。
     * 这用于兼容性检查和特定的类型推导场景。
     *
     * @param types 要检查的类型集合
     * @return 如果交集为空则返回 true
     */
    fun isOldIntersectionIsEmpty(types: Collection<CangJieType>): Boolean

    /**
     * 为重载解析创建约束系统
     *
     * 创建一个简化的约束系统,专门用于重载解析中的候选比较。
     * 这个约束系统不需要完整的类型推导能力,只需支持类型兼容性检查。
     *
     * @param constraintInjector 约束注入器,用于生成类型约束
     * @param builtIns 内置类型定义
     * @return 用于重载解析的简单约束系统
     */
    fun createConstraintSystemForOverloadResolution(
        constraintInjector: ConstraintInjector, builtIns: CangJieBuiltIns
    ): SimpleConstraintSystem
}

/**
 * 仓颉解析有状态回调接口 (CangJie Resolution Callbacks)
 *
 * 这个接口提供了一组**有状态**的解析回调方法,用于在调用解析过程中执行需要访问或修改
 * 解析追踪(trace)和会话状态的操作。与 [CangJieResolutionStatelessCallbacks] 不同,
 * 这些方法会产生副作用或依赖于当前的解析上下文。
 *
 * **重要**: 这些组件持有状态(trace),使用时需要格外小心。
 * (This components hold state (trace). Work with this carefully.)
 *
 * ## 有状态设计的含义
 * - **依赖追踪**: 方法依赖于当前的解析追踪,记录解析决策
 * - **修改状态**: 方法可能修改解析会话的状态
 * - **非线程安全**: 不能在多线程环境中并发调用同一实例
 * - **上下文敏感**: 调用结果取决于之前的解析历史
 *
 * ## 主要职责
 * 1. **Lambda 分析**: 分析 lambda 表达式并获取返回参数信息
 * 2. **invoke 候选工厂**: 为 invoke 调用创建候选工厂
 * 3. **可调用引用解析**: 解析可调用引用参数
 * 4. **类型查询**: 从约束系统中查询推导结果
 * 5. **追踪记录**: 在追踪中记录类型信息和解析决策
 * 6. **契约管理**: 根据需要禁用契约
 *
 * ## 使用场景
 * ```kotlin
 * // 分析 lambda 返回值
 * val result = resolutionCallbacks.analyzeAndGetLambdaReturnArguments(
 *     lambdaArgument, receiverType, parameters, expectedReturnType,
 *     annotations, stubsForPostponedVariables
 * )
 *
 * // 解析可调用引用
 * val candidates = resolutionCallbacks.resolveCallableReferenceArgument(
 *     argument, expectedType, baseSystem
 * )
 * ```
 *
 * @see CangJieResolutionStatelessCallbacks 无状态的解析回调接口
 * @see InferenceSession 类型推导会话
 */
interface CangJieResolutionCallbacks {
    /**
     * 分析 Lambda 并获取返回参数
     *
     * 这是 lambda 类型推导的核心方法。它会:
     * 1. 分析 lambda 体,确定所有返回表达式
     * 2. 收集最后一个表达式的信息
     * 3. 判断是否有显式 return 语句
     * 4. 处理强制转换为 Unit 的情况
     *
     * ## 延迟分析
     * Lambda 分析通常被延迟到获得足够的类型信息后进行。在初始阶段,
     * lambda 的参数类型可能未知,此时会使用存根类型作为占位符。
     *
     * ## 示例场景
     * ```kotlin
     * fun <T> foo(x: T, block: (T) -> Unit) { ... }
     * foo(42) { it ->  // lambda 的参数类型从第一个参数推导
     *     println(it)  // 最后表达式,类型为 Unit
     * }
     * ```
     *
     * @param lambdaArgument Lambda 调用参数
     * @param receiverType Lambda 接收者类型(如果有)
     * @param parameters Lambda 参数类型列表
     * @param expectedReturnType 期望的返回类型,null 表示返回类型不确定(依赖于类型变量)
     * @param annotations Lambda 的注解
     * @param stubsForPostponedVariables 延迟类型变量到存根类型的映射
     * @return Lambda 返回参数分析结果,包含返回参数信息和推导会话
     */
    fun analyzeAndGetLambdaReturnArguments(
        lambdaArgument: LambdaCangJieCallArgument,

        receiverType: UnwrappedType?,
        parameters: List<UnwrappedType>,
        expectedReturnType: UnwrappedType?, // null means, that return type is not proper i.e. it depends on some type variables
        annotations: Annotations,
        stubsForPostponedVariables: Map<NewTypeVariable, StubTypeForBuilderInference>,
    ): ReturnArgumentsAnalysisResult

    /**
     * 获取 invoke 调用的候选工厂
     *
     * 当解析 `obj()` 形式的调用时,需要两步:
     * 1. 将 `obj` 解析为变量
     * 2. 在该变量的类型上查找 `invoke` 操作符
     *
     * 此方法为第二步创建候选工厂提供者,用于生成 invoke 操作符的解析候选。
     *
     * @param scopeTower 作用域塔,用于符号查找
     * @param cangjieCall 函数调用
     * @return invoke 候选工厂提供者
     */
    fun getCandidateFactoryForInvoke(
        scopeTower: ImplicitScopeTower,
        cangjieCall: CangJieCall,
    ): CandidateFactoryProviderForInvoke<ResolutionCandidate>

    /**
     * 解析可调用引用参数
     *
     * 解析可调用引用(如 `::foo`、`String::length`)到具体的函数或属性。
     * 返回所有可能的候选,后续会通过重载解析选择最合适的那个。
     *
     * ## 解析策略
     * - **无绑定引用**: `::foo` - 在当前作用域中查找
     * - **类成员引用**: `String::length` - 在指定类型的成员中查找
     * - **实例成员引用**: `obj::method` - 绑定到特定实例
     *
     * @param argument 可调用引用参数
     * @param expectedType 期望的函数类型(如 `(String) -> Int`)
     * @param baseSystem 基础约束系统,包含外部上下文的约束
     * @return 所有可能的可调用引用候选集合
     */
    fun resolveCallableReferenceArgument(
        argument: CallableReferenceCangJieCallArgument,
        expectedType: UnwrappedType?,
        baseSystem: ConstraintStorage,
    ): Collection<CallableReferenceResolutionCandidate>

    /**
     * 查找类型变量的结果类型
     *
     * 从约束系统中查询指定类型变量的推导结果。
     * 如果类型变量尚未固定或推导失败,返回 null。
     *
     * @param constraintSystem 约束系统
     * @param typeVariable 类型变量构造器
     * @return 推导出的类型,如果未能推导则返回 null
     */
    fun findResultType(constraintSystem: NewConstraintSystem, typeVariable: TypeVariableTypeConstructor): CangJieType?

    /**
     * 创建空的约束系统
     *
     * 创建一个新的约束系统实例,用于类型推导。
     * 这个约束系统会连接到当前的推导会话,以便共享类型信息。
     *
     * @return 新的约束系统实例
     */
    fun createEmptyConstraintSystem(): NewConstraintSystem

    /**
     * 为候选绑定存根已解析调用
     *
     * 在解析过程中,为候选创建存根解析调用并绑定到追踪中。
     * 这允许后续代码在完成完整解析前访问部分解析信息。
     *
     * @param candidate 已解析的调用原子
     */
    fun bindStubResolvedCallForCandidate(candidate: ResolvedCallAtom)

    /**
     * 将有符号常量转换为无符号常量
     *
     * 当参数期望无符号类型但提供了有符号字面量时,
     * 尝试将其转换为对应的无符号类型常量。
     *
     * 示例:
     * ```kotlin
     * fun foo(x: UInt) { ... }
     * foo(42)  // 42 被转换为 42u
     * ```
     *
     * @param argument 调用参数
     * @return 转换后的无符号整数常量,如果无法转换则返回 null
     */
    fun convertSignedConstantToUnsigned(argument: CangJieCallArgument): IntegerValueTypeConstant?

    /**
     * 当前的推导会话
     *
     * 推导会话维护跨多个调用的类型推导状态,用于处理复杂的类型推导场景,
     * 如构建器推导、延迟 lambda 分析等。
     */
    val inferenceSession: InferenceSession

    /**
     * 从 as 表达式获取期望类型并记录到追踪
     *
     * 当调用出现在 `as` 表达式中时(如 `foo() as String`),
     * 提取期望类型并记录到解析追踪中,以便用于类型推导。
     *
     * @param resolvedAtom 已解析的调用原子
     * @return 从 as 表达式推导出的期望类型
     */
    fun getExpectedTypeFromAsExpressionAndRecordItInTrace(resolvedAtom: ResolvedCallAtom): UnwrappedType?

    /**
     * 必要时禁用契约
     *
     * 在某些情况下(如错误恢复、不完整的解析),需要禁用函数契约,
     * 以避免基于不可靠信息的智能类型转换。
     *
     * @param resolvedAtom 已解析的调用原子
     */
    fun disableContractsIfNecessary(resolvedAtom: ResolvedCallAtom)

    /**
     * 获取左侧结果
     *
     * 对于赋值或属性访问,获取左侧表达式的解析结果。
     * 左侧结果包含变量、属性或其他可赋值的目标。
     *
     * @param call 调用
     * @return 左侧表达式的解析结果
     */
    fun getLhsResult(call: CangJieCall): LHSResult

//    fun convertSignedConstantToUnsigned(argument: CangJieCallArgument): IntegerValueTypeConstant?

//    fun recordInlinabilityOfLambda(atom: Set<Map.Entry<SimpleResolutionCandidate, ResolvedLambdaAtom>>)
}

/**
 * Lambda 返回参数信息 (Return Arguments Info)
 *
 * 这个数据类封装了 lambda 表达式返回参数的分析结果。在 lambda 类型推导中,
 * 需要分析 lambda 体内的所有返回表达式,以推导 lambda 的返回类型。
 *
 * ## Lambda 返回语义
 * Lambda 的返回值可以来自两个来源:
 * 1. **显式 return 语句**: `return@label value`
 * 2. **最后一个表达式**: Lambda 体的最后一个表达式自动作为返回值
 *
 * ## 特殊情况: Unit 强制转换
 * 当 lambda 期望返回 Unit 时,最后一个表达式的值会被忽略(强制转换为 Unit)。
 * [lastExpressionCoercedToUnit] 标志指示是否发生了这种强制转换。
 *
 * ## 示例场景
 * ```kotlin
 * // 场景 1: 最后表达式作为返回值
 * val lambda1: (Int) -> String = { it.toString() }  // lastExpression = it.toString()
 *
 * // 场景 2: 显式 return 语句
 * val lambda2: (Int) -> String = {
 *     if (it > 0) return@lambda2 "positive"  // returnArgument
 *     "non-positive"  // lastExpression
 * }
 *
 * // 场景 3: Unit 强制转换
 * val lambda3: (Int) -> Unit = { it.toString() }  // lastExpressionCoercedToUnit = true
 * ```
 *
 * @property nonErrorArguments 所有非错误的返回参数列表(包括显式 return 和最后表达式)
 * @property lastExpression Lambda 体的最后一个表达式,可能为 null(空 lambda 或只有 return 语句)
 * @property lastExpressionCoercedToUnit 最后一个表达式是否被强制转换为 Unit
 * @property returnArgumentsExist 是否存在显式 return 语句
 *
 * @see ReturnArgumentsAnalysisResult Lambda 返回参数分析结果的包装类
 */
data class ReturnArgumentsInfo(
    val nonErrorArguments: List<CangJieCallArgument>,
    val lastExpression: CangJieCallArgument?,
    val lastExpressionCoercedToUnit: Boolean,
    val returnArgumentsExist: Boolean
) {
    companion object {
        /**
         * 空的返回参数信息
         *
         * 用于表示空 lambda 或分析失败的情况。
         * 所有字段都设置为空或 false。
         */
        val empty =
            ReturnArgumentsInfo(emptyList(), null, lastExpressionCoercedToUnit = false, returnArgumentsExist = false)
    }
}

/**
 * Lambda 返回参数分析结果 (Return Arguments Analysis Result)
 *
 * 这个数据类是 [ReturnArgumentsInfo] 的包装,提供了额外的上下文信息:
 * - **推导会话**: 用于继续类型推导
 * - **构建器推导失败标志**: 指示构建器推导是否遇到不适用的调用
 *
 * ## 推导会话的作用
 * Lambda 分析可能创建新的推导会话来处理嵌套的类型推导。
 * 例如,lambda 体内的函数调用可能引入新的类型变量,
 * 这些变量的约束需要与外层推导会话共享。
 *
 * ## 构建器推导
 * 构建器推导(Builder Inference)是一种高级类型推导技术,用于推导构建器 lambda 的类型参数。
 * 如果 lambda 体内包含无法解析的调用,[hasInapplicableCallForBuilderInference] 会被设置为 true,
 * 指示构建器推导失败,需要回退到其他策略。
 *
 * @property returnArgumentsInfo Lambda 返回参数的详细信息
 * @property inferenceSession 推导会话,用于处理嵌套的类型推导
 * @property hasInapplicableCallForBuilderInference 是否因不适用的调用导致构建器推导失败
 *
 * @see ReturnArgumentsInfo 包装的返回参数信息
 * @see InferenceSession 类型推导会话
 */
data class ReturnArgumentsAnalysisResult(
    val returnArgumentsInfo: ReturnArgumentsInfo,
    val inferenceSession: InferenceSession?,
    val hasInapplicableCallForBuilderInference: Boolean = false
)
