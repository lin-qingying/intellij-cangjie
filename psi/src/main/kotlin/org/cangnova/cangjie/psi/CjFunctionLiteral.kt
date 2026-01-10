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
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import org.cangnova.cangjie.name.*

class CjFunctionLiteral(node: ASTNode) : CjFunctionNotStubbed(node) {
    override fun hasBlockBody(): Boolean {
        return false
    }

    override fun getName(): String {
        return SpecialNames.ANONYMOUS_STRING
    }

    override fun getNameIdentifier(): PsiElement? {
        return null
    }

    fun hasParameterSpecification(): Boolean {
        return findChildByType<PsiElement?>(CjTokens.ARROW) != null
    }

    override val bodyExpression: CjBlockExpression?
        get() {
            return super.bodyExpression as CjBlockExpression?
        }

    override val equalsToken: PsiElement? = null
    val lBrace: PsiElement
        get() = findChildByType(CjTokens.LBRACE)!!

    @get:IfNotParsed
    val rBrace: PsiElement?
        get() = findChildByType(CjTokens.RBRACE)

    val arrow: PsiElement?
        get() = findChildByType(CjTokens.ARROW)

    override val fqName: FqName?
        get() = null

    override fun hasBody(): Boolean {
        return bodyExpression != null
    }

    override fun getUseScope(): SearchScope {
        return LocalSearchScope(this)
    }
}
