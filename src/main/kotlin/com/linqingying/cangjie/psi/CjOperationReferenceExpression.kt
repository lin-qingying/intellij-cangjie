package com.linqingying.cangjie.psi

import com.linqingying.cangjie.lexer.CjSingleValueToken
import com.linqingying.cangjie.parsing.CangJieExpressionParsing
import com.linqingying.cangjie.utils.exceptions.OperatorConventions

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.TreeElement

class CjOperationReferenceExpression(node: ASTNode) : CjSimpleNameExpressionImpl(node) {

    override fun getReferencedNameElement() = findChildByType<PsiElement?>(CangJieExpressionParsing.ALL_OPERATIONS) ?: this

    fun isConventionOperator(): Boolean {
        val tokenType = operationSignTokenType ?: return false
        return OperatorConventions.getNameForOperationSymbol(tokenType) != null
    }
    val operationSignTokenType: CjSingleValueToken?
        get() = (firstChild as? TreeElement)?.elementType as? CjSingleValueToken


}
