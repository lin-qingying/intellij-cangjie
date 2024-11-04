package com.linqingying.cangjie.ide.completion

import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.ClassKind
import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.psi.CjNameReferenceExpression
import com.linqingying.cangjie.resolve.caches.getResolutionFacade
import com.linqingying.cangjie.resolve.getReferenceTargets
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
import com.intellij.openapi.components.Service


class CangJieCompletionDummyIdentifierProviderService: AbstractCompletionDummyIdentifierProviderService()  {
    override fun allTargetsAreFunctionsOrClasses(nameReferenceExpression: CjNameReferenceExpression): Boolean {
        val bindingContext = nameReferenceExpression.getResolutionFacade().analyze(nameReferenceExpression, BodyResolveMode.PARTIAL)
        val targets = nameReferenceExpression.getReferenceTargets(bindingContext)
        return targets.isNotEmpty() && targets.all { it is FunctionDescriptor || it is ClassDescriptor && it.kind == ClassKind.CLASS }
    }
}
