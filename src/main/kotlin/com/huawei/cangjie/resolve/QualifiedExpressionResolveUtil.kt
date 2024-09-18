package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.diagnostics.Errors
import com.huawei.cangjie.resolve.descriptorUtil.classValueDescriptor
import com.huawei.cangjie.resolve.descriptorUtil.classValueTypeDescriptor
import com.huawei.cangjie.resolve.scopes.receivers.ClassifierQualifier
import com.huawei.cangjie.resolve.scopes.receivers.Qualifier
import com.huawei.cangjie.resolve.scopes.receivers.expression
import com.huawei.cangjie.types.expressions.ExpressionTypingContext


fun resolveQualifierAsReceiverInExpression(
    qualifier: Qualifier, selector: DeclarationDescriptor?, context: ExpressionTypingContext
): DeclarationDescriptor {
    val referenceTarget = resolveQualifierReferenceTarget(qualifier, selector, context)

    if (referenceTarget is TypeParameterDescriptor) {
        context.trace.report(Errors.TYPE_PARAMETER_ON_LHS_OF_DOT.on(qualifier.referenceExpression, referenceTarget))
    }

    return referenceTarget
}

fun resolveQualifierAsStandaloneExpression(
    qualifier: Qualifier, context: ExpressionTypingContext
): DeclarationDescriptor {
    val referenceTarget = resolveQualifierReferenceTarget(qualifier, null, context)

    when (referenceTarget) {
        is TypeAliasDescriptor -> {
            referenceTarget.classDescriptor?.let { classDescriptor ->
                if (!classDescriptor.kind.isObject) {
                    context.trace.report(Errors.EXPECTED_MEMBER_OR_CONSTRUCTOR_AFTER_TYPE.on(qualifier.referenceExpression, referenceTarget))
                }
            }
        }
        is TypeParameterDescriptor -> {
            context.trace.report(Errors.TYPE_PARAMETER_IS_NOT_AN_EXPRESSION.on(qualifier.referenceExpression, referenceTarget))
        }
        is ClassDescriptor -> {
            if (!referenceTarget.kind.isObject) {
                context.trace.report(Errors.EXPECTED_MEMBER_OR_CONSTRUCTOR_AFTER_TYPE.on(qualifier.referenceExpression, referenceTarget))
            }
        }
        is PackageViewDescriptor -> {
            context.trace.report(Errors.EXPRESSION_EXPECTED_PACKAGE_FOUND.on(qualifier.referenceExpression))
        }
    }

    return referenceTarget
}

private fun resolveQualifierReferenceTarget(
    qualifier: Qualifier,
    selector: DeclarationDescriptor?,
    context: ExpressionTypingContext
): DeclarationDescriptor {
//    if (qualifier is TypeParameterQualifier) {
//        return qualifier.descriptor
//    }

    val selectorContainer = when (selector) {
        is ConstructorDescriptor ->
            selector.containingDeclaration.containingDeclaration
        else ->
            selector?.containingDeclaration
    }

//    if (qualifier is PackageQualifier &&
//        (selectorContainer is PackageFragmentDescriptor || selectorContainer is PackageViewDescriptor) &&
//        DescriptorUtils.getFqName(qualifier.descriptor) == DescriptorUtils.getFqName(selectorContainer)
//    ) {
//        return qualifier.descriptor
//    }

    // TODO make decisions about short reference to companion object somewhere else
    if (qualifier is ClassifierQualifier) {
        val classifier = qualifier.descriptor
        val selectorIsCallable = selector is CallableDescriptor &&
                (selector.dispatchReceiverParameter != null || selector.extensionReceiverParameter != null)
        // TODO simplify this code.
        // Given a class qualifier in expression position,
        // it should provide a proper REFERENCE_TARGET (with type),
        // and, in case of implicit companion object reference, SHORT_REFERENCE_TO_COMPANION_OBJECT.
        val receiverClassifierDescriptor = classifier.getCallableReceiverDescriptorRetainingTypeAliasReference()
        if (selectorIsCallable && receiverClassifierDescriptor != null) {
            val classValueTypeDescriptor = classifier.classValueTypeDescriptor!!
            context.trace.record(BindingContext.REFERENCE_TARGET, qualifier.referenceExpression, receiverClassifierDescriptor)
            context.trace.recordType(qualifier.expression, classValueTypeDescriptor.defaultType)
//            if (classifier.hasCompanionObject) {
//                context.trace.record(BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, qualifier.referenceExpression, classifier)
//            }
            return classValueTypeDescriptor
        }
    }

    return qualifier.descriptor
}

private fun ClassifierDescriptor.getCallableReceiverDescriptorRetainingTypeAliasReference(): DeclarationDescriptor? =
    when (this) {
        is ClassDescriptor -> classValueDescriptor

        is TypeAliasDescriptor ->
//            if (classDescriptor?.classValueDescriptor != null)
//                FakeCallableDescriptorForTypeAliasObject(this)
//            else
                this

        else -> null
    }
