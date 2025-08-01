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

package cn.cangnova.cangjie.references

import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.doc.psi.impl.CDocName
import cn.cangnova.cangjie.psi.CjElement
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.resolve.BindingContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope

interface CjReferenceResolutionHelper {
    fun partialAnalyze(element: CjElement): BindingContext
    fun resolveImportReference(file: CjFile, fqName: FqName): Collection<DeclarationDescriptor>
    fun findPsiDeclarations(declaration: DeclarationDescriptor, project: Project, resolveScope: GlobalSearchScope): Collection<PsiElement>
    fun resolveCDocLink(element: CDocName): Collection<DeclarationDescriptor>

//    fun findDecompiledDeclaration(
//        project: Project,
//        referencedDescriptor: DeclarationDescriptor,
//        builtInsSearchScope: GlobalSearchScope?
//    ): CjDeclaration?
    companion object {
        fun getInstance(): CjReferenceResolutionHelper = ApplicationManager.getApplication().getService(CjReferenceResolutionHelper::class.java)
    }
}
