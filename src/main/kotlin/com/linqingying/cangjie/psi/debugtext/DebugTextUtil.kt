package com.linqingying.cangjie.psi.debugtext

import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjElementImplStub
import com.linqingying.cangjie.psi.CjPackageDirective
import com.linqingying.cangjie.psi.CjVisitor

// invoke this instead of getText() when you need debug text to identify some place in PSI without storing the element itself
// this is need to avoid unnecessary file parses
// this defaults to get text if the element is not stubbed
fun CjElement.getDebugText(): String {
    if (this !is CjElementImplStub<*> || this.stub == null) {
        return text
    }
    if (this is CjPackageDirective) {
        val fqName = fqName
        if (fqName.isRoot) {
            return ""
        }
        return "package " + fqName.asString()
    }
    return accept(DebugTextBuildingVisitor, Unit)
}

private object DebugTextBuildingVisitor : CjVisitor<String, Unit>()
