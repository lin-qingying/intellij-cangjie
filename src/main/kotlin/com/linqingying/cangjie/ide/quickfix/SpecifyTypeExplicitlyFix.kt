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

package com.linqingying.cangjie.ide.quickfix

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.ide.intentions.SpecifyTypeExplicitlyIntention
import com.linqingying.cangjie.psi.CjCallableDeclaration
import com.linqingying.cangjie.psi.CjNamedFunction
import com.linqingying.cangjie.psi.CjProperty
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.CangJieTypeFactory
import com.linqingying.cangjie.types.asSimpleType
import com.linqingying.cangjie.types.isError

class SpecifyTypeExplicitlyFix(private val convertToNullable: Boolean = false) : PsiElementBaseIntentionAction() {
    override fun getFamilyName() = CangJieBundle.message("specify.type.explicitly")

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val declaration = declarationByElement(element)!!
        val type = SpecifyTypeExplicitlyIntention.getTypeForDeclaration(declaration)
            .let { if (convertToNullable) it.convertToNullable() else it }
        SpecifyTypeExplicitlyIntention.addTypeAnnotation(editor, declaration, type)
    }

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        val declaration = declarationByElement(element)
        if (declaration?.typeReference != null) return false
        text = when (declaration) {
            is CjProperty -> CangJieBundle.message("specify.type.explicitly")
            is CjNamedFunction -> CangJieBundle.message("specify.return.type.explicitly")
            else -> return false
        }

        return !SpecifyTypeExplicitlyIntention.getTypeForDeclaration(declaration).isError
    }

    private fun declarationByElement(element: PsiElement): CjCallableDeclaration? {
        return PsiTreeUtil.getParentOfType(element, CjProperty::class.java, CjNamedFunction::class.java)
    }
}

fun CangJieType.convertToNullable(): CangJieType = CangJieTypeFactory.simpleType(asSimpleType(), nullable = true)
