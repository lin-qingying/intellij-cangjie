package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.tower.EnumClassCallableDescriptor
import com.huawei.cangjie.resolve.calls.tower.psiCangJieCall
import com.huawei.cangjie.resolve.scopes.receivers.EnumClassQualifier
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

            override fun iterator() =
                typeParameterToArgumentMap.mapValues { (it.value as? SimpleTypeArgument)?.type }.iterator()
        }
    }

    fun mapTypeArguments(call: CangJieCall, descriptor: CallableDescriptor): TypeArgumentsMapping {


        /**
         *  enum 奇怪的语法
         *   type arguments cannot appear after 'enum entry' when enum type 'enum' is given
         *   如果枚举类语句类型参数，则不允许枚举项使用类型参数
         */
//        if (descriptor is EnumClassCallableDescriptor) {
//            if (call.explicitReceiver != null && call.explicitReceiver!!.receiver is EnumClassQualifier) {
//                val enumClassQualifier = call.explicitReceiver!!.receiver as EnumClassQualifier
//                if (enumClassQualifier.referenceExpression.typeArguments.isNotEmpty() && call.psiCangJieCall.psiCall.typeArgumentList != null ) {
//                    (descriptor.type as? ClassDescriptor)?.let {
//                        return TypeArgumentsMapping.TypeArgumentsMappingImpl(
//
//                            listOf(TypeArgumentsAfterEnumEntry(it, enumClassQualifier.descriptor)),
//                            emptyMap()
//                        )
//                    }
//
//
//                } else if (call.typeArguments.isEmpty() && enumClassQualifier.referenceExpression.typeArguments.isNotEmpty()) {
////将类型参数传递
//                    enumClassQualifier.call?.typeArguments?.let { (call.typeArguments as ArrayList).addAll(it) }
//                } /*else if (call.typeArguments.isNotEmpty() && enumClassQualifier.referenceExpression.typeArguments.isEmpty()) {
//                    return TypeArgumentsMapping.TypeArgumentsMappingImpl(
//                        listOf(TypeArgumentsCompilerError),
//                        emptyMap()
//                    )
//                }*/
//            }/* else if (call.typeArguments.isNotEmpty() && call.explicitReceiver?.receiver is ClassQualifier &&
//                (call.explicitReceiver!!.receiver as ClassQualifier).descriptor.kind == ClassKind.ENUM
//            ) {
//                return TypeArgumentsMapping.TypeArgumentsMappingImpl(
//                    listOf(TypeArgumentsCompilerError),
//                    emptyMap()
//                )
//            }*/
//        }

        if (call.typeArguments.isEmpty() && call.topTypeArguments.isEmpty()) {
            return TypeArgumentsMapping.NoExplicitArguments
        }

//上层原子声明
        val topDescriptor = descriptor.containingDeclaration as? ClassDescriptor
        if (call.typeArguments.size != descriptor.typeParameters.size) {
            return TypeArgumentsMapping.TypeArgumentsMappingImpl(
                listOf(WrongCountOfTypeArguments(descriptor, call.typeArguments.size)), emptyMap()
            )
        }
//        if( topDescriptor != null &&  call.topTypeArguments.size != topDescriptor .declaredTypeParameters?.size){
//            return TypeArgumentsMapping.TypeArgumentsMappingImpl(
//                listOf(WrongCountOfTypeArguments(topDescriptor, call.typeArguments.size)), emptyMap()
//            )
//        }
        else {
            val topTypeParameterToArgumentMap = topDescriptor?.declaredTypeParameters?.zip(call.topTypeArguments)?.associate { it } ?: emptyMap()
            val typeParameterToArgumentMap =  descriptor.typeParameters .zip(call.typeArguments).associate { it } + topTypeParameterToArgumentMap
            return TypeArgumentsMapping.TypeArgumentsMappingImpl(listOf(), typeParameterToArgumentMap)
        }
    }

}

