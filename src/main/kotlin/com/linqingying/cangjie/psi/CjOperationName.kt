package com.linqingying.cangjie.psi

import com.linqingying.cangjie.utils.OperatorNameConventions.asOperatorName
import com.intellij.lang.ASTNode

class CjOperationName(node: ASTNode) : CjElementImpl(node) {


    override fun getName(): String {

        return text.asOperatorName().asString()


    }
}
