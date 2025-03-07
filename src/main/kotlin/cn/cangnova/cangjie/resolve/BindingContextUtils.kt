/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.resolve

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import cn.cangnova.cangjie.descriptors.BindingTrace
import cn.cangnova.cangjie.descriptors.ClassDescriptor
import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.descriptors.FunctionDescriptor
import cn.cangnova.cangjie.descriptors.impl.AnonymousFunctionDescriptor
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.getNonStrictParentOfType
import cn.cangnova.cangjie.psi.psiUtil.parentsWithSelf
import cn.cangnova.cangjie.resolve.BindingContext.*
import cn.cangnova.cangjie.resolve.calls.context.ResolutionContext
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import cn.cangnova.cangjie.resolve.scopes.LexicalScope
import cn.cangnova.cangjie.resolve.scopes.takeSnapshot
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import cn.cangnova.cangjie.utils.CangJieExceptionWithAttachments

fun CjReturnExpression.getTargetFunctionDescriptor(context: BindingContext): FunctionDescriptor? {
    val targetLabel = getTargetLabel()
    if (targetLabel != null) return context[LABEL_TARGET, targetLabel]?.let { context[FUNCTION, it] }

    val declarationDescriptor = context[DECLARATION_TO_DESCRIPTOR, getNonStrictParentOfType<CjDeclarationWithBody>()]
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

fun BindingTrace.recordScope(scope: LexicalScope, element: CjElement?) {
    if (element != null) {

        record(LEXICAL_SCOPE, element, scope.takeSnapshot() as LexicalScope)
    }
}

fun getEnclosingDescriptor(context: BindingContext, element: CjElement): DeclarationDescriptor {
    val declaration =
        element.getParentOfTypeCodeFragmentAware(CjNamedDeclaration::class.java)
            ?: throw CangJieExceptionWithAttachments("No parent CjNamedDeclaration for of type ${element.javaClass}")
                .withPsiAttachment("element.cj", element)
    return if (declaration is CjFunctionLiteral) {
        getEnclosingDescriptor(context, declaration)
    } else {
        context.get(DECLARATION_TO_DESCRIPTOR, declaration)
            ?: throw CangJieExceptionWithAttachments("No descriptor for named declaration of type ${declaration.javaClass}")
                .withPsiAttachment("declaration.cj", declaration)
    }
}

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

fun CjExpression.isUsedAsResultOfLambda(context: BindingContext): Boolean = context[USED_AS_RESULT_OF_LAMBDA, this]!!
fun CjTypeReference.getType(context: BindingContext): CangJieType? {

    return context[TYPE, this]
}

fun CjExpression.getReferenceTarget(context: BindingContext): DeclarationDescriptor? {
    return getReferenceTargets(context).firstOrNull()
}

fun CjExpression.getReferenceTargets(context: BindingContext): Collection<DeclarationDescriptor> {
    val targetDescriptor = if (this is CjReferenceExpression) context[REFERENCE_TARGET, this] else null
    return targetDescriptor?.let { listOf(it) } ?: context[AMBIGUOUS_REFERENCE_TARGET, this].orEmpty()
}

fun <C : ResolutionContext<C>> ResolutionContext<C>.recordDataFlowInfo(expression: CjExpression?) {
    if (expression == null) return

    val typeInfo = trace.get(EXPRESSION_TYPE_INFO, expression)
    if (typeInfo != null) {
        trace.record(EXPRESSION_TYPE_INFO, expression, typeInfo.replaceDataFlowInfo(dataFlowInfo))
    } else if (dataFlowInfo != DataFlowInfo.EMPTY) {
        // Don't store anything in BindingTrace if it's simply an empty DataFlowInfo
        trace.record(EXPRESSION_TYPE_INFO, expression, noTypeInfo(dataFlowInfo))
    }
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

fun CjElement.isUsedAsExpression(context: BindingContext): Boolean =
    context[USED_AS_EXPRESSION, this] ?: false

fun CjPureElement.findClassDescriptor(bindingContext: BindingContext): ClassDescriptor = when (this) {
    is PsiElement -> BindingContextUtils.getNotNull(bindingContext, CLASS, this)
//    is SyntheticClassOrObjectDescriptor.SyntheticDeclaration -> descriptor()
    else -> throw IllegalArgumentException("$this shall be PsiElement or SyntheticClassOrObjectDescriptor.SyntheticDeclaration")
}

fun CjElement.recordUsedAsExpression(trace: BindingTrace, value: Boolean) {
    if (isUsedAsExpression(trace.bindingContext)) return
    trace.record(USED_AS_EXPRESSION, this, value)
}

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

fun getEnclosingFunctionDescriptor(
    context: BindingContext,
    element: CjElement,
    skipInlineFunctionLiterals: Boolean
): FunctionDescriptor? {
    var current = element
    while (true) {
        val functionOrClass =
            current.getParentOfTypeCodeFragmentAware(CjFunction::class.java, CjTypeStatement::class.java)
        val descriptor = context.get(DECLARATION_TO_DESCRIPTOR, functionOrClass)
        if (functionOrClass is CjFunction) {
            if (descriptor is FunctionDescriptor) {
                if (skipInlineFunctionLiterals) {
                    current = functionOrClass
                } else {
                    return descriptor
                }
            } else {
                return null
            }
        } else {
            return if (descriptor is ClassDescriptor) descriptor.unsubstitutedPrimaryConstructor else null
        }
    }
}
