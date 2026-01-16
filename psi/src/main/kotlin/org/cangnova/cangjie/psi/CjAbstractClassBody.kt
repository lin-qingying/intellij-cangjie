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

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.stubs.CangJieEnumStub
import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes.*
import org.cangnova.cangjie.psi.stubs.elements.CjTokenSets

class CjInterfaceBody : CjAbstractClassBody {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjInterfaceBody>) : super(stub, INTERFACE_BODY)
}

class CjEnumBody : CjAbstractClassBody {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjEnumBody>) : super(stub, ENUM_BODY)

    val constructor get() = getStubOrPsiChildrenAsList(ENUM_CONSTRUCTOR)
    /**
     * 是否非穷枚举
     */
    val isNonExhaustive: Boolean
        get() {

            return findChildByType<PsiElement>(CjTokens.ELLIPSIS) != null
        }
}

class CjClassBody : CjAbstractClassBody {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjEnumBody>) : super(stub, CLASS_BODY)
}

abstract class CjAbstractClassBody :
    CjElementImplStub<CangJiePlaceHolderStub<out CjAbstractClassBody>>,
    CjDeclarationContainer {
    private val lBraceTokenSet = TokenSet.create(CjTokens.LBRACE)
    private val rBraceTokenSet = TokenSet.create(CjTokens.RBRACE)

    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<out CjAbstractClassBody>, nodeType: IStubElementType<*, *>) : super(
        stub,
        nodeType,
    )

    constructor(stub: CangJiePlaceHolderStub<CjAbstractClassBody>) : super(stub, CLASS_BODY)

    override fun getParent() = parentByStub
    internal val secondaryConstructors: List<CjSecondaryConstructor>
        get() = getStubOrPsiChildrenAsList(SECONDARY_CONSTRUCTOR)
    internal val primaryConstructors: List<CjPrimaryConstructor>
        get() = getStubOrPsiChildrenAsList(PRIMARY_CONSTRUCTOR)
    internal val endSecondaryConstructors: List<CjEndSecondaryConstructor>
        get() = getStubOrPsiChildrenAsList(END_SECONDARY_CONSTRUCTOR)

    /**
     * @return annotations that do not belong to any declaration due to incomplete code or syntax errors
     */
//    val danglingAnnotations: List<CjAnnotation>
//        get() = danglingModifierLists.flatMap { it.annotationEntries }

    override fun toString(): String {
        return node.elementType.toString()
    }

    val properties: List<CjProperty>
        get() = getStubOrPsiChildrenAsList(PROPERTY)
    val variables: List<CjFieldVariable>
        get() = getStubOrPsiChildrenAsList(FIELD)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? = visitor.visitClassBody(this, data)
    override val declarations: List<CjDeclaration>
        get() = stub?.getChildrenByType(CjTokenSets.CLASS_MEMBER_DECLARATION_TYPES, CjDeclaration.ARRAY_FACTORY)?.toList()
            ?: PsiTreeUtil.getChildrenOfTypeAsList(this, CjDeclaration::class.java)

    val rBrace: PsiElement?
        get() = node.getChildren(rBraceTokenSet).singleOrNull()?.psi

    val lBrace: PsiElement?
        get() = node.getChildren(lBraceTokenSet).singleOrNull()?.psi
}
