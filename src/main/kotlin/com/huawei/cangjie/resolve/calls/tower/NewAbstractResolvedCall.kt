package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.synthetic.SyntheticMemberDescriptor
import com.huawei.cangjie.psi.Call
import com.huawei.cangjie.psi.ValueArgument
import com.huawei.cangjie.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import com.huawei.cangjie.resolve.calls.inference.model.ResolvedValueArgument
import com.huawei.cangjie.resolve.calls.inference.substitute
import com.huawei.cangjie.resolve.calls.inference.substituteAndApproximateTypes
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.calls.util.isNotSimpleCall
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeApproximator
import com.huawei.cangjie.types.isFlexible
import com.huawei.cangjie.types.util.makeNotNullable
import com.huawei.cangjie.types.util.makeOptional
import com.huawei.cangjie.utils.compactIfPossible


sealed class NewAbstractResolvedCall<D : CallableDescriptor> : ResolvedCall<D> {
    abstract val psiCangJieCall: PSICangJieCall
    abstract val cangjieCall: CangJieCall?
    private var isCompleted: Boolean = false
    abstract val freshSubstitutor: FreshVariableNewTypeSubstitutor?
    abstract val typeApproximator: TypeApproximator
    abstract val languageVersionSettings: LanguageVersionSettings
    abstract val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>

    abstract val resolvedCallAtom: ResolvedCallAtom?
    abstract val diagnostics: Collection<CangJieCallDiagnostic>
    abstract fun updateExtensionReceiverType(newType: CangJieType)
    fun isCompleted() = isCompleted
    protected open val positionDependentApproximation = false
    private var nonTrivialUpdatedResultInfo: DataFlowInfo? = null

    abstract fun setResultingSubstitutor(substitutor: NewTypeSubstitutor?)
    abstract fun updateDispatchReceiverType(newType: CangJieType)
    private var valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>? = null
    private var argumentToParameterMap: Map<ValueArgument, ArgumentMatchImpl>? = null
    abstract fun argumentToParameterMap(
        resultingDescriptor: CallableDescriptor,
        valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>,
    ): Map<ValueArgument, ArgumentMatchImpl>
    protected fun updateArgumentsMapping(newMapping: Map<ValueArgument, ArgumentMatchImpl>?) {
        argumentToParameterMap = newMapping
    }
    override fun getCall(): Call = psiCangJieCall.psiCall
    override fun getArgumentMapping(valueArgument: ValueArgument): ArgumentMapping {
        if (argumentToParameterMap == null) {
            updateArgumentsMapping(argumentToParameterMap(resultingDescriptor, getValueArguments()))
        }
        return argumentToParameterMap!![valueArgument] ?: ArgumentUnmapped
    }
    override fun getDataFlowInfoForArguments() = object : DataFlowInfoForArguments {
        override fun getResultInfo(): DataFlowInfo = nonTrivialUpdatedResultInfo ?: psiCangJieCall.resultDataFlowInfo

        override fun getInfo(valueArgument: ValueArgument): DataFlowInfo {
            val externalPsiCallArgument = cangjieCall?.externalArgument?.psiCallArgument
            if (externalPsiCallArgument?.valueArgument == valueArgument) {
                return externalPsiCallArgument.dataFlowInfoAfterThisArgument
            }
            return psiCangJieCall.dataFlowInfoForArguments.getInfo(valueArgument)
        }
    }
    //    protected fun updateArgumentsMapping(newMapping: Map<ValueArgument, ArgumentMatchImpl>?) {
//        argumentToParameterMap = newMapping
//    }
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

    override fun getValueArguments(): Map<ValueParameterDescriptor, ResolvedValueArgument> {
        if (valueArguments == null) {
            valueArguments = createValueArguments()
        }
        return valueArguments!!
    }

    private fun createValueArguments(): Map<ValueParameterDescriptor, ResolvedValueArgument> =
        LinkedHashMap<ValueParameterDescriptor, ResolvedValueArgument>().also { result ->
            val needToUseCorrectExecutionOrderForVarargArguments =
                languageVersionSettings.supportsFeature(LanguageFeature.UseCorrectExecutionOrderForVarargArguments)
            var varargMappings: MutableList<Pair<ValueParameterDescriptor, ResolvedValueArgument>>? = null
            for ((originalParameter, resolvedCallArgument) in argumentMappingByOriginal) {
                val resultingParameter = resultingDescriptor.valueParameters[originalParameter.index]

                result[resultingParameter] = when (resolvedCallArgument) {
                    ResolvedCallArgument.DefaultArgument ->
                        DefaultValueArgument.DEFAULT

                    is ResolvedCallArgument.SimpleArgument -> {
                        val valueArgument = resolvedCallArgument.callArgument.psiCallArgument.valueArgument
//                        if (resultingParameter.isVararg) {
//                            if (needToUseCorrectExecutionOrderForVarargArguments) {
//                                VarargValueArgument().apply { addArgument(valueArgument) }
//                            } else {
//                                val vararg = VarargValueArgument().apply { addArgument(valueArgument) }
//                                if (varargMappings == null) varargMappings = SmartList()
//                                varargMappings.add(resultingParameter to vararg)
//                                continue
//                            }
//                        } else {
                        ExpressionValueArgument(valueArgument)
//                        }
                    }
//                    is ResolvedCallArgument.VarargArgument ->
//                        VarargValueArgument().apply {
//                            resolvedCallArgument.arguments.map { it.psiCallArgument.valueArgument }.forEach { addArgument(it) }
//                        }
                }
            }

            if (varargMappings != null && !needToUseCorrectExecutionOrderForVarargArguments) {
                for ((parameter, argument) in varargMappings) {
                    result[parameter] = argument
                }
            }
        }.compactIfPossible()

    override fun getValueArgumentsByIndex(): List<ResolvedValueArgument>? {
        val arguments = ArrayList<ResolvedValueArgument?>(candidateDescriptor.valueParameters.size)
        for (i in 0 until candidateDescriptor.valueParameters.size) {
            arguments.add(null)
        }

        for ((parameterDescriptor, value) in getValueArguments()) {
            val oldValue = arguments.set(parameterDescriptor.index, value)
            if (oldValue != null) {
                return null
            }
        }

        if (arguments.any { it == null }) return null

        @Suppress("UNCHECKED_CAST")
        return arguments as List<ResolvedValueArgument>
    }

    fun substituteReceivers(substitutor: NewTypeSubstitutor?) {
        if (substitutor != null) {
            // todo: add asset that we do not complete call many times
            isCompleted = true

            dispatchReceiver?.type?.let {
                val newType = substitutor.safeSubstitute(it.unwrap())
                updateDispatchReceiverType(newType)
            }

//            extensionReceiver?.type?.let {
//                val newType = substitutor.safeSubstitute(it.unwrap())
//                updateExtensionReceiverType(newType)
//            }
        }
    }

    private fun CangJieType.withNullabilityFromExplicitTypeArgument(typeArgument: SimpleTypeArgument) =
        (if (typeArgument.type.isMarkedOption) makeOptional() else makeNotNullable()).unwrap()

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
