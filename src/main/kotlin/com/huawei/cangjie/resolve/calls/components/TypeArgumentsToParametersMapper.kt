package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.types.CangJieType


class TypeArgumentsToParametersMapper {

    sealed class TypeArgumentsMapping(val diagnostics: List<CangJieCallDiagnostic>) :
        Iterable<Map.Entry<TypeParameterDescriptor, CangJieType?>> {

        abstract fun getTypeArgument(typeParameterDescriptor: TypeParameterDescriptor): TypeArgument

        object NoExplicitArguments : TypeArgumentsMapping(emptyList()) {
            private val emptyIterator = mapOf<Nothing, Nothing>().iterator()

            override fun getTypeArgument(typeParameterDescriptor: TypeParameterDescriptor): TypeArgument =
                TypeArgumentPlaceholder

            override fun iterator() = emptyIterator
        }

        class TypeArgumentsMappingImpl(
            diagnostics: List<CangJieCallDiagnostic>,
            private val typeParameterToArgumentMap: Map<TypeParameterDescriptor, TypeArgument>
        ) : TypeArgumentsMapping(diagnostics) {
            override fun getTypeArgument(typeParameterDescriptor: TypeParameterDescriptor): TypeArgument =
                typeParameterToArgumentMap[typeParameterDescriptor] ?: TypeArgumentPlaceholder

            override fun iterator() = typeParameterToArgumentMap.mapValues { (it.value as? SimpleTypeArgument)?.type }.iterator()
        }
    }

    fun mapTypeArguments(call: CangJieCall, descriptor: CallableDescriptor): TypeArgumentsMapping {
        if (call.typeArguments.isEmpty()) {
            return TypeArgumentsMapping.NoExplicitArguments
        }

        if (call.typeArguments.size != descriptor.typeParameters.size) {
            return TypeArgumentsMapping.TypeArgumentsMappingImpl(
                listOf(WrongCountOfTypeArguments(descriptor, call.typeArguments.size)), emptyMap()
            )
        } else {
            val typeParameterToArgumentMap = descriptor.typeParameters.zip(call.typeArguments).associate { it }
            return TypeArgumentsMapping.TypeArgumentsMappingImpl(listOf(), typeParameterToArgumentMap)
        }
    }

}

