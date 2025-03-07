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

package cn.cangnova.cangjie.ide.completion.handlers

import cn.cangnova.cangjie.builtins.getReceiverTypeFromFunctionType
import cn.cangnova.cangjie.builtins.getReturnTypeFromFunctionType
import cn.cangnova.cangjie.builtins.getValueParameterTypesFromFunctionType
import cn.cangnova.cangjie.builtins.isBuiltinFunctionalType
import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.ide.ExpectedInfo
import cn.cangnova.cangjie.ide.fuzzyType
import cn.cangnova.cangjie.resolve.calls.components.hasDefaultValue
import cn.cangnova.cangjie.resolve.calls.util.getValueParametersCountFromFunctionType
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.fuzzyReturnType
import cn.cangnova.cangjie.utils.CallType
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable

class InsertHandlerProvider(
    private val callType: CallType<*>,
    private val editor: Editor,
    expectedInfosCalculator: () -> Collection<ExpectedInfo>,
) {
    private val expectedInfos by lazy(LazyThreadSafetyMode.NONE) { expectedInfosCalculator() }

    companion object {
        fun isCangJieLambda(descriptor: DeclarationDescriptor, callType: CallType<*>): Boolean {
            if (descriptor is FunctionDescriptor) {
                if (listOf(CallType.DEFAULT, CallType.DOT, CallType.SAFE).contains(callType)) {
                    val parameters = descriptor.valueParameters
                    if (parameters.size == 1) {
                        val parameter = parameters.single()
                        val parameterType = parameter.type

                        if (parameterType.isBuiltinFunctionalType && getValueParametersCountFromFunctionType(
                                parameterType
                            ) <= 1 && !parameter.hasDefaultValue()
                        ) {
                            return true
                        }
                    }
                }
            }

            return false
        }
    }

    fun insertHandler(descriptor: DeclarationDescriptor, argumentsOnly: Boolean = false): InsertHandler<LookupElement> {
        return when (descriptor) {
            is FunctionDescriptor -> {
                when (callType) {
                    CallType.DEFAULT, CallType.DOT, CallType.SAFE, CallType.SUPER_MEMBERS -> {
                        if (!EditorSettingsExternalizable.getInstance().isInsertParenthesesAutomatically) {
                            return CangJieFunctionInsertHandler.OnlyName(callType)
                        }
                        val needTypeArguments = needTypeArguments(descriptor)
                        val parameters = descriptor.valueParameters
                        val functionName = descriptor.name
                        when (parameters.size) {
                            0 -> {
                                createNormalFunctionInsertHandler(
                                    editor, callType, functionName, needTypeArguments,
                                    inputValueArguments = false, argumentsOnly = argumentsOnly
                                )
                            }

                            1 -> {
                                if (callType != CallType.SUPER_MEMBERS) { // for super call we don't suggest to generate "super.foo { ... }" (seems to be non-typical use)
                                    val parameter = parameters.single()
                                    val parameterType = parameter.type
                                    if (parameterType.isBuiltinFunctionalType) {
                                        if (getValueParametersCountFromFunctionType(parameterType) <= 1 && !parameter.hasDefaultValue()) {
                                            // otherwise additional item with lambda template is to be added
                                            return CangJieFunctionInsertHandler.Normal(
                                                callType,
                                                needTypeArguments,
                                                inputValueArguments = false,
                                                lambdaInfo = GenerateLambdaInfo(parameterType, false),
                                                argumentsOnly = argumentsOnly
                                            )
                                        }
                                    }
                                }

                                createNormalFunctionInsertHandler(
                                    editor,
                                    callType,
                                    functionName,
                                    inputTypeArguments = needTypeArguments,
                                    inputValueArguments = true,
                                    argumentsOnly = argumentsOnly
                                )
                            }

                            else -> createNormalFunctionInsertHandler(
                                editor,
                                callType,
                                functionName,
                                needTypeArguments,
                                inputValueArguments = true,
                                argumentsOnly = argumentsOnly
                            )
                        }
                    }

                    else -> CangJieFunctionInsertHandler.OnlyName(callType)
                }

            }

            is PropertyDescriptor, is VariableDescriptor -> CangJiePropertyInsertHandler(callType)

            is ClassifierDescriptor -> CangJieClassifierInsertHandler

            else -> BaseDeclarationInsertHandler()
        }
    }

    fun needTypeArguments(function: FunctionDescriptor): Boolean {
        if (function.typeParameters.isEmpty()) return false

        val originalFunction = function.original
        val typeParameters = originalFunction.typeParameters

        val potentiallyInferred = HashSet<TypeParameterDescriptor>()

        /**
         * @param onlyCollectReturnTypeOfFunctionalType if true, then only the return type of functional type is considered inferred.
         * For example, in the following case:
         * ```
         * fun <T1, T2> T1.foo(handler: (T2) -> Boolean) {}
         *
         * fun f() {
         *     "".foo<String> { <caret> }
         * }
         * ```
         * we can't rely on the inference from `handler`, because lambda input types may not be inferred without explicit type arguments.
         */
        fun addPotentiallyInferred(type: CangJieType, onlyCollectReturnTypeOfFunctionalType: Boolean) {
            val descriptor = type.constructor.declarationDescriptor as? TypeParameterDescriptor
            if (descriptor != null && descriptor in typeParameters && descriptor !in potentiallyInferred) {
                potentiallyInferred.add(descriptor)
                // Add possible inferred by type-arguments of upper-bound of parameter
                // e.g. <T, C: Iterable<T>>, so T inferred from C
                descriptor.upperBounds.filter { it.arguments.isNotEmpty() }.forEach {
                    addPotentiallyInferred(it, onlyCollectReturnTypeOfFunctionalType = false)
                }
            }

            if (type.isBuiltinFunctionalType && getValueParametersCountFromFunctionType(type) <= 1) {
                val typesToProcess = if (onlyCollectReturnTypeOfFunctionalType) {
                    listOf(type.getReturnTypeFromFunctionType())
                } else {
                    listOfNotNull(type.getReceiverTypeFromFunctionType()) +
                            type.getReturnTypeFromFunctionType() +
                            type.getValueParameterTypesFromFunctionType().map { it.type }
                }
                typesToProcess.forEach { addPotentiallyInferred(it, onlyCollectReturnTypeOfFunctionalType) }
                return
            }

            for (argument in type.arguments) {

                addPotentiallyInferred(argument.type, onlyCollectReturnTypeOfFunctionalType)

            }
        }

        originalFunction.extensionReceiverParameter?.type?.let {
            addPotentiallyInferred(
                it,
                onlyCollectReturnTypeOfFunctionalType = false
            )
        }
        originalFunction.valueParameters.forEach {
            addPotentiallyInferred(
                it.type,
                onlyCollectReturnTypeOfFunctionalType = true
            )
        }

        fun allTypeParametersPotentiallyInferred() = originalFunction.typeParameters.all { it in potentiallyInferred }

        if (allTypeParametersPotentiallyInferred()) return false

        val returnType = originalFunction.returnType
        // check that there is an expected type and return value from the function can potentially match it
        if (returnType != null) {
            addPotentiallyInferred(returnType, onlyCollectReturnTypeOfFunctionalType = false)

            if (allTypeParametersPotentiallyInferred() && expectedInfos.any {
                    it.fuzzyType?.checkIsSuperTypeOf(originalFunction.fuzzyReturnType()!!) != null
                }
            ) {
                return false
            }
        }

        return true
    }
}
