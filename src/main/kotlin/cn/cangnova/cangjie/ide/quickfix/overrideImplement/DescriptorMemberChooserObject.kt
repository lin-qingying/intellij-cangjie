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

package cn.cangnova.cangjie.ide.quickfix.overrideImplement

import cn.cangnova.cangjie.builtins.fqNameUnsafe
import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.psi.CjClass
import cn.cangnova.cangjie.psi.CjDeclaration
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.psi.CjNamedDeclaration
import com.intellij.codeInsight.generation.ClassMemberWithElement
import com.intellij.codeInsight.generation.MemberChooserObject
import com.intellij.codeInsight.generation.PsiElementMemberChooserObject
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import javax.swing.Icon
import cn.cangnova.cangjie.renderer.DescriptorRenderer
import cn.cangnova.cangjie.descriptors.ClassDescriptor
import cn.cangnova.cangjie.ide.CangJieDescriptorIconProvider
import cn.cangnova.cangjie.renderer.ClassifierNamePolicy
import cn.cangnova.cangjie.renderer.render

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
            val flags = if (isClass) 0 else Iconable.ICON_FLAG_VISIBILITY
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
