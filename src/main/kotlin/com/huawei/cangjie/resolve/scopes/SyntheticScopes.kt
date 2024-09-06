package com.huawei.cangjie.resolve.scopes

import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.scopes.synthetic.FunInterfaceConstructorsScopeProvider
import com.huawei.cangjie.types.CangJieType

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
