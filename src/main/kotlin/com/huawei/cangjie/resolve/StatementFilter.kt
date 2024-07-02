package com.huawei.cangjie.resolve

import com.huawei.cangjie.psi.CjBlockExpression
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.CjPsiUtil


open class StatementFilter {
    open val filter: ((CjExpression) -> Boolean)?
        get() = null

    companion object {
        @JvmField
        val NONE = object : StatementFilter() {
            override fun toString() = "NONE"
        }
    }
}

fun StatementFilter.filterStatements(block: CjBlockExpression): List<CjExpression> {
    if (filter == null || block is CjPsiUtil.CjExpressionWrapper) return block.statements
    return block.statements.filter { filter!!(it) }
}

fun StatementFilter.getLastStatementInABlock(block: CjBlockExpression) = filterStatements(block).lastOrNull()