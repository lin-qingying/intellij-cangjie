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

package org.cangnova.cangjie.resolve.calls.components

import org.cangnova.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import org.cangnova.cangjie.resolve.calls.inference.components.CangJieConstraintSystemCompleter
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionContext
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.inference.components.TrivialConstraintTypeInferenceOracle
import org.cangnova.cangjie.resolve.calls.inference.model.Constraint
import org.cangnova.cangjie.resolve.calls.inference.model.VariableWithConstraints
import org.cangnova.cangjie.resolve.calls.model.PostponedResolvedAtom
import org.cangnova.cangjie.types.AbstractTypeChecker
import org.cangnova.cangjie.types.UnwrappedType
import org.cangnova.cangjie.types.model.*
import org.cangnova.cangjie.utils.newLinkedHashMapWithExpectedSize
import java.util.*

typealias CsCompleterContext = ConstraintSystemCompletionContext

class CompletionModeCalculator {
    companion object {
        fun computeCompletionMode(
            candidate: ResolutionCandidate,
            expectedType: UnwrappedType?,
            returnType: UnwrappedType?,
            trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
            inferenceSession: InferenceSession
        ): ConstraintSystemCompletionMode = with(candidate) {
            inferenceSession.computeCompletionMode(candidate)?.let { return it }

            val csCompleterContext = getSystem().asConstraintSystemCompleterContext()

            if (candidate.isErrorCandidate()) return ConstraintSystemCompletionMode.FULL

            // Presence of expected type means that we are trying to complete outermost call => completion mode should be full
            if (expectedType != null) return ConstraintSystemCompletionMode.FULL

            // This is questionable as null return type can be only for error call
            if (returnType == null) return ConstraintSystemCompletionMode.PARTIAL

            // Full if return type for call has no type variables
            if (getSystem().getBuilder().isProperType(returnType)) return ConstraintSystemCompletionMode.FULL

            // For nested call with variables in return type check possibility of full completion
            return CalculatorForNestedCall(
                candidate, returnType, csCompleterContext, trivialConstraintTypeInferenceOracle
            ).computeCompletionMode()
        }
    }

    private class CalculatorForNestedCall(
        private val candidate: ResolutionCandidate,
        private val returnType: UnwrappedType?,
        private val csCompleterContext: CsCompleterContext,
        private val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    ) {
        private enum class FixationDirection {
            TO_SUBTYPE, EQUALITY
        }

        private val fixationDirectionsForVariables: MutableMap<VariableWithConstraints, FixationDirection> =
            newLinkedHashMapWithExpectedSize(csCompleterContext.notFixedTypeVariables.size)
        private val variablesWithQueuedConstraints = mutableSetOf<TypeVariableMarker>()
        private val typesToProcess: Queue<CangJieTypeMarker> = ArrayDeque()

        private val postponedAtoms: List<PostponedResolvedAtom> by lazy {
            CangJieConstraintSystemCompleter.getOrderedNotAnalyzedPostponedArguments(listOf(candidate.resolvedCall))
        }

        fun computeCompletionMode(): ConstraintSystemCompletionMode = with(csCompleterContext) {
            // Add fixation directions for variables based on effective variance in type
            typesToProcess.add(returnType)
            computeDirections()

            // If all variables have required proper constraint, run full completion
            if (directionRequirementsForVariablesHold())
                return ConstraintSystemCompletionMode.FULL

            return ConstraintSystemCompletionMode.PARTIAL
        }

        private fun CsCompleterContext.computeDirections() {
            while (typesToProcess.isNotEmpty()) {
                val type = typesToProcess.poll() ?: break

                if (!type.contains { it.typeConstructor() in notFixedTypeVariables })
                    continue

                val fixationDirectionsFromType = mutableSetOf<FixationDirectionForVariable>()
//                collectRequiredDirectionsForVariables(type, TypeVariance.OUT, fixationDirectionsFromType)

                for (directionForVariable in fixationDirectionsFromType) {
                    updateDirection(directionForVariable)
                    enqueueTypesFromConstraints(directionForVariable.variable)
                }
            }
        }

        private fun enqueueTypesFromConstraints(variableWithConstraints: VariableWithConstraints) {
            val variable = variableWithConstraints.typeVariable
            if (variable !in variablesWithQueuedConstraints) {
                for (constraint in variableWithConstraints.constraints) {
                    typesToProcess.add(constraint.type)
                }

                variablesWithQueuedConstraints.add(variable)
            }
        }

        private fun CsCompleterContext.directionRequirementsForVariablesHold(): Boolean {
            for ((variable, fixationDirection) in fixationDirectionsForVariables) {
                if (!hasProperConstraint(variable, fixationDirection))
                    return false
            }
            return true
        }

        private fun updateDirection(directionForVariable: FixationDirectionForVariable) {
            val (variable, newDirection) = directionForVariable
            fixationDirectionsForVariables[variable]?.let { oldDirection ->
                if (oldDirection != FixationDirection.EQUALITY && oldDirection != newDirection)
                    fixationDirectionsForVariables[variable] = FixationDirection.EQUALITY
            } ?: run {
                fixationDirectionsForVariables[variable] = newDirection
            }
        }

        private data class FixationDirectionForVariable(
            val variable: VariableWithConstraints,
            val direction: FixationDirection
        )

        private fun CsCompleterContext.collectRequiredDirectionsForVariables(
            type: CangJieTypeMarker, outerVariance: TypeVariance,
            fixationDirectionsCollector: MutableSet<FixationDirectionForVariable>
        ) {
            val typeArgumentsCount = type.argumentsCount()
            val typeConstructor = type.typeConstructor()
            if (typeArgumentsCount > 0 && typeArgumentsCount == typeConstructor.parametersCount()) {
                for (position in 0 until typeArgumentsCount) {
                    val argument = type.getArgument(position)
                    val parameter = typeConstructor.getParameter(position)



                    collectRequiredDirectionsForVariables(
                        argument.getType(),
                        compositeVariance(outerVariance, argument, parameter),
                        fixationDirectionsCollector
                    )
                }
            } else {
                processTypeWithoutParameters(type, outerVariance, fixationDirectionsCollector)
            }
        }

        private fun CsCompleterContext.compositeVariance(
            outerVariance: TypeVariance,
            argument: TypeArgumentMarker,
            parameter: TypeParameterMarker
        ): TypeVariance {
            val effectiveArgumentVariance =
                AbstractTypeChecker.effectiveVariance(parameter.getVariance(), argument.getVariance())
                    ?: TypeVariance.INV // conflicting variance
            return when (outerVariance) {
                TypeVariance.INV -> TypeVariance.INV
//                TypeVariance.OUT -> effectiveArgumentVariance
//                TypeVariance.IN -> effectiveArgumentVariance.reversed()
            }
        }

        private fun TypeVariance.reversed(): TypeVariance = when (this) {
//            TypeVariance.IN -> TypeVariance.OUT
//            TypeVariance.OUT -> TypeVariance.IN
            TypeVariance.INV -> TypeVariance.INV
        }

        private fun CsCompleterContext.processTypeWithoutParameters(
            type: CangJieTypeMarker, compositeVariance: TypeVariance,
            newRequirementsCollector: MutableSet<FixationDirectionForVariable>
        ) {
            val variableWithConstraints = notFixedTypeVariables[type.typeConstructor()] ?: return
            val direction = when (compositeVariance) {
//                TypeVariance.IN -> FixationDirection.EQUALITY // Assuming that variables in contravariant positions are fixed to subtype
//                TypeVariance.OUT -> FixationDirection.TO_SUBTYPE
                TypeVariance.INV -> FixationDirection.EQUALITY
            }
            val requirement = FixationDirectionForVariable(variableWithConstraints, direction)
            newRequirementsCollector.add(requirement)
        }

        private fun CsCompleterContext.hasProperConstraint(
            variableWithConstraints: VariableWithConstraints,
            direction: FixationDirection
        ): Boolean {
            val constraints = variableWithConstraints.constraints
            val variable = variableWithConstraints.typeVariable

            // ILT constraint tracking is necessary to prevent incorrect full completion from Nothing constraint
            // Consider ILT <: T; Nothing <: T for T requiring lower constraint
            // Nothing would trigger full completion, but resulting type would be Int
            // Possible restrictions on integer constant from outer calls would be ignored

            var iltConstraintPresent = false
            var properConstraintPresent = false
            var nonNothingProperConstraintPresent = false

            for (constraint in constraints) {
                if (!constraint.hasRequiredKind(direction) || !isProperType(constraint.type))
                    continue

                if (constraint.type.typeConstructor().isIntegerLiteralTypeConstructor()) {
                    iltConstraintPresent = true
                } else if (trivialConstraintTypeInferenceOracle.isSuitableResultedType(constraint.type)) {
                    properConstraintPresent = true
                    nonNothingProperConstraintPresent = true
                } else if (!isLowerConstraintForPartiallyAnalyzedVariable(constraint, variable)) {
                    properConstraintPresent = true
                }
            }

            if (!properConstraintPresent) return false

            return !iltConstraintPresent || nonNothingProperConstraintPresent
        }

        private fun Constraint.hasRequiredKind(direction: FixationDirection) = when (direction) {
            FixationDirection.TO_SUBTYPE -> kind.isLower() || kind.isEqual()
            FixationDirection.EQUALITY -> kind.isEqual()
        }

        private fun CsCompleterContext.isLowerConstraintForPartiallyAnalyzedVariable(
            constraint: Constraint,
            variable: TypeVariableMarker
        ): Boolean {
            val defaultType = variable.defaultType()
            return constraint.kind.isLower() && postponedAtoms.any { atom ->
                atom.expectedType?.contains { type -> defaultType == type } ?: false
            }
        }
    }
}
