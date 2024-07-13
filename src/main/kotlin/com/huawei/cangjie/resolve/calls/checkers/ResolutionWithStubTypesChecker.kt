package com.huawei.cangjie.resolve.calls.checkers

import com.huawei.cangjie.resolve.calls.CangJieCallResolver
import com.huawei.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import com.huawei.cangjie.resolve.calls.context.BasicCallResolutionContext
import com.huawei.cangjie.resolve.calls.model.LambdaCangJieCallArgument
import com.huawei.cangjie.resolve.calls.results.OverloadResolutionResults
import com.huawei.cangjie.resolve.calls.tower.ImplicitScopeTower
import com.huawei.cangjie.resolve.calls.tower.NewAbstractResolvedCall
import com.huawei.cangjie.types.UnwrappedType

class ResolutionWithStubTypesChecker(private val cangjieCallResolver: CangJieCallResolver) : CallCheckerWithAdditionalResolve {
    override fun check(
        overloadResolutionResults: OverloadResolutionResults<*>,
        scopeTower: ImplicitScopeTower,
        resolutionCallbacks: CangJieResolutionCallbacks,
        expectedType: UnwrappedType?,
        context: BasicCallResolutionContext
    ) {
        // Don't check builder inference lambdas if the entire builder call itself has resolution ambiguity
        if (!overloadResolutionResults.isSingleResult) return

//        val builderResolvedCall = overloadResolutionResults.resultingCall as? NewAbstractResolvedCall<*> ?: return

//        val builderLambdas = (builderResolvedCall.psiCangJieCall.argumentsInParenthesis + builderResolvedCall.psiCangJieCall.externalArgument)
//            .filterIsInstance<LambdaCangJieCallArgument>()
//            .filter { it.hasBuilderInferenceAnnotation }

//        for (lambda in builderLambdas) {
//            val builderInferenceSession = lambda.builderInferenceSession as? BuilderInferenceSession ?: continue
//            val errorCalls = builderInferenceSession.errorCallsInfo
//            for (errorCall in errorCalls) {
//                val resolutionResult = errorCall.result
//                if (resolutionResult.isAmbiguity) {
//                    val firstResolvedCall = resolutionResult.resultingCalls.first() as? NewAbstractResolvedCall<*> ?: continue
//                    processResolutionAmbiguityError(context, firstResolvedCall, lambda, resolutionCallbacks, expectedType, scopeTower)
//                }
//            }
//        }
    }
}
