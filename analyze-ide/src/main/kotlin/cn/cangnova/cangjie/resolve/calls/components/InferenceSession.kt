/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.resolve.calls.components

import cn.cangnova.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import cn.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import cn.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import cn.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import cn.cangnova.cangjie.resolve.calls.model.*
import cn.cangnova.cangjie.types.TypeConstructor
import cn.cangnova.cangjie.types.UnwrappedType

interface PartialCallInfo {
    val callResolutionResult: PartialCallResolutionResult
}
//interface CompletedCallInfo {
//    val callResolutionResult: CompletedCallResolutionResult
//}

interface ErrorCallInfo {
    val callResolutionResult: CallResolutionResult
}

interface CompletedCallInfo {
    val callResolutionResult: CompletedCallResolutionResult
}

interface InferenceSession {
    val parentSession: InferenceSession?
    fun shouldRunCompletion(candidate: ResolutionCandidate): Boolean
    fun addErrorCallInfo(callInfo: ErrorCallInfo)
    fun inferPostponedVariables(
        lambda: ResolvedLambdaAtom,
        constraintSystemBuilder: ConstraintSystemBuilder,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticsHolder: CangJieDiagnosticsHolder
    ): Map<TypeConstructor, UnwrappedType>?

    fun addPartialCallInfo(callInfo: PartialCallInfo)
    fun callCompleted(resolvedAtom: ResolvedAtom): Boolean
    fun writeOnlyStubs(callInfo: SingleCallResolutionResult): Boolean
    fun shouldCompleteResolvedSubAtomsOf(resolvedCallAtom: ResolvedCallAtom): Boolean
    fun addCompletedCallInfo(callInfo: CompletedCallInfo)
    fun computeCompletionMode(candidate: ResolutionCandidate): ConstraintSystemCompletionMode?
    fun resolveReceiverIndependently(): Boolean
    fun currentConstraintSystem(): ConstraintStorage
    fun initializeLambda(lambda: ResolvedLambdaAtom)

    companion object {

        val default = object : InferenceSession {
            override val parentSession: InferenceSession? = null
            override fun addPartialCallInfo(callInfo: PartialCallInfo) {}
            override fun callCompleted(resolvedAtom: ResolvedAtom): Boolean = false
            override fun writeOnlyStubs(callInfo: SingleCallResolutionResult): Boolean = false
            override fun shouldRunCompletion(candidate: ResolutionCandidate): Boolean = true
            override fun addErrorCallInfo(callInfo: ErrorCallInfo) {

            }

            override fun inferPostponedVariables(
                lambda: ResolvedLambdaAtom,
                constraintSystemBuilder: ConstraintSystemBuilder,
                completionMode: ConstraintSystemCompletionMode,
                diagnosticsHolder: CangJieDiagnosticsHolder
            ): Map<TypeConstructor, UnwrappedType> = emptyMap()

            override fun resolveReceiverIndependently(): Boolean = false
            override fun currentConstraintSystem(): ConstraintStorage = ConstraintStorage.Empty
            override fun initializeLambda(lambda: ResolvedLambdaAtom) {

            }


            override fun shouldCompleteResolvedSubAtomsOf(resolvedCallAtom: ResolvedCallAtom) = true
            override fun addCompletedCallInfo(callInfo: CompletedCallInfo) {}
            override fun computeCompletionMode(
                candidate: ResolutionCandidate
            ): ConstraintSystemCompletionMode? = null


        }
    }
}
