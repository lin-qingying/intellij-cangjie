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

import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.ide.projectStructure.languageVersionSettings
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.getReceiverExpression
import com.linqingying.cangjie.psi.psiUtil.isImportDirectiveExpression
import com.linqingying.cangjie.psi.psiUtil.isPackageDirectiveExpression
import com.linqingying.cangjie.resolve.calls.DslMarkerUtils
import com.linqingying.cangjie.resolve.scopes.DescriptorKindExclude
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValue
import com.linqingying.cangjie.resolve.unwrapIfTypeAlias
import com.linqingying.cangjie.types.CangJieType


@Suppress("ClassName")
sealed class CallType<TReceiver : CjElement?>(val descriptorKindFilter: DescriptorKindFilter) {
    data object UNKNOWN : CallType<Nothing?>(DescriptorKindFilter.ALL)

    data object DEFAULT : CallType<Nothing?>(DescriptorKindFilter.ALL)

    data object DOT : CallType<CjExpression>(DescriptorKindFilter.ALL)

    data object SAFE : CallType<CjExpression>(DescriptorKindFilter.ALL)
    private object AbstractMembersExclude : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor) =
            descriptor is CallableMemberDescriptor && descriptor.modality == Modality.ABSTRACT

        override val fullyExcludedDescriptorKinds: Int
            get() = 0
    }
    data object SUPER_MEMBERS : CallType<CjSuperExpression>(
        DescriptorKindFilter.CALLABLES exclude DescriptorKindExclude.Extensions exclude AbstractMembersExclude
    )


    data object OPERATOR : CallType<CjExpression>(DescriptorKindFilter.FUNCTIONS exclude NonOperatorExclude)

    class CallableReference(settings: LanguageVersionSettings) :
        CallType<CjExpression?>(DescriptorKindFilter.CALLABLES exclude LocalsAndSyntheticExclude(settings)) {
        override fun equals(other: Any?): Boolean = other is CallableReference
        override fun hashCode(): Int = javaClass.hashCode()
    }

    data object IMPORT_DIRECTIVE : CallType<CjExpression?>(DescriptorKindFilter.ALL)

    data object PACKAGE_DIRECTIVE : CallType<CjExpression?>(DescriptorKindFilter.PACKAGES)

    data object TYPE : CallType<CjExpression?>(
        DescriptorKindFilter(DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK)
                exclude DescriptorKindExclude.EnumEntry
    )

    data object DELEGATE : CallType<CjExpression?>(DescriptorKindFilter.FUNCTIONS exclude NonOperatorExclude)

    data object ANNOTATION : CallType<CjExpression?>(
        DescriptorKindFilter(DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK)
                exclude NonAnnotationClassifierExclude
    )
    private object NonAnnotationClassifierExclude : DescriptorKindExclude() {

        override fun excludes(descriptor: DeclarationDescriptor): Boolean {
            val descriptorToCheck = descriptor.unwrapIfTypeAlias()
            if (descriptorToCheck !is ClassifierDescriptor) return false
            return descriptorToCheck !is ClassDescriptor || descriptorToCheck.kind != ClassKind.ANNOTATION_CLASS
        }

        override val fullyExcludedDescriptorKinds: Int get() = 0

    }

    private class LocalsAndSyntheticExclude(private val settings: LanguageVersionSettings) : DescriptorKindExclude() {
        // Currently, Kotlin doesn't support references to local variables
        // References to Java synthetic properties are supported only since Kotlin 1.9
        override fun excludes(descriptor: DeclarationDescriptor): Boolean  =
            descriptor !is CallableMemberDescriptor || descriptor.kind == CallableMemberDescriptor.Kind.SYNTHESIZED

        override val fullyExcludedDescriptorKinds: Int
            get() = 0
    }

    private object NonOperatorExclude : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor) =
            !(descriptor is SimpleFunctionDescriptor && descriptor.isOperator)

        override val fullyExcludedDescriptorKinds: Int
            get() = 0
    }

}

@Suppress("ClassName")
sealed class CallTypeAndReceiver<TReceiver : CjElement?, out TCallType : CallType<TReceiver>>(
    val callType: TCallType,
    val receiver: TReceiver
) {
    data object UNKNOWN : CallTypeAndReceiver<Nothing?, CallType.UNKNOWN>(CallType.UNKNOWN, null)
    data object DEFAULT : CallTypeAndReceiver<Nothing?, CallType.DEFAULT>(CallType.DEFAULT, null)
    class DOT(receiver: CjExpression) : CallTypeAndReceiver<CjExpression, CallType.DOT>(CallType.DOT, receiver)
    class SAFE(receiver: CjExpression) : CallTypeAndReceiver<CjExpression, CallType.SAFE>(CallType.SAFE, receiver)
    class SUPER_MEMBERS(receiver: CjSuperExpression) : CallTypeAndReceiver<CjSuperExpression, CallType.SUPER_MEMBERS>(
        CallType.SUPER_MEMBERS, receiver
    )


    class OPERATOR(receiver: CjExpression) : CallTypeAndReceiver<CjExpression, CallType.OPERATOR>(CallType.OPERATOR, receiver)
    class CALLABLE_REFERENCE(
        receiver: CjExpression?,
        val settings: LanguageVersionSettings
    ) : CallTypeAndReceiver<CjExpression?, CallType.CallableReference>(CallType.CallableReference(settings), receiver)

    class IMPORT_DIRECTIVE(receiver: CjExpression?) : CallTypeAndReceiver<CjExpression?, CallType.IMPORT_DIRECTIVE>(
        CallType.IMPORT_DIRECTIVE, receiver
    )

    class PACKAGE_DIRECTIVE(receiver: CjExpression?) :
        CallTypeAndReceiver<CjExpression?, CallType.PACKAGE_DIRECTIVE>(CallType.PACKAGE_DIRECTIVE, receiver)

    class TYPE(receiver: CjExpression?) : CallTypeAndReceiver<CjExpression?, CallType.TYPE>(CallType.TYPE, receiver)
    class DELEGATE(receiver: CjExpression?) : CallTypeAndReceiver<CjExpression?, CallType.DELEGATE>(CallType.DELEGATE, receiver)
    class ANNOTATION(receiver: CjExpression?) : CallTypeAndReceiver<CjExpression?, CallType.ANNOTATION>(CallType.ANNOTATION, receiver)

    companion object {
        fun detect(expression: CjSimpleNameExpression): CallTypeAndReceiver<*, *> {
            val parent = expression.parent
            if (parent is CjCallableReferenceExpression && expression == parent.callableReference) {
                return CALLABLE_REFERENCE(parent.receiverExpression, expression.languageVersionSettings)
            }

            val receiverExpression = expression.getReceiverExpression()

            if (parent != null) {
                if (expression.isImportDirectiveExpression()) {
                    return IMPORT_DIRECTIVE(receiverExpression)
                }

                if (expression.isPackageDirectiveExpression()) {
                    return PACKAGE_DIRECTIVE(receiverExpression)
                }
                if (parent is CjUserType) {
                    val constructorCallee = (parent.parent as? CjTypeReference)?.parent as? CjConstructorCalleeExpression
                    if (constructorCallee != null && constructorCallee.parent is CjAnnotationEntry) {
                        return ANNOTATION(receiverExpression)
                    }

                    return TYPE(receiverExpression)
                }
            }

            when (expression) {
                is CjOperationReferenceExpression -> {
                    if (receiverExpression == null) {
                        return UNKNOWN // incomplete code
                    }
                    return when (parent) {
                        is CjBinaryExpression -> {

                                OPERATOR(receiverExpression)
                        }

                        is CjUnaryExpression -> OPERATOR(receiverExpression)

                        else -> error("Unknown parent for CjOperationReferenceExpression: $parent with text '${parent?.text}'")
                    }
                }

                is CjNameReferenceExpression -> {
                    if (receiverExpression == null) {
                        return DEFAULT
                    }

                    if (receiverExpression is CjSuperExpression) {
                        return SUPER_MEMBERS(receiverExpression)
                    }

                    return when (parent) {
                        is CjCallExpression -> {
                            if ((parent.parent as CjQualifiedExpression).operationSign == CjTokens.SAFE_ACCESS)
                                SAFE(receiverExpression)
                            else
                                DOT(receiverExpression)
                        }

                        is CjQualifiedExpression -> {
                            if (parent.operationSign == CjTokens.SAFE_ACCESS)
                                SAFE(receiverExpression)
                            else
                                DOT(receiverExpression)
                        }

                        else -> error("Unknown parent for CjNameReferenceExpression with receiver: $parent")
                    }
                }

                else -> return UNKNOWN
            }
        }
    }
}
data class ReceiverType(
    val type: CangJieType,
    val receiverIndex: Int,
    val implicitValue: ReceiverValue? = null
) {
    val implicit: Boolean get() = implicitValue != null


}
