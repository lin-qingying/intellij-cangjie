package com.linqingying.cangjie.ide.lineMarkers.markers

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.descriptors.Modality
import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.CjParameter
import com.linqingying.cangjie.psi.CjProperty
import com.linqingying.cangjie.psi.psiUtil.getParentOfType
import com.linqingying.cangjie.references.util.DescriptorToSourceUtilsIde
import com.intellij.codeInsight.daemon.impl.GutterTooltipBuilder
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.psi.PsiElement
import com.intellij.util.Function
import java.util.concurrent.atomic.AtomicReference

object SuperDeclarationMarkerTooltip: Function<PsiElement, String> {
    override fun `fun`(element: PsiElement?): String? {
        val cjDeclaration = element?.getParentOfType<CjDeclaration>(false) ?: return null
        val (elementDescriptor, overriddenDescriptors) = resolveDeclarationWithParents(cjDeclaration)
        if (overriddenDescriptors.isEmpty()) return ""

        val isAbstract = elementDescriptor!!.modality == Modality.ABSTRACT

        val project = cjDeclaration.project

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
