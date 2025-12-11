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

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import org.cangnova.cangjie.resolve.calls.inference.model.*
import org.cangnova.cangjie.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import org.cangnova.cangjie.resolve.calls.model.PostponedResolvedAtomMarker

import org.cangnova.cangjie.types.model.CangJieTypeMarker
import org.cangnova.cangjie.types.model.TypeConstructorMarker
import org.cangnova.cangjie.types.model.TypeVariableMarker


abstract class ConstraintSystemCompletionContext : VariableFixationFinder.Context, ResultTypeResolver.Context {
    abstract override val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>
    abstract override val fixedTypeVariables: Map<TypeConstructorMarker, CangJieTypeMarker>
    abstract override val postponedTypeVariables: List<TypeVariableMarker>

    abstract fun getBuilder(): ConstraintSystemBuilder

    // type can be proper if it not contains not fixed type variables
    abstract fun canBeProper(type: CangJieTypeMarker): Boolean

    abstract fun containsOnlyFixedOrPostponedVariables(type: CangJieTypeMarker): Boolean
    abstract fun containsOnlyFixedVariables(type: CangJieTypeMarker): Boolean

    // mutable operations
    abstract fun addError(error: ConstraintSystemError)

    abstract fun fixVariable(
        variable: TypeVariableMarker,
        resultType: CangJieTypeMarker,
        position: FixVariableConstraintPosition<*>,
        resultTypeForOnlyInputTypes: CangJieTypeMarker = resultType
    )

    abstract fun couldBeResolvedWithUnrestrictedBuilderInference(): Boolean
    abstract fun resolveForkPointsConstraints()

    fun <A : PostponedResolvedAtomMarker> analyzeArgumentWithFixedParameterTypes(
//        languageVersionSettings: LanguageVersionSettings,
        postponedArguments: List<A>,
        analyze: (A) -> Unit
    ): Boolean {
//        val useBuilderInferenceOnlyIfNeeded =
//            languageVersionSettings.supportsFeature(LanguageFeature.UseBuilderInferenceOnlyIfNeeded)
        val argumentToAnalyze =
            //        if (useBuilderInferenceOnlyIfNeeded) {
            findPostponedArgumentWithFixedInputTypes(postponedArguments)
//        } else {
//            findPostponedArgumentWithFixedOrPostponedInputTypes(postponedArguments)
//        }

        if (argumentToAnalyze != null) {
            analyze(argumentToAnalyze)
            return true
        }

        return false
    }

    fun <A : PostponedResolvedAtomMarker> analyzeNextReadyPostponedArgument(
//        languageVersionSettings: LanguageVersionSettings,
        postponedArguments: List<A>,
        completionMode: ConstraintSystemCompletionMode,
        analyze: (A) -> Unit
    ): Boolean {
        if (completionMode.allLambdasShouldBeAnalyzed) {
            val argumentWithTypeVariableAsExpectedType =
                findPostponedArgumentWithRevisableExpectedType(postponedArguments)

            if (argumentWithTypeVariableAsExpectedType != null) {
                analyze(argumentWithTypeVariableAsExpectedType)
                return true
            }
        }

        return analyzeArgumentWithFixedParameterTypes(/*languageVersionSettings, */postponedArguments, analyze)
    }

    fun <A : PostponedResolvedAtomMarker> analyzeRemainingNotAnalyzedPostponedArgument(
        postponedArguments: List<A>,
        analyze: (A) -> Unit
    ): Boolean {
        val remainingNotAnalyzedPostponedArgument = postponedArguments.firstOrNull { !it.analyzed }

        if (remainingNotAnalyzedPostponedArgument != null) {
            analyze(remainingNotAnalyzedPostponedArgument)
            return true
        }

        return false
    }

    fun <A : PostponedResolvedAtomMarker> hasLambdaToAnalyze(
//        languageVersionSettings: LanguageVersionSettings,
        postponedArguments: List<A>
    ): Boolean {
        return analyzeArgumentWithFixedParameterTypes(/*languageVersionSettings,*/ postponedArguments) {}
    }

    // Avoiding smart cast from filterIsInstanceOrNull looks dirty
    private fun <A : PostponedResolvedAtomMarker> findPostponedArgumentWithRevisableExpectedType(postponedArguments: List<A>): A? =
        postponedArguments.firstOrNull { argument -> argument is PostponedAtomWithRevisableExpectedType }

    private fun <T : PostponedResolvedAtomMarker> findPostponedArgumentWithFixedOrPostponedInputTypes(
        postponedArguments: List<T>
    ) =
        postponedArguments.firstOrNull { argument -> argument.inputTypes.all { containsOnlyFixedOrPostponedVariables(it) } }

    private fun <T : PostponedResolvedAtomMarker> findPostponedArgumentWithFixedInputTypes(
        postponedArguments: List<T>
    ) = postponedArguments.firstOrNull { argument -> argument.inputTypes.all { containsOnlyFixedVariables(it) } }

    fun List<Constraint>.extractUpperTypesToCheckIntersectionEmptiness(): List<CangJieTypeMarker> =
        filter { constraint ->
            constraint.kind == ConstraintKind.UPPER && !constraint.type.contains {
                !it.typeConstructor().isClassTypeConstructor() && !it.typeConstructor().isTypeParameterTypeConstructor()
            }
        }.map { it.type }

    /**
     * @see [org.cangnova.cangjie.resolve.calls.inference.components.VariableFixationFinder.Context.typeVariablesThatAreNotCountedAsProperTypes]
     * @see [org.cangnova.cangjie.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer.fixInnerVariablesForProvideDelegateIfNeeded]
     */
    abstract fun <R> withTypeVariablesThatAreCountedAsProperTypes(
        typeVariables: Set<TypeConstructorMarker>,
        block: () -> R
    ): R
}
