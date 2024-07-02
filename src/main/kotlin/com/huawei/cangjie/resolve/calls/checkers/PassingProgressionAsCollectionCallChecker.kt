package com.huawei.cangjie.resolve.calls.checkers

import com.huawei.cangjie.resolve.calls.CangJieCallResolver
import com.huawei.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import com.huawei.cangjie.resolve.calls.context.BasicCallResolutionContext
import com.huawei.cangjie.resolve.calls.results.OverloadResolutionResults
import com.huawei.cangjie.resolve.calls.tower.ImplicitScopeTower
import com.huawei.cangjie.types.UnwrappedType

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
