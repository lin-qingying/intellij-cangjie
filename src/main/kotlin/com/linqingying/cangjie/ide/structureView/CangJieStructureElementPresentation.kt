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

package com.linqingying.cangjie.ide.structureView

import com.intellij.navigation.ColoredItemPresentation
import com.intellij.navigation.LocationPresentation
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.Iconable
import com.intellij.psi.NavigatablePsiElement
import com.intellij.util.PsiIconUtil.getIconFromProviders
import com.intellij.util.ui.StartupUiUtil
import com.linqingying.cangjie.descriptors.CallableMemberDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.ide.CangJieDescriptorIconProvider
import com.linqingying.cangjie.ide.projectView.CjDeclarationTreeNode.Companion.tryGetRepresentableText
import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.CjModifierListOwner
import com.linqingying.cangjie.psi.CjPsiUtil
import com.linqingying.cangjie.renderer.DescriptorRenderer.Companion.ONLY_NAMES_WITH_SHORT_TYPES
import com.linqingying.cangjie.resolve.DescriptorUtils.getAllOverriddenDeclarations
import com.linqingying.cangjie.resolve.OverridingUtil.filterOutOverridden
import javax.swing.Icon

internal class CangJieStructureElementPresentation(
    private val isInherited: Boolean,
    navigatablePsiElement: NavigatablePsiElement,
    descriptor: DeclarationDescriptor?
) : ColoredItemPresentation, LocationPresentation {
    private val attributesKey = getElementAttributesKey(isInherited, navigatablePsiElement)
    private val elementText = getElementText(navigatablePsiElement, descriptor)
    private val locationString = getElementLocationString(isInherited, descriptor)
    private val icon = getElementIcon(navigatablePsiElement, descriptor)

    override fun getTextAttributesKey() = attributesKey

    override fun getPresentableText() = elementText

    override fun getLocationString() = locationString

    override fun getIcon(unused: Boolean) = icon

    override fun getLocationPrefix(): String {
        return if (isInherited) " " else LocationPresentation.DEFAULT_LOCATION_PREFIX
    }

    override fun getLocationSuffix(): String {
        return if (isInherited) "" else LocationPresentation.DEFAULT_LOCATION_SUFFIX
    }

    private fun getElementAttributesKey(
        isInherited: Boolean,
        navigatablePsiElement: NavigatablePsiElement
    ): TextAttributesKey? {
        if (isInherited) {
            return CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES
        }

        if (navigatablePsiElement is CjModifierListOwner && CjPsiUtil.isDeprecated(navigatablePsiElement)) {
            return CodeInsightColors.DEPRECATED_ATTRIBUTES
        }

        return null
    }

    private fun getElementIcon(
        navigatablePsiElement: NavigatablePsiElement,
        descriptor: DeclarationDescriptor?
    ): Icon? {
        if (descriptor != null) {
            return CangJieDescriptorIconProvider.getIcon(
                descriptor,
                navigatablePsiElement,
                Iconable.ICON_FLAG_VISIBILITY
            )
        }

        if (!navigatablePsiElement.isValid) {
            return null
        }

        return getIconFromProviders(navigatablePsiElement, Iconable.ICON_FLAG_VISIBILITY)

    }

    private fun getElementText(
        navigatablePsiElement: NavigatablePsiElement,
        descriptor: DeclarationDescriptor?
    ): String? {


        if (descriptor != null) {
            return ONLY_NAMES_WITH_SHORT_TYPES.render(descriptor)
        }

        navigatablePsiElement.name.takeUnless { it.isNullOrEmpty() }?.let { return it }

        return (navigatablePsiElement as? CjDeclaration)?.let(::tryGetRepresentableText)
    }

    private fun getElementLocationString(isInherited: Boolean, descriptor: DeclarationDescriptor?): String? {
        if (!(isInherited && descriptor is CallableMemberDescriptor)) return null

        if (descriptor.kind == CallableMemberDescriptor.Kind.DECLARATION) {
            return withRightArrow(ONLY_NAMES_WITH_SHORT_TYPES.render(descriptor.containingDeclaration))
        }

        val overridingDescriptors = filterOutOverridden(getAllOverriddenDeclarations(descriptor))
        // Location can be missing when base in synthesized
        return overridingDescriptors.firstOrNull()?.let {
            withRightArrow(ONLY_NAMES_WITH_SHORT_TYPES.render(it.containingDeclaration))
        }
    }

    private fun withRightArrow(str: String): String {
        val rightArrow = '\u2192'
        return if (StartupUiUtil.labelFont.canDisplay(rightArrow)) rightArrow + str else "->$str"
    }
}
