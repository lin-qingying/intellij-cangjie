/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.references.util

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptorWithSource
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjPropertyAccessor
import org.cangnova.cangjie.psi.psiUtil.getNonStrictParentOfType
import org.cangnova.cangjie.resolve.source.getPsi
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.utils.importableFqName

// Navigation element of the resolved reference
// For property accessor return enclosing property
val PsiReference.unwrappedTargets: Set<PsiElement>
    get() {
        fun PsiElement.adjust(): PsiElement? = when (val target = unwrapped?.originalElement) {
            is CjPropertyAccessor -> target.getNonStrictParentOfType<CjProperty>()
            else -> target
        }

        return when (this) {
            is PsiPolyVariantReference -> multiResolve(false).mapNotNullTo(HashSet()) { it.element?.adjust() }
            else -> listOfNotNull(resolve()?.adjust()).toSet()
        }
    }

fun DeclarationDescriptor.findPsiDeclarations(
    project: Project,
    resolveScope: GlobalSearchScope
): Collection<PsiElement> {
    val fqName = importableFqName ?: return emptyList()

    fun Collection<CjNamedDeclaration>.fqNameFilter() = filter { it.fqName == fqName }
    return when (this) {
//        is DeserializedClassDescriptor ->
//            CangJieFullClassNameIndex.get(fqName.asString(), project, resolveScope)
//
//        is DeserializedTypeAliasDescriptor ->
//            CangJieTypeAliasShortNameIndex.get(fqName.shortName().asString(), project, resolveScope).fqNameFilter()
//
//        is DeserializedSimpleFunctionDescriptor,
//        is FunctionImportedFromObject ->
//            CangJieFunctionShortNameIndex.get(fqName.shortName().asString(), project, resolveScope).fqNameFilter()
//
//        is DeserializedPropertyDescriptor,
//        is PropertyImportedFromObject ->
//            CangJiePropertyShortNameIndex.get(fqName.shortName().asString(), project, resolveScope).fqNameFilter()

        is DeclarationDescriptorWithSource -> listOfNotNull(source.getPsi())

        else -> emptyList()
    }
}
