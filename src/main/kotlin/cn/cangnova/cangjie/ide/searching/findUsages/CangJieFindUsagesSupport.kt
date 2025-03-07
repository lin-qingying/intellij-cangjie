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

package cn.cangnova.cangjie.ide.searching.findUsages

import cn.cangnova.cangjie.ide.searching.isCangJieConstructorUsage
import cn.cangnova.cangjie.psi.CjDeclaration
import cn.cangnova.cangjie.psi.CjTypeStatement
import cn.cangnova.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.util.Processor

interface CangJieFindUsagesSupport
{
    fun getSuperMethods(declaration: CjDeclaration, ignore: Collection<PsiElement>?) : List<PsiElement>
    fun tryRenderDeclarationCompactStyle(declaration: CjDeclaration): String?
    fun isCangJieConstructorUsage(psiReference: PsiReference,cjClassOrObject: CjTypeStatement): Boolean

    companion object{
        fun getInstance(project: Project): CangJieFindUsagesSupport = project.service()
        fun tryRenderDeclarationCompactStyle(declaration: CjDeclaration): String? =
            getInstance(declaration.project).tryRenderDeclarationCompactStyle(declaration)
        fun PsiReference.isConstructorUsage(cjClassOrObject: CjTypeStatement): Boolean {

            return   getInstance(cjClassOrObject.project).isCangJieConstructorUsage(this, cjClassOrObject)
        }
        fun getSuperMethods(declaration: CjDeclaration, ignore: Collection<PsiElement>?) : List<PsiElement> =
            getInstance(declaration.project).getSuperMethods(declaration, ignore)
    }
}
class CangJieFindUsagesSupportImpl : CangJieFindUsagesSupport {
    override fun getSuperMethods(declaration:CjDeclaration, ignore: Collection<PsiElement>?): List<PsiElement> =
        cn.cangnova.cangjie.ide .refactoring.getSuperMethods(declaration, ignore)
    override fun tryRenderDeclarationCompactStyle(declaration: CjDeclaration): String? =
        cn.cangnova.cangjie.ide.searching.  tryRenderDeclarationCompactStyle(declaration)

    override fun isCangJieConstructorUsage(psiReference: PsiReference, cjClassOrObject: CjTypeStatement): Boolean {
        return       psiReference.isCangJieConstructorUsage(cjClassOrObject)
    }
}
