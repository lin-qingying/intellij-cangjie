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

package cn.cangnova.cangjie.highlighter

import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.descriptors.PropertyDescriptor
import cn.cangnova.cangjie.descriptors.ValueParameterDescriptor
import cn.cangnova.cangjie.descriptors.VariableDescriptor
import cn.cangnova.cangjie.descriptors.impl.LocalVariableDescriptor
import cn.cangnova.cangjie.descriptors.impl.SyntheticFieldDescriptor
import cn.cangnova.cangjie.descriptors.impl.VariableDescriptorImpl
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.getReceiverExpression
import cn.cangnova.cangjie.renderer.DescriptorRenderer
import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.resolve.BindingContext.*
import cn.cangnova.cangjie.resolve.DescriptorUtils
import cn.cangnova.cangjie.resolve.caches.getResolutionFacade
import cn.cangnova.cangjie.resolve.calls.smartcasts.MultipleSmartCasts
import cn.cangnova.cangjie.resolve.calls.tasks.isDynamic
import cn.cangnova.cangjie.resolve.isVisible
import cn.cangnova.cangjie.types.expressions.CaptureKind
import cn.cangnova.cangjie.utils.safeAs
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement

internal class VariablesHighlightingVisitor(holder: HighlightInfoHolder, bindingContext: BindingContext) :
    AfterAnalysisHighlightingVisitor(holder, bindingContext) {

    override fun visitSimpleNameExpression(expression: CjSimpleNameExpression) {
        val target = bindingContext.get(REFERENCE_TARGET, expression) ?: return
        if (target is ValueParameterDescriptor && bindingContext.get(AUTO_CREATED_IT, target) == true) {
            highlightName(
                expression,
                CangJieHighlightInfoTypeSemanticNames.FUNCTION_LITERAL_DEFAULT_PARAMETER,
                CangJieHighlightingBundle.message("automatically.declared.based.on.the.expected.type")
            )
        } else if (expression.parent !is CjValueArgumentName) { // highlighted separately
            highlightVariable(expression, target)
        }

        super.visitSimpleNameExpression(expression)
    }

    override fun visitVariable(property: CjVariable) {
        visitVariableDeclaration(property)
        super.visitVariable(property)
    }

    override fun visitProperty(property: CjProperty) {
        visitVariableDeclaration(property)
        super.visitProperty(property)
    }

    override fun visitParameter(parameter: CjParameter) {
        val propertyDescriptor = bindingContext.get(PRIMARY_CONSTRUCTOR_PARAMETER, parameter)
        if (propertyDescriptor == null) {
            visitVariableDeclaration(parameter)
        }
        super.visitParameter(parameter)
    }

//    override fun visitDestructuringDeclarationEntry(multiDeclarationEntry: CjDestructuringDeclarationEntry) {
//        visitVariableDeclaration(multiDeclarationEntry)
//        super.visitDestructuringDeclarationEntry(multiDeclarationEntry)
//    }

    private fun getSmartCastTarget(expression: CjExpression): PsiElement {
        var target: PsiElement = expression
        if (target is CjParenthesizedExpression) {
            target = CjPsiUtil.deparenthesize(target) ?: expression
        }
        return when (target) {
            is CjIfExpression -> target.ifKeyword
            is CjMatchExpression -> target.matchKeyword
            is CjBinaryExpression -> target.operationReference
            else -> target
        }
    }

    override fun visitExpression(expression: CjExpression) {
//        val implicitSmartCast = bindingContext.get(IMPLICIT_RECEIVER_SMARTCAST, expression)
//        if (implicitSmartCast != null) {
//            for ((receiver, type) in implicitSmartCast.receiverTypes) {
//                val receiverName = when (receiver) {
//                    is ExtensionReceiver -> CangJieBaseHighlightingBundle.message("extension.implicit.receiver")
//                    is ImplicitClassReceiver -> CangJieBaseHighlightingBundle.message("implicit.receiver")
//                    else -> CangJieHighlightingBundle.message("unknown.receiver")
//                }
//                highlightName(
//                    expression,
//                    CangJieHighlightInfoTypeSemanticNames.SMART_CAST_RECEIVER,
//                    CangJieBaseHighlightingBundle.message(
//                        "0.smart.cast.to.1",
//                        receiverName,
//                        DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(type)
//                    )
//                )
//            }
//        }

        val nullSmartCast = bindingContext.get(SMARTCAST_NULL, expression) == true
        if (nullSmartCast) {
            highlightName(
                expression,
                CangJieHighlightInfoTypeSemanticNames.SMART_CONSTANT,
                CangJieHighlightingBundle.message("always.null")
            )
        }

        val smartCast = bindingContext.get(SMARTCAST, expression)
        if (smartCast != null) {
            val defaultType = smartCast.defaultType
            if (defaultType != null) {
                highlightName(
                    getSmartCastTarget(expression),
                    CangJieHighlightInfoTypeSemanticNames.SMART_CAST_VALUE,
                    CangJieHighlightingBundle.message(
                        "smart.cast.to.0",
                        DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(defaultType)
                    )
                )
            } else if (smartCast is MultipleSmartCasts) {
                for ((call, type) in smartCast.map) {
                    highlightName(
                        getSmartCastTarget(expression),
                        CangJieHighlightInfoTypeSemanticNames.SMART_CAST_VALUE,
                        CangJieHighlightingBundle.message(
                            "smart.cast.to.0.for.1.call",
                            DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(type),
                            (call?.callElement as? PsiNamedElement)?.name ?: "null"
                        )
                    )
                }
            }
        }

        super.visitExpression(expression)
    }

    private fun visitVariableDeclaration(declaration: CjNamedDeclaration) {
        val declarationDescriptor = bindingContext.get(DECLARATION_TO_DESCRIPTOR, declaration)
        val nameIdentifier = declaration.nameIdentifier
        if (nameIdentifier != null && declarationDescriptor != null) {
            highlightVariable(nameIdentifier, declarationDescriptor)
        }
    }

    private fun highlightVariable(elementToHighlight: PsiElement, descriptor: DeclarationDescriptor) {
        if (descriptor is VariableDescriptor) {

            if (descriptor.isDynamic()) {
                highlightName(elementToHighlight, CangJieHighlightInfoTypeSemanticNames.DYNAMIC_PROPERTY_CALL)
                return
            }

            if (descriptor.isVar) {
                val shouldHighlight = if (elementToHighlight is CjNameReferenceExpression) {
                    val setter = descriptor.safeAs<PropertyDescriptor>()?.setter
                    setter == null || setter.isVisible(
                        elementToHighlight, elementToHighlight.getReceiverExpression(), bindingContext,
                        elementToHighlight.getResolutionFacade()
                    )
                } else true
                if (shouldHighlight) {
                    highlightName(elementToHighlight, CangJieHighlightInfoTypeSemanticNames.MUTABLE_VARIABLE)
                }
            }

            if (bindingContext.get(CAPTURED_IN_CLOSURE, descriptor) == CaptureKind.NOT_INLINE) {
                val msg = if (descriptor.isVar)
                    CangJieHighlightingBundle.message("wrapped.into.a.reference.object.to.be.modified.when.captured.in.a.closure")
                else
                    CangJieHighlightingBundle.message("value.captured.in.a.closure")

                val parent = elementToHighlight.parent
                if (!(parent is PsiNameIdentifierOwner && parent.nameIdentifier == elementToHighlight)) {
                    highlightName(elementToHighlight, CangJieHighlightInfoTypeSemanticNames.WRAPPED_INTO_REF, msg)
                    return
                }
            }

            if (descriptor is LocalVariableDescriptor && descriptor !is SyntheticFieldDescriptor) {
                highlightName(elementToHighlight, CangJieHighlightInfoTypeSemanticNames.LOCAL_VARIABLE)
            }

            if (descriptor is ValueParameterDescriptor) {
                highlightName(elementToHighlight, CangJieHighlightInfoTypeSemanticNames.PARAMETER)
            }

            if (descriptor is PropertyDescriptor && hasCustomVariableDeclaration(descriptor)) {
                val isStaticDeclaration = DescriptorUtils.isStaticDeclaration(descriptor)
                highlightName(
                    elementToHighlight,
                    if (isStaticDeclaration)
                        CangJieHighlightInfoTypeSemanticNames.PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION
                    else
                        CangJieHighlightInfoTypeSemanticNames.INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION
                )
            }
            if (descriptor is VariableDescriptorImpl) {
                val isStaticDeclaration = DescriptorUtils.isStaticDeclaration(descriptor)
                highlightName(
                    elementToHighlight,
                    if (isStaticDeclaration)
                        CangJieHighlightInfoTypeSemanticNames.PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION
                    else
                        CangJieHighlightInfoTypeSemanticNames.INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION
                )
            }
        }
    }
}

internal fun hasCustomVariableDeclaration(descriptor: VariableDescriptor): Boolean {
    var hasCustomPropertyDeclaration = false
    if (!hasExtensionReceiverParameter(descriptor) && descriptor is PropertyDescriptor) {
        if (descriptor.getter?.isDefault == false || descriptor.setter?.isDefault == false)
            hasCustomPropertyDeclaration = true
    }
    return hasCustomPropertyDeclaration
}

internal fun hasExtensionReceiverParameter(descriptor: VariableDescriptor): Boolean {
    return descriptor.extensionReceiverParameter != null
}
