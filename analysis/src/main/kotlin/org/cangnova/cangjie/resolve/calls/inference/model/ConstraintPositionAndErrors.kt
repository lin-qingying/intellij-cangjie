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

package org.cangnova.cangjie.resolve.calls.inference.model

import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.EmptyIntersectionTypeKind
import org.cangnova.cangjie.types.model.CangJieTypeMarker
import org.cangnova.cangjie.types.model.TypeVariableMarker

interface OnlyInputTypeConstraintPosition
class LowerPriorityToPreserveCompatibility(val needToReportWarning: Boolean) :
    ConstraintSystemError(CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY)

abstract class FixVariableConstraintPosition<T>(val variable: TypeVariableMarker, val resolvedAtom: T) :
    ConstraintPosition() {
    override fun toString(): String = "Fix variable $variable"
}

sealed interface InferredEmptyIntersection {
    val incompatibleTypes: List<CangJieTypeMarker>
    val causingTypes: List<CangJieTypeMarker>
    val typeVariable: TypeVariableMarker
    val kind: EmptyIntersectionTypeKind
}

class KnownTypeParameterConstraintPositionImpl(typeArgument: CangJieType) :
    KnownTypeParameterConstraintPosition<CangJieType>(typeArgument)

abstract class KnownTypeParameterConstraintPosition<T : CangJieTypeMarker>(val typeArgument: T) : ConstraintPosition() {
    override fun toString(): String = "TypeArgument $typeArgument"
}

class InferredEmptyIntersectionWarning(
    override val incompatibleTypes: List<CangJieTypeMarker>,
    override val causingTypes: List<CangJieTypeMarker>,
    override val typeVariable: TypeVariableMarker,
    override val kind: EmptyIntersectionTypeKind,
) : ConstraintSystemError(CandidateApplicability.RESOLVED), InferredEmptyIntersection

class InferredEmptyIntersectionError(
    override val incompatibleTypes: List<CangJieTypeMarker>,
    override val causingTypes: List<CangJieTypeMarker>,
    override val typeVariable: TypeVariableMarker,
    override val kind: EmptyIntersectionTypeKind,
) : ConstraintSystemError(CandidateApplicability.INAPPLICABLE), InferredEmptyIntersection

class ConstrainingTypeIsError(
    val typeVariable: TypeVariableMarker,
    val constraintType: CangJieTypeMarker,
    val position: IncorporationConstraintPosition
) : ConstraintSystemError(CandidateApplicability.INAPPLICABLE)

class NoSuccessfulFork(val position: IncorporationConstraintPosition) :
    ConstraintSystemError(CandidateApplicability.INAPPLICABLE)

// TODO: should be used only in SimpleConstraintSystemImpl
object SimpleConstraintSystemConstraintPosition : ConstraintPosition()

sealed class ConstraintSystemError(val applicability: CandidateApplicability)

/**
 * 密封类ConstraintPosition表示约束位置的类型
 * 密封类用于限制类的子类，使得子类只能在本文件内定义，从而更好地控制类的继承结构
 * 这里使用密封类是为了定义一组受限的约束位置类型，以便在类型检查时能够更精确地知道对象的类型
 */
sealed class ConstraintPosition

data class IncorporationConstraintPosition(
    val initialConstraint: InitialConstraint,
    var isFromDeclaredUpperBound: Boolean = false
) : ConstraintPosition() {
    val from: ConstraintPosition get() = initialConstraint.position

    override fun toString(): String = "Incorporate $initialConstraint from position $from"
}


abstract class ArgumentConstraintPosition<out T>(val argument: T) : ConstraintPosition(),
    OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Argument $argument"
}

abstract class CallableReferenceConstraintPosition<out T>(val call: T) : ConstraintPosition(),
    OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Callable reference $call"
}

class ArgumentConstraintPositionImpl(argument: CangJieCallArgument) :
    ArgumentConstraintPosition<CangJieCallArgument>(argument)

class CallableReferenceConstraintPositionImpl(val callableReferenceCall: CallableReferenceCangJieCall) :
    CallableReferenceConstraintPosition<CallableReferenceResolutionAtom>(callableReferenceCall)

abstract class ReceiverConstraintPosition<T>(val argument: T) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Receiver $argument"
}

sealed interface NewConstraintMismatch {
    val lowerType: CangJieTypeMarker
    val upperType: CangJieTypeMarker
    val position: IncorporationConstraintPosition
}


class NewConstraintError(
    override val lowerType: CangJieTypeMarker,
    override val upperType: CangJieTypeMarker,
    override val position: IncorporationConstraintPosition,
) : ConstraintSystemError(if (position.from is ReceiverConstraintPosition<*>) CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER else CandidateApplicability.INAPPLICABLE),
    NewConstraintMismatch {
    override fun toString(): String {
        return "$lowerType <: $upperType"
    }
}

class ExplicitTypeParameterConstraintPositionImpl(
    typeArgument: SimpleTypeArgument
) : ExplicitTypeParameterConstraintPosition<SimpleTypeArgument>(typeArgument)

class DeclaredUpperBoundConstraintPositionImpl(
    typeParameter: TypeParameterDescriptor,
    val cangjieCall: CangJieCall
) : DeclaredUpperBoundConstraintPosition<TypeParameterDescriptor>(typeParameter) {
    override fun toString() = "DeclaredUpperBound ${typeParameter.name} from ${typeParameter.containingDeclaration}"
}

class ReceiverConstraintPositionImpl(
    argument: CangJieCallArgument,
    val selectorCall: CangJieCall?
) : ReceiverConstraintPosition<CangJieCallArgument>(argument)

class NewConstraintWarning(
    override val lowerType: CangJieTypeMarker,
    override val upperType: CangJieTypeMarker,
    override val position: IncorporationConstraintPosition,
) : ConstraintSystemError(CandidateApplicability.RESOLVED), NewConstraintMismatch

fun NewConstraintError.transformToWarning() = NewConstraintWarning(lowerType, upperType, position)
abstract class DeclaredUpperBoundConstraintPosition<T>(val typeParameter: T) : ConstraintPosition() {
    override fun toString(): String = "DeclaredUpperBound $typeParameter"
}

class FixVariableConstraintPositionImpl(
    variable: TypeVariableMarker,
    resolvedAtom: ResolvedAtom?
) : FixVariableConstraintPosition<ResolvedAtom?>(variable, resolvedAtom)

class NotEnoughInformationForTypeParameterImpl(
    typeVariable: TypeVariableMarker,
    resolvedAtom: ResolvedAtom,
    couldBeResolvedWithUnrestrictedBuilderInference: Boolean
) : NotEnoughInformationForTypeParameter<ResolvedAtom>(
    typeVariable,
    resolvedAtom,
    couldBeResolvedWithUnrestrictedBuilderInference
)

class OnlyInputTypesDiagnostic(val typeVariable: TypeVariableMarker) :
    ConstraintSystemError(CandidateApplicability.INAPPLICABLE)

class CapturedTypeFromSubtyping(
    val typeVariable: TypeVariableMarker,
    val constraintType: CangJieTypeMarker,
    val position: ConstraintPosition
) : ConstraintSystemError(CandidateApplicability.INAPPLICABLE)

open class NotEnoughInformationForTypeParameter<T>(
    val typeVariable: TypeVariableMarker,
    val resolvedAtom: T,
    val couldBeResolvedWithUnrestrictedBuilderInference: Boolean
) : ConstraintSystemError(CandidateApplicability.INAPPLICABLE)

class MultipleMinimalCommonSupertypes(
    val typeVariable: TypeVariableMarker,
    val candidates: List<CangJieType>
) : ConstraintSystemError(CandidateApplicability.INAPPLICABLE) {

    override fun toString(): String {
        return "Multiple minimal common supertypes found for $typeVariable: ${candidates.joinToString(", ")}"
    }
}

class InferredIntoDeclaredUpperBounds(val typeVariable: TypeVariableMarker) : ConstraintSystemError(
    CandidateApplicability.RESOLVED
)

abstract class BuilderInferenceSubstitutionConstraintPosition<L>(
    private val builderInferenceLambda: L,
    val initialConstraint: InitialConstraint,
    val isFromNotSubstitutedDeclaredUpperBound: Boolean = false
) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Incorporated builder inference constraint $initialConstraint " +
            "into $builderInferenceLambda call"
}

abstract class ExplicitTypeParameterConstraintPosition<T>(val typeArgument: T) : ConstraintPosition(),
    OnlyInputTypeConstraintPosition {
    override fun toString(): String = "TypeParameter $typeArgument"
}

class ExpectedTypeConstraintPositionImpl(topLevelCall: CangJieCall) :
    ExpectedTypeConstraintPosition<CangJieCall>(topLevelCall)

abstract class ExpectedTypeConstraintPosition<T>(val topLevelCall: T) : ConstraintPosition(),
    OnlyInputTypeConstraintPosition {
    override fun toString(): String = "ExpectedType for call $topLevelCall"
}

abstract class LambdaArgumentConstraintPosition<T>(val lambda: T) : ConstraintPosition() {
    override fun toString(): String {
        return "LambdaArgument $lambda"
    }
}

class LambdaArgumentConstraintPositionImpl(lambda: ResolvedLambdaAtom) :
    LambdaArgumentConstraintPosition<ResolvedLambdaAtom>(lambda)

object BuilderInferencePosition : ConstraintPosition() {
    override fun toString(): String = "For builder inference call"
}

class BuilderInferenceSubstitutionConstraintPositionImpl(
    builderInferenceLambda: LambdaCangJieCallArgument,
    initialConstraint: InitialConstraint,
    isFromNotSubstitutedDeclaredUpperBound: Boolean = false
) : BuilderInferenceSubstitutionConstraintPosition<LambdaCangJieCallArgument>(
    builderInferenceLambda, initialConstraint, isFromNotSubstitutedDeclaredUpperBound
)

abstract class InjectedAnotherStubTypeConstraintPosition<T>(private val builderInferenceLambdaOfInjectedStubType: T) :
    ConstraintPosition(),
    OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Injected from $builderInferenceLambdaOfInjectedStubType builder inference call"
}

class InjectedAnotherStubTypeConstraintPositionImpl(builderInferenceLambdaOfInjectedStubType: LambdaCangJieCallArgument) :
    InjectedAnotherStubTypeConstraintPosition<LambdaCangJieCallArgument>(builderInferenceLambdaOfInjectedStubType)
