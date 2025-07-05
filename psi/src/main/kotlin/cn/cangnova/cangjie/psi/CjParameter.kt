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

package cn.cangnova.cangjie.psi

import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.psi.stubs.CangJieParameterStub
import cn.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
interface CjParameterBase : CjCallableDeclaration, CjLetVarKeywordOwner {
    fun hasLetOrVar(): Boolean
    fun hasDefaultValue(): Boolean
}

class CjParameter : CjNamedDeclarationStub<CangJieParameterStub>, CjParameterBase {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieParameterStub) : super(stub, CjStubElementTypes.VALUE_PARAMETER)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R? {
        return visitor.visitParameter(this, data)
    }
    val isVarArg: Boolean get() {

        return modifierList != null && modifierList!!.hasModifier(CjTokens.VARARG_KEYWORD)
    }
    val isLoopParameter get() = parent is CjForExpression
    override val typeReference: CjTypeReference?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE)

    override fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference? {
        return setTypeReference(this, nameIdentifier, typeRef)
    }

    val destructuringDeclaration: CjDestructuringDeclaration?
        get() {
            if (stub != null) return null

            return findChildByType(CjNodeTypes.DESTRUCTURING_DECLARATION)
        }

    /**
     * For example,
     * fun foo(lambdaArgument: (functionTypeParameter: T, ...) -> R) { ... }
     *
     * @return [true] if this [KtParameter] is a parameter of a function type.
     */
    fun isFunctionTypeParameter(): Boolean {
        return checkParentOfParentType(CjFunctionType::class.java)
    }

    override val colon: PsiElement?
        get() = findChildByType(CjTokens.COLON)
    val isNamed: Boolean
        get() {
//            if (colon != null) {
//                return colon!!.node.treePrev.elementType === CjTokens.EXCL
//            }

            return findChildByType<PsiElement>(CjTokens.EXCL) != null
        }

    val equalsToken: PsiElement?
        get() = findChildByType(CjTokens.EQ)

    override fun hasDefaultValue(): Boolean {
        val stub = stub
        if (stub != null) {
            return stub.hasDefaultValue()
        }
        return defaultValue != null
    }

    val isLambdaParameter: Boolean
        /**
         * For example,
         * lambdaConsumer { lambdaParameter ->
         * ...
         * }
         *
         * @return [true] if this [CjParameter] is a parameter of a lambda.
         */
        get() = checkParentOfParentType(CjFunctionLiteral::class.java)

    val defaultValue: CjExpression?
        get() {
            val stub = stub
            if (stub != null) {
                if (!stub.hasDefaultValue()) {
                    return null
                }

                if (containingCjFile.isCompiled) {
                    return null
                }
            }

            val equalsToken = equalsToken
            return if (equalsToken != null) {
                PsiTreeUtil.getNextSiblingOfType(
                    equalsToken,
                    CjExpression::class.java,
                )
            } else {
                null
            }
        }

    val isMutable: Boolean
        get() {
            val stub = stub
            if (stub != null) {
                return stub.isMutable()
            }

            return findChildByType<PsiElement?>(CjTokens.VAR_KEYWORD) != null
        }

    override fun hasLetOrVar(): Boolean {
        val stub = stub
        if (stub != null) {
            return stub.hasValOrVar()
        }
        return letOrVarKeyword != null
    }

    override val letOrVarKeyword: PsiElement?
        get() {
            val stub = stub
            if (stub != null && !stub.hasValOrVar()) {
                return null
            }
            return findChildByType(LET_VAR_TOKEN_SET)
        }

    override fun getPresentation(): ItemPresentation? {
        return ItemPresentationProviders.getItemPresentation(this)
    }

    private fun <T : PsiElement> checkParentOfParentType(klass: Class<T>): Boolean {
        val parent = parent ?: return false
        return klass.isInstance(parent.parent)
    }

    val isCatchParameter: Boolean
        get() = checkParentOfParentType(CjCatchClause::class.java)

    override val contextReceivers: List<CjContextReceiver> = emptyList()
    override val valueParameterList: CjParameterList? = null
    override val valueParameters: List<CjParameter> = emptyList()
    override val receiverTypeReference: CjTypeReference? = null
    override val typeParameterList: CjTypeParameterList? = null
    override val typeConstraintList: CjTypeConstraintList? = null
    override val typeConstraints: List<CjTypeConstraint> = emptyList()
    override val typeParameters: List<CjTypeParameter> = emptyList()

    val ownerFunction: CjDeclarationWithBody?
        get() {
            val parent = parentByStub as? CjParameterList ?: return null
            return parent.ownerFunction
        }

    override fun getUseScope(): SearchScope {
        var owner: CjExpression? = ownerFunction
        if (owner is CjPrimaryConstructor) {
            if (hasLetOrVar()) return super.getUseScope()
            owner = owner.getContainingTypeStatement()
        }
        if (owner == null) {
            owner = PsiTreeUtil.getParentOfType(this, CjExpression::class.java)
        }
        return LocalSearchScope(owner ?: this)
    }

    companion object {
        val LET_VAR_TOKEN_SET: TokenSet =
            TokenSet.create(CjTokens.LET_KEYWORD, CjTokens.CONST_KEYWORD, CjTokens.VAR_KEYWORD)
    }
}
