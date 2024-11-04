package com.linqingying.cangjie.ide.quickfix.overrideImplement

import com.linqingying.cangjie.builtins.fqNameUnsafe
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.psi.CjClass
import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.psi.CjNamedDeclaration
import com.intellij.codeInsight.generation.ClassMemberWithElement
import com.intellij.codeInsight.generation.MemberChooserObject
import com.intellij.codeInsight.generation.PsiElementMemberChooserObject
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import javax.swing.Icon
import com.linqingying.cangjie.renderer.DescriptorRenderer
import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.ide.CangJieDescriptorIconProvider
import com.linqingying.cangjie.renderer.ClassifierNamePolicy
import com.linqingying.cangjie.renderer.render

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
