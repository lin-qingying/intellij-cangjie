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

package org.cangnova.cangjie.quickfix.overrideImplement

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedDeclaration
import com.intellij.codeInsight.generation.ClassMemberWithElement
import com.intellij.codeInsight.generation.MemberChooserObject
import com.intellij.codeInsight.generation.PsiElementMemberChooserObject
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import javax.swing.Icon
import org.cangnova.cangjie.renderer.DescriptorRenderer
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.icon.CangJieDescriptorIconProvider
import org.cangnova.cangjie.renderer.ClassifierNamePolicy
import org.cangnova.cangjie.renderer.render
import org.cangnova.cangjie.types.fqNameUnsafe

open class DescriptorMemberChooserObject(
    psiElement: PsiElement,
    open val descriptor: DeclarationDescriptor
) : PsiElementMemberChooserObject(
    psiElement,
    getText(descriptor),
    getIcon(psiElement, descriptor)
), ClassMemberWithElement {

    override fun getParentNodeDelegate(): MemberChooserObject {
        val parent = descriptor.containingDeclaration ?: error("No parent for $descriptor")

        val declaration = if (psiElement is CjDeclaration) {
            PsiTreeUtil.getStubOrPsiParentOfType(psiElement, CjNamedDeclaration::class.java)
                ?: PsiTreeUtil.getStubOrPsiParentOfType(psiElement, CjFile::class.java)
        } else {
            PsiTreeUtil.getParentOfType(psiElement, CjNamedDeclaration::class.java)
        } ?: error("No parent for $psiElement")

        return when (declaration) {
            is CjFile -> PsiElementMemberChooserObject(declaration, declaration.name)
            else -> DescriptorMemberChooserObject(declaration, parent)
        }
    }

    override fun equals(other: Any?) = this === other || other is DescriptorMemberChooserObject && descriptor == other.descriptor

    override fun hashCode() = descriptor.hashCode()

    override fun getElement() = psiElement

    companion object {
        private val MEMBER_RENDERER = DescriptorRenderer.withOptions {
            withDefinedIn = false
            modifiers = emptySet()
            startFromName = true
            classifierNamePolicy = ClassifierNamePolicy.SHORT
            presentableUnresolvedTypes = true
        }

        @NlsSafe
        fun getText(descriptor: DeclarationDescriptor): String {
            return if (descriptor is ClassDescriptor)
                descriptor.fqNameUnsafe.render()
            else
                MEMBER_RENDERER.render(descriptor)
        }

        fun getIcon(declaration: PsiElement?, descriptor: DeclarationDescriptor): Icon? = if (declaration != null && declaration.isValid) {
            val isClass =  declaration is CjClass
            val flags = if (isClass) 0 else ICON_FLAG_VISIBILITY
            if (declaration is CjDeclaration) {

                CangJieDescriptorIconProvider.getIcon(descriptor, declaration, flags)
            } else {
                declaration.getIcon(flags)
            }
        } else {
            CangJieDescriptorIconProvider.getIcon(descriptor, declaration, 0)
        }
    }
}
