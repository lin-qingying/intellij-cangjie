package com.huawei.cangjie.resolve.descriptorUtil

import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.ClassKind.ENUM_ENTRY
import com.huawei.cangjie.descriptors.ClassifierDescriptorWithTypeParameters
import com.huawei.cangjie.descriptors.TypeAliasDescriptor
import com.huawei.cangjie.descriptors.VariableDescriptor
import com.huawei.cangjie.types.CangJieType

val VariableDescriptor.isUnderscoreNamed
    get() = !name.isSpecial && name.identifier == "_"

val ClassifierDescriptorWithTypeParameters.denotedClassDescriptor: ClassDescriptor?
    get() = when (this) {
        is ClassDescriptor -> this
        is TypeAliasDescriptor -> classDescriptor
        else -> throw UnsupportedOperationException("Unexpected descriptor kind: $this")
    }

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
