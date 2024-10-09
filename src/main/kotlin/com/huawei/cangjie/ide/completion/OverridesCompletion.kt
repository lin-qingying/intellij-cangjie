package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.ide.CangJieDescriptorIconProvider
import com.huawei.cangjie.ide.IdeDescriptorRenderers
import com.huawei.cangjie.ide.ShortenReferences
import com.huawei.cangjie.ide.overrideImplement.OverrideMembersHandler
import com.huawei.cangjie.ide.quickfix.overrideImplement.generateMember
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.huawei.cangjie.references.util.DescriptorToSourceUtilsIde
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.ui.RowIcon


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
