package com.huawei.cangjie.resolve.calls.checkers

import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.resolve.calls.context.ResolutionContext
import com.huawei.cangjie.resolve.calls.model.CangJieCallComponents
import com.huawei.cangjie.resolve.calls.model.ResolvedCall
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.deprecation.DeprecationResolver
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.intellij.psi.PsiElement


interface CheckerContext {
    val trace: BindingTrace

    val deprecationResolver: DeprecationResolver

    val moduleDescriptor: ModuleDescriptor
}


interface CallChecker {
    /**
     * Note that [reportOn] should only be used as a target element for diagnostics reported by checkers.
     * Logic of the checker should not depend on what element is the target of the diagnostic!
     */
    fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext)
}

class CallCheckerContext @JvmOverloads constructor(
    val resolutionContext: ResolutionContext<*>,
    override val deprecationResolver: DeprecationResolver,
    override val moduleDescriptor: ModuleDescriptor,
//    val missingSupertypesResolver: MissingSupertypesResolver,
//    val callComponents: CangJieCallComponents,
    override val trace: BindingTrace = resolutionContext.trace
) : CheckerContext {
    val scope: LexicalScope
        get() = resolutionContext.scope

    val dataFlowInfo: DataFlowInfo
        get() = resolutionContext.dataFlowInfo

    val isAnnotationContext: Boolean
        get() = resolutionContext.isAnnotationContext

//    val dataFlowValueFactory: DataFlowValueFactory
//        get() = resolutionContext.dataFlowValueFactory


}

// Use this utility to avoid premature computation of deferred return type of a resolved callable descriptor.
// Computing it in CallChecker#check is not feasible since it would trigger "type checking has run into a recursive problem" errors.
// Receiver parameter is present to emphasize that this function should ideally be only used from call checkers.
//@Suppress("unused")
//fun CallChecker.isComputingDeferredType(type: CangJieType) =
//    type is DeferredType && type.isComputing
