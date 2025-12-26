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

package org.cangnova.cangjie.resolve.calls.smartcasts

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.lexer.CjToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.calls.util.getResolvedCall
import org.cangnova.cangjie.resolve.calls.util.isSafeCall
import org.cangnova.cangjie.resolve.scopes.receivers.ContextReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ImplicitReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import org.cangnova.cangjie.types.CangJieType


private fun postfix(argumentInfo: IdentifierInfo, op: CjToken): IdentifierInfo =
    if (argumentInfo == IdentifierInfo.NO) IdentifierInfo.NO else IdentifierInfo.PostfixIdentifierInfo(argumentInfo, op)

private fun qualified(
    receiverInfo: IdentifierInfo,
    receiverType: CangJieType?,
    selectorInfo: IdentifierInfo,
    safe: Boolean
) =
    when (receiverInfo) {
        IdentifierInfo.NO -> IdentifierInfo.NO
        is IdentifierInfo.PackageOrClass -> selectorInfo
        else -> IdentifierInfo.Qualified(receiverInfo, selectorInfo, safe, receiverType)
    }

internal fun getIdForStableIdentifier(
    expression: CjExpression?,
    bindingContext: BindingContext,
    containingDeclarationOrModule: DeclarationDescriptor,
//    languageVersionSettings: LanguageVersionSettings
): IdentifierInfo {
    if (expression != null) {
        val deparenthesized = CjPsiUtil.deparenthesize(expression)
        if (expression !== deparenthesized) {
            return getIdForStableIdentifier(
                deparenthesized,
                bindingContext,
                containingDeclarationOrModule/*, languageVersionSettings*/
            )
        }
    }
    return when (expression) {
        is CjQualifiedExpression -> {
            val receiverExpression = expression.receiverExpression
            val selectorExpression = expression.selectorExpression
            val receiverInfo =
                getIdForStableIdentifier(
                    receiverExpression,
                    bindingContext,
                    containingDeclarationOrModule/*, languageVersionSettings*/
                )
            val selectorInfo =
                getIdForStableIdentifier(
                    selectorExpression,
                    bindingContext,
                    containingDeclarationOrModule/*, languageVersionSettings*/
                )

            qualified(
                receiverInfo, bindingContext.getType(receiverExpression),
                selectorInfo, expression.operationSign === CjTokens.SAFE_ACCESS
            )
        }

        is CjBinaryExpressionWithTypeRHS -> {
            val subjectExpression = expression.left
            val targetTypeReference = expression.right ?: return IdentifierInfo.NO
            val operationToken = expression.operationReference.referencedNameElementType
            if (operationToken == CjTokens.IS_KEYWORD || operationToken == CjTokens.AS_KEYWORD) {
                IdentifierInfo.NO
            } else {
                IdentifierInfo.SafeCast(
                    getIdForStableIdentifier(
                        subjectExpression,
                        bindingContext,
                        containingDeclarationOrModule/*, languageVersionSettings*/
                    ),
                    bindingContext.getType(subjectExpression),
                    bindingContext[BindingContext.TYPE, targetTypeReference]
                )
            }
        }

        is CjSimpleNameExpression ->
            getIdForSimpleNameExpression(
                expression,
                bindingContext,
                containingDeclarationOrModule/*, languageVersionSettings*/
            )

        is CjThisExpression -> {
            val declarationDescriptor =
                bindingContext[BindingContext.REFERENCE_TARGET, expression.instanceReference]
            val labelName = expression.getLabelName()
            if (labelName == null) {
                getIdForThisReceiver(declarationDescriptor)
            } else {
                getIdForThisReceiver(declarationDescriptor, bindingContext, labelName)
            }
        }

        is CjPostfixExpression -> {
            val operationType = expression.operationReference.referencedNameElementType
            if (operationType === CjTokens.PLUSPLUS || operationType === CjTokens.MINUSMINUS)
                postfix(
                    getIdForStableIdentifier(
                        expression.baseExpression,
                        bindingContext,
                        containingDeclarationOrModule,
//                        languageVersionSettings
                    ),
                    operationType
                )
            else
                IdentifierInfo.NO
        }

        else -> IdentifierInfo.NO
    }
}

private fun getIdForThisReceiver(descriptorOfThisReceiver: DeclarationDescriptor?) = when (descriptorOfThisReceiver) {
    is CallableDescriptor -> {
        val receiverParameter = descriptorOfThisReceiver.extensionReceiverParameter
            ?: error("'This' refers to the callable member without a receiver parameter: $descriptorOfThisReceiver")
        IdentifierInfo.Receiver(receiverParameter.value)
    }

    is ClassDescriptor -> IdentifierInfo.Receiver(descriptorOfThisReceiver.thisAsReceiverParameter.value)

    else -> IdentifierInfo.NO
}


interface IdentifierInfo {
    val kind: DataFlowValue.Kind get() = DataFlowValue.Kind.OTHER

    class Variable(
        val variable: VariableDescriptor,
        override val kind: DataFlowValue.Kind,
        val bound: DataFlowValue?
    ) : IdentifierInfo {
        override val canBeBound
            get() = kind == DataFlowValue.Kind.STABLE_VALUE

        override fun equals(other: Any?) =
            other is Variable
//

        override fun hashCode() = variable.name.hashCode() * 31 + variable.containingDeclaration.original.hashCode()

        override fun toString() = variable.toString()
    }

    val canBeBound get() = false

    data class PackageOrClass(val descriptor: DeclarationDescriptor) : IdentifierInfo {
        override val kind = DataFlowValue.Kind.STABLE_VALUE

        override fun toString() = descriptor.toString()
    }

    object NO : IdentifierInfo {
        override fun toString() = "NO_IDENTIFIER_INFO"
    }

    object NULL : IdentifierInfo {
        override fun toString() = "NULL"
    }

    class Qualified(
        val receiverInfo: IdentifierInfo,
          val selectorInfo: IdentifierInfo,
        val safe: Boolean,
        val receiverType: CangJieType?
    ) : IdentifierInfo {

        override val kind: DataFlowValue.Kind get() = if (receiverInfo.kind == DataFlowValue.Kind.STABLE_VALUE) selectorInfo.kind else DataFlowValue.Kind.OTHER

        override val canBeBound
            get() = receiverInfo.canBeBound

        override fun equals(other: Any?) =
            other is Qualified && receiverInfo == other.receiverInfo && selectorInfo == other.selectorInfo

        override fun hashCode() = 31 * receiverInfo.hashCode() + selectorInfo.hashCode()

        override fun toString() = "$receiverInfo${if (safe) "?." else "."}$selectorInfo"
    }

    // For only ++ and -- postfix operations
    data class PostfixIdentifierInfo(private val argumentInfo: IdentifierInfo, private val op: CjToken) :
        IdentifierInfo {
        override val kind: DataFlowValue.Kind get() = argumentInfo.kind

        override fun toString() = "$argumentInfo($op)"
    }

    data class Receiver(val value: ReceiverValue) : IdentifierInfo {
        override val kind = DataFlowValue.Kind.STABLE_VALUE

        override fun toString() = value.toString()
    }

    data class SafeCast(val subjectInfo: IdentifierInfo, val subjectType: CangJieType?, val targetType: CangJieType?) :
        IdentifierInfo {
        override val kind get() = DataFlowValue.Kind.OTHER

        override val canBeBound get() = subjectInfo.canBeBound

        override fun toString() = "$subjectInfo as? ${targetType ?: "???"}"
    }

    data class EnumEntry(val descriptor: EnumConstructorDescriptor) : IdentifierInfo {
        override val kind: DataFlowValue.Kind = DataFlowValue.Kind.STABLE_VALUE
    }

    class Expression(val expression: CjExpression, stableComplex: Boolean = false) : IdentifierInfo {
        override val kind =
            if (stableComplex) DataFlowValue.Kind.STABLE_COMPLEX_EXPRESSION else DataFlowValue.Kind.OTHER

        override fun equals(other: Any?) = other is Expression && expression == other.expression

        override fun hashCode() = expression.hashCode()

        override fun toString() = expression.text ?: "(empty expression)"
    }

    object ERROR : IdentifierInfo {
        override fun toString() = "ERROR"
    }
}


private fun getIdForSimpleNameExpression(
    simpleNameExpression: CjSimpleNameExpression,
    bindingContext: BindingContext,
    containingDeclarationOrModule: DeclarationDescriptor,
//    languageVersionSettings: LanguageVersionSettings
): IdentifierInfo {
    return when (val declarationDescriptor = bindingContext[BindingContext.REFERENCE_TARGET, simpleNameExpression]) {
        is VariableDescriptor -> {
            val resolvedCall = simpleNameExpression.getResolvedCall(bindingContext)

            // todo uncomment assert

            // for now it fails for resolving 'invoke' convention, return it after 'invoke' algorithm changes
            // assert resolvedCall != null : "Cannot create right identifier info if the resolved call is not known yet for
            val usageModuleDescriptor = DescriptorUtils.getContainingModuleOrNull(containingDeclarationOrModule)
            val selectorInfo = IdentifierInfo.Variable(
                declarationDescriptor,
                declarationDescriptor.variableKind(
                    usageModuleDescriptor,
                    bindingContext,
                    simpleNameExpression/*, languageVersionSettings*/
                ),
                bindingContext[BindingContext.BOUND_INITIALIZER_VALUE, declarationDescriptor]
            )

            val implicitReceiver = resolvedCall?.dispatchReceiver
            if (implicitReceiver == null) {
                selectorInfo
            } else {
                val receiverInfo = getIdForImplicitReceiver(implicitReceiver)

                if (receiverInfo == null) {
                    selectorInfo
                } else {
                    qualified(
                        receiverInfo, implicitReceiver.type,
                        selectorInfo, resolvedCall.call.isSafeCall()
                    )
                }
            }
        }
is EnumConstructorDescriptor ->   IdentifierInfo.EnumEntry(declarationDescriptor)
        is ClassDescriptor -> {

                IdentifierInfo.PackageOrClass(declarationDescriptor)
        }

        is PackageViewDescriptor -> IdentifierInfo.PackageOrClass(declarationDescriptor)

        else -> IdentifierInfo.NO
    }
}

private fun findReceiverByLabelOrGetDefault(
    descriptorOfThisReceiver: DeclarationDescriptor,
    default: ReceiverParameterDescriptor?,
    bindingContext: BindingContext,
    labelName: String
): ReceiverParameterDescriptor {
    val labelNameToReceiverMap =
        bindingContext[BindingContext.DESCRIPTOR_TO_CONTEXT_RECEIVER_MAP, descriptorOfThisReceiver]
    return labelNameToReceiverMap?.get(labelName)?.singleOrNull()
        ?: default
        ?: error("'This' refers to the callable member without a receiver parameter: $descriptorOfThisReceiver")
}

private fun getIdForThisReceiver(
    descriptorOfThisReceiver: DeclarationDescriptor?,
    bindingContext: BindingContext,
    labelName: String
) =
    when (descriptorOfThisReceiver) {
        is CallableDescriptor -> {
            val receiverParameter = findReceiverByLabelOrGetDefault(
                descriptorOfThisReceiver,
                descriptorOfThisReceiver.extensionReceiverParameter,
                bindingContext,
                labelName
            )
            IdentifierInfo.Receiver(receiverParameter.value)
        }

        is ClassDescriptor -> {
            val receiverParameter = findReceiverByLabelOrGetDefault(
                descriptorOfThisReceiver,
                descriptorOfThisReceiver.thisAsReceiverParameter,
                bindingContext,
                labelName
            )
            IdentifierInfo.Receiver(receiverParameter.value)
        }

        else -> IdentifierInfo.NO
    }

private fun getIdForImplicitReceiver(receiverValue: ReceiverValue?): IdentifierInfo? =
    when (receiverValue) {
        is ContextReceiver -> IdentifierInfo.Receiver(receiverValue)
        is ImplicitReceiver -> getIdForThisReceiver(receiverValue.declarationDescriptor)
        else -> null
    }
