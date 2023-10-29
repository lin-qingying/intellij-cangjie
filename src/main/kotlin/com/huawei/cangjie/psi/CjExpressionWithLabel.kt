package com.huawei.cangjie.psi

import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.name.Name
import com.intellij.lang.ASTNode


open class CjExpressionWithLabel(node: ASTNode) : CjExpressionImpl(node) {

    fun getTargetLabel(): CjSimpleNameExpression? =
        labelQualifier?.findChildByType(CjNodeTypes.LABEL) as? CjSimpleNameExpression

    val labelQualifier: CjContainerNode?
        get() = findChildByType(CjNodeTypes.LABEL_QUALIFIER)

    fun getLabelName(): String? = getTargetLabel()?.getReferencedName()
    fun getLabelNameAsName(): Name? = getTargetLabel()?.getReferencedNameAsName()

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?) = visitor.visitExpressionWithLabel(this, data)
}
