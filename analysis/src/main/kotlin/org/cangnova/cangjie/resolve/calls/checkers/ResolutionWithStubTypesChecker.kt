/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.resolve.calls.checkers

import org.cangnova.cangjie.resolve.calls.CangJieCallResolver
import org.cangnova.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import org.cangnova.cangjie.resolve.calls.context.BasicCallResolutionContext
import org.cangnova.cangjie.resolve.calls.model.LambdaCangJieCallArgument
import org.cangnova.cangjie.resolve.calls.results.OverloadResolutionResults
import org.cangnova.cangjie.resolve.calls.tower.ImplicitScopeTower
import org.cangnova.cangjie.resolve.calls.tower.NewAbstractResolvedCall
import org.cangnova.cangjie.types.UnwrappedType

class ResolutionWithStubTypesChecker(private val cangjieCallResolver: CangJieCallResolver) :
    CallCheckerWithAdditionalResolve {
    override fun check(
        overloadResolutionResults: OverloadResolutionResults<*>,
        scopeTower: ImplicitScopeTower,
        resolutionCallbacks: CangJieResolutionCallbacks,
        expectedType: UnwrappedType?,
        context: BasicCallResolutionContext
    ) {
        // Don't check builder inference lambdas if the entire builder call itself has resolution ambiguity
        if (!overloadResolutionResults.isSingleResult()) return

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
