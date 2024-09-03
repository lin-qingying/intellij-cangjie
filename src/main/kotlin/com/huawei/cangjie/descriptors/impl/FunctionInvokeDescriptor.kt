package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.annotations.Annotations

import com.huawei.cangjie.builtins.extractParameterNameFromFunctionTypeArgument
import com.huawei.cangjie.descriptors.*

import com.huawei.cangjie.descriptors.impl.FunctionDescriptorImpl
import com.huawei.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import com.huawei.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.types.TypeSubstitutor
import com.huawei.cangjie.types.Variance
import com.huawei.cangjie.utils.OperatorNameConventions

class FunctionInvokeDescriptor private constructor(
    container: DeclarationDescriptor,
    original: FunctionInvokeDescriptor?,
    callableKind: CallableMemberDescriptor.Kind,
//    isSuspend: Boolean
) : SimpleFunctionDescriptorImpl(
    container,
    original,
    Annotations.EMPTY,
    OperatorNameConventions.INVOKE,
    callableKind,
    SourceElement.NO_SOURCE
) {
    init {
        this.isOperator = true
//        this.isSuspend = isSuspend
        this.setHasStableParameterNames(false)
    }


    override fun doSubstitute(configuration: CopyConfiguration): FunctionDescriptor? {
        val substituted = super.doSubstitute(configuration) as FunctionInvokeDescriptor? ?: return null
        if (substituted.valueParameters.none { it.type.extractParameterNameFromFunctionTypeArgument() != null }) return substituted
        val parameterNames = substituted.valueParameters.map { it.type.extractParameterNameFromFunctionTypeArgument() }
        return substituted.replaceParameterNames(parameterNames)
    }

    override fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        original: FunctionDescriptor?,
        kind: CallableMemberDescriptor.Kind,
        newName: Name?,
        annotations: Annotations,
        source: SourceElement
    ): FunctionDescriptorImpl {
        return FunctionInvokeDescriptor(newOwner, original as FunctionInvokeDescriptor?, kind )
    }



    private fun replaceParameterNames(parameterNames: List<Name?>): FunctionDescriptor {
        val indexShift = valueParameters.size - parameterNames.size
        assert(indexShift == 0 || indexShift == 1) // indexShift == 1 for extension function type
        if (indexShift == 0 && parameterNames.zip(valueParameters).all { (name, parameter) -> name == parameter.name }) {
            return this
        }

        val newValueParameters = valueParameters.map {
            var newName = it.name
            val parameterIndex = it.index
            val nameIndex = parameterIndex - indexShift
            if (nameIndex >= 0) {
                val parameterName = parameterNames[nameIndex]
                if (parameterName != null) {
                    newName = parameterName
                }
            }
            it.copy(this, newName, parameterIndex)
        }

        val copyConfiguration = newCopyBuilder(TypeSubstitutor.EMPTY)
            .setHasSynthesizedParameterNames(parameterNames.any { it == null })
            .setValueParameters(newValueParameters)
            .setOriginal(original)

        return super.doSubstitute(copyConfiguration)!!
    }

    companion object Factory {
        fun create(functionClass: FunctionClassDescriptor): FunctionInvokeDescriptor {
            val typeParameters = functionClass.declaredTypeParameters

            val result = FunctionInvokeDescriptor(functionClass, null, CallableMemberDescriptor.Kind.DECLARATION)
            result.initialize(
                null,
                functionClass.thisAsReceiverParameter,
                listOf(), listOf(),
                listOf(),
//                typeParameters.takeWhile { it.variance == Variance.IN_VARIANCE }
//                    .withIndex()
//                    .map { createValueParameter(result, it.index, it.value) },
                typeParameters.last().defaultType,
                Modality.ABSTRACT,
                DescriptorVisibilities.PUBLIC
            )
            result.setHasSynthesizedParameterNames(true)
            return result
        }

        private fun createValueParameter(
            containingDeclaration: FunctionInvokeDescriptor,
            index: Int,
            typeParameter: TypeParameterDescriptor
        ): ValueParameterDescriptor {
            val name = when (val typeParameterName = typeParameter.name.asString()) {
                "T" -> "instance"
                "E" -> "receiver"
                else -> {
                    // Type parameter "P1" -> value parameter "p1", "P2" -> "p2", etc.
                    typeParameterName.lowercase()
                }
            }

            return ValueParameterDescriptorImpl(
                containingDeclaration, null, index,
                Annotations.EMPTY,
                Name.identifier(name),
                typeParameter.defaultType,
                declaresDefaultValue = false,

                SourceElement.NO_SOURCE
            )
        }
    }
}
