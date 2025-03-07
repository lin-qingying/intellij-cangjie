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

package cn.cangnova.cangjie.ide.search

import cn.cangnova.cangjie.psi.CjDeclaration
import cn.cangnova.cangjie.psi.CjElement
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.psi.CjTypeStatement
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
data class ReceiverTypeSearcherInfo(
    val psiClass: CjTypeStatement?,
    val containsTypeOrDerivedInside: ((CjDeclaration) -> Boolean)
)
interface CangJieSearchUsagesSupport {
    object SearchUtils {
        fun PsiElement.getReceiverTypeSearcherInfo( ): ReceiverTypeSearcherInfo? =
            getInstance(project).getReceiverTypeSearcherInfo(this )
        fun CjFile.forceResolveReferences(elements: List<CjElement>) =
            getInstance(project).forceResolveReferences(this, elements)
        fun findDeepestSuperMethodsNoWrapping(method: PsiElement): List<PsiElement> =
            getInstance(method.project).findSuperMethodsNoWrapping(method, true)

    }
    fun forceResolveReferences(file: CjFile, elements: List<CjElement>)
    fun findSuperMethodsNoWrapping(method: PsiElement, deepest: Boolean): List<PsiElement>

    /**
     *
     * Extract the PSI class for the receiver type of [psiElement] assuming it is an _operator_.
     * Additionally compute an occurence check for uses of the type in another, used to
     * conservatively discard search candidates in which the type does not occur at all.
     *
     * TODO: rename to something more apt? The FE1.0 implementation requires that the target
     *       be an operator.
     */
    fun getReceiverTypeSearcherInfo(psiElement: PsiElement ): ReceiverTypeSearcherInfo?

    companion object {
        fun getInstance(project: Project): CangJieSearchUsagesSupport = project.service()
    }
}
