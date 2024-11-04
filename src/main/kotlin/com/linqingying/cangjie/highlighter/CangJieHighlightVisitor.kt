package com.linqingying.cangjie.highlighter

import com.linqingying.cangjie.psi.CjFunction
import com.linqingying.cangjie.psi.CjMainFunction
import com.linqingying.cangjie.psi.CjParameter
import com.linqingying.cangjie.psi.CjParameterList
import com.linqingying.cangjie.psi.psiUtil.parents
import com.linqingying.cangjie.utils.match
import com.intellij.codeInsight.daemon.impl.HighlightVisitor

class CangJieHighlightVisitor : AbstractCangJieHighlightVisitor() {

    override fun shouldSuppressUnusedParameter(parameter: CjParameter): Boolean {
        val grandParent = parameter.parents.match(CjParameterList::class, last = CjFunction::class) ?: return false
//        if (!UnusedSymbolInspection.isEntryPoint(grandParent)) return false
        return  grandParent !is CjMainFunction
    }

    override fun clone(): HighlightVisitor = CangJieHighlightVisitor()


}
