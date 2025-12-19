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

package org.cangnova.cangjie.resolve.calls.tasks


import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.*
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorConventions
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.DescriptorFactory
import org.cangnova.cangjie.resolve.builtIns
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.storage.getValue
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.utils.Printer
import java.util.*
import org.cangnova.cangjie.resolve.source.MemberScopeImpl
import  org.cangnova.cangjie.resolve.calls.util.isConventionCall
import org.cangnova.cangjie.resolve.scopes.receivers.TransientReceiver

class DynamicCallableDescriptors(private val storageManager: StorageManager, builtIns: CangJieBuiltIns) {

    val dynamicType by storageManager.createLazyValue {
        createDynamicType(builtIns)
    }

    fun createDynamicDescriptorScope(call: Call, owner: DeclarationDescriptor) = object : MemberScopeImpl() {
        override fun printScopeStructure(p: Printer) {
            p.println(this::class.java.simpleName, ": dynamic candidates for $call")
        }

        override fun getContributedFunctions(
            name: Name,
            location: LookupLocation
        ): Collection<SimpleFunctionDescriptor> {
            if (isAugmentedAssignmentConvention(name)) return listOf()
            if (call.callType == Call.CallType.INVOKE
                && call.valueArgumentList == null && call.functionLiteralArguments.isEmpty()
            ) {
                // this means that we are looking for "imaginary" invokes,
                // e.g. in `+d` we are looking for property "plus" with member "invoke"
                return listOf()
            }
            return listOf(createDynamicFunction(owner, name, call))
        }

        /*
         * Detects the case when name "plusAssign" is requested for "+=" call,
         * since both "plus" and "plusAssign" are resolvable on dynamic receivers,
         * we have to prefer ne of them, and prefer "plusAssign" for generality:
         * it may be called even on a val
         */
        private fun isAugmentedAssignmentConvention(name: Name): Boolean {
            val callee = call.calleeExpression
            if (callee is CjOperationReferenceExpression) {
                val token = callee.referencedNameElementType
                if (token in CjTokens.AUGMENTED_ASSIGNMENTS && OperatorConventions.ASSIGNMENT_OPERATIONS[token] != name) {
                    return true
                }
            }
            return false
        }

        override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
            return if (call.valueArgumentList == null && call.valueArguments.isEmpty()) {
                listOf(createDynamicVariable(owner, name, call))
            } else listOf()
        }
    }

    private fun createDynamicVariable(owner: DeclarationDescriptor, name: Name, call: Call): VariableDescriptorImpl {
        val variableDescriptor = VariableDescriptorImpl.create(
            owner,
            name,

            DescriptorVisibilities.PUBLIC,
            true,

            SourceElement.NO_SOURCE,

            )
        variableDescriptor.setType(
            dynamicType,
            createTypeParameters(variableDescriptor, call),
            createDynamicDispatchReceiverParameter(variableDescriptor),
            null,
            emptyList()
        )


        return variableDescriptor
    }

    private fun createDynamicProperty(owner: DeclarationDescriptor, name: Name, call: Call): PropertyDescriptorImpl {
        val propertyDescriptor = PropertyDescriptorImpl.create(
            owner,
            Annotations.EMPTY,
            Modality.FINAL,
            DescriptorVisibilities.PUBLIC,
            true,
            name,
            CallableMemberDescriptor.Kind.DECLARATION,
            SourceElement.NO_SOURCE,
//            /* lateInit = */ false,
//            /* isConst = */ false,
//            /* isExpect = */ false,
//            /* isActual = */ false,
//            /* isExternal = */ false,
//            /* isDelegated = */ false
        )
        propertyDescriptor.setType(
            dynamicType,
            createTypeParameters(propertyDescriptor, call),
            createDynamicDispatchReceiverParameter(propertyDescriptor),
            null,
            emptyList()
        )

//        val getter = DescriptorFactory.createDefaultGetter(propertyDescriptor, Annotations.EMPTY)
//        getter.initialize(propertyDescriptor.type)
//        val setter = DescriptorFactory.createDefaultSetter(propertyDescriptor, Annotations.EMPTY, Annotations.EMPTY)
//
//        propertyDescriptor.initialize(getter, setter)

        return propertyDescriptor
    }

    private fun createDynamicFunction(
        owner: DeclarationDescriptor,
        name: Name,
        call: Call
    ): SimpleFunctionDescriptorImpl {
        val functionDescriptor = SimpleFunctionDescriptorImpl.create(
            owner,
            Annotations.EMPTY,
            name,
            CallableMemberDescriptor.Kind.DECLARATION,
            SourceElement.NO_SOURCE
        )
        functionDescriptor.initialize(
            null,
            createDynamicDispatchReceiverParameter(functionDescriptor),
            emptyList(),
            createTypeParameters(functionDescriptor, call),
            createValueParameters(functionDescriptor, call),
            dynamicType,
            Modality.FINAL,
            DescriptorVisibilities.PUBLIC
        )
        functionDescriptor.setHasSynthesizedParameterNames(true)
        functionDescriptor.isOperator = isConventionCall(call)
        return functionDescriptor
    }

    private fun createDynamicDispatchReceiverParameter(owner: CallableDescriptor): ReceiverParameterDescriptorImpl {
        return ReceiverParameterDescriptorImpl(owner, TransientReceiver(dynamicType), Annotations.EMPTY)
    }

    private fun createTypeParameters(owner: DeclarationDescriptor, call: Call): List<TypeParameterDescriptor> =
        call.typeArguments.indices.map { index
            ->
            TypeParameterDescriptorImpl.createWithDefaultBound(
                owner,
                Annotations.EMPTY,

                Variance.INVARIANT,
                Name.identifier("T$index"),
                index,
                storageManager
            )
        }

    private fun createValueParameters(owner: FunctionDescriptor, call: Call): List<ValueParameterDescriptor> {
        val parameters = ArrayList<ValueParameterDescriptor>()

        fun addParameter(arg: ValueArgument, outType: CangJieType, varargElementType: CangJieType?) {
            val index = parameters.size

            parameters.add(
                ValueParameterDescriptorImpl(
                    owner,
                    null,
                    index,
                    Annotations.EMPTY,
                    arg.getArgumentName()?.asName ?: Name.identifier("p$index"),
                    arg.isNamed(),

                    outType,
                    /* declaresDefaultValue = */ false,
//                    /* isCrossinline = */ false,
//                    /* isNoinline = */ false,

                    SourceElement.NO_SOURCE
                )
            )
        }

        fun getFunctionType(funLiteralExpr: CjLambdaExpression): CangJieType {
            val funLiteral = funLiteralExpr.functionLiteral

            val receiverType = funLiteral.receiverTypeReference?.let { dynamicType }
            val contextReceiversTypes = funLiteral.contextReceivers.map { dynamicType }

            val parameterTypes = funLiteral.valueParameters.map { dynamicType }

            return createFunctionType(
                owner.builtIns,
                Annotations.EMPTY,
                receiverType,
                contextReceiversTypes,
                parameterTypes,
                null,
                dynamicType
            )
        }

        for (arg in call.valueArguments) {
            val outType: CangJieType
            val varargElementType: CangJieType?
            var hasSpreadOperator = false

            val argExpression = CjPsiUtil.deparenthesize(arg.getArgumentExpression())

            when {
                argExpression is CjLambdaExpression -> {
                    outType = getFunctionType(argExpression)
                    varargElementType = null
                }

                arg.getSpreadElement() != null -> {
                    hasSpreadOperator = true
                    outType = owner.builtIns.stdlibTypes.getArrayType(dynamicType)
                    varargElementType = dynamicType
                }

                else -> {
                    outType = dynamicType
                    varargElementType = null
                }
            }

            addParameter(arg, outType, varargElementType)

            if (hasSpreadOperator) {
                for (funLiteralArg in call.functionLiteralArguments) {
                    addParameter(
                        funLiteralArg,
                        funLiteralArg.getLambdaExpression()?.let { getFunctionType(it) }
                            ?: TypeUtils.CANNOT_INFER_FUNCTION_PARAM_TYPE,
                        null)
                }

                break
            }
        }

        return parameters
    }
}

fun DeclarationDescriptor.isDynamic(): Boolean {
    if (this !is CallableDescriptor) return false
    val dispatchReceiverParameter = dispatchReceiverParameter
    return dispatchReceiverParameter != null && dispatchReceiverParameter.type.isDynamic()
}
