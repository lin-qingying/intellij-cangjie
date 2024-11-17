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

package com.linqingying.cangjie.psi

import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.psiUtil.getParentOfType
import com.linqingying.cangjie.psi.psiUtil.sure
import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement


interface CjAnonymousInitializer : CjDeclaration, CjStatementExpression {
    val containingDeclaration: CjDeclaration
    val body: CjExpression?
}

class CjClassInitializer : CjDeclarationStub<CangJiePlaceHolderStub<CjClassInitializer>>, CjAnonymousInitializer {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjClassInitializer>) : super(stub, CjStubElementTypes.CLASS_INITIALIZER)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?) = visitor.visitClassInitializer(this, data)
    override val body: CjExpression?
        get() = findChildByClass(CjExpression::class.java)


    val openBraceNode: PsiElement?
        get() = (body as? CjBlockExpression)?.lBrace

    val initKeyword: PsiElement
        get() = findChildByType(CjTokens.INIT_KEYWORD)!!

    override val containingDeclaration: CjClass
        get() = getParentOfType<CjClass>(true).sure { "Should only be present in class or object" }

}

