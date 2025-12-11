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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.infos.errors.EXPECTED_MEMBER_OR_CONSTRUCTOR_AFTER_TYPE
import org.cangnova.cangjie.diagnostics.infos.errors.EXPRESSION_EXPECTED_PACKAGE_FOUND
import org.cangnova.cangjie.diagnostics.infos.errors.TYPE_PARAMETER_IS_NOT_AN_EXPRESSION
import org.cangnova.cangjie.diagnostics.infos.errors.TYPE_PARAMETER_ON_LHS_OF_DOT
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.scopes.receivers.*
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext


fun resolveQualifierAsReceiverInExpression(
    qualifier: Qualifier, selector: DeclarationDescriptor?, context: ExpressionTypingContext
): DeclarationDescriptor {
    val referenceTarget = resolveQualifierReferenceTarget(qualifier, selector, context)

    if (referenceTarget is TypeParameterDescriptor) {
        context.trace.report(TYPE_PARAMETER_ON_LHS_OF_DOT.on(qualifier.referenceExpression, referenceTarget))
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
                    context.trace.report(
                        EXPECTED_MEMBER_OR_CONSTRUCTOR_AFTER_TYPE.on(
                            qualifier.expression,
                            referenceTarget
                        )
                    )
                }
            }
        }

        is TypeParameterDescriptor -> {
            context.trace.report(TYPE_PARAMETER_IS_NOT_AN_EXPRESSION.on(qualifier.expression, referenceTarget))
        }

        is ClassDescriptor -> {
            if (!context.config.isDotEnumGetType && !referenceTarget.hasClassValueDescriptor) {
                context.trace.report(
                    EXPECTED_MEMBER_OR_CONSTRUCTOR_AFTER_TYPE.on(
                        qualifier.expression,
                        referenceTarget
                    )
                )
            }
        }

        is PackageViewDescriptor -> {
            context.trace.report(EXPRESSION_EXPECTED_PACKAGE_FOUND.on(qualifier.expression))
        }
    }

    return referenceTarget
}

val ClassDescriptor.hasClassValueDescriptor: Boolean get() = classValueDescriptor != null

private fun resolveQualifierReferenceTarget(
    qualifier: QualifierReceiver,
    selector: DeclarationDescriptor?,
    context: ExpressionTypingContext
): DeclarationDescriptor {
    if (qualifier is TypeParameterQualifier) {
        return qualifier.descriptor
    }

    val selectorContainer = when (selector) {
        is ConstructorDescriptor ->
            selector.containingDeclaration.containingDeclaration

        else ->
            selector?.containingDeclaration
    }

    if (qualifier is PackageQualifier &&
        (selectorContainer is PackageFragmentDescriptor || selectorContainer is PackageViewDescriptor) &&
        DescriptorUtils.getFqName(qualifier.descriptor) == DescriptorUtils.getFqName(selectorContainer)
    ) {
        return qualifier.descriptor
    }

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
            context.trace.record(
                BindingContext.REFERENCE_TARGET,
                qualifier.referenceExpression,
                receiverClassifierDescriptor
            )
            context.trace.recordType(qualifier.expression, classValueTypeDescriptor.defaultType)
//            if (classifier.hasCompanionObject) {
//                context.trace.record(BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, qualifier.referenceExpression, classifier)
//            }
            return classValueTypeDescriptor
        }
    }

    return qualifier.descriptor
}

val ClassDescriptor.classValueDescriptor: ClassDescriptor?
    get() =
//        TODO 这里需要实现具体的逻辑
        this

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
