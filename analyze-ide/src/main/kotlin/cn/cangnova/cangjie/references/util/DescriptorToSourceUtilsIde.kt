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

package cn.cangnova.cangjie.references.util

import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.references.CjReferenceResolutionHelper
import cn.cangnova.cangjie.resolve.DescriptorToSourceUtils
import cn.cangnova.cangjie.utils.sequenceOfLazyValues
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope

object DescriptorToSourceUtilsIde {
    // Returns PSI element for descriptor. If there are many relevant elements (e.g. it is fake override
    // with multiple declarations), finds any of them. It can find declarations in builtins or decompiled code.
    fun getAnyDeclaration(project: Project, descriptor: DeclarationDescriptor): PsiElement? {
        return getDeclarationsStream(project, descriptor).firstOrNull()
    }

    // Returns all PSI elements for descriptor. It can find declarations in builtins or decompiled code.
    fun getAllDeclarations(
        project: Project,
        targetDescriptor: DeclarationDescriptor,
        builtInsSearchScope: GlobalSearchScope? = null
    ): Collection<PsiElement> {
        val result = getDeclarationsStream(project, targetDescriptor, builtInsSearchScope).toHashSet()
        // filter out elements which are navigate to some other element of the result
        // this is needed to avoid duplicated results for references to declaration in same library source file
        return result.filter { element -> result.none { element != it && it.navigationElement == element } }
    }

    private fun getDeclarationsStream(
        project: Project, targetDescriptor: DeclarationDescriptor, builtInsSearchScope: GlobalSearchScope? = null
    ): Sequence<PsiElement> {
        val effectiveReferencedDescriptors = DescriptorToSourceUtils.getEffectiveReferencedDescriptors(targetDescriptor).asSequence()
        return effectiveReferencedDescriptors.flatMap { effectiveReferenced ->
            // References in library sources should be resolved to corresponding decompiled declarations,
            // therefore we put both source declaration and decompiled declaration to stream, and afterwards we filter it in getAllDeclarations
            sequenceOfLazyValues(
                { DescriptorToSourceUtils.getSourceFromDescriptor(effectiveReferenced) },
//                { CjReferenceResolutionHelper.getInstance().findDecompiledDeclaration(project, effectiveReferenced, builtInsSearchScope) }
            )
        }.filterNotNull()
    }
}
