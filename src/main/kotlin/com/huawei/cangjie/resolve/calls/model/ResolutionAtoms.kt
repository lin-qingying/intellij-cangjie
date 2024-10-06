package com.huawei.cangjie.resolve.calls.model

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.calls.components.ReturnArgumentsInfo
import com.huawei.cangjie.resolve.calls.components.TypeArgumentsToParametersMapper
import com.huawei.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import com.huawei.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.huawei.cangjie.resolve.calls.components.extractInputOutputTypesFromCallableReferenceExpectedType
import com.huawei.cangjie.resolve.calls.inference.NewConstraintSystem
import com.huawei.cangjie.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.inference.model.NewConstraintError
import com.huawei.cangjie.resolve.calls.inference.model.NewConstraintMismatch
import com.huawei.cangjie.resolve.calls.inference.model.NewConstraintWarning
import com.huawei.cangjie.resolve.calls.inference.model.TypeVariableForLambdaReturnType
import com.huawei.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import com.huawei.cangjie.resolve.constants.IntegerValueTypeConstant
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeConstructor
import com.huawei.cangjie.types.UnwrappedType
import com.huawei.cangjie.types.model.CangJieTypeMarker
import com.huawei.cangjie.types.util.unCapture
import com.huawei.cangjie.utils.addIfNotNull


/**
 * Call, Callable reference, lambda & function expression, collection literal.
 * In future we should add literals here, because they have similar lifecycle.
 *
 * Expression with type is also primitive. This is done for simplification. todo
 */
interface ResolutionAtom

sealed class ResolvedAtom {
    abstract val atom: ResolutionAtom? // CallResolutionResult has no ResolutionAtom

    var analyzed: Boolean = false
        private set

    var subResolvedAtoms: List<ResolvedAtom>? = null
        private set

    protected open fun setAnalyzedResults(subResolvedAtoms: List<ResolvedAtom>) {
        assert(!analyzed) {
            "Already analyzed: $this"
        }

        analyzed = true

        this.subResolvedAtoms = subResolvedAtoms
    }

    // For AllCandidates mode to avoid analyzing postponed arguments
    fun setEmptyAnalyzedResults() {
        setAnalyzedResults(emptyList())
    }
}

data class CandidateWithDiagnostics(val candidate: ResolutionCandidate, val diagnostics: List<CangJieCallDiagnostic>)

class AllCandidatesResolutionResult(
    val allCandidates: Collection<CandidateWithDiagnostics>,
    constraintSystem: NewConstraintSystem
) : CallResolutionResult(null, emptyList(), constraintSystem)

sealed interface ResolvedCallableReferenceAtom

abstract class ResolvedCallAtom : ResolvedAtom() {
    abstract override val atom: CangJieCall
    abstract val candidateDescriptor: CallableDescriptor
    abstract val explicitReceiverKind: ExplicitReceiverKind
    abstract val dispatchReceiverArgument: SimpleCangJieCallArgument?
    abstract var extensionReceiverArgument: SimpleCangJieCallArgument?
    abstract val extensionReceiverArgumentCandidates: List<SimpleCangJieCallArgument>?
    abstract var contextReceiversArguments: List<SimpleCangJieCallArgument>
    abstract val typeArgumentMappingByOriginal: TypeArgumentsToParametersMapper.TypeArgumentsMapping

    abstract val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
    abstract val freshVariablesSubstitutor: FreshVariableNewTypeSubstitutor
    abstract val argumentsWithSuspendConversion: Map<CangJieCallArgument, UnwrappedType>

    abstract val knownParametersSubstitutor: NewTypeSubstitutor

        abstract val argumentsWithConversion: Map<CangJieCallArgument, SamConversionDescription>

    abstract val argumentsWithUnitConversion: Map<CangJieCallArgument, UnwrappedType>

    abstract val argumentsWithConstantConversion: Map<CangJieCallArgument, IntegerValueTypeConstant>

    abstract fun setCandidateDescriptor(newCandidateDescriptor: CallableDescriptor)
}

class SamConversionDescription(
    val convertedTypeByOriginParameter: UnwrappedType,
    val convertedTypeByCandidateParameter: UnwrappedType, // expected type for corresponding argument
    val originalParameterType: UnwrappedType // need to overload resolution on inherited SAM interfaces
)

sealed class CallResolutionResult(
    resultCallAtom: ResolvedCallAtom?,
    val diagnostics: List<CangJieCallDiagnostic>,
    val constraintSystem: NewConstraintSystem
) : ResolvedAtom() {
    override val atom: ResolutionAtom? get() = null

    override fun toString(): String = "diagnostics: (${diagnostics.joinToString()})"

    fun completedDiagnostic(substitutor: NewTypeSubstitutor): List<CangJieCallDiagnostic> {
        return diagnostics.map {
            val error = it.constraintSystemError ?: return@map it
            if (error !is NewConstraintMismatch) return@map it
            val lowerType = (error.lowerType as? CangJieType)?.unwrap() ?: return@map it
            val newLowerType = substitutor.safeSubstitute(lowerType.unCapture())
            when (error) {
                is NewConstraintError -> NewConstraintError(
                    newLowerType,
                    error.upperType,
                    error.position
                ).asDiagnostic()

                is NewConstraintWarning -> NewConstraintWarning(
                    newLowerType,
                    error.upperType,
                    error.position
                ).asDiagnostic()
            }
        }
    }
}

open class SingleCallResolutionResult(
    val resultCallAtom: ResolvedCallAtom,
    diagnostics: List<CangJieCallDiagnostic>,
    constraintSystem: NewConstraintSystem
) : CallResolutionResult(resultCallAtom, diagnostics, constraintSystem)

class PartialCallResolutionResult(
    resultCallAtom: ResolvedCallAtom,
    diagnostics: List<CangJieCallDiagnostic>,
    constraintSystem: NewConstraintSystem,
    val forwardToInferenceSession: Boolean = false
) : SingleCallResolutionResult(resultCallAtom, diagnostics, constraintSystem)


class PartialCallContainer(val result: PartialCallResolutionResult?) {
    companion object {
        val empty = PartialCallContainer(null)
    }
}

sealed class PostponedResolvedAtom : ResolvedAtom(), PostponedResolvedAtomMarker {
    abstract override val inputTypes: Collection<UnwrappedType>
    abstract override val outputType: UnwrappedType?
    abstract override val expectedType: UnwrappedType?
}

sealed interface CallableReferenceResolutionAtom : ResolutionAtom {
    val lhsResult: LHSResult
    val rhsName: Name
    val call: CangJieCall
}

class ErrorCallResolutionResult(
    resultCallAtom: ResolvedCallAtom,
    diagnostics: List<CangJieCallDiagnostic>,
    constraintSystem: NewConstraintSystem
) : SingleCallResolutionResult(resultCallAtom, diagnostics, constraintSystem)

class CompletedCallResolutionResult(
    resultCallAtom: ResolvedCallAtom,
    diagnostics: List<CangJieCallDiagnostic>,
    constraintSystem: NewConstraintSystem
) : SingleCallResolutionResult(resultCallAtom, diagnostics, constraintSystem)

abstract class ResolvedCallableReferenceArgumentAtom(
    override val atom: CallableReferenceCangJieCallArgument,
    override val expectedType: UnwrappedType?
) : PostponedResolvedAtom(), ResolvedCallableReferenceAtom {
    var candidate: CallableReferenceResolutionCandidate? = null
        private set

    var completed: Boolean = false

    fun setAnalyzedResults(
        candidate: CallableReferenceResolutionCandidate?,
        subResolvedAtoms: List<ResolvedAtom>
    ) {
        this.candidate = candidate
        setAnalyzedResults(subResolvedAtoms)
    }

}


class CallableReferenceCangJieCall(
    override val call: CangJieCall,
    override val lhsResult: LHSResult,
    override val rhsName: Name,
) : CallableReferenceResolutionAtom

val ResolvedCallAtom.freshReturnType: UnwrappedType?
    get() {
        val returnType = candidateDescriptor.returnType ?: return null
        return freshVariablesSubstitutor.safeSubstitute(returnType.unwrap())
    }

fun CallResolutionResult.resultCallAtom(): ResolvedCallAtom? =
    if (this is SingleCallResolutionResult) resultCallAtom else null
/*
 * Used only for delegated properties with one good candidate and one for bad
 * e.g. in case `var x by lazy { "" }
 */
class StubResolvedAtom(val typeVariable: TypeConstructor) : ResolvedAtom() {
    override val atom: ResolutionAtom? get() = null
}
class   ResolvedExpressionAtom(override val atom: ExpressionCangJieCallArgument) : ResolvedAtom() {
    init {
        setAnalyzedResults(listOf())
    }
}
class ResolvedLambdaAtom(
    override val atom: LambdaCangJieCallArgument,

    val receiver: UnwrappedType?,
    val contextReceivers: List<UnwrappedType>,
    val parameters: List<UnwrappedType>,
    val returnType: UnwrappedType,
    val typeVariableForLambdaReturnType: TypeVariableForLambdaReturnType?,
    override val expectedType: UnwrappedType?
) : PostponedResolvedAtom() {
    /**
     * [resultArgumentsInfo] can be null only if lambda was analyzed in process of resolve
     *   ambiguity by lambda return type
     * There is a contract that [resultArgumentsInfo] will be not null for unwrapped lambda atom
     *   (see [unwrap])
     */
    var resultArgumentsInfo: ReturnArgumentsInfo? = null
        private set

    fun setAnalyzedResults(
        resultArguments: ReturnArgumentsInfo?,
        subResolvedAtoms: List<ResolvedAtom>
    ) {
        this.resultArgumentsInfo = resultArguments
        setAnalyzedResults(subResolvedAtoms)
    }

    override val inputTypes: Collection<UnwrappedType>
        get() {
            if (receiver == null && contextReceivers.isEmpty()) return parameters
            return ArrayList<UnwrappedType>(parameters.size + contextReceivers.size + (if (receiver != null) 1 else 0)).apply {
                addAll(parameters)
                addIfNotNull(receiver)
                addAll(contextReceivers)
            }
        }

    override val outputType: UnwrappedType get() = returnType
}

sealed class AbstractPostponedCallableReferenceAtom(
    atom: CallableReferenceCangJieCallArgument,
    expectedType: UnwrappedType?
) : ResolvedCallableReferenceArgumentAtom(atom, expectedType) {
    override val inputTypes: Collection<UnwrappedType>
        get() = extractInputOutputTypesFromCallableReferenceExpectedType(expectedType)?.inputTypes ?: listOfNotNull(expectedType)

    override val outputType: UnwrappedType?
            get() = extractInputOutputTypesFromCallableReferenceExpectedType(expectedType)?.outputType
}
class EagerCallableReferenceAtom(
    atom: CallableReferenceCangJieCallArgument,
    expectedType: UnwrappedType?
) : ResolvedCallableReferenceArgumentAtom(atom, expectedType) {
    override val inputTypes: Collection<UnwrappedType> get() = emptyList()
    override val outputType: UnwrappedType? get() = null

    fun transformToPostponed(): PostponedCallableReferenceAtom = PostponedCallableReferenceAtom(this)
}
class PostponedCallableReferenceAtom(
    eagerCallableReferenceAtom: EagerCallableReferenceAtom
) : AbstractPostponedCallableReferenceAtom(eagerCallableReferenceAtom.atom, eagerCallableReferenceAtom.expectedType),
    PostponedCallableReferenceMarker {
    override var revisedExpectedType: UnwrappedType? = null
        private set

    override fun reviseExpectedType(expectedType: CangJieTypeMarker) {
        require(expectedType is UnwrappedType)
        revisedExpectedType = expectedType
    }
}
class CallableReferenceWithRevisedExpectedTypeAtom(
    atom: CallableReferenceCangJieCallArgument,
    expectedType: UnwrappedType?,
) : AbstractPostponedCallableReferenceAtom(atom, expectedType)
class LambdaWithTypeVariableAsExpectedTypeAtom(
    override val atom: LambdaCangJieCallArgument,
    override val expectedType: UnwrappedType
) : PostponedResolvedAtom(), LambdaWithTypeVariableAsExpectedTypeMarker {
    override val inputTypes: Collection<UnwrappedType> get() = listOf(expectedType)
    override val outputType: UnwrappedType? get() = null

    override var revisedExpectedType: UnwrappedType? = null
        private set

    override var parameterTypesFromDeclaration: List<UnwrappedType?>? = null
        private set

    override fun updateParameterTypesFromDeclaration(types: List<CangJieTypeMarker?>?) {
        @Suppress("UNCHECKED_CAST")
        types as List<UnwrappedType?>?
        parameterTypesFromDeclaration = types
    }

    override fun reviseExpectedType(expectedType: CangJieTypeMarker) {
        require(expectedType is UnwrappedType)
        revisedExpectedType = expectedType
    }

    fun setAnalyzed(resolvedLambdaAtom: ResolvedLambdaAtom) {
        setAnalyzedResults(listOf(resolvedLambdaAtom))
    }
}
class ResolvedCollectionLiteralAtom(
    override val atom: CollectionLiteralCangJieCallArgument,
    val expectedType: UnwrappedType?
) : ResolvedAtom() {
    init {
        setAnalyzedResults(listOf())
    }
}
class ResolvedSubCallArgument(override val atom: SubCangJieCallArgument, resolveIndependently: Boolean) : ResolvedAtom() {
    init {
        if (resolveIndependently)
            setAnalyzedResults(listOf())
        else
            setAnalyzedResults(listOf(atom.callResult))
    }
}
