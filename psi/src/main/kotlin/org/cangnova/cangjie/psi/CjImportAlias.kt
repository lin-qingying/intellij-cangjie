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
import org.cangnova.cangjie.psi.psiUtil.startOffset
import org.cangnova.cangjie.psi.stubs.CangJieImportAliasStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.search.LocalSearchScope

class CjImportAlias : CjElementImplStub<CangJieImportAliasStub>, PsiNameIdentifierOwner {
    @Suppress("unused")
    constructor(node: ASTNode) : super(node)

    @Suppress("unused")
    constructor(stub: CangJieImportAliasStub) : super(stub, CjStubElementTypes.IMPORT_ALIAS)

    override fun <R : Any?, D : Any?> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitImportAlias(this, data)
    }

    val importDirective: CjImportInfo?
        get() = parent as? CjImportInfo

    override fun getName() = stub?.getName() ?: nameIdentifier?.text

    override fun setName(name: String): PsiElement {
        nameIdentifier?.replace(CjPsiFactory(project).createNameIdentifier(name))
        return this
    }

    override fun getNameIdentifier(): PsiElement? = findChildByType(CjTokens.IDENTIFIER)

    override fun getTextOffset() = nameIdentifier?.textOffset ?: startOffset

    override fun getUseScope() = LocalSearchScope(containingFile)
}
