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

package org.cangnova.cangjie.search.declarationsSearch

import org.cangnova.cangjie.descriptors.CallableMemberDescriptor
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.references.util.DescriptorToSourceUtilsIde
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.caches.resolveToDescriptorIfAny
import org.cangnova.cangjie.resolve.source.getPsi
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.resolve.deprecation.getDirectlyOverriddenDeclarations


fun findSuperMethodsNoWrapping(method: PsiElement, deepest: Boolean): List<PsiElement> {
    return when (val element = method) {

        is CjCallableDeclaration -> {
            val descriptor = element.resolveToDescriptorIfAny() as? CallableMemberDescriptor ?: return emptyList()
            val superDeclarations = if (deepest) descriptor.getDeepestSuperDeclarations(false) else descriptor.getDirectlyOverriddenDeclarations()
            superDeclarations.mapNotNull {
                it.source.getPsi() ?: DescriptorToSourceUtilsIde.getAnyDeclaration(element.project, it)
            }
        }

        else -> emptyList()
    }
}
fun <D : CallableMemberDescriptor> D.getDeepestSuperDeclarations(withThis: Boolean = true): Collection<D> {
    val overriddenDeclarations = DescriptorUtils.getAllOverriddenDeclarations(this)
    if (overriddenDeclarations.isEmpty() && withThis) {
        return setOf(this)
    }

    return overriddenDeclarations.filterNot(DescriptorUtils::isOverride)
}
