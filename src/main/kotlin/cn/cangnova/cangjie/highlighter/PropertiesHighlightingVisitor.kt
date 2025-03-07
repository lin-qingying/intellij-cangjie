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

import cn.cangnova.cangjie.descriptors.PropertyDescriptor
import cn.cangnova.cangjie.descriptors.VariableDescriptor
import cn.cangnova.cangjie.descriptors.impl.SyntheticFieldDescriptor
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.resolve.BindingContext.*
import cn.cangnova.cangjie.resolve.DescriptorUtils
import cn.cangnova.cangjie.resolve.calls.tasks.isDynamic
import cn.cangnova.cangjie.resolve.calls.tower.isSynthesized
import cn.cangnova.cangjie.resolve.calls.util.getResolvedCall
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.psi.PsiElement

internal class PropertiesHighlightingVisitor(holder: HighlightInfoHolder, bindingContext: BindingContext) :
    AfterAnalysisHighlightingVisitor(holder, bindingContext) {

    override fun visitSimpleNameExpression(expression: CjSimpleNameExpression) {
        if (expression.parent is CjThisExpression) {
            return
        }
        val target = bindingContext.get(REFERENCE_TARGET, expression)
        if (target is SyntheticFieldDescriptor) {
            highlightName(expression, CangJieHighlightInfoTypeSemanticNames.BACKING_FIELD_VARIABLE)
            return
        }
        if (target !is VariableDescriptor) {
            return
        }

        val resolvedCall = expression.getResolvedCall(bindingContext)

        val attributesKey = resolvedCall?.let { call ->
            @Suppress("DEPRECATION")
            CangJieHighlightingVisitorExtension.EP_NAME.extensionList.firstNotNullOfOrNull { extension ->
                extension.highlightCall(expression, call)
            }
        }

        val highlightInfoType = attributesKey ?: attributeKeyByVariableType(target)
        if (highlightInfoType != null) {
            highlightName(expression, highlightInfoType)
        }
    }

    override fun visitVariable(variable: CjVariable) {
        val nameIdentifier = variable.nameIdentifier ?: return
        val propertyDescriptor = bindingContext.get(VARIABLE, variable)
        if (propertyDescriptor is VariableDescriptor) {
            highlightVariableDeclaration(nameIdentifier, propertyDescriptor)
        }

        super.visitVariable(variable)
    }

    override fun visitProperty(property: CjProperty) {
        val nameIdentifier = property.nameIdentifier ?: return
        val propertyDescriptor = bindingContext.get(VARIABLE, property)
        if (propertyDescriptor is VariableDescriptor) {
            highlightVariableDeclaration(nameIdentifier, propertyDescriptor)
        }

        super.visitProperty(property)
    }

    override fun visitParameter(parameter: CjParameter) {
        val nameIdentifier = parameter.nameIdentifier ?: return
        val propertyDescriptor = bindingContext.get(PRIMARY_CONSTRUCTOR_PARAMETER, parameter)
        if (propertyDescriptor != null) {
            if (propertyDescriptor.isVar) {
                highlightName(nameIdentifier, CangJieHighlightInfoTypeSemanticNames.MUTABLE_VARIABLE)
            }
            highlightVariableDeclaration(nameIdentifier, propertyDescriptor)
        }

        super.visitParameter(parameter)
    }

    private fun highlightVariableDeclaration(
        elementToHighlight: PsiElement,
        descriptor: VariableDescriptor
    ) {
        val highlightInfoType =
            attributeKeyForDeclarationFromExtensions(elementToHighlight, descriptor) ?: attributeKeyByVariableType(
                descriptor
            )
        if (highlightInfoType != null) {
            highlightName(elementToHighlight, highlightInfoType)
        }
    }

    private fun attributeKeyByVariableType(descriptor: VariableDescriptor): HighlightInfoType? = when {
        descriptor.isDynamic() ->
            // The property is set in VariablesHighlightingVisitor
            null

        hasExtensionReceiverParameter(descriptor) ->
            if (descriptor.isSynthesized) CangJieHighlightInfoTypeSemanticNames.SYNTHETIC_EXTENSION_PROPERTY else CangJieHighlightInfoTypeSemanticNames.EXTENSION_PROPERTY

        DescriptorUtils.isStaticDeclaration(descriptor) ->
            if (hasCustomVariableDeclaration(descriptor))
                CangJieHighlightInfoTypeSemanticNames.PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION
            else
                CangJieHighlightInfoTypeSemanticNames.PACKAGE_PROPERTY

        else ->
            if (hasCustomVariableDeclaration(descriptor))
                CangJieHighlightInfoTypeSemanticNames.INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION
            else
                CangJieHighlightInfoTypeSemanticNames.INSTANCE_PROPERTY
    }
}


internal fun hasExtensionReceiverParameter(descriptor: PropertyDescriptor): Boolean {
    return descriptor.extensionReceiverParameter != null
}
