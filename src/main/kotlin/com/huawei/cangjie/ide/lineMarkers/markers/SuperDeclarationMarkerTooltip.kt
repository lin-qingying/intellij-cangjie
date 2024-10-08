package com.huawei.cangjie.ide.lineMarkers.markers

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.descriptors.Modality
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjParameter
import com.huawei.cangjie.psi.CjProperty
import com.huawei.cangjie.psi.psiUtil.getParentOfType
import com.huawei.cangjie.references.util.DescriptorToSourceUtilsIde
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.impl.GutterTooltipBuilder
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.util.Function
import java.awt.event.MouseEvent
import java.util.concurrent.atomic.AtomicReference

object SuperDeclarationMarkerTooltip: Function<PsiElement, String> {
    override fun `fun`(element: PsiElement?): String? {
        val ktDeclaration = element?.getParentOfType<CjDeclaration>(false) ?: return null
        val (elementDescriptor, overriddenDescriptors) = resolveDeclarationWithParents(ktDeclaration)
        if (overriddenDescriptors.isEmpty()) return ""

        val isAbstract = elementDescriptor!!.modality == Modality.ABSTRACT

        val project = ktDeclaration.project

        val abstracts = hashSetOf<PsiElement>()
        val supers = overriddenDescriptors.mapNotNull {
            val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(
                project,
                it
            )
            if (declaration != null && it.modality == Modality.ABSTRACT) {
                abstracts.add(declaration)
            }
            declaration
        }

        val divider = GutterTooltipBuilder.getElementDivider(false, false, overriddenDescriptors.size)
        val reference = AtomicReference("")

        return CangJieGutterTooltipHelper.buildTooltipText(
            supers,
            { superMethod: PsiElement? ->
                val key =
                    if (abstracts.contains(superMethod) && !isAbstract) {
                        if (superMethod is CjProperty || superMethod is CjParameter) "tooltip.implements.property" else "tooltip.implements.function"
                    } else {
                        if (superMethod is CjProperty || superMethod is CjParameter) "tooltip.overrides.property" else "tooltip.overrides.function"
                    }
                reference.getAndSet(divider) + CangJieBundle.message(key) + " "
            },
            { true },
            IdeActions.ACTION_GOTO_SUPER
        )
    }
}
