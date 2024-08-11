package com.huawei.cangjie.resolve.descriptorUtil

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.ClassKind.ENUM_ENTRY
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.StubTypeForBuilderInference
import com.huawei.cangjie.types.isError
import com.huawei.cangjie.types.util.contains

val VariableDescriptor.isUnderscoreNamed
    get() = !name.isSpecial && name.identifier == "_"

val ClassifierDescriptorWithTypeParameters.denotedClassDescriptor: ClassDescriptor?
    get() = when (this) {
        is ClassDescriptor -> this
        is TypeAliasDescriptor -> classDescriptor
        else -> throw UnsupportedOperationException("Unexpected descriptor kind: $this")
    }
private fun <D : CallableDescriptor> D.containsStubTypes() =
    valueParameters.any { parameter -> parameter.type.contains { it is StubTypeForBuilderInference } }
            || returnType?.contains { it is StubTypeForBuilderInference } == true
            || dispatchReceiverParameter?.type?.contains { it is StubTypeForBuilderInference } == true
            || extensionReceiverParameter?.type?.contains { it is StubTypeForBuilderInference } == true

fun <D : CallableDescriptor> D.shouldBeSubstituteWithStubTypes() =
    valueParameters.none { it.type.isError }
            && returnType?.isError != true
            && dispatchReceiverParameter?.type?.isError != true
            && extensionReceiverParameter?.type?.isError != true
            && containsStubTypes()

val ClassifierDescriptorWithTypeParameters.classValueTypeDescriptor: ClassDescriptor?
    get() = denotedClassDescriptor?.let {
        when (it.kind) {
//            OBJECT -> it
            ENUM_ENTRY -> {
                // enum entry has the type of enum class
                val container = this.containingDeclaration
                assert(container is ClassDescriptor /*&& container.kind == ENUM_CLASS*/)
                container as ClassDescriptor
            }

            else -> it
//            else -> it.companionObjectDescriptor
        }
    }


/** If a literal of this class can be used as a value, returns the type of this value */
val ClassDescriptor.classValueType: CangJieType?
    get() = classValueTypeDescriptor?.defaultType
