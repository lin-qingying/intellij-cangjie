package com.linqingying.cangjie.resolve.scopes

import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.scopes.synthetic.FunInterfaceConstructorsScopeProvider
import com.linqingying.cangjie.types.CangJieType

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

