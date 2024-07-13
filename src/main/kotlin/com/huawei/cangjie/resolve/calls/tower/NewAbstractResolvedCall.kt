package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.ClassConstructorDescriptor
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.descriptors.PropertyDescriptor
import com.huawei.cangjie.descriptors.synthetic.SyntheticMemberDescriptor
import com.huawei.cangjie.psi.Call
import com.huawei.cangjie.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import com.huawei.cangjie.resolve.calls.inference.substitute
import com.huawei.cangjie.resolve.calls.inference.substituteAndApproximateTypes
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.util.isNotSimpleCall
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeApproximator
import com.huawei.cangjie.types.isFlexible
import com.huawei.cangjie.types.util.makeNotNullable
import com.huawei.cangjie.types.util.makeNullable


sealed class NewAbstractResolvedCall<D : CallableDescriptor> : ResolvedCall<D> {
    abstract val psiCangJieCall: PSICangJieCall
    abstract val cangjieCall: CangJieCall?
    private var isCompleted: Boolean = false
    abstract val freshSubstitutor: FreshVariableNewTypeSubstitutor?
    abstract val typeApproximator: TypeApproximator
    abstract val languageVersionSettings: LanguageVersionSettings

    abstract val resolvedCallAtom: ResolvedCallAtom?
    abstract val diagnostics: Collection<CangJieCallDiagnostic>
    abstract fun updateExtensionReceiverType(newType: CangJieType)
    fun isCompleted() = isCompleted
    protected open val positionDependentApproximation = false

    abstract fun setResultingSubstitutor(substitutor: NewTypeSubstitutor?)
    abstract fun updateDispatchReceiverType(newType: CangJieType)

    override fun getCall(): Call = psiCangJieCall.psiCall

    private fun CallableDescriptor.substituteInferredVariablesAndApproximate(
        substitutor: NewTypeSubstitutor?,
        shouldApproximate: Boolean = true
    ): CallableDescriptor {
        val inferredTypeVariablesSubstitutor = substitutor ?: FreshVariableNewTypeSubstitutor.Empty

        val freshVariablesSubstituted = freshSubstitutor?.let(::substitute) ?: this
        val knownTypeParameterSubstituted =
            resolvedCallAtom?.knownParametersSubstitutor?.let(freshVariablesSubstituted::substitute)
                ?: freshVariablesSubstituted

        return knownTypeParameterSubstituted.substituteAndApproximateTypes(
            inferredTypeVariablesSubstitutor,
            typeApproximator = if (shouldApproximate) typeApproximator else null,
            positionDependentApproximation
        )
    }

    private fun CangJieType.withNullabilityFromExplicitTypeArgument(typeArgument: SimpleTypeArgument) =
        (if (typeArgument.type.isMarkedNullable) makeNullable() else makeNotNullable()).unwrap()

    private fun getSubstitutorWithoutFlexibleTypes(
        currentSubstitutor: NewTypeSubstitutor?,
        explicitTypeArguments: List<SimpleTypeArgument>,
    ): NewTypeSubstitutor? {
        if (currentSubstitutor !is NewTypeSubstitutorByConstructorMap || explicitTypeArguments.isEmpty()) return currentSubstitutor
        if (!currentSubstitutor.map.any { (_, value) -> value.isFlexible() }) return currentSubstitutor

        val typeVariables = freshSubstitutor?.freshVariables ?: return null
        val newSubstitutorMap = currentSubstitutor.map.toMutableMap()

        explicitTypeArguments.forEachIndexed { index, typeArgument ->
            val typeVariableConstructor = typeVariables.getOrNull(index)?.freshTypeConstructor ?: return@forEachIndexed

            newSubstitutorMap[typeVariableConstructor] =
                newSubstitutorMap[typeVariableConstructor]?.withNullabilityFromExplicitTypeArgument(typeArgument)
                    ?: return@forEachIndexed
        }

        return NewTypeSubstitutorByConstructorMap(newSubstitutorMap)
    }

    protected fun substitutedResultingDescriptor(substitutor: NewTypeSubstitutor?) =
        when (val candidateDescriptor = candidateDescriptor) {
            is ClassConstructorDescriptor, is SyntheticMemberDescriptor<*> -> {
                val explicitTypeArguments =
                    resolvedCallAtom?.atom?.typeArguments?.filterIsInstance<SimpleTypeArgument>() ?: emptyList()

                candidateDescriptor.substituteInferredVariablesAndApproximate(
                    getSubstitutorWithoutFlexibleTypes(substitutor, explicitTypeArguments),
                )
            }

            is FunctionDescriptor -> {
                candidateDescriptor.substituteInferredVariablesAndApproximate(
                    substitutor,
                    candidateDescriptor.isNotSimpleCall()
                )
            }

            is PropertyDescriptor -> {
                if (candidateDescriptor.isNotSimpleCall()) {
                    candidateDescriptor.substituteInferredVariablesAndApproximate(substitutor)
                } else {
                    candidateDescriptor
                }
            }

            else -> candidateDescriptor
        }

}
