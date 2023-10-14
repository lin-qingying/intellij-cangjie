package com.huawei.cangjie.lang.doc.psi

import com.huawei.cangjie.lang.core.psi.ext.CjElement
import com.intellij.psi.PsiDocCommentBase

interface CjDocComment : PsiDocCommentBase, CjElement {
//    override fun getOwner(): CjDocAndAttributeOwner?
//
//    val codeFences: List<CjDocCodeFence>
//
//    val linkDefinitions: List<CjDocLinkDefinition>
//
//    val linkReferenceMap: Map<String, CjDocLinkDefinition>
}
