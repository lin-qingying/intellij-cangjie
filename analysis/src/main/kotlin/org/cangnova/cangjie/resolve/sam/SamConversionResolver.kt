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

package org.cangnova.cangjie.resolve.sam


import org.cangnova.cangjie.container.DefaultImplementation
import org.cangnova.cangjie.container.PlatformSpecificExtension
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.descriptorUtil.builtIns
import org.cangnova.cangjie.resolve.descriptorUtil.fqNameSafe
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.SimpleType
import org.cangnova.cangjie.types.createFunctionType

interface SamWithReceiverResolver {
    fun shouldConvertFirstSamParameterToReceiver(function: FunctionDescriptor): Boolean
}

val SAM_LOOKUP_NAME = Name.special("<SAM-CONSTRUCTOR>")

@DefaultImplementation(impl = SamConversionResolver.Empty::class)
//@DefaultImplementation(impl = SamConversionResolverImpl.SamConversionResolverWithoutReceiverConversion::class)
interface SamConversionResolver : PlatformSpecificExtension<SamConversionResolver> {
    object Empty : SamConversionResolver {
        override fun resolveFunctionTypeIfSamInterface(classDescriptor: ClassDescriptor): SimpleType? = null
    }

    fun resolveFunctionTypeIfSamInterface(classDescriptor: ClassDescriptor): SimpleType?
}

class SamConversionResolverImpl(
    storageManager: StorageManager,
    private val samWithReceiverResolvers: Iterable<SamWithReceiverResolver>
) : SamConversionResolver {
    class SamConversionResolverWithoutReceiverConversion(storageManager: StorageManager) : SamConversionResolver {
        val resolver = SamConversionResolverImpl(storageManager, emptyList())

        override fun resolveFunctionTypeIfSamInterface(classDescriptor: ClassDescriptor): SimpleType? {
            return resolver.resolveFunctionTypeIfSamInterface(classDescriptor)
        }
    }

    private val functionTypesForSamInterfaces =
        storageManager.createCacheWithNullableValues<ClassDescriptor, SimpleType>()

    override fun resolveFunctionTypeIfSamInterface(classDescriptor: ClassDescriptor): SimpleType? {
        return functionTypesForSamInterfaces.computeIfAbsent(classDescriptor) {
            val abstractMethod = getSingleAbstractMethodOrNull(classDescriptor) ?: return@computeIfAbsent null
            val shouldConvertFirstParameterToDescriptor =
                samWithReceiverResolvers.any { it.shouldConvertFirstSamParameterToReceiver(abstractMethod) }
            getFunctionTypeForAbstractMethod(abstractMethod, shouldConvertFirstParameterToDescriptor)
        }
    }
}

fun getSingleAbstractMethodOrNull(klass: ClassDescriptor): FunctionDescriptor? {
    // NB: this check MUST BE at start. Please do not touch until following to-do is resolved
    // Otherwise android data binding can cause resolve re-entrance

    // TODO: prevent resolve re-entrance on architecture level, or (alternatively) ask data binding owners not to do it
    if (klass.fqNameSafe.asString().endsWith(".databinding.DataBindingComponent")) return null

    if (klass.isDefinitelyNotSamInterface) return null

    val abstractMember = getAbstractMembers(klass).singleOrNull() ?: return null

    return if (abstractMember is SimpleFunctionDescriptor && abstractMember.typeParameters.isEmpty())
        abstractMember
    else
        null
}

@Suppress("UNCHECKED_CAST")
fun getAbstractMembers(classDescriptor: ClassDescriptor): List<CallableMemberDescriptor> {
    return DescriptorUtils
        .getAllDescriptors(classDescriptor.unsubstitutedMemberScope)
        .filter { it is CallableMemberDescriptor && it.modality == Modality.ABSTRACT } as List<CallableMemberDescriptor>
}

fun getFunctionTypeForAbstractMethod(
    function: FunctionDescriptor,
    shouldConvertFirstParameterToDescriptor: Boolean
): SimpleType {
    val returnType = function.returnType ?: error("function is not initialized: $function")
    val valueParameters = function.valueParameters

    val parameterTypes = ArrayList<CangJieType>(valueParameters.size)
    val parameterNames = ArrayList<Name>(valueParameters.size)

    val contextReceiversTypes = function.contextReceiverParameters.map { it.type }
    var startIndex = 0
    var receiverType: CangJieType? = null
    val extensionReceiver = function.extensionReceiverParameter
    if (extensionReceiver != null) {
        receiverType = extensionReceiver.type
    } else if (shouldConvertFirstParameterToDescriptor && function.valueParameters.isNotEmpty()) {
        receiverType = valueParameters[0].type
        startIndex = 1
    }

    for (i in startIndex until valueParameters.size) {
        val parameter = valueParameters[i]
        parameterTypes.add(parameter.type)
        parameterNames.add(if (function.hasSynthesizedParameterNames()) SpecialNames.NO_NAME_PROVIDED else parameter.name)
    }

    return createFunctionType(
        function.builtIns, Annotations.EMPTY, receiverType, contextReceiversTypes, parameterTypes,
        parameterNames, returnType
    )
}
