package com.linqingying.cangjie.resolve.calls.components

import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor
import com.linqingying.cangjie.resolve.calls.model.*
import com.linqingying.cangjie.resolve.calls.tower.EnumClassCallableDescriptor
import com.linqingying.cangjie.types.CangJieType


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


        if (call.typeArguments.isEmpty() && call.topTypeArguments.isEmpty()) {
            return TypeArgumentsMapping.NoExplicitArguments
        }


        val typeDiagnostics = mutableListOf<CangJieCallDiagnostic>()
        var typeParameterToArgumentMap = emptyMap<TypeParameterDescriptor, TypeArgument>()
//上层原子声明
        val topDescriptor = descriptor.containingDeclaration as? ClassDescriptor


        val sharedTypeParameter = getSharedTypeParametersByDeclarationDescriptor(topDescriptor, descriptor)

        if (descriptor is EnumClassCallableDescriptor && sharedTypeParameter.isNotEmpty()) {
            if (call.typeArguments.isNotEmpty() && call.topTypeArguments.isNotEmpty()) {
                topDescriptor?.let {
                    typeDiagnostics.add(TypeArgumentsAfterEnumEntry(descriptor.type as ClassDescriptor, it))
                }
            }
        }

        if (call.typeArguments.size != descriptor.typeParameters.size) {


            if (descriptor is EnumClassCallableDescriptor && sharedTypeParameter.isNotEmpty()) {
                /*   if (call.typeArguments.isNotEmpty() && call.topTypeArguments.isNotEmpty()) {
   //type arguments cannot appear after 'B' when enum type 'A' is given

   //                    return TypeArgumentsMapping.TypeArgumentsMappingImpl(
   //                        topDescriptor?.let {
   //                            listOf(TypeArgumentsAfterEnumEntry(descriptor.type as ClassDescriptor, it))
   //                        } ?: emptyList(),
   //
   //                        emptyMap()
   //                    )
                       topDescriptor?.let {
                           typeDiagnostics.add(TypeArgumentsAfterEnumEntry(descriptor.type as ClassDescriptor, it))

                       }
                   } else */
                if (call.topTypeArguments.size != topDescriptor?.declaredTypeParameters?.size) {
//                    return TypeArgumentsMapping.TypeArgumentsMappingImpl(
//                        listOf(WrongCountOfTypeArguments(descriptor, call.topTypeArguments.size)), emptyMap()
//                    )
                    typeDiagnostics.add(WrongCountOfTypeArguments(descriptor, call.topTypeArguments.size))
                } else if (call.topTypeArguments.isEmpty() && call.typeArguments.size != descriptor.typeParameters.size) {

                    typeDiagnostics.add(WrongCountOfTypeArguments(descriptor, call.typeArguments.size))
//                    return TypeArgumentsMapping.TypeArgumentsMappingImpl(
//                        listOf(WrongCountOfTypeArguments(descriptor, call.typeArguments.size)), emptyMap()
//                    )
                } else {
                    val topTypeParameterToArgumentMap =
                        topDescriptor.declaredTypeParameters.zip(call.topTypeArguments).associate { it }
                    typeParameterToArgumentMap =
                        descriptor.typeParameters.zip(call.typeArguments)
                            .associate { it } + topTypeParameterToArgumentMap
//                    return TypeArgumentsMapping.TypeArgumentsMappingImpl(listOf(), typeParameterToArgumentMap)
                }

            } else {
                typeDiagnostics.add(WrongCountOfTypeArguments(descriptor, call.typeArguments.size))
//                return TypeArgumentsMapping.TypeArgumentsMappingImpl(
//                    listOf(WrongCountOfTypeArguments(descriptor, call.typeArguments.size)), emptyMap()
//                )
            }


        } else {


            val topTypeParameterToArgumentMap =
                topDescriptor?.declaredTypeParameters?.zip(call.topTypeArguments)?.associate { it } ?: emptyMap()


//            return TypeArgumentsMapping.TypeArgumentsMappingImpl(listOf(), typeParameterToArgumentMap)

            typeParameterToArgumentMap =
                descriptor.typeParameters.zip(call.typeArguments).associate { it } + topTypeParameterToArgumentMap
        }

        return TypeArgumentsMapping.TypeArgumentsMappingImpl(typeDiagnostics, typeParameterToArgumentMap)

    }

}

/**
 * 获取共享的类型参数
 *
 * 例如
 * enum e<T>{
 *  entry(T)  //该枚举值附带了类型参数  所以是共享的类型参数
 *  }
 */
fun getSharedTypeParametersByDeclarationDescriptor(vararg descriptor: DeclarationDescriptor?): Set<TypeParameterDescriptor> {

//    获取所有类型参数
    return descriptor.filterNotNull().flatMap {


        when (it) {
            is ClassDescriptor -> it.declaredTypeParameters
            is CallableDescriptor -> it.typeParameters
            else -> emptyList()
        }
    }.groupBy { it }.filter {
        it.value.size > 1
    }.keys
}

//fun getSharedTypeParametersByTypeArgument(vararg typeArguments: TypeArgument): Set<TypeParameterDescriptor> {
//
//}

fun getAllTypeParameter(vararg descriptor: DeclarationDescriptor?): Set<TypeParameterDescriptor> {
    return descriptor.filterNotNull().flatMap {


        when (it) {
            is ClassDescriptor -> it.declaredTypeParameters
            is CallableDescriptor -> it.typeParameters
            else -> emptyList()
        }
    }.groupBy { it }.keys

}
