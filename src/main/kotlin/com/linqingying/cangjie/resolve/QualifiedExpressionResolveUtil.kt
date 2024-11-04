package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.impl.LazySubstitutingClassDescriptor
import com.linqingying.cangjie.diagnostics.Errors
import com.linqingying.cangjie.resolve.descriptorUtil.classValueDescriptor
import com.linqingying.cangjie.resolve.descriptorUtil.classValueTypeDescriptor

import com.linqingying.cangjie.resolve.scopes.receivers.*
import com.linqingying.cangjie.types.expressions.ExpressionTypingContext


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
    qualifier: QualifierReceiver, context: ExpressionTypingContext
): DeclarationDescriptor {
    val referenceTarget = resolveQualifierReferenceTarget(qualifier, null, context)

    when (referenceTarget) {
        is TypeAliasDescriptor -> {
            referenceTarget.classDescriptor?.let { classDescriptor ->
                if (!classDescriptor.kind.isObject) {
                    context.trace.report(Errors.EXPECTED_MEMBER_OR_CONSTRUCTOR_AFTER_TYPE.on(qualifier.expression, referenceTarget))
                }
            }
        }
        is TypeParameterDescriptor -> {
            context.trace.report(Errors.TYPE_PARAMETER_IS_NOT_AN_EXPRESSION.on(qualifier.expression, referenceTarget))
        }
        is ClassDescriptor -> {
            if (!context.config.isDotEnumGetType && !referenceTarget.hasClassValueDescriptor ) {
                context.trace.report(Errors.EXPECTED_MEMBER_OR_CONSTRUCTOR_AFTER_TYPE.on(qualifier.expression, referenceTarget))
            }
        }
        is PackageViewDescriptor -> {
            context.trace.report(Errors.EXPRESSION_EXPECTED_PACKAGE_FOUND.on(qualifier.expression))
        }
    }

    return referenceTarget
}

private fun resolveQualifierReferenceTarget(
    qualifier: QualifierReceiver,
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
