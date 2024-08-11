package com.huawei.cangjie.ide.references

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.doc.psi.impl.CDocName
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjReferenceExpression
import com.huawei.cangjie.psi.CjSimpleNameExpression
import com.huawei.cangjie.references.CjReference
import com.huawei.cangjie.references.CjSimpleNameReference
import com.huawei.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.huawei.cangjie.utils.firstIsInstanceOrNull
import com.huawei.cangjie.utils.firstIsInstance

fun CjElement.resolveMainReferenceToDescriptors(): Collection<DeclarationDescriptor> {
    val bindingContext = safeAnalyzeNonSourceRootCode(BodyResolveMode.PARTIAL)
    return mainReference?.resolveToDescriptors(bindingContext) ?: emptyList()
}

val CjElement.mainReference: CjReference?
    get() = when (this) {
        is CjReferenceExpression -> mainReference
//        is CDocName -> mainReference
        else -> references.firstIsInstanceOrNull()
    }
//val CDocName.mainReference: CDocReference
//    get() = references.firstIsInstance()

val CjReferenceExpression.mainReference: CjReference
    get() = if (this is CjSimpleNameExpression) mainReference else references.firstIsInstance()
val CjSimpleNameExpression.mainReference: CjSimpleNameReference
    get() = references.firstIsInstance()
