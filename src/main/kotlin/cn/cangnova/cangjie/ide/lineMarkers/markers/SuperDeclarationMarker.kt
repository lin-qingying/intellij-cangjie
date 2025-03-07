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

package cn.cangnova.cangjie.ide.lineMarkers.markers

import cn.cangnova.cangjie.CangJieBundle
import cn.cangnova.cangjie.descriptors.CallableMemberDescriptor
import cn.cangnova.cangjie.descriptors.VariableDescriptor
import cn.cangnova.cangjie.ide.codeinsight.CjFunctionPsiElementCellRenderer
import cn.cangnova.cangjie.ide.lineMarkers.shared.NavigationPopupDescriptor
import cn.cangnova.cangjie.ide.lineMarkers.shared.TestableLineMarkerNavigator
import cn.cangnova.cangjie.psi.CjDeclaration
import cn.cangnova.cangjie.psi.CjParameter
import cn.cangnova.cangjie.psi.psiUtil.getParentOfType
import cn.cangnova.cangjie.references.util.DescriptorToSourceUtilsIde
import cn.cangnova.cangjie.resolve.caches.resolveToDescriptorIfAny
import cn.cangnova.cangjie.resolve.getDirectlyOverriddenDeclarations
import cn.cangnova.cangjie.resolve.lazy.BodyResolveMode
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

