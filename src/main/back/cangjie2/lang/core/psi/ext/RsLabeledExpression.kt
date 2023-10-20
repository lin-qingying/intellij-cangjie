package com.huawei.cangjie.lang.core.psi.ext


import com.huawei.cangjie.lang.core.psi.CjBlock
import com.huawei.cangjie.lang.core.psi.CjLabelDecl

interface CjLabeledExpression : CjElement {
    val labelDecl: CjLabelDecl?
    val block: CjBlock?
}
