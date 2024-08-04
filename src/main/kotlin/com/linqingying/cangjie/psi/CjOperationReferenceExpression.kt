package com.linqingying.cangjie.psi

import com.linqingying.cangjie.lexer.CjSingleValueToken
import com.linqingying.cangjie.parsing.CangJieExpressionParsing

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.TreeElement

class CjOperationReferenceExpression(node: ASTNode) : CjSimpleNameExpressionImpl(node) {

    override fun getReferencedNameElement() = findChildByType<PsiElement?>(CangJieExpressionParsing.ALL_OPERATIONS) ?: this


    val operationSignTokenType: CjSingleValueToken?
        get() = (firstChild as? TreeElement)?.elementType as? CjSingleValueToken


}
