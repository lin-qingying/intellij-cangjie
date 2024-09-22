package com.huawei.cangjie.psi

import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.psiUtil.getTrailingCommaByClosingElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil

class CjDestructuringDeclaration(node: ASTNode) : CjDeclarationImpl(node), CjLetVarKeywordOwner,
    CjDeclarationWithInitializer {
    override val expression: CjExpression?
        get() = PsiTreeUtil.getStubChildOfType(
            this,
            CjExpression::class.java
        )

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitDestructuringDeclaration(this, data)
    }

    val entries: List<CjDestructuringDeclarationEntry>
        get() {
            return findChildrenByType(CjNodeTypes.DESTRUCTURING_DECLARATION_ENTRY)
        }

    override val initializer: CjExpression?
        get() {
            val eqNode: ASTNode? = node.findChildByType(CjTokens.EQ)
            if (eqNode == null) {
                return null
            }
            return PsiTreeUtil.getNextSiblingOfType(
                eqNode.psi,
                CjExpression::class.java
            )
        }

    override fun hasInitializer(): Boolean {
        return initializer != null
    }

    val isVar: Boolean
        get() {
            return node.findChildByType(CjTokens.VAR_KEYWORD) != null
        }

    override val letOrVarKeyword: PsiElement?
        get() {
            return findChildByType(VAL_VAR_KEYWORDS)
        }

    val rPar: PsiElement?
        get() {
            return findChildByType(CjTokens.RPAR)
        }

    val lPar: PsiElement?
        get() {
            return findChildByType(CjTokens.LPAR)
        }

    val trailingComma: PsiElement?
        get() {
            return getTrailingCommaByClosingElement(rPar)
        }

    companion object {
        private val VAL_VAR_KEYWORDS: TokenSet =
            TokenSet.create(CjTokens.LET_KEYWORD, CjTokens.VAR_KEYWORD, CjTokens.CONST_KEYWORD)
    }
}
