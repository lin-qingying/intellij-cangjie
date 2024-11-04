package com.linqingying.cangjie.references

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjReferenceExpression
import com.linqingying.cangjie.psi.CjSimpleNameExpression
import com.linqingying.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
import com.linqingying.cangjie.utils.firstIsInstanceOrNull
import com.linqingying.cangjie.utils.firstIsInstance

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
