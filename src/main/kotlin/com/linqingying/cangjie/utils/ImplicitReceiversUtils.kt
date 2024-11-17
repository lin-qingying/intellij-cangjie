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

package com.linqingying.cangjie.utils

import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.ReceiverParameterDescriptor
import com.linqingying.cangjie.ide.IdeDescriptorRenderers
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.CjFunctionLiteral
import com.linqingying.cangjie.psi.CjPsiFactory
import com.linqingying.cangjie.psi.psiUtil.findLabelAndCall
import com.linqingying.cangjie.renderer.render
import com.linqingying.cangjie.resolve.DescriptorToSourceUtils
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.scopes.LexicalScope
import com.linqingying.cangjie.resolve.scopes.getImplicitReceiversHierarchy

fun LexicalScope.getImplicitReceiversWithInstance(): Collection<ReceiverParameterDescriptor> {
    return getImplicitReceiversWithInstanceToExpression().keys
}


interface ReceiverExpressionFactory {
    val isImmediate: Boolean
    val expressionText: String
    fun createExpression(psiFactory: CjPsiFactory, shortThis: Boolean = true): CjExpression
}

fun LexicalScope.getImplicitReceiversWithInstanceToExpression(

): Map<ReceiverParameterDescriptor, ReceiverExpressionFactory?> {
    val allReceivers = getImplicitReceiversHierarchy()

    val receivers = allReceivers
    val outerDeclarationsWithInstance = LinkedHashSet<DeclarationDescriptor>()
    var current: DeclarationDescriptor? = ownerDescriptor
    while (current != null) {
//        if (current is PropertyAccessorDescriptor) {
//            current = current.correspondingProperty
//        }
        outerDeclarationsWithInstance.add(current)

        val classDescriptor = current as? ClassDescriptor
        if (classDescriptor != null && !DescriptorUtils.isLocal(classDescriptor)) break

        current = current.containingDeclaration
    }

    val result = LinkedHashMap<ReceiverParameterDescriptor, ReceiverExpressionFactory?>()
    for ((index, receiver) in receivers.withIndex()) {
        val owner = receiver.containingDeclaration


        val (expressionText, isImmediateThis) = when {
            owner in outerDeclarationsWithInstance -> {
                val thisWithLabel = getThisQualifierName(receiver)?.let { "this@${it.render()}" } //extended
                when (index) {
                    0 -> (thisWithLabel ?: "this") to true
                    else -> thisWithLabel to false
                }

            }

            owner is ClassDescriptor && owner.kind.isSingleton -> {
                IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(owner) to false
            }

            else -> continue
        }

        result[receiver] =
            if (expressionText != null)
                createReceiverExpressionFactory(expressionText, isImmediateThis)
            else null
    }

    return result
}

private fun getThisQualifierName(receiver: ReceiverParameterDescriptor): Name? {
    val descriptor = receiver.containingDeclaration
    val name = descriptor.name
    if (!name.isSpecial) {
        return name
    }

    val functionLiteral = DescriptorToSourceUtils.descriptorToDeclaration(descriptor) as? CjFunctionLiteral
    return functionLiteral?.findLabelAndCall()?.first
}

private fun getParametersShadowedByDslMarkers(receiverParameters: List<ReceiverParameterDescriptor>): Set<ReceiverParameterDescriptor> {
    val typesByDslScopes = mutableMapOf<FqName, MutableList<ReceiverParameterDescriptor>>()

//    for (receiverParameter in receiverParameters) {
//        val dslMarkers = DslMarkerUtils.extractDslMarkerFqNames(receiverParameter.value).all()
//        for (marker in dslMarkers) {
//            typesByDslScopes.getOrPut(marker) { mutableListOf() } += receiverParameter
//        }
//    }

    // For each DSL marker, all receivers except the closest one are shadowed by it; that is why we drop it
    return typesByDslScopes.values.flatMapTo(mutableSetOf()) { it.drop(1) }
}

private fun createReceiverExpressionFactory(
    expressionText: String,
    isImmediateThis: Boolean
): ReceiverExpressionFactory {
    return object : ReceiverExpressionFactory {
        override val isImmediate = isImmediateThis
        override val expressionText: String get() = expressionText
        override fun createExpression(psiFactory: CjPsiFactory, shortThis: Boolean): CjExpression {
            return psiFactory.createExpression(if (shortThis && isImmediateThis) "this" else expressionText)
        }
    }
}
