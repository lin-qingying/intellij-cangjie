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
