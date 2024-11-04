package com.linqingying.cangjie.resolve.calls.checkers

import com.linqingying.cangjie.resolve.calls.CangJieCallResolver
import com.linqingying.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import com.linqingying.cangjie.resolve.calls.context.BasicCallResolutionContext
import com.linqingying.cangjie.resolve.calls.results.OverloadResolutionResults
import com.linqingying.cangjie.resolve.calls.tower.ImplicitScopeTower
import com.linqingying.cangjie.types.UnwrappedType

class PassingProgressionAsCollectionCallChecker(private val cangjieCallResolver: CangJieCallResolver) : CallCheckerWithAdditionalResolve {
    override fun check(
        overloadResolutionResults: OverloadResolutionResults<*>,
        scopeTower: ImplicitScopeTower,
        resolutionCallbacks: CangJieResolutionCallbacks,
        expectedType: UnwrappedType?,
        context: BasicCallResolutionContext
    ) {

    }

}
