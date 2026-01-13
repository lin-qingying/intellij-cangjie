/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.calls.tower

import com.intellij.util.SmartList
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.psi.Call
import org.cangnova.cangjie.psi.ValueArgument
import org.cangnova.cangjie.resolve.calls.components.isVararg
import org.cangnova.cangjie.resolve.calls.inference.model.ResolvedValueArgument
import org.cangnova.cangjie.resolve.calls.inference.substituteAndApproximateTypes

import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.util.isNotSimpleCall
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.utils.compactIfPossible


sealed class AbstractResolvedCall<D : CallableDescriptor> : ResolvedCall<D> {
    abstract val psiCangJieCall: PSICangJieCall
    abstract val cangjieCall: CangJieCall?
    private var isCompleted: Boolean = false
    abstract val freshSubstitutor: ComposableTypeSubstitutor?
    abstract val typeApproximator: TypeApproximator
    abstract val languageVersionSettings: LanguageVersionSettings
    abstract val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>

    abstract val resolvedCallAtom: ResolvedCallAtom?
    abstract val diagnostics: Collection<CangJieCallDiagnostic>
    fun isCompleted() = isCompleted
    protected open val positionDependentApproximation = false
    private var nonTrivialUpdatedResultInfo: DataFlowInfo? = null

    abstract fun setResultingSubstitutor(substitutor: ComposableTypeSubstitutor?)
    abstract fun updateDispatchReceiverType(newType: CangJieType)
    private var _valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>? = null
    override val valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>
        get() {

            return _valueArguments ?: createValueArguments().also {
                _valueArguments = it
            }
        }

    private var argumentToParameterMap: Map<ValueArgument, ArgumentMatchImpl>? = null
    abstract fun argumentToParameterMap(
        resultingDescriptor: CallableDescriptor,
        valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>,
    ): Map<ValueArgument, ArgumentMatchImpl>

    protected fun updateArgumentsMapping(newMapping: Map<ValueArgument, ArgumentMatchImpl>?) {
        argumentToParameterMap = newMapping
    }

    override val call: Call
        get() = psiCangJieCall.psiCall

    override fun getArgumentMapping(valueArgument: ValueArgument): ArgumentMapping {
        if (argumentToParameterMap == null) {
            updateArgumentsMapping(argumentToParameterMap(resultingDescriptor, valueArguments))
        }
        return argumentToParameterMap!![valueArgument] ?: ArgumentUnmapped
    }

    override val dataFlowInfoForArguments: DataFlowInfoForArguments
        get() = object : DataFlowInfoForArguments {
            override val resultInfo: DataFlowInfo
                get() =
                    nonTrivialUpdatedResultInfo ?: psiCangJieCall.resultDataFlowInfo

            override fun getInfo(valueArgument: ValueArgument): DataFlowInfo {
                val externalPsiCallArgument = cangjieCall?.externalArgument?.psiCallArgument
                if (externalPsiCallArgument?.valueArgument == valueArgument) {
                    return externalPsiCallArgument.dataFlowInfoAfterThisArgument
                }
                return psiCangJieCall.dataFlowInfoForArguments.getInfo(valueArgument)
            }
        }

    fun updateValueArguments(newValueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>) {

        _valueArguments = newValueArguments
    }

    /**
     * 使用推断的类型变量替换并近似处理可调用描述符
     *
     * 此函数的目的是在给定的类型替换器基础上，对可调用描述符中包含的类型变量进行替换，
     * 并根据需求进行类型近似处理。这在类型推断和处理泛型调用时极为重要
     *
     * @param substitutor 新类型替换器，用于替换类型变量。如果为null，则不进行替换
     * @param shouldApproximate 是否应该进行类型近似处理。默认为true，表示进行近似处理
     * @return 返回经过类型变量替换和近似处理后的可调用描述符
     */
    private fun CallableDescriptor.substituteInferredVariablesAndApproximate(
        inferredSubstitutor: ComposableTypeSubstitutor?,
        shouldApproximate: Boolean = true
    ): CallableDescriptor {
        // 1️⃣ fresh variables（T → α）
        val withFreshVariables =
            freshSubstitutor?.let { substitute(it) } ?: this

        // 2️⃣ known type parameters（显式类型实参）
        val withKnownTypeParameters =
            resolvedCallAtom?.knownParametersSubstitutor
                ?.let { withFreshVariables.substitute(it) }
                ?: withFreshVariables

        // 3️⃣ inferred variables（α → Int）
        val fullySubstituted =
            inferredSubstitutor?.let { withKnownTypeParameters.substitute(it) }
                ?: withKnownTypeParameters

        // 4️⃣ approximation（独立步骤）
        return if (shouldApproximate) {
            fullySubstituted.substituteAndApproximateTypes(
                ComposableTypeSubstitutor.EMPTY,
                typeApproximator,
                positionDependentApproximation
            )
        } else {
            fullySubstituted
        }
    }


    private fun createValueArguments(): Map<ValueParameterDescriptor, ResolvedValueArgument> =
        LinkedHashMap<ValueParameterDescriptor, ResolvedValueArgument>().also { result ->
            val needToUseCorrectExecutionOrderForVarargArguments = true
            var varargMappings: MutableList<Pair<ValueParameterDescriptor, ResolvedValueArgument>>? = null
            for ((originalParameter, resolvedCallArgument) in argumentMappingByOriginal) {
                val resultingParameter = resultingDescriptor.valueParameters[originalParameter.index]

                result[resultingParameter] = when (resolvedCallArgument) {
                    ResolvedCallArgument.DefaultArgument ->
                        DefaultValueArgument.DEFAULT

                    is ResolvedCallArgument.SimpleArgument -> {
                        val valueArgument = resolvedCallArgument.callArgument.psiCallArgument.valueArgument
                        if (resultingParameter.isVararg) {
                            if (needToUseCorrectExecutionOrderForVarargArguments) {
                                VarargValueArgument().apply { addArgument(valueArgument) }
                            } else {
                                val vararg = VarargValueArgument().apply { addArgument(valueArgument) }
                                if (varargMappings == null) varargMappings = SmartList()
                                varargMappings.add(resultingParameter to vararg)
                                continue
                            }
                        } else {
                            ExpressionValueArgument(valueArgument)
                        }
                    }

                    is ResolvedCallArgument.VarargArgument ->
                        VarargValueArgument().apply {
                            resolvedCallArgument.arguments.map { it.psiCallArgument.valueArgument }
                                .forEach { addArgument(it) }
                        }
                }
            }

            if (varargMappings != null && !needToUseCorrectExecutionOrderForVarargArguments) {
                for ((parameter, argument) in varargMappings) {
                    result[parameter] = argument
                }
            }
        }.compactIfPossible()

    override val valueArgumentsByIndex: List<ResolvedValueArgument>?
        get() {

            val arguments = ArrayList<ResolvedValueArgument?>(candidateDescriptor.valueParameters.size)
            for (i in 0 until candidateDescriptor.valueParameters.size) {
                arguments.add(null)
            }

            for ((parameterDescriptor, value) in valueArguments) {
                val oldValue = arguments.set(parameterDescriptor.index, value)
                if (oldValue != null) {
                    return null
                }
            }

            if (arguments.any { it == null }) return null

            @Suppress("UNCHECKED_CAST")
            return arguments as List<ResolvedValueArgument>
        }


    fun substituteReceivers(substitutor: ComposableTypeSubstitutor?) {
        if (substitutor != null) {
            // todo: add asset that we do not complete call many times
            isCompleted = true

            dispatchReceiver?.type?.let {
                val newType = substitutor.safeSubstitute(it.unwrap())
                updateDispatchReceiverType(newType)
            }

        }
    }

    private fun CangJieType.withNullabilityFromExplicitTypeArgument(typeArgument: SimpleTypeArgument) =
        (if (typeArgument.type.isOption) makeOption() else makeNonOption()).unwrap()

    private fun getSubstitutorWithoutFlexibleTypes(
        current: ComposableTypeSubstitutor?,
        explicitTypeArguments: List<SimpleTypeArgument>
    ): ComposableTypeSubstitutor? {

        if (current == null || explicitTypeArguments.isEmpty()) return current
        val fresh = freshSubstitutor ?: return current

        // 用 explicit type args 构造一个 override substitutor
        val overrideSubstitutor = TypeSubstitutors.create(
            SubstitutorFunction { constructor ->

                val typeParameter =
                    constructor.declarationDescriptor as? TypeParameterDescriptor
                        ?: return@SubstitutorFunction null

                val index = typeParameter.index
                val explicitArg =
                    explicitTypeArguments.getOrNull(index)
                        ?: return@SubstitutorFunction null

                // 先用原 substitutor 算出类型
                val substituted =
                    current.substitute(typeParameter.defaultType)?.unwrap()
                        ?: return@SubstitutorFunction null

                // 如果不是 flexible，直接忽略
                if (!substituted.isFlexible()) return@SubstitutorFunction null

                // 用显式类型参数修正 nullability
                substituted.withNullabilityFromExplicitTypeArgument(explicitArg)
            }
        )

        // override 优先级高于 current
        return overrideSubstitutor.compose(current)
    }
    protected fun substitutedResultingDescriptor(
        inferredSubstitutor: ComposableTypeSubstitutor?
    ): CallableDescriptor {

        val candidate = candidateDescriptor

        // 1️⃣ 是否需要 explicit type argument 修正
        val explicitTypeArguments =
            resolvedCallAtom?.atom?.typeArguments
                ?.filterIsInstance<SimpleTypeArgument>()
                ?: emptyList()

        val finalSubstitutor =
            when {
                explicitTypeArguments.isNotEmpty() ->
                    getSubstitutorWithoutFlexibleTypes(
                        inferredSubstitutor,
                        explicitTypeArguments
                    )

                else -> inferredSubstitutor
            }

        // 2️⃣ 是否需要 approximate
        val shouldApproximate =
            when (candidate) {
                is FunctionDescriptor -> candidate.isNotSimpleCall()
                is PropertyDescriptor -> candidate.isNotSimpleCall()
                is VariableDescriptor -> candidate.isNotSimpleCall()
                else -> true
            }

        // 3️⃣ 统一走 substituteInferredVariablesAndApproximate（Composable 版本）
        return candidate.substituteInferredVariablesAndApproximate(
            finalSubstitutor,
            shouldApproximate
        )
    }

}
