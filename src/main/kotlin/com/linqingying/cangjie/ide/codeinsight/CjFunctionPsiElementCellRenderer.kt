package com.linqingying.cangjie.ide.codeinsight

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.psi.CjNamedFunction
import com.linqingying.cangjie.renderer.DescriptorRenderer.Companion.SHORT_NAMES_IN_TYPES
import com.linqingying.cangjie.resolve.caches.unsafeResolveToDescriptor
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.presentation.java.SymbolPresentationUtil

class CjFunctionPsiElementCellRenderer : PsiTargetPresentationRenderer<PsiElement>() {
    override fun getElementText(element: PsiElement): String {
        if (element is CjNamedFunction) {
            val descriptor: DeclarationDescriptor =
                element.unsafeResolveToDescriptor(BodyResolveMode.PARTIAL)
            return SHORT_NAMES_IN_TYPES.render(descriptor) //NON-NLS
        }
        return super.getElementText(element)
    }

    override fun getPresentation(element: PsiElement): TargetPresentation {
        return TargetPresentation.builder(getElementText(element))
            .containerText(SymbolPresentationUtil.getSymbolContainerText(element))
            .icon(getIcon(element))
            .presentation()
    }
}
