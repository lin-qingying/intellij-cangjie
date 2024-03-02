//package com.huawei.cangjie.references
//
//import com.huawei.cangjie.name.Name
//import com.huawei.cangjie.psi.CjSimpleNameExpression
//import com.huawei.cangjie.psi.psiUtil.startOffset
//import com.intellij.openapi.util.TextRange
//
//
//class CjSimpleNameReference(expression: CjSimpleNameExpression) : CjSimpleReference<CjSimpleNameExpression>(expression),
//    CjReference {
//
//    override val resolvesByNames: Collection<Name>
//        get() {
//
//            val element = element
//
//
////            TODO
//
//            return listOf(element.getReferencedNameAsName())
//
//        }
//
//
//    override fun getRangeInElement(): TextRange {
//        val element = element.getReferencedNameElement()
//        val startOffset = getElement().startOffset
//        return element.textRange.shiftRight(-startOffset)
//    }
//}
//
//
