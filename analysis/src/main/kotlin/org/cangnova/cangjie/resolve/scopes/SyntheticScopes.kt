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

package org.cangnova.cangjie.resolve.scopes

import org.cangnova.cangjie.container.DefaultImplementation
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.scopes.synthetic.FunInterfaceConstructorsScopeProvider
import org.cangnova.cangjie.types.CangJieType

interface SyntheticScope {

    fun getSyntheticExtensionProperties(receiverTypes: Collection<CangJieType>, name: Name, location: LookupLocation): Collection<PropertyDescriptor>
    fun getSyntheticMemberFunctions(receiverTypes: Collection<CangJieType>, name: Name, location: LookupLocation): Collection<FunctionDescriptor>
    fun getSyntheticStaticFunctions(contributedFunctions: Collection<FunctionDescriptor>, location: LookupLocation): Collection<FunctionDescriptor>
    fun getSyntheticConstructors(contributedClassifier: ClassifierDescriptor, location: LookupLocation): Collection<FunctionDescriptor>

    fun getSyntheticExtensionProperties(receiverTypes: Collection<CangJieType>, location: LookupLocation): Collection<PropertyDescriptor>
    fun getSyntheticMemberFunctions(receiverTypes: Collection<CangJieType>): Collection<FunctionDescriptor>
    fun getSyntheticStaticFunctions(functionDescriptors: Collection<DeclarationDescriptor>): Collection<FunctionDescriptor>
    fun getSyntheticConstructors(classifierDescriptors: Collection<DeclarationDescriptor>): Collection<FunctionDescriptor>

    fun getSyntheticConstructor(constructor: ConstructorDescriptor): ConstructorDescriptor?

    open class Default : SyntheticScope {

        override fun getSyntheticExtensionProperties(
            receiverTypes: Collection<CangJieType>,
            name: Name,
            location: LookupLocation
        ): Collection<PropertyDescriptor> {
            return emptyList()
        }

        override fun getSyntheticMemberFunctions(
            receiverTypes: Collection<CangJieType>,
            name: Name,
            location: LookupLocation
        ): Collection<FunctionDescriptor> {
            return emptyList()
        }

        override fun getSyntheticStaticFunctions(
            contributedFunctions: Collection<FunctionDescriptor>,
            location: LookupLocation
        ): Collection<FunctionDescriptor> {
            return emptyList()
        }

        override fun getSyntheticConstructors(
            contributedClassifier: ClassifierDescriptor,
            location: LookupLocation
        ): Collection<FunctionDescriptor> {
            return emptyList()
        }

        override fun getSyntheticExtensionProperties(
            receiverTypes: Collection<CangJieType>,
            location: LookupLocation
        ): Collection<PropertyDescriptor> {
            return emptyList()
        }

        override fun getSyntheticMemberFunctions(receiverTypes: Collection<CangJieType>): Collection<FunctionDescriptor> {
            return emptyList()
        }

        override fun getSyntheticStaticFunctions(functionDescriptors: Collection<DeclarationDescriptor>): Collection<FunctionDescriptor> {
            return emptyList()
        }

        override fun getSyntheticConstructors(classifierDescriptors: Collection<DeclarationDescriptor>): Collection<FunctionDescriptor> {
            return emptyList()
        }

        override fun getSyntheticConstructor(constructor: ConstructorDescriptor): ConstructorDescriptor? {
            return null
        }
    }
}

@DefaultImplementation(impl = FunInterfaceConstructorsScopeProvider::class)
interface SyntheticScopes {
    val scopes: Collection<SyntheticScope>

    object Empty : SyntheticScopes {
        override val scopes: Collection<SyntheticScope> = emptyList()

    }
}
fun SyntheticScopes.collectSyntheticExtensionProperties(receiverTypes: Collection<CangJieType>, name: Name, location: LookupLocation)
        = scopes.flatMap { it.getSyntheticExtensionProperties(receiverTypes, name, location) }

fun SyntheticScopes.collectSyntheticConstructors(contributedClassifier: ClassifierDescriptor, location: LookupLocation)
        = scopes.flatMap { it.getSyntheticConstructors(contributedClassifier, location) }
fun SyntheticScopes.collectSyntheticConstructors(constructor: ConstructorDescriptor)
        = scopes.mapNotNull { it.getSyntheticConstructor(constructor) }
fun SyntheticScopes.collectSyntheticStaticFunctions(
    contributedFunctions: Collection<FunctionDescriptor>,
    location: LookupLocation
) = scopes.flatMap {
    it.getSyntheticStaticFunctions(contributedFunctions, location)
}
fun SyntheticScopes.collectSyntheticMemberFunctions(receiverTypes: Collection<CangJieType>, name: Name, location: LookupLocation)
        = scopes.flatMap { it.getSyntheticMemberFunctions(receiverTypes, name, location) }
fun SyntheticScopes.collectSyntheticStaticFunctions(functionDescriptors: Collection<DeclarationDescriptor>)
        = scopes.flatMap { it.getSyntheticStaticFunctions(functionDescriptors) }
fun SyntheticScopes.collectSyntheticConstructors(classifierDescriptors: Collection<DeclarationDescriptor>)
        = scopes.flatMap { it.getSyntheticConstructors(classifierDescriptors) }
fun SyntheticScopes.collectSyntheticExtensionProperties(receiverTypes: Collection<CangJieType>, location: LookupLocation)
        = scopes.flatMap { it.getSyntheticExtensionProperties(receiverTypes, location) }

fun SyntheticScopes.collectSyntheticMemberFunctions(receiverTypes: Collection<CangJieType>)
        = scopes.flatMap { it.getSyntheticMemberFunctions(receiverTypes) }

