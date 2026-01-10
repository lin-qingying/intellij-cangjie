/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.psiUtil.firstIsInstanceOrNull
import org.cangnova.cangjie.psi.psiUtil.siblings
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace

fun setTypeReference(declaration: CjCallableDeclaration, addAfter: PsiElement?, typeRef: CjTypeReference?): CjTypeReference? {
    val oldTypeRef = getTypeReference(declaration)
    if (typeRef != null) {
        return if (oldTypeRef != null) {
            oldTypeRef.replace(typeRef) as CjTypeReference
        } else {
            val anchor = addAfter
                ?: declaration.nameIdentifier?.siblings(forward = true)?.firstOrNull { it is PsiErrorElement }
            val newTypeRef = declaration.addAfter(typeRef, anchor) as CjTypeReference
            declaration.addAfter(CjPsiFactory(declaration.project).createColon(), anchor)
            newTypeRef
        }
    } else {
        if (oldTypeRef != null) {
            val colon = declaration.colon!!
            val removeFrom = colon.prevSibling as? PsiWhiteSpace ?: colon
            declaration.deleteChildRange(removeFrom, oldTypeRef)
        }
        return null
    }
}
fun getTypeReference(declaration: CjCallableDeclaration): CjTypeReference? {
    return declaration.firstChild!!.siblings(forward = true)
        .dropWhile { it.node!!.elementType != CjTokens.COLON }
        .firstIsInstanceOrNull<CjTypeReference>()
}
