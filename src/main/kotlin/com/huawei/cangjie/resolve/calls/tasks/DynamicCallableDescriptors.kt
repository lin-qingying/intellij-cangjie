package com.huawei.cangjie.resolve.calls.tasks


import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.builtins.createFunctionType
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.descriptors.impl.*
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.DescriptorFactory
import com.huawei.cangjie.resolve.descriptorUtil.builtIns
import com.huawei.cangjie.resolve.scopes.receivers.TransientReceiver
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.storage.getValue
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.utils.Printer
import java.util.*
import com.huawei.cangjie.resolve.source.MemberScopeImpl
import com.huawei.cangjie.utils.exceptions.OperatorConventions
import  com.huawei.cangjie.resolve.calls.util.isConventionCall
class DynamicCallableDescriptors(private val storageManager: StorageManager, builtIns: CangJieBuiltIns) {

    val dynamicType by storageManager.createLazyValue {
        createDynamicType(builtIns)
    }

    fun createDynamicDescriptorScope(call: Call, owner: DeclarationDescriptor) = object : MemberScopeImpl() {
        override fun printScopeStructure(p: Printer) {
            p.println(this::class.java.simpleName, ": dynamic candidates for " + call)
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
                val token = callee.getReferencedNameElementType()
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
                    outType = owner.builtIns.getArrayType(  dynamicType)
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
