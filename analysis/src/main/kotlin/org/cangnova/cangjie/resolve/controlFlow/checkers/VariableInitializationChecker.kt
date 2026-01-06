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

package org.cangnova.cangjie.resolve.controlFlow.checkers

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.cfg.pseudocodeTraverser.Edges
import org.cangnova.cangjie.cfg.pseudocodeTraverser.TraversalOrder
import org.cangnova.cangjie.cfg.pseudocodeTraverser.traverse
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.diagnostics.isEffectivelyExternal
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.OverridingUtil
import org.cangnova.cangjie.descriptors.DescriptorVisibilityUtils
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.AMBIGUOUS_REFERENCE_TARGET
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.BACKING_FIELD_REQUIRED
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.IS_UNINITIALIZED
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.LAMBDA_INVOCATIONS
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.REFERENCE_TARGET
import org.cangnova.cangjie.resolve.binding.getEnclosingDescriptor
import org.cangnova.cangjie.resolve.caches.getEffectiveModality
import org.cangnova.cangjie.resolve.calls.util.EnumConstructorAccessDescriptor
import org.cangnova.cangjie.resolve.calls.util.getDispatchReceiverWithSmartCast
import org.cangnova.cangjie.resolve.calls.util.getResolvedCall
import org.cangnova.cangjie.resolve.controlFlow.ControlFlowAnalysisContext
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.Pseudocode
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.PseudocodeUtil
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.Instruction
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.ReadValueInstruction
import org.cangnova.cangjie.resolve.controlFlow.pseudocode.instructions.eval.WriteValueInstruction
import org.cangnova.cangjie.resolve.controlFlow.variable.VariableInitReadOnlyControlFlowInfo
import org.cangnova.cangjie.utils.firstOverridden

/**
 * 变量初始化检查器
 *
 * 负责检查仓颉代码中变量初始化相关的问题：
 *
 * ## 主要检查项
 *
 * 1. **未初始化变量使用**: 检测在使用变量前是否已正确初始化
 * 2. **let 变量重新赋值**: 检测不可变变量的非法重新赋值
 * 3. **声明前赋值**: 检测在变量声明前对其进行赋值（仅类/构造器）
 * 4. **自定义 setter 初始化**: 检测带自定义 setter 属性的首次初始化
 * 5. **struct 不可变函数修改**: 检测在非 mut 函数中修改 struct 成员
 *
 * ## 仓颉语言特性支持
 *
 * - **let/var 区分**: let 声明的变量不可重新赋值
 * - **主/次级构造器**: 正确处理不同构造器中的初始化顺序
 * - **struct mut 函数**: struct 类型的成员修改只能在 mut 函数中进行
 * - **属性参数**: 主构造器中的 let/var 参数自动成为属性
 *
 * ## 使用示例
 *
 * ```kotlin
 * val checker = VariableInitializationChecker()
 * checker.check(context)
 * ```
 *
 * @see ControlFlowAnalysisContext
 */
class VariableInitializationChecker {

    /**
     * 执行完整的变量初始化检查（记录 + 检查）
     *
     * 包含两个步骤：
     * 1. 记录变量初始化状态
     * 2. 检查变量使用是否合法
     *
     * @param context 控制流分析上下文
     */
    fun check(context: ControlFlowAnalysisContext) {
        recordInitializedVariables(context)
        checkVariableUsages(context)
    }

    /**
     * 仅记录已初始化的变量
     *
     * 在检查本地函数/闭包之前调用，以便它们能正确判断
     * 对外部变量的使用是否合法。
     *
     * @param context 控制流分析上下文
     */
    fun recordOnly(context: ControlFlowAnalysisContext) {
        recordInitializedVariables(context)
    }

    /**
     * 仅检查变量使用（假设已经调用过 recordOnly）
     *
     * 在 recordOnly 之后调用，避免重复记录初始化状态。
     *
     * @param context 控制流分析上下文
     */
    fun checkUsagesOnly(context: ControlFlowAnalysisContext) {
        checkVariableUsages(context)
    }

    // ==================== 记录初始化状态 ====================

    /**
     * 记录已初始化的变量
     *
     * 在伪代码的出口点检查变量的初始化状态，
     * 将未初始化的变量标记到 trace 中。
     */
    private fun recordInitializedVariables(context: ControlFlowAnalysisContext) {
        val variablesData = context.variablesData
        val initializers = variablesData.variableInitializers

        // 记录主伪代码中的变量
        recordInitializedVariablesInPseudocode(context, context.pseudocode, initializers)

        // 递归记录本地声明中的变量
        for (localDecl in context.pseudocode.localDeclarations) {
            recordInitializedVariablesInPseudocode(context, localDecl.body, initializers)
        }
    }

    /**
     * 记录指定伪代码中的已初始化变量
     */
    private fun recordInitializedVariablesInPseudocode(
        context: ControlFlowAnalysisContext,
        pseudocode: Pseudocode,
        initializersMap: Map<Instruction, Edges<VariableInitReadOnlyControlFlowInfo>>
    ) {
        val exitInfo = initializersMap[pseudocode.exitInstruction] ?: return
        val declaredVariables = context.variablesData.getDeclaredVariables(pseudocode, false)

        for (variable in declaredVariables) {
            // 如果变量在出口点已确定初始化，则跳过
            if (exitInfo.incoming.getOrNull(variable)?.definitelyInitialized() == true) {
                continue
            }
            // 否则标记为未初始化
            context.trace.record(IS_UNINITIALIZED, variable)
        }
    }

    // ==================== 检查变量使用 ====================

    /**
     * 检查变量的使用和赋值
     *
     * 遍历所有指令，检查：
     * - 读取指令：变量是否已初始化
     * - 写入指令：是否违反 let 不可变性、声明顺序等
     */
    private fun checkVariableUsages(context: ControlFlowAnalysisContext) {
        val variablesData = context.variablesData
        val initializers = variablesData.variableInitializers
        val declaredVariables = variablesData.getDeclaredVariables(context.pseudocode, true)
        val blockScopeInfo = variablesData.blockScopeVariableInfo

        context.pseudocode.traverse(TraversalOrder.FORWARD, initializers) { instruction, enterData, exitData ->
            // 提取变量描述符
            val variableDescriptor = PseudocodeUtil.extractVariableDescriptorFromReference(
                instruction, context.bindingContext
            ) ?: return@traverse

            when (instruction) {
                is ReadValueInstruction -> {
                    checkVariableRead(
                        context, instruction, variableDescriptor,
                        enterData, exitData, blockScopeInfo, declaredVariables
                    )
                }
                is WriteValueInstruction -> {
                    checkVariableWrite(
                        context, instruction, variableDescriptor,
                        enterData, exitData, blockScopeInfo
                    )
                }
            }
        }
    }

    // ==================== 读取检查 ====================

    /**
     * 检查变量读取
     *
     * 验证变量在读取时是否已被正确初始化。
     */
    private fun checkVariableRead(
        context: ControlFlowAnalysisContext,
        instruction: ReadValueInstruction,
        variable: VariableDescriptor,
        enterData: VariableInitReadOnlyControlFlowInfo,
        exitData: VariableInitReadOnlyControlFlowInfo,
        blockScopeInfo: org.cangnova.cangjie.resolve.controlFlow.variable.BlockScopeVariableInfo,
        declaredVariables: Set<VariableDescriptor>
    ) {
        val element = instruction.element
        if (element !is CjSimpleNameExpression) return

        // 检查是否是 this 引用或无分发接收者
        if (!PseudocodeUtil.isThisOrNoDispatchReceiver(instruction, context.bindingContext)) {
            return
        }

        // 变量必须是在当前作用域声明的
        if (!declaredVariables.contains(variable)) {
            return
        }

        // 获取初始化状态
        val initState = context.getVariableInitState(variable, instruction, enterData, exitData, blockScopeInfo)

        // 检查是否已初始化
        if (!initState.isDefinitelyInitializedOnExit) {
            reportUninitializedError(context, element, variable, instruction)
        }
    }

    /**
     * 报告未初始化错误
     */
    private fun reportUninitializedError(
        context: ControlFlowAnalysisContext,
        element: CjSimpleNameExpression,
        variable: VariableDescriptor,
        instruction: Instruction
    ) {
        // 避免重复报告
        if (context.diagnosticReporter.hasUninitializedErrorReported(variable)) {
            return
        }

        val diagnostic = when (variable) {
            is ValueParameterDescriptor -> UNINITIALIZED_PARAMETER.on(element, variable)
            is EnumConstructorAccessDescriptor -> return // 枚举构造器访问暂不报告
            else -> {
                if (variable.isEffectivelyExternal()) return
                UNINITIALIZED_VARIABLE.on(element, variable)
            }
        }

        context.diagnosticReporter.report(diagnostic, instruction)
    }

    // ==================== 写入检查 ====================

    /**
     * 检查变量写入
     *
     * 验证变量赋值的合法性。
     */
    private fun checkVariableWrite(
        context: ControlFlowAnalysisContext,
        instruction: WriteValueInstruction,
        variable: VariableDescriptor,
        enterData: VariableInitReadOnlyControlFlowInfo,
        exitData: VariableInitReadOnlyControlFlowInfo,
        blockScopeInfo: org.cangnova.cangjie.resolve.controlFlow.variable.BlockScopeVariableInfo
    ) {
        val element = instruction.lValue as? CjExpression ?: return
        val initState = context.getVariableInitState(variable, instruction, enterData, exitData, blockScopeInfo)

        // 检查1：let 变量重新赋值
        if (checkLetReassignment(context, instruction, element, variable, initState)) {
            return
        }

        // 以下检查仅适用于类/构造器
        if (!context.isClassOrConstructor) return

        // 检查2：声明前赋值
        if (checkAssignmentBeforeDeclaration(context, instruction, element, variable, initState)) {
            return
        }

        // 检查3：自定义 setter 的首次初始化
        checkCustomSetterInitialization(context, element, variable, initState)
    }

    // ==================== let 重新赋值检查 ====================

    /**
     * 检查 let 变量的重新赋值
     *
     * @return true 如果检测到错误并已报告
     */
    private fun checkLetReassignment(
        context: ControlFlowAnalysisContext,
        instruction: WriteValueInstruction,
        element: CjExpression,
        variable: VariableDescriptor,
        initState: ControlFlowAnalysisContext.VariableInitState
    ): Boolean {
        // 检查 var 属性的 setter 可见性
        if (variable is PropertyDescriptor && variable.isVar) {
            if (checkSetterVisibility(context, instruction, element, variable)) {
                return true
            }
        }

        // 如果是 var 变量，不检查重新赋值
        if (variable.isVar) {
            // 但需要检查 struct 的不可变函数
            checkStructMemberModification(context, instruction, element, variable)
            return false
        }

        // let 变量的重新赋值检查
        val mayBeInitializedBefore = initState.mayBeInitializedOnEnter
        val hasBackingField = (variable as? PropertyDescriptor)?.let {
            context.trace[BACKING_FIELD_REQUIRED, it] ?: false
        } ?: true
        val isThisOrNoReceiver = PseudocodeUtil.isThisOrNoDispatchReceiver(instruction, context.bindingContext)
        val isCaptured = isCapturedWrite(context, variable, instruction)

        // 判断是否是非法重新赋值
        val isIllegalReassignment = mayBeInitializedBefore || !hasBackingField || !isThisOrNoReceiver || isCaptured

        if (isIllegalReassignment) {
            // 检查是否是返回 Unit 的操作符重载（如 +=）
            if (hasReassignMethodReturningUnit(context, element)) {
                return false
            }

            // 避免重复报告
            if (isThisOrNoReceiver && context.diagnosticReporter.hasLetReassignErrorReported(variable)) {
                return true
            }

            // 报告适当的错误
            reportLetReassignmentError(context, instruction, element, variable, isCaptured, mayBeInitializedBefore, hasBackingField, isThisOrNoReceiver)

            if (isThisOrNoReceiver) {
                context.diagnosticReporter.markLetReassignErrorReported(variable)
            }
            return true
        }

        return false
    }

    /**
     * 检查 setter 可见性
     */
    private fun checkSetterVisibility(
        context: ControlFlowAnalysisContext,
        instruction: WriteValueInstruction,
        element: CjExpression,
        variable: PropertyDescriptor
    ): Boolean {
        val enclosingDescriptor = getEnclosingDescriptor(context.bindingContext, element)
        val setter = variable.setter ?: return false
        val receiverValue = element.getResolvedCall(context.bindingContext)?.getDispatchReceiverWithSmartCast()

        if (!DescriptorVisibilityUtils.isVisible(receiverValue, variable, enclosingDescriptor, context.languageVersionSettings)) {
            return false
        }

        if (!DescriptorVisibilityUtils.isVisible(receiverValue, setter, enclosingDescriptor, context.languageVersionSettings)) {
            context.diagnosticReporter.report(
                INVISIBLE_SETTER.on(element, variable, setter.visibility, setter),
                instruction
            )
            return true
        }

        // 检查内部 fake setter override 的警告
        checkInternalFakeSetterOverride(context, instruction, element, variable, setter)
        return false
    }

    /**
     * 检查内部 fake setter override
     */
    private fun checkInternalFakeSetterOverride(
        context: ControlFlowAnalysisContext,
        instruction: WriteValueInstruction,
        element: CjExpression,
        variable: PropertyDescriptor,
        setter: PropertySetterDescriptor
    ) {
        if (setter.kind.isReal) return
        if (setter.visibility.isPublicAPI) return

        val containingClass = setter.containingDeclaration as? ClassDescriptor ?: return
        val firstRealOverridden = setter.firstOverridden { it.kind.isReal } ?: return
        val visibleOverrides = OverridingUtil.filterVisibleFakeOverrides(containingClass, listOf(firstRealOverridden))

        if (visibleOverrides.isEmpty()) {
            context.diagnosticReporter.report(
                INVISIBLE_SETTER.on(element, variable, setter.visibility, setter),
                instruction
            )
        }
    }

    /**
     * 报告 let 重新赋值错误
     */
    private fun reportLetReassignmentError(
        context: ControlFlowAnalysisContext,
        instruction: WriteValueInstruction,
        element: CjExpression,
        variable: VariableDescriptor,
        isCaptured: Boolean,
        mayBeInitializedBefore: Boolean,
        hasBackingField: Boolean,
        isThisOrNoReceiver: Boolean
    ) {
        val diagnostic = when {
            // 捕获的 let 变量初始化
            isCaptured && !mayBeInitializedBefore && hasBackingField && isThisOrNoReceiver -> {
                if (variable.containingDeclaration is ClassDescriptor) {
                    CAPTURED_MEMBER_LET_INITIALIZATION.on(element, variable)
                } else {
                    CAPTURED_LET_INITIALIZATION.on(element, variable)
                }
            }
            // 普通 let 重新赋值
            else -> LET_REASSIGNMENT.on(element, variable)
        }

        context.diagnosticReporter.report(diagnostic, instruction)
    }

    /**
     * 检查是否有返回 Unit 的重新赋值方法（如 +=）
     */
    private fun hasReassignMethodReturningUnit(context: ControlFlowAnalysisContext, element: CjExpression): Boolean {
        val operationReference = when (val parent = element.parent) {
            is CjBinaryExpression -> parent.operationReference
            is CjUnaryExpression -> parent.operationReference
            else -> return false
        }

        val descriptor = context.trace[REFERENCE_TARGET, operationReference]
        if (descriptor is FunctionDescriptor && CangJieBuiltIns.isUnit(descriptor.returnType ?: return false)) {
            return true
        }

        val descriptors = context.trace[AMBIGUOUS_REFERENCE_TARGET, operationReference] ?: return false
        return descriptors.any { (it as? FunctionDescriptor)?.returnType?.let(CangJieBuiltIns::isUnit) == true }
    }

    /**
     * 检查 struct 成员修改
     */
    private fun checkStructMemberModification(
        context: ControlFlowAnalysisContext,
        instruction: WriteValueInstruction,
        element: CjExpression,
        variable: VariableDescriptor
    ) {
        val containingClass = variable.containingDeclaration as? ClassDescriptor ?: return
        if (containingClass.kind != ClassKind.STRUCT) return

        val parentFunction = instruction.blockScope.block.parent as? CjFunction ?: return
        if (!parentFunction.isMut) {
            context.diagnosticReporter.report(
                IMMUTABLE_FUNCTION_INSTANCE_MEMBER_MODIFICATION.on(element, variable),
                instruction
            )
        }
    }

    // ==================== 声明前赋值检查 ====================

    /**
     * 检查声明前赋值
     *
     * @return true 如果检测到错误并已报告
     */
    private fun checkAssignmentBeforeDeclaration(
        context: ControlFlowAnalysisContext,
        instruction: WriteValueInstruction,
        element: CjExpression,
        variable: VariableDescriptor,
        initState: ControlFlowAnalysisContext.VariableInitState
    ): Boolean {
        if (!initState.isBeforeDeclaration) return false

        context.diagnosticReporter.report(
            INITIALIZATION_BEFORE_DECLARATION.on(element, variable),
            instruction
        )
        return true
    }

    // ==================== 自定义 setter 初始化检查 ====================

    /**
     * 检查自定义 setter 的首次初始化
     *
     * 当属性有自定义 setter 且有后备字段时，
     * 首次赋值需要特殊处理。
     */
    private fun checkCustomSetterInitialization(
        context: ControlFlowAnalysisContext,
        element: CjExpression,
        variable: VariableDescriptor,
        initState: ControlFlowAnalysisContext.VariableInitState
    ) {
        if (variable !is PropertyDescriptor) return
        if (initState.mayBeInitializedOnEnter) return  // 已可能初始化
        if (!initState.mayBeInitializedOnExit) return   // 未执行初始化
        if (context.trace[BACKING_FIELD_REQUIRED, variable] != true) return  // 不需要后备字段

        // 获取属性声明
        val property = DescriptorToSourceUtils.descriptorToDeclaration(variable) as? CjProperty ?: return
        val setter = property.setter

        // 如果是 final 且没有自定义 setter 或 setter 无函数体，则无需标记
        if (variable.getEffectiveModality(context.languageVersionSettings) == Modality.FINAL &&
            (setter == null || !setter.hasBody())) {
            return
        }

        // 规范化表达式：将 this.property 转换为 property
        val normalizedElement = if (element is CjDotQualifiedExpression &&
            element.receiverExpression is CjThisExpression) {
            element.selectorExpression
        } else {
            element
        }

        // 标记为未初始化（需要通过后备字段初始化）
        if (normalizedElement is CjSimpleNameExpression) {
            context.trace.record(IS_UNINITIALIZED, variable)
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查是否是捕获的写入
     *
     * 判断变量的写入是否发生在闭包（lambda 或本地函数）中。
     */
    private fun isCapturedWrite(
        context: ControlFlowAnalysisContext,
        variable: VariableDescriptor,
        instruction: WriteValueInstruction
    ): Boolean {
        val containingDeclaration = variable.containingDeclaration

        // 顶层属性不算捕获
        if (containingDeclaration is PackageFragmentDescriptor) return false

        var parentDeclaration = instruction.element.getElementParentDeclaration()

        while (true) {
            val parentDescriptor = parentDeclaration.getDeclarationDescriptorIncludingConstructors(context.bindingContext)
            if (parentDescriptor == containingDeclaration) {
                return false
            }

            when (parentDeclaration) {
                is CjDeclarationWithBody -> {
                    // 如果是在 in-place 调用的 lambda 中，视为不捕获
                    val maybeEnclosingLambda = parentDeclaration.parent
                    if (maybeEnclosingLambda is CjLambdaExpression &&
                        context.trace[LAMBDA_INVOCATIONS, maybeEnclosingLambda] != null) {
                        parentDeclaration = parentDeclaration.getElementParentDeclaration()
                        continue
                    }

                    // 本地函数是捕获
                    if (parentDeclaration is CjFunction && parentDeclaration.isLocal) {
                        return true
                    }

                    // 继续向上查找
                    parentDeclaration = parentDeclaration.getElementParentDeclaration()
                    return parentDeclaration.getDeclarationDescriptorIncludingConstructors(context.bindingContext) != containingDeclaration
                }
                else -> return true
            }
        }
    }

    companion object {
        /** 获取元素的父声明 */
        private fun CjElement.getElementParentDeclaration(): CjDeclaration? =
            PsiTreeUtil.getParentOfType(this, CjDeclarationWithBody::class.java, CjTypeStatement::class.java)

        /** 获取声明的描述符（包括构造器） */
        private fun CjDeclaration?.getDeclarationDescriptorIncludingConstructors(context: org.cangnova.cangjie.resolve.binding.BindingContext): DeclarationDescriptor? {
            return context.get(org.cangnova.cangjie.resolve.binding.BindingContext.DECLARATION_TO_DESCRIPTOR, this ?: return null)
        }
    }
}
