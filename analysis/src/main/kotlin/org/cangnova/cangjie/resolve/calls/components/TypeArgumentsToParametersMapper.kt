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

package org.cangnova.cangjie.resolve.calls.components

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.tower.EnumClassCallableDescriptor
import org.cangnova.cangjie.types.CangJieType


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
