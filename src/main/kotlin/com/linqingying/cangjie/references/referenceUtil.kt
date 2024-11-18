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

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.ide.stubindex.CangJieFullClassNameIndex
import com.linqingying.cangjie.ide.stubindex.CangJieTopLevelTypeAliasFqNameIndex
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
import com.linqingying.cangjie.utils.firstIsInstanceOrNull
import com.linqingying.cangjie.utils.firstIsInstance

fun CjElement.resolveMainReferenceToDescriptors(): Collection<DeclarationDescriptor> {
    val bindingContext = safeAnalyzeNonSourceRootCode(BodyResolveMode.PARTIAL)
    return mainReference?.resolveToDescriptors(bindingContext) ?: emptyList()
}

val CjElement.mainReference: CjReference?
    get() = when (this) {
        is CjReferenceExpression -> mainReference
//        is CDocName -> mainReference
        else -> references.firstIsInstanceOrNull()
    }
//val CDocName.mainReference: CDocReference
//    get() = references.firstIsInstance()

val CjReferenceExpression.mainReference: CjReference
    get() = if (this is CjSimpleNameExpression) mainReference else references.firstIsInstance()
val CjSimpleNameExpression.mainReference: CjSimpleNameReference
    get() = references.firstIsInstance()
internal fun Project.resolveClass(fqNameString: String, scope: GlobalSearchScope = GlobalSearchScope.allScope(this)): PsiElement? =
    resolveFqNameOfCjClassByIndex(fqNameString, scope)
private fun Project.resolveFqNameOfCjClassByIndex(fqNameString: String, scope: GlobalSearchScope): CjDeclaration? {
    val classesPsi = CangJieFullClassNameIndex.get(fqNameString, this, scope)
    val typeAliasesPsi = CangJieTopLevelTypeAliasFqNameIndex.get(fqNameString, this, scope)

    return scope.selectNearest(classesPsi, typeAliasesPsi)
}
private fun GlobalSearchScope.selectNearest(classesPsi: Collection<CjDeclaration>, typeAliasesPsi: Collection<CjTypeAlias>): CjDeclaration? {
    val scope = this
    return when {
        typeAliasesPsi.isEmpty() -> classesPsi.firstOrNull()
        classesPsi.isEmpty() -> typeAliasesPsi.firstOrNull()
        else -> (classesPsi.asSequence() + typeAliasesPsi.asSequence()).minWithOrNull(Comparator { o1, o2 ->
            scope.compare(o1.containingFile.virtualFile, o2.containingFile.virtualFile)
        })
    }
}
