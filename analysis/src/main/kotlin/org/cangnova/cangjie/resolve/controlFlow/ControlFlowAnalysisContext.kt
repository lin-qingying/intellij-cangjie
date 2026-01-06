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

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.diagnostics.Diagnostic
import org.cangnova.cangjie.diagnostics.DiagnosticFactory
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.Pseudocode
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.Instruction
import org.cangnova.cangjie.resolve.controlFlow.variable.BlockScopeVariableInfo
import org.cangnova.cangjie.resolve.controlFlow.variable.PseudocodeVariablesData
import org.cangnova.cangjie.resolve.controlFlow.variable.VariableControlFlowState
import org.cangnova.cangjie.resolve.controlFlow.variable.VariableInitReadOnlyControlFlowInfo

/**
 * 控制流分析上下文
 *
 * 封装控制流分析过程中所需的所有共享数据和状态，
 * 作为各个检查器之间的通信桥梁。
 *
 * ## 设计目标
 *
 * 1. **统一数据访问**: 提供对伪代码、绑定上下文、变量数据等的统一访问
 * 2. **状态管理**: 管理分析过程中的诊断报告和错误跟踪
 * 3. **上下文信息**: 提供分析目标的元信息（是否是类、构造器等）
 *
 * ## 使用示例
 *
 * ```kotlin
 * val context = ControlFlowAnalysisContext.create(
 *     subroutine = function,
 *     trace = bindingTrace,
 *     pseudocode = pseudocode,
 *     languageVersionSettings = settings
 * )
 *
 * // 各检查器使用同一个上下文
 * variableChecker.check(context)
 * matchChecker.check(context)
 * ```
 *
 * @property subroutine 待分析的代码元素（函数、类、构造器等）
 * @property trace 绑定跟踪器，用于记录诊断信息
 * @property pseudocode 伪代码指令序列
 * @property languageVersionSettings 语言版本设置
 */
class ControlFlowAnalysisContext private constructor(
    val subroutine: CjElement,
    val trace: BindingTrace,
    val pseudocode: Pseudocode,
    val languageVersionSettings: LanguageVersionSettings
) {
    // ==================== 基础访问器 ====================

    /** 绑定上下文，提供类型和描述符查询 */
    val bindingContext: BindingContext
        get() = trace.bindingContext

    /** 伪代码变量数据，延迟初始化 */
    val variablesData: PseudocodeVariablesData by lazy {
        PseudocodeVariablesData(pseudocode, bindingContext)
    }

    // ==================== 分析目标类型判断 ====================

    /**
     * 分析目标的类型
     *
     * 用于确定应该执行哪些检查，不同类型有不同的检查规则。
     */
    enum class SubroutineKind {
        /** 类或接口声明 */
        CLASS,
        /** 主构造器 */
        PRIMARY_CONSTRUCTOR,
        /** 次级构造器 (init) */
        SECONDARY_CONSTRUCTOR,
        /** 普通函数 */
        FUNCTION,
        /** Lambda 表达式 */
        LAMBDA,
        /** 属性访问器 */
        PROPERTY_ACCESSOR,
        /** 文件级代码 */
        FILE,
        /** 其他 */
        OTHER
    }

    /** 分析目标的类型 */
    val subroutineKind: SubroutineKind by lazy {
        when (subroutine) {
            is CjTypeStatement -> SubroutineKind.CLASS
            is CjPrimaryConstructor -> SubroutineKind.PRIMARY_CONSTRUCTOR
            is CjSecondaryConstructor -> SubroutineKind.SECONDARY_CONSTRUCTOR
            is CjFunctionLiteral -> SubroutineKind.LAMBDA
            is CjPropertyAccessor -> SubroutineKind.PROPERTY_ACCESSOR
            is CjFunction -> SubroutineKind.FUNCTION
            is CjFile -> SubroutineKind.FILE
            else -> SubroutineKind.OTHER
        }
    }

    /** 是否是类或对象的构造过程 */
    val isClassOrConstructor: Boolean
        get() = subroutineKind == SubroutineKind.CLASS ||
                subroutineKind == SubroutineKind.PRIMARY_CONSTRUCTOR ||
                subroutineKind == SubroutineKind.SECONDARY_CONSTRUCTOR

    /** 是否是函数体（包括普通函数和 lambda） */
    val isFunctionBody: Boolean
        get() = subroutineKind == SubroutineKind.FUNCTION ||
                subroutineKind == SubroutineKind.LAMBDA ||
                subroutineKind == SubroutineKind.PROPERTY_ACCESSOR

    /** 是否是 Lambda 表达式 */
    val isLambda: Boolean
        get() = subroutineKind == SubroutineKind.LAMBDA

    // ==================== 诊断报告状态管理 ====================

    /**
     * 诊断报告器
     *
     * 管理诊断报告的去重和状态跟踪。
     */
    inner class DiagnosticReporter {
        /** 已报告未初始化错误的变量集合 */
        val uninitializedErrorVariables = hashSetOf<VariableDescriptor>()

        /** 已报告 let 重新赋值错误的变量集合 */
        val letReassignErrorVariables = hashSetOf<VariableDescriptor>()

        /** 每条指令已报告的诊断类型映射 */
        val reportedDiagnostics = hashMapOf<Instruction, DiagnosticFactory<*>>()

        /**
         * 报告诊断信息
         *
         * 智能处理重复报告和指令复制的情况。
         *
         * @param diagnostic 要报告的诊断
         * @param instruction 关联的指令（可选）
         */
        fun report(diagnostic: Diagnostic, instruction: Instruction? = null) {
            if (instruction == null || instruction.copies.isEmpty()) {
                trace.report(diagnostic)
                return
            }

            reportedDiagnostics[instruction] = diagnostic.factory

            val alreadyReported = instruction.copies.any { reportedDiagnostics[it] != null }
            val sameErrorForAllCopies = instruction.copies.all {
                reportedDiagnostics[it] === diagnostic.factory
            }

            if (mustBeReportedOnAllCopies(diagnostic.factory)) {
                if (sameErrorForAllCopies) {
                    trace.report(diagnostic)
                }
            } else {
                if (!alreadyReported) {
                    trace.report(diagnostic)
                }
            }
        }

        /**
         * 标记变量已报告未初始化错误
         */
        fun markUninitializedErrorReported(variable: VariableDescriptor) {
            uninitializedErrorVariables.add(variable)
        }

        /**
         * 检查变量是否已报告未初始化错误
         */
        fun hasUninitializedErrorReported(variable: VariableDescriptor): Boolean =
            uninitializedErrorVariables.contains(variable)

        /**
         * 标记变量已报告 let 重新赋值错误
         */
        fun markLetReassignErrorReported(variable: VariableDescriptor) {
            letReassignErrorVariables.add(variable)
        }

        /**
         * 检查变量是否已报告 let 重新赋值错误
         */
        fun hasLetReassignErrorReported(variable: VariableDescriptor): Boolean =
            letReassignErrorVariables.contains(variable)

        private fun mustBeReportedOnAllCopies(factory: DiagnosticFactory<*>): Boolean =
            factory.name in setOf(
                "UNUSED_VARIABLE",
                "UNUSED_PARAMETER",
                "UNUSED_ANONYMOUS_PARAMETER",
                "UNUSED_CHANGED_VALUE"
            )
    }

    /** 诊断报告器实例 */
    val diagnosticReporter = DiagnosticReporter()

    // ==================== 变量初始化状态 ====================

    /**
     * 变量初始化状态
     *
     * 封装变量在某个控制流点的初始化状态信息。
     *
     * @property descriptor 变量描述符
     * @property enterState 进入该点时的状态
     * @property exitState 退出该点时的状态
     */
    data class VariableInitState(
        val descriptor: VariableDescriptor,
        val enterState: VariableControlFlowState?,
        val exitState: VariableControlFlowState?
    ) {
        /** 是否在进入时已确定初始化 */
        val isDefinitelyInitializedOnEnter: Boolean
            get() = enterState?.definitelyInitialized() == true

        /** 是否在退出时已确定初始化 */
        val isDefinitelyInitializedOnExit: Boolean
            get() = exitState?.definitelyInitialized() == true

        /** 是否在进入时可能已初始化 */
        val mayBeInitializedOnEnter: Boolean
            get() = enterState?.mayBeInitialized() == true

        /** 是否在退出时可能已初始化 */
        val mayBeInitializedOnExit: Boolean
            get() = exitState?.mayBeInitialized() == true

        /** 是否是首次初始化（进入时未初始化，退出时已初始化） */
        val isFirstInitialization: Boolean
            get() = !mayBeInitializedOnEnter && mayBeInitializedOnExit

        /** 是否在声明之前 */
        val isBeforeDeclaration: Boolean
            get() = enterState?.isDeclared != true && exitState?.isDeclared != true &&
                    !mayBeInitializedOnEnter
    }

    /**
     * 获取变量在指定指令处的初始化状态
     */
    fun getVariableInitState(
        variable: VariableDescriptor,
        instruction: Instruction,
        enterData: VariableInitReadOnlyControlFlowInfo,
        exitData: VariableInitReadOnlyControlFlowInfo,
        blockScopeInfo: BlockScopeVariableInfo
    ): VariableInitState {
        val enterState = enterData.getOrNull(variable)
            ?: PseudocodeVariablesData.getDefaultValueForInitializers(variable, instruction, blockScopeInfo)
        val exitState = exitData.getOrNull(variable)
            ?: PseudocodeVariablesData.getDefaultValueForInitializers(variable, instruction, blockScopeInfo)

        return VariableInitState(variable, enterState, exitState)
    }

    // ==================== 工厂方法 ====================

    companion object {
        /**
         * 创建控制流分析上下文
         *
         * @param subroutine 待分析的代码元素
         * @param trace 绑定跟踪器
         * @param pseudocode 伪代码
         * @param languageVersionSettings 语言版本设置
         */
        fun create(
            subroutine: CjElement,
            trace: BindingTrace,
            pseudocode: Pseudocode,
            languageVersionSettings: LanguageVersionSettings
        ): ControlFlowAnalysisContext {
            return ControlFlowAnalysisContext(
                subroutine,
                trace,
                pseudocode,
                languageVersionSettings
            )
        }

        /**
         * 为本地声明创建子上下文
         *
         * 复用父上下文的 trace 和设置，但使用新的伪代码。
         */
        fun createForLocalDeclaration(
            parent: ControlFlowAnalysisContext,
            element: CjElement,
            pseudocode: Pseudocode
        ): ControlFlowAnalysisContext {
            return ControlFlowAnalysisContext(
                element,
                parent.trace,
                pseudocode,
                parent.languageVersionSettings
            )
        }
    }
}
