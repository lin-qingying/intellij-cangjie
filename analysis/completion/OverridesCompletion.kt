/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.completion

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getNonStrictParentOfType
import org.cangnova.cangjie.references.util.DescriptorToSourceUtilsIde
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.ui.RowIcon
import org.cangnova.cangjie.codeinsight.ShortenReferences
import org.cangnova.cangjie.diagnostics.rendering.IdeDescriptorRenderers
import org.cangnova.cangjie.icon.CangJieDescriptorIconProvider
import org.cangnova.cangjie.quickfix.overrideImplement.OverrideMembersHandler
import org.cangnova.cangjie.quickfix.overrideImplement.generateMember


class OverridesCompletion(
    private val collector: LookupElementsCollector,
    private val lookupElementFactory: BasicLookupElementFactory
) {
    private val PRESENTATION_RENDERER = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.withOptions {
        modifiers = emptySet()
        includeAdditionalModifiers = false
    }

    fun complete(position: PsiElement, declaration: CjCallableDeclaration?) {
        val isConstructorParameter = position.getNonStrictParentOfType<CjPrimaryConstructor>() != null

        val classOrObject = position.getNonStrictParentOfType<CjTypeStatement>() ?: return

        val members = OverrideMembersHandler(isConstructorParameter).collectMembersToGenerate(classOrObject)

        for (memberObject in members) {
            val descriptor = memberObject.descriptor
            if (declaration != null && !canOverride(descriptor, declaration)) continue
            if (isConstructorParameter && descriptor !is PropertyDescriptor) continue

            var lookupElement = lookupElementFactory.createLookupElement(descriptor)

            var text = "override " + PRESENTATION_RENDERER.render(descriptor)
            if (descriptor is FunctionDescriptor) {
                text += " {...}"
            }

            val baseClass = descriptor.containingDeclaration as ClassDescriptor
            val baseClassName = baseClass.name.asString()

            val baseIcon = (lookupElement.`object` as DescriptorBasedDeclarationLookupObject).getIcon(0)
            val isImplement = descriptor.modality == Modality.ABSTRACT
            val additionalIcon = if (isImplement)
                AllIcons.Gutter.ImplementingMethod
            else
                AllIcons.Gutter.OverridingMethod
            val icon = RowIcon(baseIcon, additionalIcon)

            val baseClassDeclaration = DescriptorToSourceUtilsIde.getAnyDeclaration(position.project, baseClass)
            val baseClassIcon = CangJieDescriptorIconProvider.getIcon(baseClass, baseClassDeclaration, 0)

            lookupElement = OverridesCompletionLookupElementDecorator(
                lookupElement,
                declaration,
                text,
                isImplement,
                icon,
                baseClassName,
                baseClassIcon,
                isConstructorParameter,

                generateMember = { memberObject.generateMember(classOrObject, copyDoc = false) },
                shortenReferences = ShortenReferences.DEFAULT::process,
            )

            lookupElement.assignPriority(if (isImplement) ItemPriority.IMPLEMENT else ItemPriority.OVERRIDE)

            collector.addElement(lookupElement)
        }
    }

    private fun canOverride(descriptorToOverride: CallableMemberDescriptor, declaration: CjCallableDeclaration): Boolean {
        when (declaration) {
            is CjFunction -> return descriptorToOverride is FunctionDescriptor

            is CjLetVarKeywordOwner -> {
                if (descriptorToOverride !is PropertyDescriptor) return false
                return if (declaration.letOrVarKeyword?.node?.elementType == CjTokens.LET_KEYWORD) {
                    !descriptorToOverride.isVar
                } else {
                    true // var can override either var or val
                }
            }

            else -> return false
        }
    }
}
