package com.linqingying.cangjie.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import com.linqingying.cangjie.parsing.CangJieExpressionParsing

class CjMacroInput(node: ASTNode) : CjExpressionImpl(node) {


    val declarations: CjDeclaration?
        get() {
            return PsiTreeUtil.getChildrenOfTypeAsList(this, CjDeclaration::class.java).firstOrNull()
        }
    val tokens: List<CjElement> get() = findChildrenByType(CangJieExpressionParsing.QUOTE_TOKENS)

}
class CjQuoteParameters(node: ASTNode) : CjExpressionImpl(node)
class CjMacroAttr(node: ASTNode) : CjExpressionImpl(node)
class CjQuoteTokens(node: ASTNode) : CjExpressionImpl(node) {

    val tokens: List<CjElement> get() = findChildrenByType(CangJieExpressionParsing.QUOTE_TOKENS)
}
