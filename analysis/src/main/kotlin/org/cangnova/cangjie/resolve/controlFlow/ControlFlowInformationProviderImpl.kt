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

package org.cangnova.cangjie.resolve.controlFlow


import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.cfg.pseudocodeTraverser.Edges
import org.cangnova.cangjie.cfg.pseudocodeTraverser.TraversalOrder
import org.cangnova.cangjie.cfg.pseudocodeTraverser.traverse
import org.cangnova.cangjie.cfg.pseudocodeTraverser.traverseIncludingDeadCode
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.impl.SyntheticFieldDescriptor
import org.cangnova.cangjie.diagnostics.Diagnostic
import org.cangnova.cangjie.diagnostics.DiagnosticFactory
import org.cangnova.cangjie.diagnostics.MatchMissingCase
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.diagnostics.infos.warnings.IMPLICIT_CAST_TO_ANY
import org.cangnova.cangjie.diagnostics.infos.warnings.UNREACHABLE_CODE
import org.cangnova.cangjie.diagnostics.infos.warnings.*
import org.cangnova.cangjie.diagnostics.isEffectivelyExternal
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.AMBIGUOUS_REFERENCE_TARGET
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.BACKING_FIELD_REQUIRED
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.DECLARATION_TO_DESCRIPTOR
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.EXPECTED_EXPRESSION_TYPE
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.IMPLICIT_EXHAUSTIVE_MATCH
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.IS_UNINITIALIZED
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.LAMBDA_INVOCATIONS
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.REFERENCE_TARGET
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.USED_AS_RESULT_OF_LAMBDA
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.getEnclosingDescriptor
import org.cangnova.cangjie.resolve.binding.isUsedAsExpression
import org.cangnova.cangjie.resolve.binding.isUsedAsResultOfLambda
import org.cangnova.cangjie.resolve.binding.recordUsedAsExpression
import org.cangnova.cangjie.resolve.caches.getEffectiveModality
import org.cangnova.cangjie.resolve.calls.util.EnumConstructorAccessDescriptor
import org.cangnova.cangjie.resolve.calls.util.getDispatchReceiverWithSmartCast
import org.cangnova.cangjie.resolve.calls.util.getResolvedCall
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.Pseudocode
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.PseudocodeUtil
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.CjElementInstruction
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.Instruction
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitor
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.*
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.jumps.*
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.special.MarkInstruction
import org.cangnova.cangjie.resolve.controlFlow.variable.BlockScopeVariableInfo
import org.cangnova.cangjie.resolve.controlFlow.variable.PseudocodeVariablesData
import org.cangnova.cangjie.resolve.controlFlow.variable.VariableControlFlowState
import org.cangnova.cangjie.resolve.controlFlow.variable.VariableInitReadOnlyControlFlowInfo
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeUtils.DONT_CARE
import org.cangnova.cangjie.types.TypeUtils.NO_EXPECTED_TYPE
import org.cangnova.cangjie.types.TypeUtils.noExpectedType
import org.cangnova.cangjie.types.error.MultipleSupertypeTypeInferenceFailure
import org.cangnova.cangjie.types.expressions.match.PatternAnalyzer
import org.cangnova.cangjie.types.expressions.match.checkExhaustive
import org.cangnova.cangjie.types.isBoolean
import org.cangnova.cangjie.utils.firstOverridden

/**
 * 控制流信息提供者接口
 *
 * 负责对仓颉代码进行控制流分析，检测诸如未初始化变量、不可达代码、
 * 非穷尽的 match 表达式等问题。
 */
interface ControlFlowInformationProvider {
    /**
     * 检查本地类或对象模式
     *
     * 主要用于记录已初始化的变量。
     */
    fun checkForLocalClassOrObjectMode()

    /**
     * 检查声明的控制流
     *
     * 执行完整的控制流检查，包括：
     * - 变量初始化检查
     * - 本地函数检查
     * - match 表达式穷尽性检查
     * - 多父类型推断检查
     */
    fun checkDeclaration()

    /**
     * 检查函数的控制流
     *
     * 主要检查：
     * - 不可达代码
     * - 确定性返回
     * - 尾递归调用
     *
     * @param expectedReturnType 期望的返回类型，用于检查返回语句的完整性
     */
    fun checkFunction(expectedReturnType: CangJieType?)

    /**
     * 控制流信息提供者工厂接口
     */
    interface Factory {
        /**
         * 创建控制流信息提供者实例
         *
         * @param declaration 需要分析的声明元素
         * @param trace 绑定跟踪器，用于记录诊断信息
         * @param languageVersionSettings 语言版本设置
         * @return 控制流信息提供者实例
         */
        fun createControlFlowInformationProvider(
            declaration: CjElement,
            trace: BindingTrace,
            languageVersionSettings: LanguageVersionSettings,
//            diagnosticSuppressor: PlatformDiagnosticSuppressor,
//            enumMatchTracker: EnumMatchTracker
        ): ControlFlowInformationProvider
    }
}

/**
 * 控制流信息提供者实现类
 *
 * 该类实现了对仓颉代码的完整控制流分析，通过生成和分析伪代码（Pseudocode）
 * 来检测各种控制流相关的问题。
 *
 * ## 主要功能
 *
 * ### 1. 变量初始化分析
 * - 检测未初始化变量的使用
 * - 检测 let 变量的重新赋值
 * - 检测声明前的初始化
 * - 检测捕获变量的初始化
 *
 * ### 2. 控制流检查
 * - 检测不可达代码（Unreachable Code）
 * - 检测确定性返回（Definite Return）
 * - 检测表达式体函数中的 return 语句
 *
 * ### 3. Match 表达式分析
 * - 检查 match 表达式的穷尽性（Exhaustiveness）
 * - 检测模式匹配的缺失分支
 * - 检测枚举、密封类、元组等特殊类型的完整性
 *
 * ### 4. 类型推断检查
 * - 检测多个共同父类型的推断失败
 * - 检测条件表达式的隐式类型转换
 *
 * ### 5. 本地函数分析
 * - 递归检查本地函数的控制流
 * - 检查嵌套函数的变量捕获
 *
 * ## 实现原理
 *
 * 1. **伪代码生成**: 通过 [ControlFlowProcessor] 将 PSI 树转换为伪代码指令序列
 * 2. **数据流分析**: 使用 [PseudocodeVariablesData] 进行变量初始化的数据流分析
 * 3. **指令遍历**: 遍历伪代码指令，检查每个指令的控制流状态
 * 4. **诊断报告**: 通过 [BindingTrace] 报告发现的问题
 *
 * ## 使用示例
 *
 * ```kotlin
 * val provider = ControlFlowInformationProviderImpl(
 *     declaration = function,
 *     trace = trace,
 *     languageVersionSettings = settings
 * )
 * provider.checkFunction(expectedReturnType)
 * ```
 *
 * @property subroutine 待分析的子程序元素（函数、类、构造器等）
 * @property trace 绑定跟踪器，用于记录分析结果和诊断信息
 * @property pseudocode 生成的伪代码，包含控制流指令序列
 * @property languageVersionSettings 语言版本设置，影响某些检查的行为
 *
 * @see ControlFlowProcessor 伪代码生成器
 * @see PseudocodeVariablesData 变量数据流分析
 * @see Pseudocode 伪代码指令序列
 */
class ControlFlowInformationProviderImpl private constructor(
    private val subroutine: CjElement,
    private val trace: BindingTrace,
    private val pseudocode: Pseudocode,
    private val languageVersionSettings: LanguageVersionSettings,
//    private val diagnosticSuppressor: PlatformDiagnosticSuppressor,
//    private val enumMatchTracker: EnumMatchTracker?
) : ControlFlowInformationProvider {

    /**
     * 伪代码变量数据的延迟初始化属性
     *
     * 包含变量的初始化状态和数据流信息，用于变量初始化分析。
     */
    private val pseudocodeVariablesData by lazy {
        PseudocodeVariablesData(pseudocode, trace.bindingContext)
    }


    /**
     * 控制流信息提供者的静态工厂对象
     *
     * 提供创建 [ControlFlowInformationProviderImpl] 实例的工厂方法。
     */
    object Factory : ControlFlowInformationProvider.Factory {
        override fun createControlFlowInformationProvider(
            declaration: CjElement,
            trace: BindingTrace,
            languageVersionSettings: LanguageVersionSettings,
//        diagnosticSuppressor: PlatformDiagnosticSuppressor,
//        enumMatchTracker: EnumMatchTracker
        ): ControlFlowInformationProvider =
            ControlFlowInformationProviderImpl(
                declaration,
                trace,
                languageVersionSettings,/* diagnosticSuppressor, enumMatchTracker*/
            )
    }

    /**
     * 主构造器，自动生成伪代码
     *
     * 该构造器会调用 [ControlFlowProcessor] 生成伪代码，然后委托给私有主构造器。
     *
     * @param declaration 需要分析的声明元素
     * @param trace 绑定跟踪器
     * @param languageVersionSettings 语言版本设置
     */
    constructor(
        declaration: CjElement,
        trace: BindingTrace,
        languageVersionSettings: LanguageVersionSettings,
//        diagnosticSuppressor: PlatformDiagnosticSuppressor,
//        enumMatchTracker: EnumMatchTracker? = null
    ) : this(
        declaration,
        trace,
        ControlFlowProcessor(trace, languageVersionSettings).generatePseudocode(declaration),
        languageVersionSettings,
//        diagnosticSuppressor,
//        enumMatchTracker
    )

    /**
     * 返回表达式信息的数据类
     *
     * 包含函数/lambda 中所有返回表达式的信息。
     *
     * @property returnedExpressions 所有返回表达式的集合
     * @property hasReturnsInInlinedLambda 是否在内联 lambda 中有返回语句
     */
    private data class ReturnedExpressionsInfo(
        val returnedExpressions: Collection<CjElement>,
        val hasReturnsInInlinedLambda: Boolean
    )

    /**
     * 收集函数中的所有返回表达式
     *
     * 遍历伪代码的出口指令，收集所有返回表达式，包括显式的 return 语句
     * 和隐式的表达式返回。
     *
     * @return 返回表达式信息，包含所有返回表达式和内联 lambda 返回标志
     */
    private fun collectReturnExpressions(): ReturnedExpressionsInfo {
        val instructions = pseudocode.instructions.toHashSet()
        val exitInstruction = pseudocode.exitInstruction
//
        val returnedExpressions = arrayListOf<CjElement>()
        var hasReturnsInInlinedLambda = false
//
        for (previousInstruction in exitInstruction.previousInstructions) {
            previousInstruction.accept(object : InstructionVisitor() {
                override fun visitReturnValue(instruction: ReturnValueInstruction) {
                    if (instructions.contains(instruction)) { //exclude non-local return expressions
                        returnedExpressions.add(instruction.element)
                    }

                    if (instruction.owner.isInlined) {
                        hasReturnsInInlinedLambda = true
                    }
                }

                override fun visitReturnNoValue(instruction: ReturnNoValueInstruction) {
                    if (instructions.contains(instruction)) {
                        returnedExpressions.add(instruction.element)
                    }

                    if (instruction.owner.isInlined) {
                        hasReturnsInInlinedLambda = true
                    }
                }

                override fun visitUnconditionalJump(instruction: UnconditionalJumpInstruction) {
                    redirectToPrevInstructions(instruction)
                }

                override fun visitConditionalJump(instruction: ConditionalJumpInstruction) {
                    redirectToPrevInstructions(instruction)
                }

                // Note that there's no need to overload `visitThrowException`, because
                // it can never be a predecessor of EXIT (throwing always leads to ERROR)

                private fun redirectToPrevInstructions(instruction: Instruction) {
                    for (redirectInstruction in instruction.previousInstructions) {
                        redirectInstruction.accept(this)
                    }
                }

                override fun visitNondeterministicJump(instruction: NondeterministicJumpInstruction) {
                    redirectToPrevInstructions(instruction)
                }

                override fun visitMarkInstruction(instruction: MarkInstruction) {
                    redirectToPrevInstructions(instruction)
                }

                override fun visitInstruction(instruction: Instruction) {
                    if (instruction is CjElementInstruction) {
                        // Caveats:
                        // - for empty block-bodies, read(Unit) is emitted and will be processed here
                        // - for Unit-coerced blocks, last expression will be processed here
                        returnedExpressions.add(instruction.element)
                    } else {
                        throw IllegalStateException("$instruction precedes the exit point")
                    }
                }
            })
        }

        return ReturnedExpressionsInfo(returnedExpressions, hasReturnsInInlinedLambda)

    }

    /**
     * 检查函数是否有确定的返回值
     *
     * 对于有返回类型的函数，检查是否所有控制流路径都返回了值。
     * 特别处理表达式体函数和块体函数的不同情况。
     *
     * @param expectedReturnType 期望的返回类型
     * @param unreachableCode 不可达代码集合
     */
    private fun checkDefiniteReturn(expectedReturnType: CangJieType, unreachableCode: UnreachableCode) {
        val function = subroutine as? CjDeclarationWithBody
            ?: throw AssertionError("checkDefiniteReturn is called for ${subroutine.text} which is not CjDeclarationWithBody")

        if (!function.hasBody()) return

        val (returnedExpressions, hasReturnsInInlinedLambdas) = collectReturnExpressions()

        val blockBody = function.hasBlockBody()

        var noReturnError = false
        for (returnedExpression in returnedExpressions) {
            returnedExpression.accept(object : CjVisitorUnit() {
                override fun visitReturnExpression(expression: CjReturnExpression) {
                    noReturnError = false

                    if (!blockBody) {
                        trace.report(RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY.on(expression))
                    }
                }

                override fun visitExpression(expression: CjExpression) {


                }

                override fun visitBlockExpression(expression: CjBlockExpression) {
                    visitCjElement(expression)
                }

                override fun visitCjElement(element: CjElement) {
                    if (!(element is CjExpression || element is CjCasePatternElement)) return

                    if (blockBody && !noExpectedType(expectedReturnType)
                        && !CangJieBuiltIns.isUnit(expectedReturnType)
                        && !unreachableCode.elements.contains(element)
                    ) {
                        noReturnError = true
                    }

                }
            })
        }

        if (noReturnError) {
            if (hasReturnsInInlinedLambdas) {
                trace.report(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY_MIGRATION.on(function))
            } else {
                trace.report(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY.on(function))
            }
        }
    }

    override fun checkForLocalClassOrObjectMode() {
        recordInitializedVariables()
    }

    /**
     * 记录已初始化的变量
     *
     * 遍历伪代码，将在所有控制流路径上都已初始化的变量记录到 trace 中。
     * 对于未初始化的变量，标记 [IS_UNINITIALIZED]。
     */
    private fun recordInitializedVariables() {
        val pseudocode = pseudocodeVariablesData.pseudocode
        val initializers = pseudocodeVariablesData.variableInitializers
        recordInitializedVariables(pseudocode, initializers)
        for (instruction in pseudocode.localDeclarations) {
            recordInitializedVariables(instruction.body, initializers)
        }
    }

    /**
     * 记录指定伪代码中的已初始化变量
     *
     * @param pseudocode 要分析的伪代码
     * @param initializersMap 变量初始化状态映射
     */
    private fun recordInitializedVariables(
        pseudocode: Pseudocode,
        initializersMap: Map<Instruction, Edges<VariableInitReadOnlyControlFlowInfo>>
    ) {
        val initializers = initializersMap[pseudocode.exitInstruction] ?: return
        val declaredVariables = pseudocodeVariablesData.getDeclaredVariables(pseudocode, false)
        for (variable in declaredVariables) {
            // - If we have a primary constructor and several secondary constructors then the `if` below is called only once for the primary
            //   constructor/init block
            // - If we have several secondary constructors without a primary constructor then the `if` below is called each time for every
            //   secondary constructor. (init block is considered as part of each secondary constructor in that case)
//            if (true) {
            if (initializers.incoming.getOrNull(variable)?.definitelyInitialized() == true) continue
//                trace.record(IS_DEFINITELY_NOT_ASSIGNED_IN_CONSTRUCTOR, variable)
            trace.record(IS_UNINITIALIZED, variable)
//            }
        }
    }

    /**
     * 获取所有本地函数及其描述符
     *
     * @return 本地函数及其描述符的集合
     */
    fun getLocalFunctions(): Set<Pair<CjFunction, FunctionDescriptor?>> {
        return pseudocode.localDeclarations.mapNotNull {
            if (it.element is CjFunction) {
                Pair(
                    it.element as CjFunction,
                    trace.bindingContext.get(DECLARATION_TO_DESCRIPTOR, it.element) as? FunctionDescriptor
                )
            } else {
                null
            }
        }.toSet()


    }

    /**
     * 检查本地函数的控制流
     *
     * 递归地为每个本地函数创建控制流信息提供者，并检查其控制流。
     */
    private fun checkLocalFunctions() {
        for (localDeclarationInstruction in pseudocode.localDeclarations) {
            val element = localDeclarationInstruction.element
            if (element is CjDeclarationWithBody) {

                val functionDescriptor =
                    trace.bindingContext.get(DECLARATION_TO_DESCRIPTOR, element) as? CallableDescriptor

                val expectedType = functionDescriptor?.returnType

                val providerForLocalDeclaration = ControlFlowInformationProviderImpl(
                    element,
                    trace,
                    localDeclarationInstruction.body,
                    languageVersionSettings/*, diagnosticSuppressor, enumMatchTracker*/
                )

                providerForLocalDeclaration.checkFunction(expectedType)
            }
        }
    }

    private fun checkMainFunction() {

    }

    override fun checkDeclaration() {
        recordInitializedVariables()

        checkLocalFunctions()

        checkMainFunction()
        markUninitializedVariables()

//        if (trace.wantsDiagnostics()) {
//            markUnusedVariables()
//        }
//
//        checkForSuspendLambdaAndMarkParameters(pseudocode)
//
        markStatements()
//        markAnnotationArguments()
//
//        markUnusedExpressions()
//
//        if (trace.wantsDiagnostics()) {
//            checkIfExpressions()
//        }
//
        checkMatchExpressions()
        checkMultipleSupertypeType()
//        checkConstructorConsistency()

    }

    private fun CjExpression.recordUsedAsExpression() {
        recordUsedAsExpression(trace, true)
    }

    private fun markStatements() {
        pseudocode.traverseIncludingDeadCode { instruction ->
            val value = (instruction as? InstructionWithValue)?.outputValue
            val pseudocode = instruction.owner
            val usages = pseudocode.getUsages(value)
            val isUsedAsExpression = usages.isNotEmpty()
            val isUsedAsResultOfLambda = isUsedAsResultOfLambda(usages)
            for (element in pseudocode.getValueElements(value)) {
                element.recordUsedAsExpression(trace, isUsedAsExpression)
                trace.record(USED_AS_RESULT_OF_LAMBDA, element, isUsedAsResultOfLambda)
                if (isUsedAsExpression) {
                    when (element) {
                        is CjTryExpression -> {
                            element.tryBlock.recordUsedAsExpression()
                            for (catchClause in element.catchClauses) {
                                catchClause.catchBody?.recordUsedAsExpression()
                            }
                        }

                        is CjIfExpression -> {
                            (element.then as? CjBlockExpression)?.recordUsedAsExpression()
                            (element.`else` as? CjBlockExpression)?.recordUsedAsExpression()
                        }

                        is CjMatchExpression -> {
                            for (entry in element.entries) {
                                (entry.expression as? CjBlockExpression)?.recordUsedAsExpression()
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     *   检查多个共同父类
     *   @see [org.cangnova.cangjie.resolve.calls.DiagnosticReporterByTrackingStrategy]
     *   @see constraintError
     *   @see MultipleMinimalCommonSupertypes分支
     */

    private fun checkMultipleSupertypeType() {
//        val initializers = pseudocodeVariablesData.variableInitializers
        pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->
            val value = (instruction as? InstructionWithValue)?.outputValue
            for (element in instruction.owner.getValueElements(value)) {
                if (element !is CjExpression) continue
                val context = trace.bindingContext
                val usedAsExpression = element.isUsedAsExpression(context)

                val type = context.getType(element) ?: continue

                if (usedAsExpression && type is MultipleSupertypeTypeInferenceFailure) {
                    trace.report(
                        TYPE_MISMATCH_MULTIPLE_SUPERTYPES.on(
                            element, type.intersectedTypes
                        )
                    )
                }

            }
        }
    }


    ////////////////////////////////////////////////////////////////////////////////
    //  未初始化变量分析

    /**
     * 检查 match 表达式
     *
     * 执行以下检查：
     * 1. 检查 match 表达式的穷尽性（是否覆盖所有可能的情况）
     * 2. 检查隐式类型转换（当 match 用作表达式时）
     * 3. 检测缺失的 else 分支
     * 4. 对模式匹配进行完整性检查
     *
     * 支持的检查类型：
     * - 枚举类型的穷尽性检查
     * - 密封类的穷尽性检查
     * - 元组类型的穷尽性检查
     * - 布尔类型的穷尽性检查
     */
    private fun checkMatchExpressions() {
        val initializers = pseudocodeVariablesData.variableInitializers
        pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->
            if (instruction is MagicInstruction) {
                if (instruction.kind === MagicKind.EXHAUSTIVE_MATCH_ELSE) {
                    val next = instruction.next
                    if (next is MergeInstruction) {
                        val mergeInfo = initializers[next]?.incoming
                        val magicInfo = initializers[instruction]?.outgoing
                        if (mergeInfo != null && magicInfo != null) {
                            if (next.element is CjMatchExpression && magicInfo.checkDefiniteInitializationInMatch(
                                    mergeInfo
                                )
                            ) {
                                trace.record(IMPLICIT_EXHAUSTIVE_MATCH, next.element)
                            }
                        }
                    }
                }
            }
            val value = (instruction as? InstructionWithValue)?.outputValue
            for (element in instruction.owner.getValueElements(value)) {
                if (element !is CjMatchExpression) continue

//                是否将match语句作为表达式赋值
                val usedAsExpression = element.isUsedAsExpression(trace.bindingContext)
                if (usedAsExpression) {
                    checkImplicitCastOnConditionalExpression(element)
                }

                val context = trace.bindingContext

                val elseEntry = element.entries.find { it.isElse }
                val subjectExpression = element.subjectExpression
//                检查连接符
                // TODO: 实现连接符检查


                if (subjectExpression != null) {
                    if (elseEntry != null) return@traverse
//模式匹配 match表达式
//                    直接对Patter对象进行检查
                    val patterns = element.checkExhaustive(context) ?: return@traverse
                    trace.report(NO_ELSE_IN_MATCH_BY_PATTERN.on(element, patterns))


//对未覆盖的模式进行错误报告

                } else {
//                    非模式匹配 match表达式  when
                    // TODO: 实现 getMissingCases 检查
                    val missingCases = emptyList<MatchMissingCase>()

                    if (/*usedAsExpression && */ missingCases.isNotEmpty()) {
                        if (elseEntry != null) continue
                        if (element.entries.any { it.conditions.first() is CjBindingPattern }) {
                            continue
                        }
                        trace.report(NO_ELSE_IN_MATCH.on(element, missingCases))
                        missingCases.firstOrNull { it is MatchMissingCase.ConditionTypeIsExpect }?.let {
                            require(it is MatchMissingCase.ConditionTypeIsExpect)
                            trace.report(EXPECT_TYPE_IN_MATCH_WITHOUT_ELSE.on(element, it.typeOfDeclaration))
                        }
                    }
                }

            }
        }
    }

    private fun checkExhaustiveMatchStatement(
        subjectType: CangJieType?,
        element: CjMatchExpression,
        missingCases: List<MatchMissingCase>
    ) {
        if (missingCases.isEmpty()) return
        val kind = when {
            missingCases.all { it is MatchMissingCase.OtherCheckIsMissing } -> AlgebraicTypeKind.Constant
            PatternAnalyzer.getClassDescriptorOfTypeIfTuple(subjectType) != null -> AlgebraicTypeKind.Tuple
            PatternAnalyzer.getClassDescriptorOfTypeIfSealed(subjectType) != null -> AlgebraicTypeKind.Sealed
            PatternAnalyzer.getClassDescriptorOfTypeIfEnum(subjectType) != null -> AlgebraicTypeKind.Enum
            subjectType?.isBoolean == true -> AlgebraicTypeKind.Boolean

            else -> null
        }

        if (kind != null) {

            trace.report(NO_ELSE_IN_MATCH.on(element, missingCases))

        }
    }

    private enum class AlgebraicTypeKind(val displayName: String) {
        Constant("constant"),
        Sealed("sealed class/interface"),
        Enum("enum"),
        Tuple("Tuple"),
        Boolean("Boolean")
    }

    private fun checkMatchStatement(
        subjectType: CangJieType?,
        element: CjMatchExpression,
        context: BindingContext
    ) {
//        val enumClassDescriptor = MatchChecker.getClassDescriptorOfTypeIfEnum(subjectType)
//        if (enumClassDescriptor != null) {
//            val enumMissingCases = MatchChecker.getEnumMissingCases(element, context, enumClassDescriptor)
//            if (enumMissingCases.isNotEmpty()) {
//                trace.report(NON_EXHAUSTIVE_WHEN.on(element, enumMissingCases))
//            }
//        }
//        val sealedClassDescriptor = MatchChecker.getClassDescriptorOfTypeIfSealed(subjectType)
//        if (sealedClassDescriptor != null) {
//            val sealedMissingCases = MatchChecker.getSealedMissingCases(element, context, sealedClassDescriptor)
//            if (sealedMissingCases.isNotEmpty()) {
//                trace.report(NON_EXHAUSTIVE_WHEN_ON_SEALED_CLASS.on(element, sealedMissingCases))
//            }
//        }
    }

    private val PsiElement.deparenthesizedParent: PsiElement
        get() {
            var result = parent
            while (result is CjParenthesizedExpression /*|| result is CjLabeledExpression || result is CjAnnotatedExpression*/) {
                result = result.parent
            }
            return result
        }

    private fun checkImplicitCastOnConditionalExpression(expression: CjExpression) {
        val branchExpressions = collectResultingExpressionsOfConditionalExpression(expression)

        val expectedExpressionType = trace[EXPECTED_EXPRESSION_TYPE, expression]
        if (expectedExpressionType != null && expectedExpressionType !== DONT_CARE) return

        val expressionType = trace.getType(expression) ?: return
        if (CangJieBuiltIns.isAny(expressionType)) {
            val isUsedAsResultOfLambda = expression.isUsedAsResultOfLambda(trace.bindingContext)
            for (branchExpression in branchExpressions) {
                val branchType = trace.getType(branchExpression) ?: return
                if (CangJieBuiltIns.isAny(branchType) ||
                    isUsedAsResultOfLambda && CangJieBuiltIns.isUnit(branchType)
                ) {
                    return
                }
            }
            for (branchExpression in branchExpressions) {
                val branchType = trace.getType(branchExpression) ?: continue
                if (CangJieBuiltIns.isNothing(branchType)) continue
                trace.report(
                    IMPLICIT_CAST_TO_ANY.on(
                        getResultingExpression(branchExpression),
                        branchType,
                        expressionType
                    )
                )
            }
        }
    }

    /**
     * 变量上下文基类
     *
     * 保存指令和已报告诊断的映射，用于避免重复报告。
     *
     * @property instruction 当前指令
     * @property reportedDiagnosticMap 已报告的诊断映射
     * @property variableDescriptor 从指令中提取的变量描述符
     */
    private open inner class VariableContext(
        val instruction: Instruction,
        val reportedDiagnosticMap: MutableMap<Instruction, DiagnosticFactory<*>>
    ) {
        val variableDescriptor =
            PseudocodeUtil.extractVariableDescriptorFromReference(instruction, trace.bindingContext)
    }

    /**
     * 变量初始化上下文
     *
     * 扩展 [VariableContext]，添加变量初始化状态信息。
     * 保存指令进入和退出时的初始化状态。
     *
     * @param instruction 当前指令
     * @param map 已报告诊断映射
     * @param in 进入指令时的初始化状态
     * @param out 退出指令时的初始化状态
     * @param blockScopeVariableInfo 块作用域变量信息
     *
     * @property enterInitState 进入时的初始化状态
     * @property exitInitState 退出时的初始化状态
     */
    private inner class VariableInitContext(
        instruction: Instruction,
        map: MutableMap<Instruction, DiagnosticFactory<*>>,
        `in`: VariableInitReadOnlyControlFlowInfo,
        out: VariableInitReadOnlyControlFlowInfo,
        blockScopeVariableInfo: BlockScopeVariableInfo
    ) : VariableContext(instruction, map) {
        val enterInitState = initialize(variableDescriptor, blockScopeVariableInfo, `in`)
        val exitInitState = initialize(variableDescriptor, blockScopeVariableInfo, out)

        private fun initialize(
            variableDescriptor: VariableDescriptor?,
            blockScopeVariableInfo: BlockScopeVariableInfo,
            map: VariableInitReadOnlyControlFlowInfo
        ): VariableControlFlowState? {
            val state = map.getOrNull(variableDescriptor ?: return null)
            if (state != null) return state
            return PseudocodeVariablesData.getDefaultValueForInitializers(
                variableDescriptor,
                instruction,
                blockScopeVariableInfo
            )
        }
    }

    /**
     * 报告诊断信息，避免对复制指令重复报告
     *
     * 该方法确保对于复制的指令（如循环展开或内联产生的指令），
     * 相同的诊断只报告一次。根据诊断类型决定是对所有副本报告还是只报告一次。
     *
     * @param diagnostic 要报告的诊断
     * @param ctxt 变量上下文，包含指令信息
     */
    private fun report(
        diagnostic: Diagnostic,
        ctxt: VariableContext
    ) {
        val instruction = ctxt.instruction
        if (instruction.copies.isEmpty()) {
            trace.report(diagnostic)
            return
        }
        val previouslyReported = ctxt.reportedDiagnosticMap
        previouslyReported[instruction] = diagnostic.factory

        var alreadyReported = false
        var sameErrorForAllCopies = true
        for (copy in instruction.copies) {
            val previouslyReportedErrorFactory = previouslyReported[copy]
            if (previouslyReportedErrorFactory != null) {
                alreadyReported = true
            }

            if (previouslyReportedErrorFactory !== diagnostic.factory) {
                sameErrorForAllCopies = false
            }
        }

        if (mustBeReportedOnAllCopies(diagnostic.factory)) {
            if (sameErrorForAllCopies) {
                trace.report(diagnostic)
            }
        } else {
            //only one reporting required
            if (!alreadyReported) {
                trace.report(diagnostic)
            }
        }
    }

    /**
     * 检查变量是否已初始化
     *
     * 对未初始化的变量报告相应的诊断信息：
     * - [UNINITIALIZED_PARAMETER] - 未初始化的参数
     * - [UNINITIALIZED_VARIABLE] - 未初始化的变量
     *
     * @param ctxt 变量初始化上下文
     * @param element 要检查的元素
     * @param varWithUninitializedErrorGenerated 已报告错误的变量集合，用于避免重复报告
     */
    private fun checkIsInitialized(
        ctxt: VariableInitContext,
        element: CjElement,
        varWithUninitializedErrorGenerated: MutableCollection<VariableDescriptor>
    ) {
        if (element !is CjSimpleNameExpression) return

        val isDefinitelyInitialized = ctxt.exitInitState?.definitelyInitialized() ?: false
        val variableDescriptor = ctxt.variableDescriptor
//        if (!isDefinitelyInitialized && variableDescriptor is VariableDescriptor) {
//            isDefinitelyInitialized = variableDescriptor.isDefinitelyInitialized()
//        }
        if (!isDefinitelyInitialized && !varWithUninitializedErrorGenerated.contains(variableDescriptor)) {
//            if (variableDescriptor !is VariableDescriptor) {
//                variableDescriptor?.let { varWithUninitializedErrorGenerated.add(it) }
//            }
            when (variableDescriptor) {
                is ValueParameterDescriptor ->
                    report(UNINITIALIZED_PARAMETER.on(element, variableDescriptor), ctxt)

                is EnumConstructorAccessDescriptor -> {
                    val classDescriptor = variableDescriptor.classDescriptor
                    when (classDescriptor.kind) {

                        else -> {
                        }
                    }
                }

                is VariableDescriptor ->
                    if (!(/*variableDescriptor is MemberDescriptor &&*/ variableDescriptor.isEffectivelyExternal())
                    ) {
                        report(UNINITIALIZED_VARIABLE.on(element, variableDescriptor), ctxt)
                    }
            }
        }
    }

    private fun PropertyDescriptor.isDefinitelyInitialized(): Boolean {
        if (trace[BACKING_FIELD_REQUIRED, this] == true) return false
        DescriptorToSourceUtils.descriptorToDeclaration(this)

        return true
    }

    /**
     * 标记未初始化的变量
     *
     * 这是变量初始化分析的核心方法，遍历伪代码并执行以下检查：
     *
     * 1. **读取检查**: 检查变量在使用前是否已初始化
     * 2. **写入检查**: 检查 let 变量的重新赋值
     * 3. **声明顺序检查**: 检查在声明前的初始化
     * 4. **自定义 setter 检查**: 检查具有自定义 setter 的属性初始化
     *
     * 该方法会跟踪每个指令的初始化状态，并在发现问题时报告相应的诊断。
     */
    private fun markUninitializedVariables() {
        val varWithUninitializedErrorGenerated = hashSetOf<VariableDescriptor>()
        val varWithLetReassignErrorGenerated = hashSetOf<VariableDescriptor>()
        val processClassOrObject = subroutine is CjTypeStatement || subroutine is CjSecondaryConstructor

        val initializers = pseudocodeVariablesData.variableInitializers
        val declaredVariables = pseudocodeVariablesData.getDeclaredVariables(pseudocode, true)
        val blockScopeVariableInfo = pseudocodeVariablesData.blockScopeVariableInfo

        val reportedDiagnosticMap = hashMapOf<Instruction, DiagnosticFactory<*>>()

        pseudocode.traverse(TraversalOrder.FORWARD, initializers) { instruction: Instruction,
                                                                    enterData: VariableInitReadOnlyControlFlowInfo,
                                                                    exitData: VariableInitReadOnlyControlFlowInfo ->

            val ctxt =
                VariableInitContext(instruction, reportedDiagnosticMap, enterData, exitData, blockScopeVariableInfo)
            if (ctxt.variableDescriptor == null) return@traverse
            if (instruction is ReadValueInstruction) {
                val element = instruction.element
                if (PseudocodeUtil.isThisOrNoDispatchReceiver(instruction, trace.bindingContext)
                    && declaredVariables.contains(ctxt.variableDescriptor)
                ) {
                    checkIsInitialized(ctxt, element, varWithUninitializedErrorGenerated)
                }
                return@traverse
            }
            if (instruction !is WriteValueInstruction) return@traverse
            val element = instruction.lValue as? CjExpression ?: return@traverse
            var error = checkLetReassignment(
                ctxt, element, instruction,
                varWithLetReassignErrorGenerated
            )
            if (!error && processClassOrObject) {
                error = checkAssignmentBeforeDeclaration(ctxt, element)
            }
            if (!error && processClassOrObject) {
                checkInitializationForCustomSetter(ctxt, element)
            }
        }
    }

    private fun checkInitializationForCustomSetter(ctxt: VariableInitContext, expression: CjExpression): Boolean {
        val variableDescriptor = ctxt.variableDescriptor
        if (variableDescriptor !is PropertyDescriptor
            || ctxt.enterInitState?.mayBeInitialized() == true
            || ctxt.exitInitState?.mayBeInitialized() != true
            || trace[BACKING_FIELD_REQUIRED, variableDescriptor] != true
        ) {
            return false
        }

        val property = DescriptorToSourceUtils.descriptorToDeclaration(variableDescriptor) as? CjProperty
            ?: throw AssertionError("$variableDescriptor is not related to CjProperty")
        val setter = property.setter
        if (variableDescriptor.getEffectiveModality(languageVersionSettings) == Modality.FINAL && (setter == null || !setter.hasBody())) {
            return false
        }

        val variable = if (expression is CjDotQualifiedExpression &&
            expression.receiverExpression is CjThisExpression
        ) {
            expression.selectorExpression
        } else {
            expression
        }
        if (variable is CjSimpleNameExpression) {
            trace.record(IS_UNINITIALIZED, variableDescriptor)
            return true
        }
        return false
    }

    private fun VariableInitContext.isInitializationBeforeDeclaration(): Boolean =
        // is not declared
        enterInitState?.isDeclared != true && exitInitState?.isDeclared != true &&
                // wasn't initialized before current instruction
                enterInitState?.mayBeInitialized() != true

    private fun checkAssignmentBeforeDeclaration(ctxt: VariableInitContext, expression: CjExpression) =
        if (ctxt.isInitializationBeforeDeclaration()) {
            if (ctxt.variableDescriptor != null) {
                report(INITIALIZATION_BEFORE_DECLARATION.on(expression, ctxt.variableDescriptor), ctxt)
            }
            true
        } else {
            false
        }

    private fun checkLetReassignment(
        ctxt: VariableInitContext,
        expression: CjExpression,
        writeValueInstruction: WriteValueInstruction,
        varWithLetReassignErrorGenerated: MutableCollection<VariableDescriptor>
    ): Boolean {
        val variableDescriptor = ctxt.variableDescriptor
        val mayBeInitializedNotHere = ctxt.enterInitState?.mayBeInitialized() ?: false
        val hasBackingField = (variableDescriptor as? PropertyDescriptor)?.let {
            trace[BACKING_FIELD_REQUIRED, it] ?: false
        } ?: true
        if (variableDescriptor is PropertyDescriptor && variableDescriptor.isVar) {
            val descriptor = getEnclosingDescriptor(trace.bindingContext, expression)
            val setterDescriptor = variableDescriptor.setter

            val receiverValue = expression.getResolvedCall(trace.bindingContext)?.getDispatchReceiverWithSmartCast()

            if (DescriptorVisibilityUtils.isVisible(
                    receiverValue,
                    variableDescriptor,
                    descriptor,
                    languageVersionSettings
                )
                && setterDescriptor != null
            ) {
                if (!DescriptorVisibilityUtils.isVisible(
                        receiverValue,
                        setterDescriptor,
                        descriptor,
                        languageVersionSettings
                    )
                ) {
                    report(
                        INVISIBLE_SETTER.on(
                            expression, variableDescriptor, setterDescriptor.visibility,
                            setterDescriptor
                        ), ctxt
                    )
                    return true
                } else {
                    // don't return anything as only warning is reported (not error), so further diagnostics are also important
                    reportVisibilityWarningForInternalFakeSetterOverride(
                        setterDescriptor,
                        expression,
                        variableDescriptor,
                        ctxt
                    )
                }
            }
        }
        val isThisOrNoDispatchReceiver =
            PseudocodeUtil.isThisOrNoDispatchReceiver(writeValueInstruction, trace.bindingContext)
        val captured = variableDescriptor?.let { isCapturedWrite(it, writeValueInstruction) } ?: false
        if ((mayBeInitializedNotHere || !hasBackingField || !isThisOrNoDispatchReceiver || captured) &&
            variableDescriptor != null && !variableDescriptor.isVar
        ) {
            var hasReassignMethodReturningUnit = false
            val operationReference =
                when (val parent = expression.parent) {
                    is CjBinaryExpression -> parent.operationReference
                    is CjUnaryExpression -> parent.operationReference
                    else -> null
                }
            if (operationReference != null) {
                val descriptor = trace[REFERENCE_TARGET, operationReference]
                if (descriptor is FunctionDescriptor) {
                    if (descriptor.returnType?.let { CangJieBuiltIns.isUnit(it) } == true) {
                        hasReassignMethodReturningUnit = true
                    }
                }
                if (descriptor == null) {
                    val descriptors =
                        trace[AMBIGUOUS_REFERENCE_TARGET, operationReference] ?: emptyList<DeclarationDescriptor>()
                    for (referenceDescriptor in descriptors) {
                        if ((referenceDescriptor as? FunctionDescriptor)?.returnType?.let { CangJieBuiltIns.isUnit(it) } == true) {
                            hasReassignMethodReturningUnit = true
                        }
                    }
                }
            }
            if (!hasReassignMethodReturningUnit) {
                if (!isThisOrNoDispatchReceiver || !varWithLetReassignErrorGenerated.contains(variableDescriptor)) {
                    if (captured && !mayBeInitializedNotHere && hasBackingField && isThisOrNoDispatchReceiver) {
                        if (variableDescriptor.containingDeclaration is ClassDescriptor) {
                            report(CAPTURED_MEMBER_LET_INITIALIZATION.on(expression, variableDescriptor), ctxt)
                        } else {
                            report(CAPTURED_LET_INITIALIZATION.on(expression, variableDescriptor), ctxt)
                        }
                    } else {
                        if (isBackingFieldReference(variableDescriptor)) {
                            reportLetReassigned(expression, variableDescriptor, ctxt)
                        } else {
                            report(LET_REASSIGNMENT.on(expression, variableDescriptor), ctxt)
                        }
                    }
                }
                if (isThisOrNoDispatchReceiver) {
                    // try to get rid of repeating VAL_REASSIGNMENT diagnostic only for vars with no receiver
                    // or when receiver is this
                    varWithLetReassignErrorGenerated.add(variableDescriptor)
                }
                return true
            }
        }

        if (variableDescriptor?.containingDeclaration is ClassDescriptor) {
            val cclass = variableDescriptor.containingDeclaration as ClassDescriptor
            if (cclass.kind == ClassKind.STRUCT) {

                when (val parentElement = writeValueInstruction.blockScope.block.parent) {
                    is CjFunction -> {

                        if (!parentElement.isMut) {
                            report(
                                IMMUTABLE_FUNCTION_INSTANCE_MEMBER_MODIFICATION.on(expression, variableDescriptor),
                                ctxt
                            )

                        }
                    }

                }


            }

        }
        return false
    }

    private fun reportLetReassigned(
        expression: CjExpression,
        variableDescriptor: VariableDescriptor,
        ctxt: VariableInitContext
    ) {
        TODO()
//        report(LET_REASSIGNMENT_VIA_BACKING_FIELD.on(languageVersionSettings, expression, variableDescriptor), ctxt)
    }

    private fun reportVisibilityWarningForInternalFakeSetterOverride(
        setterDescriptor: PropertySetterDescriptor,
        expression: CjExpression,
        variableDescriptor: PropertyDescriptor,
        ctxt: VariableInitContext
    ) {
        if (setterDescriptor.kind.isReal) return
        if (setterDescriptor.visibility.isPublicAPI) return

        val containingClass = setterDescriptor.containingDeclaration as? ClassDescriptor ?: return
        val firstRealOverridden = setterDescriptor.firstOverridden { it.kind.isReal } ?: return

        val visibleOverrides = OverridingUtil.filterVisibleFakeOverrides(containingClass, listOf(firstRealOverridden))
        if (visibleOverrides.isEmpty()) {
            val diagnostic = INVISIBLE_SETTER

            report(
                diagnostic.on(
                    expression, variableDescriptor, setterDescriptor.visibility,
                    setterDescriptor
                ), ctxt
            )
        }
    }

    private fun isCapturedWrite(
        variableDescriptor: VariableDescriptor,
        writeValueInstruction: WriteValueInstruction
    ): Boolean {
        val containingDeclarationDescriptor = variableDescriptor.containingDeclaration
        // Do not consider top-level properties
        if (containingDeclarationDescriptor is PackageFragmentDescriptor) return false
        var parentDeclaration = writeValueInstruction.element.getElementParentDeclaration()

        loop@ while (true) {
            val context = trace.bindingContext
            val parentDescriptor = parentDeclaration.getDeclarationDescriptorIncludingConstructors(context)
            if (parentDescriptor == containingDeclarationDescriptor) {
                return false
            }
            when (parentDeclaration) {

                is CjDeclarationWithBody -> {
                    // If it is captured write in lambda that is called in-place, then skip it (treat as parent)
                    val maybeEnclosingLambdaExpr = parentDeclaration.parent
                    if (maybeEnclosingLambdaExpr is CjLambdaExpression && trace[LAMBDA_INVOCATIONS, maybeEnclosingLambdaExpr] != null) {
                        parentDeclaration = parentDeclaration.getElementParentDeclaration()
                        continue@loop
                    }

                    if (parentDeclaration is CjFunction && parentDeclaration.isLocal) return true
                    // miss non-local function or accessor just once
                    parentDeclaration = parentDeclaration.getElementParentDeclaration()
                    return parentDeclaration.getDeclarationDescriptorIncludingConstructors(context) != containingDeclarationDescriptor
                }

                else -> {
                    return true
                }
            }
        }
    }

    /**
     * 报告不可达代码
     *
     * 为不可达代码集合中的每个元素报告 [UNREACHABLE_CODE] 诊断。
     *
     * @param unreachableCode 不可达代码信息
     */
    private fun reportUnreachableCode(unreachableCode: UnreachableCode) {
        for (element in unreachableCode.elements) {
            trace.report(
                UNREACHABLE_CODE.on(
                    element,
                    unreachableCode.reachableElements,
                    unreachableCode.unreachableElements
                )
            )
        }
    }

    /**
     * 收集不可达代码
     *
     * 遍历伪代码中的所有指令（包括死代码），识别不可达的代码元素。
     * 不可达代码是指永远不会被执行的代码，通常出现在 return、throw、
     * break、continue 等跳转语句之后。
     *
     * @return 包含可达元素和不可达元素的 [UnreachableCode] 对象
     */
    private fun collectUnreachableCode(): UnreachableCode {
        val reachableElements = hashSetOf<CjElement>()
        val unreachableElements = hashSetOf<CjElement>()
        for (instruction in pseudocode.instructionsIncludingDeadCode) {
            if (instruction !is CjElementInstruction
                || instruction is LoadUnitValueInstruction
                || instruction is MergeInstruction
                || instruction is MagicInstruction && instruction.synthetic
            )
                continue

            val element = instruction.element

            if (instruction is JumpInstruction) {
                val isJumpElement = element is CjBreakExpression
                        || element is CjContinueExpression
                        || element is CjReturnExpression
                        || element is CjThrowExpression
                if (!isJumpElement) continue
            }

            if (instruction.dead) {
                unreachableElements.add(element)
            } else {
                reachableElements.add(element)
            }
        }
        return UnreachableCodeImpl(reachableElements, unreachableElements)
    }

    override fun checkFunction(expectedReturnType: CangJieType?) {
        val unreachableCode = collectUnreachableCode()
        reportUnreachableCode(unreachableCode)


        if (subroutine is CjFunctionLiteral) return

        checkDefiniteReturn(expectedReturnType ?: NO_EXPECTED_TYPE, unreachableCode)

        markAndCheckTailCalls()
    }

////////////////////////////////////////////////////////////////////////////////
// Tail calls

    private fun markAndCheckTailCalls() {
        trace[DECLARATION_TO_DESCRIPTOR, subroutine] as? FunctionDescriptor ?: return

//        markAndCheckRecursiveTailCalls(subroutineDescriptor)
    }

    companion object {
        fun checkDeclaration(
            subroutine: CjElement,
            trace: BindingTrace,
            languageVersionSettings: LanguageVersionSettings,
        ) {
            ControlFlowInformationProviderImpl(
                subroutine,
                trace,

                languageVersionSettings,
            ).checkDeclaration()
        }

        private fun isUsedAsResultOfLambda(usages: List<Instruction>): Boolean {
            for (usage in usages) {
                if (usage is ReturnValueInstruction) {
                    val returnElement = usage.element
                    val parentElement = returnElement.parent
                    if (returnElement !is CjReturnExpression &&
                        (parentElement !is CjDeclaration || parentElement is CjFunctionLiteral)
                    ) {
                        return true
                    }
                }
            }
            return false
        }

        private fun collectResultingExpressionsOfConditionalExpression(expression: CjExpression): List<CjExpression> {
            val leafBranches = ArrayList<CjExpression>()
            collectResultingExpressionsOfConditionalExpressionRec(expression, leafBranches)
            return leafBranches
        }

        private fun getResultingExpression(expression: CjExpression): CjExpression {
            var finger = expression
            while (true) {
                var deparenthesized = CjPsiUtil.deparenthesize(finger)
                deparenthesized = CjPsiUtil.getExpressionOrLastStatementInBlock(deparenthesized)
                if (deparenthesized == null || deparenthesized === finger) break
                finger = deparenthesized
            }
            return finger
        }

        private fun mustBeReportedOnAllCopies(diagnosticFactory: DiagnosticFactory<*>) =
            diagnosticFactory === UNUSED_VARIABLE
                    || diagnosticFactory === UNUSED_PARAMETER
                    || diagnosticFactory === UNUSED_ANONYMOUS_PARAMETER
                    || diagnosticFactory === UNUSED_CHANGED_VALUE

        private fun collectResultingExpressionsOfConditionalExpressionRec(
            expression: CjExpression?,
            resultingExpressions: MutableList<CjExpression>
        ) {
            when (expression) {
                is CjIfExpression -> {
                    collectResultingExpressionsOfConditionalExpressionRec(expression.then, resultingExpressions)
                    collectResultingExpressionsOfConditionalExpressionRec(expression.`else`, resultingExpressions)
                }

                is CjMatchExpression -> for (whenEntry in expression.entries) {
                    collectResultingExpressionsOfConditionalExpressionRec(whenEntry.expression, resultingExpressions)
                }

                is Any -> {
                    val resultingExpression = getResultingExpression(expression)
                    if (resultingExpression is CjIfExpression || resultingExpression is CjMatchExpression) {
                        collectResultingExpressionsOfConditionalExpressionRec(resultingExpression, resultingExpressions)
                    } else {
                        resultingExpressions.add(resultingExpression)
                    }
                }
            }
        }

    }
}

fun CjElement.getElementParentDeclaration(): CjDeclaration? =
    PsiTreeUtil.getParentOfType(this, CjDeclarationWithBody::class.java, CjTypeStatement::class.java)

fun CjDeclaration?.getDeclarationDescriptorIncludingConstructors(context: BindingContext): DeclarationDescriptor? {
    val descriptor =
        context.get(DECLARATION_TO_DESCRIPTOR, this ?: return null)
    return descriptor
}

fun isBackingFieldReference(descriptor: DeclarationDescriptor?): Boolean {
    return descriptor is SyntheticFieldDescriptor
}


// TODO 可以直接报告多父类的表达式
val multiParentElementReports = listOf(

    CjCollectionLiteralExpression::class


)
