package com.huawei.cangjie.psi

import com.huawei.cangjie.utils.OperatorNameConventions.asOperatorName
import com.intellij.lang.ASTNode

class CjOperationName(node: ASTNode) : CjElementImpl(node) {


    override fun getName(): String {

        return text.asOperatorName().asString()


    }
}
