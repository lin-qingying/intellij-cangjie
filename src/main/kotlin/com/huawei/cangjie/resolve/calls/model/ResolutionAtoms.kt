package com.huawei.cangjie.resolve.calls.model

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import com.huawei.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.huawei.cangjie.resolve.calls.inference.NewConstraintSystem
import com.huawei.cangjie.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.inference.model.NewConstraintError
import com.huawei.cangjie.resolve.calls.inference.model.NewConstraintMismatch
import com.huawei.cangjie.resolve.calls.inference.model.NewConstraintWarning
import com.huawei.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.UnwrappedType
import com.huawei.cangjie.types.util.unCapture


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

    //    abstract val typeArgumentMappingByOriginal: TypeArgumentsToParametersMapper.TypeArgumentsMapping
    abstract val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
    abstract val freshVariablesSubstitutor: FreshVariableNewTypeSubstitutor

    //    abstract val knownParametersSubstitutor: NewTypeSubstitutor
//    abstract val argumentsWithConversion: Map<CangJieCallArgument, SamConversionDescription>
//    abstract val argumentsWithSuspendConversion: Map<CangJieCallArgument, UnwrappedType>
    abstract val argumentsWithUnitConversion: Map<CangJieCallArgument, UnwrappedType>

    //    abstract val argumentsWithConstantConversion: Map<CangJieCallArgument, IntegerValueTypeConstant>
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

