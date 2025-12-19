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

package org.cangnova.cangjie.references

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.references.util.findPsiDeclarations
import org.cangnova.cangjie.resolve.caches.getResolutionFacade
import org.cangnova.cangjie.resolve.caches.resolveImportReference
import org.cangnova.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import org.cangnova.cangjie.resolve.lazy.BodyResolveMode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocLink
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocName
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.caches.safeAnalyze


class CjReferenceResolutionHelperImpl : CjReferenceResolutionHelper {
    override fun partialAnalyze(element: CjElement): BindingContext = element.safeAnalyzeNonSourceRootCode(
        BodyResolveMode.PARTIAL
    )

    override fun resolveImportReference(file: CjFile, fqName: FqName): Collection<DeclarationDescriptor> =
        file.resolveImportReference(fqName)

    override fun resolveCDocLink(element: CDocName): Collection<DeclarationDescriptor> {
        val declaration = element.getContainingDoc().owner ?: return emptyList()
        val resolutionFacade = element.getResolutionFacade()
        val correctContext = declaration.safeAnalyze(resolutionFacade, BodyResolveMode.PARTIAL)
        if (correctContext == BindingContext.EMPTY) return emptyList()
        val declarationDescriptor =
            correctContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] ?: return emptyList()

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

    override fun findDecompiledDeclaration(
        project: Project,
        referencedDescriptor: DeclarationDescriptor,
        builtInsSearchScope: GlobalSearchScope?
    ): CjDeclaration? {
        return org.cangnova.cangjie.decompiler.navigation.findDecompiledDeclaration(
            project,
            referencedDescriptor,
            builtInsSearchScope
        )
    }

    override fun findPsiDeclarations(
        declaration: DeclarationDescriptor,
        project: Project,
        resolveScope: GlobalSearchScope
    ): Collection<PsiElement> = declaration.findPsiDeclarations(project, resolveScope)



}
