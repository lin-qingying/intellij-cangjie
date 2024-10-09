package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.ClassKind
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.psi.CjNameReferenceExpression
import com.huawei.cangjie.resolve.caches.getResolutionFacade
import com.huawei.cangjie.resolve.getReferenceTargets
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.intellij.openapi.components.Service


class CangJieCompletionDummyIdentifierProviderService: AbstractCompletionDummyIdentifierProviderService()  {
    override fun allTargetsAreFunctionsOrClasses(nameReferenceExpression: CjNameReferenceExpression): Boolean {
        val bindingContext = nameReferenceExpression.getResolutionFacade().analyze(nameReferenceExpression, BodyResolveMode.PARTIAL)
        val targets = nameReferenceExpression.getReferenceTargets(bindingContext)
        return targets.isNotEmpty() && targets.all { it is FunctionDescriptor || it is ClassDescriptor && it.kind == ClassKind.CLASS }
    }
}
