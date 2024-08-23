package com.huawei.cangjie.utils

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.SimpleFunctionDescriptor
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.getReceiverExpression
import com.huawei.cangjie.resolve.scopes.DescriptorKindExclude
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter


@Suppress("ClassName")
sealed class CallType<TReceiver : CjElement?>(val descriptorKindFilter: DescriptorKindFilter) {
    data object UNKNOWN : CallType<Nothing?>(DescriptorKindFilter.ALL)

    data object DEFAULT : CallType<Nothing?>(DescriptorKindFilter.ALL)

    data object DOT : CallType<CjExpression>(DescriptorKindFilter.ALL)

    data object SAFE : CallType<CjExpression>(DescriptorKindFilter.ALL)

//    object SUPER_MEMBERS : CallType<CjSuperExpression>(
//        DescriptorKindFilter.CALLABLES exclude DescriptorKindExclude.Extensions exclude AbstractMembersExclude
//    )


    data object OPERATOR : CallType<CjExpression>(DescriptorKindFilter.FUNCTIONS exclude NonOperatorExclude)

//    class CallableReference(settings: LanguageVersionSettings) :
//        CallType<CjExpression?>(DescriptorKindFilter.CALLABLES exclude LocalsAndSyntheticExclude(settings)) {
//        override fun equals(other: Any?): Boolean = other is CallableReference
//        override fun hashCode(): Int = javaClass.hashCode()
//    }

    data object IMPORT_DIRECTIVE : CallType<CjExpression?>(DescriptorKindFilter.ALL)

    data object PACKAGE_DIRECTIVE : CallType<CjExpression?>(DescriptorKindFilter.PACKAGES)

    data object TYPE : CallType<CjExpression?>(
        DescriptorKindFilter(DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK)
                exclude DescriptorKindExclude.EnumEntry
    )

    data object DELEGATE : CallType<CjExpression?>(DescriptorKindFilter.FUNCTIONS exclude NonOperatorExclude)

//    object ANNOTATION : CallType<CjExpression?>(
//        DescriptorKindFilter(DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK)
//                exclude NonAnnotationClassifierExclude
//    )


    private object NonOperatorExclude : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor) =
            !(descriptor is SimpleFunctionDescriptor && descriptor.isOperator)

        override val fullyExcludedDescriptorKinds: Int
            get() = 0
    }

//    private class LocalsAndSyntheticExclude(private val settings: LanguageVersionSettings) : DescriptorKindExclude() {
//        // Currently, Kotlin doesn't support references to local variables
//        // References to Java synthetic properties are supported only since Kotlin 1.9
//        override fun excludes(descriptor: DeclarationDescriptor): Boolean  =
//            descriptor !is CallableMemberDescriptor || descriptor.kind == CallableMemberDescriptor.Kind.SYNTHESIZED &&
//                    !settings.supportsFeature(LanguageFeature.ReferencesToSyntheticJavaProperties)
//
//        override val fullyExcludedDescriptorKinds: Int
//            get() = 0
//    }

//    private object NonAnnotationClassifierExclude : DescriptorKindExclude() {
//
//        override fun excludes(descriptor: DeclarationDescriptor): Boolean {
//            val descriptorToCheck = descriptor.unwrapIfTypeAlias()
//            if (descriptorToCheck !is ClassifierDescriptor) return false
//            return descriptorToCheck !is ClassDescriptor || descriptorToCheck.kind != ClassKind.ANNOTATION_CLASS
//        }
//
//        override val fullyExcludedDescriptorKinds: Int get() = 0
//
//    }

//    private object AbstractMembersExclude : DescriptorKindExclude() {
//        override fun excludes(descriptor: DeclarationDescriptor) =
//            descriptor is CallableMemberDescriptor && descriptor.modality == Modality.ABSTRACT
//
//        override val fullyExcludedDescriptorKinds: Int
//            get() = 0
//    }
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
//    class SUPER_MEMBERS(receiver: CjSuperExpression) : CallTypeAndReceiver<CjSuperExpression, CallType.SUPER_MEMBERS>(
//        CallType.SUPER_MEMBERS, receiver
//    )


    class OPERATOR(receiver: CjExpression) : CallTypeAndReceiver<CjExpression, CallType.OPERATOR>(CallType.OPERATOR, receiver)
//    class CALLABLE_REFERENCE(
//        receiver: CjExpression?,
//        val settings: LanguageVersionSettings
//    ) : CallTypeAndReceiver<CjExpression?, CallType.CallableReference>(CallType.CallableReference(settings), receiver)

    class IMPORT_DIRECTIVE(receiver: CjExpression?) : CallTypeAndReceiver<CjExpression?, CallType.IMPORT_DIRECTIVE>(
        CallType.IMPORT_DIRECTIVE, receiver
    )

    class PACKAGE_DIRECTIVE(receiver: CjExpression?) :
        CallTypeAndReceiver<CjExpression?, CallType.PACKAGE_DIRECTIVE>(CallType.PACKAGE_DIRECTIVE, receiver)

    class TYPE(receiver: CjExpression?) : CallTypeAndReceiver<CjExpression?, CallType.TYPE>(CallType.TYPE, receiver)
//    class DELEGATE(receiver: CjExpression?) : CallTypeAndReceiver<CjExpression?, CallType.DELEGATE>(CallType.DELEGATE, receiver)
//    class ANNOTATION(receiver: CjExpression?) : CallTypeAndReceiver<CjExpression?, CallType.ANNOTATION>(CallType.ANNOTATION, receiver)

    companion object {
        fun detect(expression: CjSimpleNameExpression): CallTypeAndReceiver<*, *> {
            val parent = expression.parent
//            if (parent is CjCallableReferenceExpression && expression == parent.callableReference) {
//                return CALLABLE_REFERENCE(parent.receiverExpression, expression.languageVersionSettings)
//            }
//
            val receiverExpression = expression.getReceiverExpression()
//
            if (parent != null) {
//                if (expression.isImportDirectiveExpression()) {
//                    return IMPORT_DIRECTIVE(receiverExpression)
//                }
//
//                if (expression.isPackageDirectiveExpression()) {
//                    return PACKAGE_DIRECTIVE(receiverExpression)
//                }
                if (parent is CjUserType) {
//                    val constructorCallee = (parent.parent as? CjTypeReference)?.parent as? CjConstructorCalleeExpression
//                    if (constructorCallee != null && constructorCallee.parent is CjAnnotationEntry) {
//                        return ANNOTATION(receiverExpression)
//                    }

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

//                    if (receiverExpression is CjSuperExpression) {
//                        return SUPER_MEMBERS(receiverExpression)
//                    }

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
