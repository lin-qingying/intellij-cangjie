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

package org.cangnova.cangjie.resolve.calls.tower

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjPsiUtil
import org.cangnova.cangjie.psi.CjReturnExpression
import org.cangnova.cangjie.psi.psiUtil.getBinaryWithTypeParent
import org.cangnova.cangjie.psi.psiUtil.lastBlockStatementOrThis
import org.cangnova.cangjie.resolve.DoubleColonExpressionResolver
import org.cangnova.cangjie.resolve.MissingSupertypesResolver
import org.cangnova.cangjie.resolve.TypeResolver
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.TemporaryBindingTrace
import org.cangnova.cangjie.resolve.builtIns
import org.cangnova.cangjie.resolve.calls.ArgumentTypeResolver
import org.cangnova.cangjie.resolve.calls.CangJieCallResolver
import org.cangnova.cangjie.resolve.calls.components.*
import org.cangnova.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.cangnova.cangjie.resolve.calls.context.BasicCallResolutionContext
import org.cangnova.cangjie.resolve.calls.context.ContextDependency
import org.cangnova.cangjie.resolve.calls.inference.BuilderInferenceSession
import org.cangnova.cangjie.resolve.calls.inference.NewConstraintSystem
import org.cangnova.cangjie.resolve.calls.inference.components.CangJieConstraintSystemCompleter
import org.cangnova.cangjie.resolve.calls.inference.components.ResultTypeResolver
import org.cangnova.cangjie.resolve.calls.inference.components.TypeVariableDirectionCalculator
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.NewTypeVariable
import org.cangnova.cangjie.resolve.calls.inference.model.TypeVariableTypeConstructor
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.calls.util.CallMaker
import org.cangnova.cangjie.resolve.calls.util.extractCallableReferenceExpression
import org.cangnova.cangjie.resolve.constants.CompileTimeConstant
import org.cangnova.cangjie.resolve.constants.IntegerValueTypeConstant
import org.cangnova.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.cangnova.cangjie.resolve.deprecation.DeprecationResolver
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isUnit
import org.cangnova.cangjie.types.expressions.CangJieTypeInfo
import org.cangnova.cangjie.types.expressions.ExpressionTypingServices
import org.cangnova.cangjie.utils.isFunctionForExpectTypeFromCastFeature

/**
 * Lambda 上下文信息
 *
 * 存储 lambda 表达式解析过程中的上下文数据,包括类型信息、数据流信息、作用域和绑定跟踪。
 *
 * @property typeInfo lambda 表达式的类型信息
 * @property dataFlowInfoAfter lambda 执行后的数据流信息
 * @property lexicalScope lambda 的词法作用域
 * @property trace 用于记录绑定信息的跟踪对象
 */
data class LambdaContextInfo(
    var typeInfo: CangJieTypeInfo? = null,
    var dataFlowInfoAfter: DataFlowInfo? = null,
    var lexicalScope: LexicalScope? = null,
    var trace: BindingTrace? = null
)

/**
 * 仓颉语言解析回调实现
 *
 * 该类是仓颉语言新类型推导系统中的核心回调接口实现,负责处理调用解析过程中的各种回调操作,
 * 包括 lambda 表达式分析、可调用引用解析、约束系统管理等。
 *
 * 主要职责:
 * 1. **Lambda 表达式分析**: 分析 lambda 表达式的返回值类型,处理 return 语句和最后一个表达式
 * 2. **可调用引用解析**: 解析可调用引用(如函数引用、属性引用)的候选项
 * 3. **类型推导**: 在约束系统中查找类型变量的结果类型
 * 4. **常量转换**: 处理有符号常量到无符号常量的转换
 * 5. **as 表达式处理**: 从 as 类型转换表达式中提取期望类型
 * 6. **Builder 推导支持**: 支持 Kotlin 风格的 builder 推导会话
 *
 * @property trace 绑定跟踪对象,用于记录解析过程中的绑定信息
 * @property expressionTypingServices 表达式类型推导服务
 * @property typeApproximator 类型近似器,用于类型近似计算
 * @property argumentTypeResolver 参数类型解析器
 * @property languageVersionSettings 语言版本设置
 * @property cangjieToResolvedCallTransformer 仓颉调用到已解析调用的转换器
 * @property dataFlowValueFactory 数据流值工厂
 * @property inferenceSession 推导会话
 * @property constantExpressionEvaluator 常量表达式求值器
 * @property typeResolver 类型解析器
 * @property psiCallResolver PSI 调用解析器
 * @property postponedArgumentsAnalyzer 延迟参数分析器
 * @property cangjieConstraintSystemCompleter 约束系统完成器
 * @property callComponents 调用组件集合
 * @property doubleColonExpressionResolver 双冒号表达式解析器(可调用引用)
 * @property deprecationResolver 废弃警告解析器
 * @property moduleDescriptor 模块描述符
 * @property topLevelCallContext 顶层调用解析上下文
 * @property missingSupertypesResolver 缺失父类型解析器
 * @property cangjieCallResolver 仓颉调用解析器
 * @property resultTypeResolver 结果类型解析器
 */
class CangJieResolutionCallbacksImpl(
    val trace: BindingTrace,
    private val expressionTypingServices: ExpressionTypingServices,
    private val typeApproximator: TypeApproximator,
    private val argumentTypeResolver: ArgumentTypeResolver,
    private val languageVersionSettings: LanguageVersionSettings,
    private val cangjieToResolvedCallTransformer: CangJieToResolvedCallTransformer,
    private val dataFlowValueFactory: DataFlowValueFactory,
    override val inferenceSession: InferenceSession,
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,
    private val typeResolver: TypeResolver,
    private val psiCallResolver: PSICallResolver,
    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
    private val cangjieConstraintSystemCompleter: CangJieConstraintSystemCompleter,
    private val callComponents: CangJieCallComponents,
    private val doubleColonExpressionResolver: DoubleColonExpressionResolver,
    private val deprecationResolver: DeprecationResolver,
    private val moduleDescriptor: ModuleDescriptor,
    private val topLevelCallContext: BasicCallResolutionContext,
    private val missingSupertypesResolver: MissingSupertypesResolver,
    private val cangjieCallResolver: CangJieCallResolver,
    private val resultTypeResolver: ResultTypeResolver,
) : CangJieResolutionCallbacks {
    /**
     * 解析可调用引用参数
     *
     * 将可调用引用(如 `::functionName` 或 `ClassName::methodName`)作为参数传递时,
     * 解析所有可能的候选项。
     *
     * @param argument 可调用引用参数
     * @param expectedType 期望的类型(如果有)
     * @param baseSystem 基础约束系统
     * @return 可调用引用的候选项集合
     */
    override fun resolveCallableReferenceArgument(
        argument: CallableReferenceCangJieCallArgument,
        expectedType: UnwrappedType?,
        baseSystem: ConstraintStorage
    ): Collection<CallableReferenceResolutionCandidate> =
        cangjieCallResolver.resolveCallableReferenceArgument(argument, expectedType, baseSystem, this)

    /**
     * Lambda 信息类
     *
     * 存储 lambda 表达式解析过程中收集的信息,包括所有 return 语句和最后一个表达式的上下文。
     *
     * @property expectedType lambda 表达式的期望返回类型
     * @property contextDependency 上下文依赖类型(独立或依赖)
     * @property returnStatements 所有显式 return 语句及其上下文信息的列表
     * @property lastExpressionInfo 最后一个表达式的上下文信息
     */
    class LambdaInfo(val expectedType: UnwrappedType, val contextDependency: ContextDependency) {
        val returnStatements = ArrayList<Pair<CjReturnExpression, LambdaContextInfo?>>()
        val lastExpressionInfo = LambdaContextInfo()

        companion object {
            /** 空的占位 LambdaInfo,用于清空绑定上下文中的 lambda 信息 */
            val STUB_EMPTY = LambdaInfo(TypeUtils.NO_EXPECTED_TYPE, ContextDependency.INDEPENDENT)
        }
    }

    /**
     * 分析并获取 Lambda 返回参数
     *
     * 这是最复杂的方法之一,负责完整地分析 lambda 表达式体,包括:
     * 1. 收集所有显式 return 语句
     * 2. 处理最后一个表达式作为隐式返回值
     * 3. 创建 builder 推导会话(如果需要)
     * 4. 处理 Unit 类型的强制转换
     *
     * 工作流程:
     * - 构建 lambda 的完整函数类型(包括接收者、参数和返回类型)
     * - 在临时 trace 中解析 lambda 体(用于 builder 推导)
     * - 收集所有 return 语句并创建对应的调用参数
     * - 处理最后一个表达式(如果不是强制为 Unit)
     *
     * @param lambdaArgument lambda 调用参数
     * @param receiverType lambda 的接收者类型(如果有,如扩展 lambda)
     * @param parameters lambda 的参数类型列表
     * @param expectedReturnType lambda 的期望返回类型
     * @param annotations lambda 类型的注解
     * @param stubsForPostponedVariables 用于 builder 推导的延迟类型变量桩
     * @return 返回参数分析结果,包括所有返回参数和可能的 builder 推导会话
     */
    override fun analyzeAndGetLambdaReturnArguments(
        lambdaArgument: LambdaCangJieCallArgument,
        receiverType: UnwrappedType?,
        parameters: List<UnwrappedType>,
        expectedReturnType: UnwrappedType?,
        annotations: Annotations,
        stubsForPostponedVariables: Map<NewTypeVariable, StubTypeForBuilderInference>
    ): ReturnArgumentsAnalysisResult {
        val psiCallArgument = lambdaArgument.psiCallArgument as PSIFunctionCangJieCallArgument
        val outerCallContext = psiCallArgument.outerCallContext

        /**
         * 创建调用参数
         *
         * 从表达式和类型信息创建 PSICangJieCallArgument,用于后续的类型推导。
         * 如果表达式是函数表达式或 lambda,会进行特殊处理。
         *
         * @param cjExpression 仓颉表达式
         * @param typeInfo 表达式的类型信息
         * @param scope 词法作用域(如果有)
         * @param newTrace 新的绑定跟踪对象(如果有)
         * @return 创建的调用参数,如果无法创建则返回 null
         */
        fun createCallArgument(
            cjExpression: CjExpression,
            typeInfo: CangJieTypeInfo,
            scope: LexicalScope?,
            newTrace: BindingTrace?
        ): PSICangJieCallArgument? {
            var newContext = outerCallContext
            if (scope != null) newContext = newContext.replaceScope(scope)
            if (newTrace != null) newContext = newContext.replaceBindingTrace(newTrace)

            // 尝试将表达式处理为函数表达式(lambda 或匿名函数)
            processFunctionalExpression(
                newContext, cjExpression, typeInfo.dataFlowInfo, CallMaker.makeExternalValueArgument(cjExpression),
                null, outerCallContext.scope.ownerDescriptor.builtIns, typeResolver
            )?.let {
                it.setResultDataFlowInfoIfRelevant(typeInfo.dataFlowInfo)
                return it
            }

            val deparenthesizedExpression = CjPsiUtil.deparenthesize(cjExpression) ?: cjExpression

            // 创建简单的 PSI 调用参数
            return createSimplePSICallArgument(
                trace.bindingContext,
                outerCallContext.statementFilter,
                outerCallContext.scope.ownerDescriptor,
                CallMaker.makeExternalValueArgument(cjExpression),
                DataFlowInfo.EMPTY,
                typeInfo,
                languageVersionSettings,
                dataFlowValueFactory,
                outerCallContext.call
            )
//
//            // 注释掉的代码:处理可调用引用的替代方案
//            return if (deparenthesizedExpression is CjCallableReferenceExpression) {
//                psiCallResolver.createCallableReferenceCangJieCallArgument(
//                    newContext, deparenthesizedExpression, DataFlowInfo.EMPTY,
//                    CallMaker.makeExternalValueArgument(deparenthesizedExpression),
//                    argumentName = null,
//                    outerCallContext,
//                    tracingStrategy = TracingStrategyImpl.create(deparenthesizedExpression.callableReference, newContext.call)
//                )
//            } else {
//                createSimplePSICallArgument(
//                    trace.bindingContext, outerCallContext.statementFilter, outerCallContext.scope.ownerDescriptor,
//                    CallMaker.makeExternalValueArgument(cjExpression), DataFlowInfo.EMPTY, typeInfo, languageVersionSettings,
//                    dataFlowValueFactory, outerCallContext.call
//                )
//            }
        }

        // 创建 LambdaInfo 对象来收集 lambda 信息
        val lambdaInfo = LambdaInfo(
            expectedReturnType ?: TypeUtils.NO_EXPECTED_TYPE,
            if (expectedReturnType == null) ContextDependency.DEPENDENT else ContextDependency.INDEPENDENT
        )

        val builtIns = outerCallContext.scope.ownerDescriptor.builtIns

        // 我们必须精化 receiverType,因为 lambda 内的名称解析需要从接收者获取正确的作用域,
        // 而对于隐式接收者,没有表达式的类型会在 ExpTypingVisitor 中被精化。
        // 相关测试: multiplatformTypeRefinement/lambdas
        //
        // 在其他带有隐式接收者的类似情况下不会发生这种情况(例如,扩展函数内的扩展接收者作用域),
        // 因为在类型解析期间我们正确地区分了头部。
        //
        // 另外注意,精化整个类型可能是不希望的,因为有时它包含 NO_EXPECTED_TYPE,
        // 在尝试调用 equals 时会抛出异常。
        val refinedReceiverType = receiverType?.let {
            callComponents.cangjieTypeChecker.cangjieTypeRefiner.refineType(it)
        }


        // 构建完整的函数类型(接收者 + 参数 + 返回类型)
        val expectedType = createFunctionType(
            builtIns, annotations, refinedReceiverType, parameters, null,
            lambdaInfo.expectedType
        )

        // 将期望类型近似为子类型(用于局部声明)
        val approximatesExpectedType =
            typeApproximator.approximateToSubType(expectedType, TypeApproximatorConfiguration.LocalDeclaration)
                ?: expectedType

        // 如果有延迟的类型变量桩,创建 builder 推导会话
        // Builder 推导用于处理如 Kotlin 的 buildList { } 这样的 DSL 构建器
        val builderInferenceSession =
            if (stubsForPostponedVariables.isNotEmpty()) {
                BuilderInferenceSession(
                    psiCallResolver,
                    postponedArgumentsAnalyzer,
                    cangjieConstraintSystemCompleter,
                    callComponents,
                    builtIns,
                    topLevelCallContext,
                    stubsForPostponedVariables,
                    trace,
                    cangjieToResolvedCallTransformer,
                    expressionTypingServices,
                    argumentTypeResolver,
                    doubleColonExpressionResolver,
                    deprecationResolver,
                    moduleDescriptor,
                    typeApproximator,
                    missingSupertypesResolver,
                    lambdaArgument
                ).apply { lambdaArgument.builderInferenceSession = this }
            } else {
                null
            }

        // 如果有 builder 推导会话,使用临时 trace 来避免污染主 trace
        val temporaryTrace = if (builderInferenceSession != null)
            TemporaryBindingTrace.create(trace, "Trace to resolveName builder inference lambda: $lambdaArgument")
        else
            null

        // 将 lambda 信息记录到绑定上下文中,供 lambda 体内的表达式类型推导使用
        (temporaryTrace ?: trace).record(
            BindingContext.NEW_INFERENCE_LAMBDA_INFO,
            psiCallArgument.cjFunction,
            lambdaInfo
        )

        // 创建实际的解析上下文,替换相关属性
        val actualContext = outerCallContext
            .replaceBindingTrace(temporaryTrace ?: trace)
            .replaceContextDependency(lambdaInfo.contextDependency)
            .replaceExpectedType(approximatesExpectedType)
            .replaceDataFlowInfo(psiCallArgument.dataFlowInfoBeforeThisArgument).let {
                if (builderInferenceSession != null) it.replaceInferenceSession(builderInferenceSession) else it
            }

        // 解析 lambda 函数体,获取类型信息
        val functionTypeInfo = expressionTypingServices.getTypeInfo(psiCallArgument.expression, actualContext)

        // 清空 lambda 信息标记,防止后续访问
        (temporaryTrace ?: trace).record(
            BindingContext.NEW_INFERENCE_LAMBDA_INFO,
            psiCallArgument.cjFunction,
            LambdaInfo.STUB_EMPTY
        )

        // 如果 builder 推导会话发现不适用的调用,返回空结果
        if (builderInferenceSession?.hasInapplicableCall() == true) {
            return ReturnArgumentsAnalysisResult(
                ReturnArgumentsInfo.empty, builderInferenceSession, hasInapplicableCallForBuilderInference = true
            )
        } else {
            // 否则,提交临时 trace 到主 trace
            temporaryTrace?.commit()
        }

        // 处理所有显式 return 语句
        var hasReturnWithoutExpression = false
        var returnArgumentFound = false
        val returnArguments = lambdaInfo.returnStatements.mapNotNullTo(ArrayList()) { (expression, contextInfo) ->
            returnArgumentFound = true
            val returnedExpression = expression.returnedExpression
            if (returnedExpression != null) {
                // return 带有表达式,创建对应的调用参数
                createCallArgument(
                    returnedExpression,
                    contextInfo?.typeInfo
                        ?: throw AssertionError("typeInfo should be non-null for return with expression"),
                    contextInfo.lexicalScope,
                    contextInfo.trace
                )
            } else {
                // return 不带表达式(返回 Unit)
                hasReturnWithoutExpression = true
                EmptyLabeledReturn(expression, builtIns)
            }
        }

        // 获取最后一个去括号化的表达式,可能作为隐式返回值
        val lastExpressionArgument = getLastDeparentesizedExpression(psiCallArgument)?.let { lastExpression ->
            // 如果最后一个表达式已经是 return 语句,跳过
            if (lambdaInfo.returnStatements.any { (expression, _) -> expression == lastExpression }) {
                return@let null
            }

            val lastExpressionType = trace.getType(lastExpression)
            val contextInfo = lambdaInfo.lastExpressionInfo
            val lastExpressionTypeInfo =
                CangJieTypeInfo(lastExpressionType, contextInfo.dataFlowInfoAfter ?: functionTypeInfo.dataFlowInfo)
            createCallArgument(lastExpression, lastExpressionTypeInfo, contextInfo.lexicalScope, contextInfo.trace)
        }

        // 判断是否需要将最后一个表达式强制为 Unit
        // 条件: 期望返回 Unit 或者有不带表达式的 return 语句
        val lastExpressionCoercedToUnit = expectedReturnType?.isUnit() == true || hasReturnWithoutExpression
        if (!lastExpressionCoercedToUnit && lastExpressionArgument != null) {
            returnArgumentFound = true
            returnArguments += lastExpressionArgument
        }

        // 返回分析结果
        return ReturnArgumentsAnalysisResult(
            ReturnArgumentsInfo(
                returnArguments,
                lastExpressionArgument,
                lastExpressionCoercedToUnit,
                returnArgumentFound
            ),
            builderInferenceSession,
        )
    }

    /**
     * 获取最后一个去括号化的表达式
     *
     * 从 lambda 或函数表达式中提取最后一个语句,并去除外层括号。
     *
     * @param psiCallArgument PSI 调用参数(lambda 或函数表达式)
     * @return 去括号化后的最后一个表达式,如果没有则返回 null
     */
    private fun getLastDeparentesizedExpression(psiCallArgument: PSICangJieCallArgument): CjExpression? {
        val lastExpression = if (psiCallArgument is LambdaCangJieCallArgumentImpl) {
            // Lambda 表达式: 获取 body 中的最后一个语句
            psiCallArgument.cjLambdaExpression.bodyExpression?.statements?.lastOrNull()
        } else {
            // 函数表达式: 获取 body 中的最后一个语句(块语句)
            (psiCallArgument as FunctionExpressionImpl).cjFunction.bodyExpression?.lastBlockStatementOrThis()
        }

        return CjPsiUtil.deparenthesize(lastExpression)
    }

    /**
     * 获取 invoke 调用的候选工厂
     *
     * 当对象被当作函数调用时(如 `obj(args)`),需要解析 `invoke` 操作符。
     * 这个方法返回用于创建 invoke 候选项的工厂。
     *
     * @param scopeTower 隐式作用域塔
     * @param cangjieCall 仓颉调用
     * @return invoke 候选工厂
     */
    override fun getCandidateFactoryForInvoke(
        scopeTower: ImplicitScopeTower,
        cangjieCall: CangJieCall
    ): PSICallResolver.FactoryProviderForInvoke =
        psiCallResolver.FactoryProviderForInvoke(topLevelCallContext, scopeTower, cangjieCall as PSICangJieCallImpl)

    /**
     * 查找类型变量的结果类型
     *
     * 在约束系统中查找指定类型变量的推导结果类型。
     *
     * @param constraintSystem 约束系统
     * @param typeVariable 类型变量的类型构造器
     * @return 推导出的仓颉类型,如果找不到则返回 null
     */
    override fun findResultType(
        constraintSystem: NewConstraintSystem,
        typeVariable: TypeVariableTypeConstructor
    ): CangJieType? {
        val variableWithConstraints =
            constraintSystem.getBuilder().currentStorage().notFixedTypeVariables[typeVariable] ?: return null
        return resultTypeResolver.findResultType(
            constraintSystem.asConstraintSystemCompleterContext(),
            variableWithConstraints,
            TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN
        ) as CangJieType
    }

    /**
     * 创建空的约束系统
     *
     * 创建一个新的空约束系统实例,用于类型推导。
     *
     * @return 新的约束系统
     */
    override fun createEmptyConstraintSystem(): NewConstraintSystem = NewConstraintSystemImpl(
        callComponents.constraintInjector,
        callComponents.builtIns,
        callComponents.cangjieTypeRefiner,
        callComponents.languageVersionSettings
    )

    /**
     * 为候选项绑定桩解析调用
     *
     * 在解析过程中为候选项创建一个桩(stub)已解析调用,并写入 trace。
     * 桩调用用于在类型推导完成前占位。
     *
     * @param candidate 已解析的调用原子
     */
    override fun bindStubResolvedCallForCandidate(candidate: ResolvedCallAtom) {
        cangjieToResolvedCallTransformer.createStubResolvedCallAndWriteItToTrace<CallableDescriptor>(
            candidate, trace, emptyList(), substitutor = null
        )
    }

    /**
     * 将有符号常量转换为无符号常量
     *
     * 尝试将调用参数中的有符号整数常量转换为对应的无符号类型常量。
     * 例如: 将 Int 常量 -1 转换为 UInt 常量 4294967295。
     *
     * @param argument 调用参数
     * @return 转换后的无符号常量,如果无法转换则返回 null
     */
    override fun convertSignedConstantToUnsigned(argument: CangJieCallArgument): IntegerValueTypeConstant? {
        val argumentExpression = argument.psiExpression ?: return null
        return convertSignedConstantToUnsigned(argumentExpression)
    }

    /**
     * 检查常量是否可以转换为无符号类型
     *
     * @param constant 编译期常量
     * @return 如果常量无错误且是纯常量,则可以转换
     */
    private fun constantCanBeConvertedToUnsigned(constant: CompileTimeConstant<*>): Boolean {
        return !constant.isError && constant.parameters.isPure
    }

    /**
     * 将表达式中的有符号常量转换为无符号常量
     *
     * 从 trace 中获取表达式的编译期常量值,如果是整数类型且满足转换条件,
     * 则转换为对应的无符号类型常量。
     *
     * @param expression 仓颉表达式
     * @return 转换后的无符号常量,如果无法转换则返回 null
     */
    private fun convertSignedConstantToUnsigned(expression: CjExpression): IntegerValueTypeConstant? {
        val constant = trace[BindingContext.COMPILE_TIME_VALUE, expression]
        if (constant !is IntegerValueTypeConstant || !constantCanBeConvertedToUnsigned(constant)) return null

        return with(IntegerValueTypeConstant) {
            constant.convertToUnsignedConstant(moduleDescriptor)
        }
    }
//    override fun isCompileTimeConstant(resolvedAtom: ResolvedCallAtom, expectedType: UnwrappedType): Boolean {
//        TODO("Not yet implemented")
//    }

    /**
     * 从 as 表达式获取期望类型并记录到 trace
     *
     * 对于特定的函数(如某些工厂函数),可以从父级的 as 类型转换表达式推断期望类型。
     * 例如: `foo() as String` 中,可以推断 foo() 的期望返回类型为 String。
     *
     * 这个特性用于改进类型推导,特别是对于泛型工厂函数。
     *
     * @param resolvedAtom 已解析的调用原子
     * @return 从 as 表达式推断的期望类型,如果无法推断则返回 null
     */
    override fun getExpectedTypeFromAsExpressionAndRecordItInTrace(resolvedAtom: ResolvedCallAtom): UnwrappedType? {
        val candidateDescriptor = resolvedAtom.candidateDescriptor as? FunctionDescriptor ?: return null
        val call = (resolvedAtom.atom as? PSICangJieCall)?.psiCall ?: return null

        // 如果有显式类型参数或不是支持此特性的函数,直接返回
        if (call.typeArgumentList != null || !candidateDescriptor.isFunctionForExpectTypeFromCastFeature()) return null

        // 获取父级的二元类型表达式(as 或 as?)
        val binaryParent = call.calleeExpression?.getBinaryWithTypeParent() ?: return null
        val operationType = binaryParent.operationReference.referencedNameElementType.takeIf {
            it == CjTokens.AS_KEYWORD  // 目前只支持 as,不支持 as?
        } ?: return null

        // 获取 as 右侧的类型
        val leftType = trace.get(BindingContext.TYPE, binaryParent.right ?: return null) ?: return null
        val expectedType = /*if (operationType == CjTokens.AS_SAFE) leftType.makeOption() else*/ leftType
        val resultType = expectedType.unwrap()

        // 记录此类型被用作期望类型
        trace.record(BindingContext.CAST_TYPE_USED_AS_EXPECTED_TYPE, binaryParent)
        return resultType
    }

    /**
     * 如有必要,禁用契约(Contracts)
     *
     * 在某些上下文中(如契约块内部),需要禁用契约以避免递归问题。
     * 目前此功能尚未实现。
     *
     * @param resolvedAtom 已解析的调用原子
     */
    override fun disableContractsIfNecessary(resolvedAtom: ResolvedCallAtom) {
//        val atom = resolvedAtom.atom as? PSICangJieCall ?: return
//        disableContractsInsideContractsBlock(atom.psiCall, resolvedAtom.descriptor, topLevelCallContext.scope, trace)

    }

    /**
     * 获取可调用引用的左侧(LHS)解析结果
     *
     * 可调用引用有两种形式:
     * 1. 无界引用: `::functionName` - LHS 为空
     * 2. 有界引用: `receiver::functionName` 或 `Type::staticFunction` - LHS 可以是表达式或类型
     *
     * 此方法解析 LHS 部分,确定它是:
     * - LHSResult.Type: 类型引用(如 `String::length`)
     * - LHSResult.Expression: 表达式引用(如 `obj::toString`)
     * - LHSResult.Error: 解析失败
     *
     * @param call 仓颉调用
     * @return LHS 解析结果
     * @throws IllegalStateException 如果调用不是可调用引用
     */
    override fun getLhsResult(call: CangJieCall): LHSResult {
        val callableReferenceExpression = call.extractCallableReferenceExpression()
            ?: throw IllegalStateException("Not a callable reference")
        val lhsResult = psiCallResolver.getLhsResult(topLevelCallContext, callableReferenceExpression)
        return lhsResult
//        // 注释掉的代码:处理显式接收者的替代逻辑
//        return if(call.psiCangJieCall.psiCall.calleeExpression is CjCallableReference ){
//            val callableReferenceExpression = call.psiCangJieCall.psiCall.calleeExpression as? CjCallableReference
//                ?: throw IllegalStateException("Not a callable reference")
//            val lhsResult = psiCallResolver.getLhsResult(topLevelCallContext, callableReferenceExpression)
//            lhsResult
//        }else if(call.explicitReceiver?.receiver != null){
//            ((call.explicitReceiver?.receiver as? QualifierReceiver)?.classValueReceiver?.type as? UnwrappedType)?.let {
//                LHSResult.Type(
//                    call.explicitReceiver?.receiver as? QualifierReceiver, it
//                )
//            } ?: LHSResult.Error
//        }else {
//            LHSResult.Error
//        }

//        return if (call.explicitReceiver?.receiver != null) {
//
//              ((call.explicitReceiver?.receiver as? QualifierReceiver)?.classValueReceiver?.type as? UnwrappedType)?.let {
//                LHSResult.Type(
//                    call.explicitReceiver?.receiver as? QualifierReceiver, it
//                )
//            } ?: LHSResult.Error
//        } else {
//            val callableReferenceExpression = call.psiCangJieCall.psiCall.calleeExpression as? CjCallableReference
//                ?: throw IllegalStateException("Not a callable reference")
//            val lhsResult = psiCallResolver.getLhsResult(topLevelCallContext, callableReferenceExpression)
//              lhsResult
//        }


    }
}
