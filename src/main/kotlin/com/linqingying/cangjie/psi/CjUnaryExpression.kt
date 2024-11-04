package com.linqingying.cangjie.psi

import com.linqingying.cangjie.CjNodeTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import java.util.*

abstract class CjUnaryExpression(node: ASTNode) : CjExpressionImpl(node), CjOperationExpression {

    @get:IfNotParsed
    open val baseExpression: CjExpression? get() = PsiTreeUtil.getPrevSiblingOfType(operationReference, CjExpression::class.java)



    override val operationReference: CjSimpleNameExpression
        get() = findChildByType(CjNodeTypes.OPERATION_REFERENCE)!!
    val operationToken: IElementType
        get() = operationReference.getReferencedNameElementType()
}
