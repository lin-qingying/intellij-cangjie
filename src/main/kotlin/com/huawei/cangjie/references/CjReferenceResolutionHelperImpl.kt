package com.huawei.cangjie.references

import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import com.huawei.cangjie.resolve.lazy.BodyResolveMode

class CjReferenceResolutionHelperImpl:CjReferenceResolutionHelper {
    override fun partialAnalyze(element: CjElement): BindingContext = element.safeAnalyzeNonSourceRootCode(
        BodyResolveMode.PARTIAL)



}