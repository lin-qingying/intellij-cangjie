package com.huawei.cangjie.highlighter

import com.huawei.cangjie.psi.CjFunction
import com.huawei.cangjie.psi.CjMainFunction
import com.huawei.cangjie.psi.CjParameter
import com.huawei.cangjie.psi.CjParameterList
import com.huawei.cangjie.psi.psiUtil.parents
import com.huawei.cangjie.utils.match
import com.intellij.codeInsight.daemon.impl.HighlightVisitor

class CangJieHighlightVisitor : AbstractCangJieHighlightVisitor() {

    override fun shouldSuppressUnusedParameter(parameter: CjParameter): Boolean {
        val grandParent = parameter.parents.match(CjParameterList::class, last = CjFunction::class) ?: return false
//        if (!UnusedSymbolInspection.isEntryPoint(grandParent)) return false
        return  grandParent !is CjMainFunction
    }

    override fun clone(): HighlightVisitor = CangJieHighlightVisitor()


}
