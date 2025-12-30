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

package org.cangnova.cangjie.resolve.binding

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.MutableDiagnosticsWithSuppression
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.impl.AnonymousFunctionDescriptor
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.diagnostics.infos.errors.AMBIGUOUS_LABEL
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getNonStrictParentOfType
import org.cangnova.cangjie.psi.psiUtil.parentsWithSelf
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.AMBIGUOUS_LABEL_TARGET
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.AMBIGUOUS_REFERENCE_TARGET
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.DATA_FLOW_INFO_BEFORE
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.DECLARATION_TO_DESCRIPTOR
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.EXPRESSION_TYPE_INFO
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.FUNCTION
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.LABEL_TARGET
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.LEXICAL_SCOPE
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.REFERENCE_TARGET
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.TYPE
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.USED_AS_EXPRESSION
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.USED_AS_RESULT_OF_LAMBDA
import org.cangnova.cangjie.resolve.binding.slicedMap.MutableSlicedMap
import org.cangnova.cangjie.resolve.binding.slicedMap.ReadOnlySlice
import org.cangnova.cangjie.resolve.binding.slicedMap.WritableSlice
import org.cangnova.cangjie.resolve.calls.context.ResolutionContext
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfoFactory
import org.cangnova.cangjie.resolve.calls.util.getResolvedCall
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.takeSnapshot
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.expressions.CangJieTypeInfo
import org.cangnova.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import org.cangnova.cangjie.utils.exceptions.CangJieExceptionWithAttachments

object BindingContextUtils {
    
    fun extractVariableDescriptorFromReference(
        bindingContext: BindingContext,
        element: CjElement?
    ): VariableDescriptor? = when (element) {
        is CjSimpleNameExpression ->
            variableDescriptorForDeclaration(bindingContext[REFERENCE_TARGET, element])

        is CjQualifiedExpression ->
            extractVariableDescriptorFromReference(bindingContext, element.selectorExpression)

        else -> null
    }

    
    fun reportAmbiguousLabel(
        trace: BindingTrace,
        targetLabel: CjSimpleNameExpression,
        declarationsByLabel: Collection<DeclarationDescriptor>
    ) {
        val targets = declarationsByLabel.map { descriptor ->
            val element = DescriptorToSourceUtils.descriptorToDeclaration(descriptor)
            requireNotNull(element) { "Label can only point to something in the same lexical scope" }
            element
        }

        if (targets.isNotEmpty()) {
            trace.record(AMBIGUOUS_LABEL_TARGET, targetLabel, targets)
        }
        trace.report(AMBIGUOUS_LABEL.on(targetLabel))
    }

    
    fun variableDescriptorForDeclaration(descriptor: DeclarationDescriptor?): VariableDescriptor? =
        descriptor as? VariableDescriptor

    
    fun extractVariableFromResolvedCall(
        bindingContext: BindingContext,
        callElement: CjElement?
    ): VariableDescriptor? {
        val resolvedCall = callElement.getResolvedCall(bindingContext) ?: return null
        return resolvedCall.resultingDescriptor as? VariableDescriptor
    }

    
    fun updateRecordedType(
        type: CangJieType?,
        expression: CjExpression,
        trace: BindingTrace,
        shouldBeMadeNullable: Boolean
    ): CangJieType? {
        if (type == null) return null

        val updatedType = if (shouldBeMadeNullable) TypeUtils.makeOption(type) else type
        trace.recordType(expression, updatedType)
        return updatedType
    }

    
    fun getContainingFunctionSkipFunctionLiterals(
        startDescriptor: DeclarationDescriptor?,
        strict: Boolean
    ): Pair<FunctionDescriptor?, PsiElement?> {
        val containingFunctionDescriptor = DescriptorUtils.getParentOfType(
            startDescriptor,
            FunctionDescriptor::class.java,
            strict
        )
        val containingFunction = containingFunctionDescriptor?.let {
            DescriptorToSourceUtils.getSourceFromDescriptor(it)
        }

        return containingFunctionDescriptor to containingFunction
    }

    
    fun <K : Any, V: Any> getNotNull(
        bindingContext: BindingContext,
        slice: ReadOnlySlice<K, V>,
        key: K
    ): V = getNotNull(bindingContext, slice, key, "Value at $slice must not be null for $key")

    
    fun <K : Any, V : Any> getNotNull(
        bindingContext: BindingContext,
        slice: ReadOnlySlice<K, V>,
        key: K,
        messageIfNull: String
    ): V = bindingContext[slice, key] ?: throw IllegalStateException(messageIfNull)

    
    fun getRecordedTypeInfo(expression: CjExpression, context: BindingContext): CangJieTypeInfo? {
        if (context[BindingContext.PROCESSED, expression] != true) return null
        // NB: should never return null if expression is already processed
        return context[EXPRESSION_TYPE_INFO, expression]
            ?: noTypeInfo(DataFlowInfoFactory.EMPTY)
    }

    /**
     * 将自定义数据添加到给定的trace对象中
     * 此方法遍历一个映射，根据条件将数据记录到trace中，并可选择性地提交诊断信息
     *
     * @param trace             BindingTrace对象，用于记录数据和诊断信息
     * @param filter            TraceEntryFilter对象，用于过滤哪些数据应被记录如果为null，则不进行过滤
     * @param commitDiagnostics 指示是否应提交诊断信息的布尔值
     * @param map               MutableSlicedMap对象，包含要添加到trace的数据
     * @param diagnostics       MutableDiagnosticsWithSuppression对象，包含可能要提交的诊断信息
     */
    
    internal fun addOwnDataTo(
        trace: BindingTrace,
        filter: TraceEntryFilter?,
        commitDiagnostics: Boolean,
        map: MutableSlicedMap,
        diagnostics: MutableDiagnosticsWithSuppression
    ) {
        // 遍历map中的每个条目，根据filter的条件将数据记录到trace中
        map.forEach { slice, key, value ->
            // 如果filter为null或当前条目通过filter的检验，则记录该条目
            if (filter == null || filter.accept(slice, key)) {
                // 由于 forEach 使用星号投影，需要进行类型转换
                // 这在运行时是安全的，因为 slice 保证了类型一致性

                recordEntry(trace, slice  , key  , value)
            }
            null
        }

        // 如果不提交诊断信息，则直接返回
        if (!commitDiagnostics) return

        // 遍历诊断信息，根据filter的条件将诊断信息提交到trace中
        for (diagnostic in diagnostics.getOwnDiagnostics()) {
            // 如果filter为null或当前诊断信息通过filter的检验，则提交该诊断信息
            if (filter == null || filter.accept(null, diagnostic.psiElement)) {
                trace.report(diagnostic)
            }
        }
    }

    /**
     * 辅助方法：安全地记录 trace 条目
     * 此方法封装了类型转换逻辑，使类型系统能够正确推断
     */

    private fun <K : Any, V: Any> recordEntry(
        trace: BindingTrace,
        slice: WritableSlice<K, V>,
        key: K,
        value: V
    ) {
        trace.record(slice, key, value  )
    }

    
    fun recordMacroDeclarationToDescriptor(
        trace: BindingTrace,
        psiElement: PsiElement,
        macroDescriptor: MacroDescriptor
    ) {
        trace.record(BindingContext.MACRO, psiElement, macroDescriptor)
    }

    
    fun recordFunctionDeclarationToDescriptor(
        trace: BindingTrace,
        psiElement: PsiElement,
        function: SimpleFunctionDescriptor
    ) {
        trace.record(BindingContext.FUNCTION, psiElement, function)
    }

    
    fun <K : Any, V: Any> removeBySlice(slice: ReadOnlySlice<K, V>, key: K, trace: BindingTrace) {
        (trace as? DelegatingBindingTrace)?.removeBySlice(slice, key)
    }

    
    fun remove(key: CjElement, trace: BindingTrace) {
        (trace as? DelegatingBindingTrace)?.remove(key)
    }
}

fun BindingTrace.recordScope(scope: LexicalScope, element: CjElement?) {
    if (element != null) {

        record(LEXICAL_SCOPE, element, scope.takeSnapshot() as LexicalScope)
    }
}

fun <C : ResolutionContext<C>> ResolutionContext<C>.recordDataFlowInfo(expression: CjExpression?) {
    if (expression == null) return

    val typeInfo = trace[EXPRESSION_TYPE_INFO, expression]
    if (typeInfo != null) {
        trace.record(EXPRESSION_TYPE_INFO, expression, typeInfo.replaceDataFlowInfo(dataFlowInfo))
    } else if (dataFlowInfo != DataFlowInfo.EMPTY) {
        // Don't store anything in BindingTrace if it's simply an empty DataFlowInfo
        trace.record(EXPRESSION_TYPE_INFO, expression, noTypeInfo(dataFlowInfo))
    }
}

fun CjTypeReference.getType(context: BindingContext): CangJieType? {

    return context[TYPE, this]
}

fun CjExpression.getReferenceTargets(context: BindingContext): Collection<DeclarationDescriptor> {
    val targetDescriptor = if (this is CjReferenceExpression) context[REFERENCE_TARGET, this] else null
    return targetDescriptor?.let { listOf(it) } ?: context[AMBIGUOUS_REFERENCE_TARGET, this].orEmpty()
}

fun CjExpression.getReferenceTarget(context: BindingContext): DeclarationDescriptor? {
    return getReferenceTargets(context).firstOrNull()
}

fun CjElement.isUsedAsExpression(context: BindingContext): Boolean =
    context[USED_AS_EXPRESSION, this] ?: false

fun BindingContext.getDataFlowInfoAfter(position: PsiElement): DataFlowInfo {
    for (element in position.parentsWithSelf) {
        (element as? CjExpression)?.let {
            val parent = it.parent
            //TODO: it's a hack because KotlinTypeInfo with wrong DataFlowInfo stored for call expression after qualifier
            if (parent is CjQualifiedExpression && it == parent.selectorExpression) return@let null
            this[EXPRESSION_TYPE_INFO, it]
        }?.let { return it.dataFlowInfo }
    }
    return DataFlowInfo.EMPTY
}

fun BindingContext.getDataFlowInfoBefore(position: PsiElement): DataFlowInfo {
    for (element in position.parentsWithSelf) {
        (element as? CjExpression)
            ?.let { this[DATA_FLOW_INFO_BEFORE, it] }
            ?.let { return it }
    }
    return DataFlowInfo.EMPTY
}

fun CjExpression.isUsedAsStatement(context: BindingContext): Boolean = !isUsedAsExpression(context)
fun CjExpression.isUsedAsResultOfLambda(context: BindingContext): Boolean = context[USED_AS_RESULT_OF_LAMBDA, this]!!

fun <T : PsiElement> CjElement.getParentOfTypeCodeFragmentAware(vararg parentClasses: Class<out T>): T? {
    PsiTreeUtil.getParentOfType(this, *parentClasses)?.let { return it }

    val containingFile = this.containingFile
    if (containingFile is CjCodeFragment) {
        val context = containingFile.context
        if (context != null) {
            return PsiTreeUtil.getParentOfType(context, *parentClasses)
        }
    }

    return null
}

fun getEnclosingDescriptor(context: BindingContext, element: CjElement): DeclarationDescriptor {
    val declaration =
        element.getParentOfTypeCodeFragmentAware(CjNamedDeclaration::class.java)
            ?: throw CangJieExceptionWithAttachments("No parent CjNamedDeclaration for of type ${element.javaClass}")
                .withPsiAttachment("element.cj", element)
    return if (declaration is CjFunctionLiteral) {
        getEnclosingDescriptor(context, declaration)
    } else {
        context[DECLARATION_TO_DESCRIPTOR, declaration]
            ?: throw CangJieExceptionWithAttachments("No descriptor for named declaration of type ${declaration.javaClass}")
                .withPsiAttachment("declaration.cj", declaration)
    }
}

fun CjElement.recordUsedAsExpression(trace: BindingTrace, value: Boolean) {
    if (isUsedAsExpression(trace.bindingContext)) return
    trace.record(USED_AS_EXPRESSION, this, value)
}


fun CjReturnExpression.getTargetFunctionDescriptor(context: BindingContext): FunctionDescriptor? {
    val targetLabel = getTargetLabel()
    if (targetLabel != null) return context[LABEL_TARGET, targetLabel]?.let { context[FUNCTION, it] }

    val declarationDescriptor = context[DECLARATION_TO_DESCRIPTOR, getNonStrictParentOfType<CjDeclarationWithBody>() as PsiElement]
    val containingFunctionDescriptor =
        DescriptorUtils.getParentOfType(declarationDescriptor, FunctionDescriptor::class.java, false)
            ?: return null

    return generateSequence(containingFunctionDescriptor) {
        DescriptorUtils.getParentOfType(
            it,
            FunctionDescriptor::class.java
        )
    }
        .dropWhile { it is AnonymousFunctionDescriptor }
        .firstOrNull()
}