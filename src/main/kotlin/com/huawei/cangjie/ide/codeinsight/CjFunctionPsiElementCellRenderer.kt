package com.huawei.cangjie.ide.codeinsight

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.psi.CjNamedFunction
import com.huawei.cangjie.renderer.DescriptorRenderer.Companion.SHORT_NAMES_IN_TYPES
import com.huawei.cangjie.resolve.caches.unsafeResolveToDescriptor
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
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
