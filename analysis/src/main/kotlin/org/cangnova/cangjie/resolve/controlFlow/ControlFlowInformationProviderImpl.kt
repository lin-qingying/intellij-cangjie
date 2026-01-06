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
 */

package org.cangnova.cangjie.resolve.controlFlow

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.cfg.pseudocodeTraverser.TraversalOrder
import org.cangnova.cangjie.cfg.pseudocodeTraverser.traverse
import org.cangnova.cangjie.cfg.pseudocodeTraverser.traverseIncludingDeadCode
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.impl.SyntheticFieldDescriptor
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.DECLARATION_TO_DESCRIPTOR
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.USED_AS_RESULT_OF_LAMBDA
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.isUsedAsExpression
import org.cangnova.cangjie.resolve.binding.recordUsedAsExpression
import org.cangnova.cangjie.resolve.controlFlow.checkers.DefiniteReturnChecker
import org.cangnova.cangjie.resolve.controlFlow.checkers.MatchExhaustivenessChecker
import org.cangnova.cangjie.resolve.controlFlow.checkers.UnreachableCodeDetector
import org.cangnova.cangjie.resolve.controlFlow.checkers.VariableInitializationChecker
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.Pseudocode
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.InstructionWithValue
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.jumps.ReturnValueInstruction
import org.cangnova.cangjie.diagnostics.infos.errors.TYPE_MISMATCH_MULTIPLE_SUPERTYPES
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeUtils.NO_EXPECTED_TYPE
import org.cangnova.cangjie.types.error.MultipleSupertypeTypeInferenceFailure

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
        ): ControlFlowInformationProvider
    }
}

/**
 * 控制流信息提供者实现类
 *
 * 该类实现了对仓颉代码的完整控制流分析，通过生成和分析伪代码（Pseudocode）
 * 来检测各种控制流相关的问题。
 *
 * ## 架构设计
 *
 * 采用策略模式，将不同类型的检查委托给专门的检查器：
 *
 * - [VariableInitializationChecker]: 变量初始化检查
 * - [MatchExhaustivenessChecker]: Match 表达式穷尽性检查
 * - [UnreachableCodeDetector]: 不可达代码检测
 * - [DefiniteReturnChecker]: 确定性返回检查
 *
 * 所有检查器共享同一个 [ControlFlowAnalysisContext]，实现数据共享和状态管理。
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
 * @see ControlFlowAnalysisContext 控制流分析上下文
 */
class ControlFlowInformationProviderImpl private constructor(
    private val subroutine: CjElement,
    private val trace: BindingTrace,
    private val pseudocode: Pseudocode,
    private val languageVersionSettings: LanguageVersionSettings,
) : ControlFlowInformationProvider {

    // ==================== 分析上下文 ====================

    /**
     * 控制流分析上下文
     *
     * 封装所有检查器共享的数据和状态。
     */
    private val context: ControlFlowAnalysisContext by lazy {
        ControlFlowAnalysisContext.create(
            subroutine,
            trace,
            pseudocode,
            languageVersionSettings
        )
    }

    // ==================== 检查器实例 ====================

    /** 变量初始化检查器 */
    private val variableInitializationChecker = VariableInitializationChecker()

    /** Match 表达式穷尽性检查器 */
    private val matchExhaustivenessChecker = MatchExhaustivenessChecker()

    /** 不可达代码检测器 */
    private val unreachableCodeDetector = UnreachableCodeDetector()

    /** 确定性返回检查器 */
    private val definiteReturnChecker = DefiniteReturnChecker()

    // ==================== 工厂 ====================

    /**
     * 控制流信息提供者的静态工厂对象
     */
    object Factory : ControlFlowInformationProvider.Factory {
        override fun createControlFlowInformationProvider(
            declaration: CjElement,
            trace: BindingTrace,
            languageVersionSettings: LanguageVersionSettings,
        ): ControlFlowInformationProvider =
            ControlFlowInformationProviderImpl(
                declaration,
                trace,
                languageVersionSettings,
            )
    }

    /**
     * 主构造器，自动生成伪代码
     */
    constructor(
        declaration: CjElement,
        trace: BindingTrace,
        languageVersionSettings: LanguageVersionSettings,
    ) : this(
        declaration,
        trace,
        ControlFlowProcessor(trace, languageVersionSettings).generatePseudocode(declaration),
        languageVersionSettings,
    )

    // ==================== 公共接口实现 ====================

    override fun checkForLocalClassOrObjectMode() {
        variableInitializationChecker.recordOnly(context)
    }

    override fun checkDeclaration() {
        // 记录初始化变量（为本地函数/闭包提供外部变量状态）
        variableInitializationChecker.recordOnly(context)

        // 检查本地函数
        checkLocalFunctions()

        // 检查主函数
        checkMainFunction()

        // 变量初始化检查（仅检查使用，避免重复记录）
        variableInitializationChecker.checkUsagesOnly(context)

        // 标记语句使用状态
        markStatements()

        // Match 表达式穷尽性检查
        matchExhaustivenessChecker.check(context)

        // 多父类型推断检查
        checkMultipleSupertypeType()
    }

    override fun checkFunction(expectedReturnType: CangJieType?) {
        // 检测并报告不可达代码
        val unreachableCode = unreachableCodeDetector.detectAndReport(context)

        // Lambda 表达式不检查确定性返回
        if (subroutine is CjFunctionLiteral) return

        // 确定性返回检查
        definiteReturnChecker.check(
            context,
            expectedReturnType ?: NO_EXPECTED_TYPE,
            unreachableCode
        )

        // 尾递归检查
        markAndCheckTailCalls()
    }

    // ==================== 本地函数检查 ====================

    /**
     * 获取所有本地函数及其描述符
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
                    languageVersionSettings
                )

                providerForLocalDeclaration.checkFunction(expectedType)
            }
        }
    }

    /**
     * 检查主函数（预留扩展）
     */
    private fun checkMainFunction() {
        // TODO: 实现主函数特定检查
    }

    // ==================== 语句标记 ====================

    /**
     * 标记语句的使用状态
     *
     * 遍历伪代码，标记每个表达式是否被用作表达式值或 lambda 结果。
     */
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
                    markNestedExpressionsAsUsed(element)
                }
            }
        }
    }

    /**
     * 标记嵌套表达式为已使用
     */
    private fun markNestedExpressionsAsUsed(element: CjElement) {
        when (element) {
            is CjTryExpression -> {
                element.tryBlock.recordUsedAsExpression(trace, true)
                for (catchClause in element.catchClauses) {
                    catchClause.catchBody?.recordUsedAsExpression(trace, true)
                }
            }

            is CjIfExpression -> {
                (element.then as? CjBlockExpression)?.recordUsedAsExpression(trace, true)
                (element.`else` as? CjBlockExpression)?.recordUsedAsExpression(trace, true)
            }

            is CjMatchExpression -> {
                for (entry in element.entries) {
                    (entry.expression as? CjBlockExpression)?.recordUsedAsExpression(trace, true)
                }
            }
        }
    }

    // ==================== 多父类型检查 ====================

    /**
     * 检查多个共同父类
     *
     * @see org.cangnova.cangjie.resolve.calls.DiagnosticReporterByTrackingStrategy
     */
    private fun checkMultipleSupertypeType() {
        pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->
            val value = (instruction as? InstructionWithValue)?.outputValue
            for (element in instruction.owner.getValueElements(value)) {
                if (element !is CjExpression) continue

                val bindingContext = trace.bindingContext
                val usedAsExpression = element.isUsedAsExpression(bindingContext)
                val type = bindingContext.getType(element) ?: continue

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

    // ==================== 尾递归检查 ====================

    /**
     * 标记和检查尾递归调用
     */
    private fun markAndCheckTailCalls() {
        trace[DECLARATION_TO_DESCRIPTOR, subroutine] as? FunctionDescriptor ?: return
        // TODO: 实现尾递归检查
    }

    // ==================== 伴生对象 ====================

    companion object {
        /**
         * 便捷方法：检查声明的控制流
         */
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

        /**
         * 判断指令是否被用作 lambda 结果
         */
        private fun isUsedAsResultOfLambda(usages: List<org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.Instruction>): Boolean {
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
    }
}

// ==================== 扩展函数 ====================

/**
 * 获取元素的父声明
 */
fun CjElement.getElementParentDeclaration(): CjDeclaration? =
    PsiTreeUtil.getParentOfType(this, CjDeclarationWithBody::class.java, CjTypeStatement::class.java)

/**
 * 获取声明的描述符（包括构造器）
 */
fun CjDeclaration?.getDeclarationDescriptorIncludingConstructors(context: BindingContext): DeclarationDescriptor? {
    val descriptor = context.get(DECLARATION_TO_DESCRIPTOR, this ?: return null)
    return descriptor
}

/**
 * 检查是否是后备字段引用
 */
fun isBackingFieldReference(descriptor: DeclarationDescriptor?): Boolean {
    return descriptor is SyntheticFieldDescriptor
}

/**
 * 获取去括号化的父元素
 */
val PsiElement.deparenthesizedParent: PsiElement
    get() {
        var result = parent
        while (result is CjParenthesizedExpression) {
            result = result.parent
        }
        return result
    }

// TODO 可以直接报告多父类的表达式
val multiParentElementReports = listOf(
    CjCollectionLiteralExpression::class
)
