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

package com.linqingying.cangjie.references

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.doc.psi.impl.CDocLink
import com.linqingying.cangjie.doc.psi.impl.CDocName
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.psi.psiUtil.getStrictParentOfType
import com.linqingying.cangjie.references.util.findPsiDeclarations
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.caches.getResolutionFacade
import com.linqingying.cangjie.resolve.caches.resolveImportReference
import com.linqingying.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.linqingying.cangjie.resolve.caches.safeAnalyze


class CjReferenceResolutionHelperImpl:CjReferenceResolutionHelper {
    override fun partialAnalyze(element: CjElement): BindingContext = element.safeAnalyzeNonSourceRootCode(
        BodyResolveMode.PARTIAL)

    override fun resolveImportReference(file: CjFile, fqName: FqName): Collection<DeclarationDescriptor> =
        file.resolveImportReference(fqName)
    override fun resolveCDocLink(element: CDocName): Collection<DeclarationDescriptor> {
        val declaration = element.getContainingDoc().owner ?: return emptyList()
        val resolutionFacade = element.getResolutionFacade()
        val correctContext = declaration.safeAnalyze(resolutionFacade, BodyResolveMode.PARTIAL)
        if (correctContext == BindingContext.EMPTY) return emptyList()
        val declarationDescriptor = correctContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] ?: return emptyList()

        val cdocLink = element.getStrictParentOfType<CDocLink>()!!
        return resolveCDocLink(
            correctContext,
            resolutionFacade,
            declarationDescriptor,
            element,
            cdocLink.getTagIfSubject(),
            element.getQualifiedName()
        )
    }
    override fun findPsiDeclarations(
        declaration: DeclarationDescriptor,
        project: Project,
        resolveScope: GlobalSearchScope
    ): Collection<PsiElement> = declaration.findPsiDeclarations(project, resolveScope)

//    override fun findDecompiledDeclaration(
//        project: Project,
//        referencedDescriptor: DeclarationDescriptor,
//        builtInsSearchScope: GlobalSearchScope?
//    ): CjDeclaration? =
//        com.linqingying.cangjie.ide.decompiler.navigation.findDecompiledDeclaration(project, referencedDescriptor, builtInsSearchScope)
//

}
