package com.linqingying.cangjie.psi

import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.SpecialNames
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope

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
