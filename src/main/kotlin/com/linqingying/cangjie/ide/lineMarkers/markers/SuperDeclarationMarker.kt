package com.linqingying.cangjie.ide.lineMarkers.markers

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.descriptors.CallableMemberDescriptor
import com.linqingying.cangjie.descriptors.VariableDescriptor
import com.linqingying.cangjie.ide.codeinsight.CjFunctionPsiElementCellRenderer
import com.linqingying.cangjie.ide.lineMarkers.shared.NavigationPopupDescriptor
import com.linqingying.cangjie.ide.lineMarkers.shared.TestableLineMarkerNavigator
import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.CjParameter
import com.linqingying.cangjie.psi.psiUtil.getParentOfType
import com.linqingying.cangjie.references.util.DescriptorToSourceUtilsIde
import com.linqingying.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.linqingying.cangjie.resolve.getDirectlyOverriddenDeclarations
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import java.awt.event.MouseEvent

data class ResolveWithParentsResult(
    val descriptor: CallableMemberDescriptor?,
    val overriddenDescriptors: Collection<CallableMemberDescriptor>
)
fun resolveDeclarationWithParents(element: CjDeclaration): ResolveWithParentsResult {
    val descriptor = if (element is CjParameter)
        element.propertyDescriptor
    else
        element.resolveToDescriptorIfAny()

    if (descriptor !is CallableMemberDescriptor) return ResolveWithParentsResult(null, listOf())

    return ResolveWithParentsResult(descriptor, descriptor.getDirectlyOverriddenDeclarations())
}
val CjParameter.propertyDescriptor: VariableDescriptor?
    get() = this.resolveToDescriptorIfAny(BodyResolveMode.FULL) as? VariableDescriptor

class SuperDeclarationMarkerNavigationHandler : GutterIconNavigationHandler<PsiElement>, TestableLineMarkerNavigator {
    override fun navigate(e: MouseEvent?, element: PsiElement?) {
        e?.let { getTargetsPopupDescriptor(element)?.showPopup(e) }
    }

    override fun getTargetsPopupDescriptor(element: PsiElement?): NavigationPopupDescriptor? {
        val declaration = element?.getParentOfType<CjDeclaration>(false) ?: return null

        val (elementDescriptor, overriddenDescriptors) = resolveDeclarationWithParents(declaration)
        if (overriddenDescriptors.isEmpty()) return null

        val superDeclarations = ArrayList<NavigatablePsiElement>()
        for (overriddenMember in overriddenDescriptors) {
            val declarations = DescriptorToSourceUtilsIde.getAllDeclarations(element.project, overriddenMember)
            superDeclarations += declarations.filterIsInstance<NavigatablePsiElement>()
        }

        val elementName = elementDescriptor!!.name
        return NavigationPopupDescriptor(
            superDeclarations,
            CangJieBundle.message("overridden.marker.overrides.choose.implementation.title", elementName),
            CangJieBundle.message("overridden.marker.overrides.choose.implementation.find.usages", elementName),
            CjFunctionPsiElementCellRenderer()
        )
    }
}

