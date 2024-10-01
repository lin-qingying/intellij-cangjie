package com.huawei.cangjie.descriptors

import com.huawei.cangjie.descriptors.impl.PropertyAccessorDescriptor


interface PropertyDescriptorWithAccessors : VariableDescriptor {
    val getter: PropertyAccessorDescriptor?

    val setter: PropertyAccessorDescriptor?

    /**
     * Please be careful with this method. Depending on the fact that a property is delegated may be dangerous in the compiler.
     * Whether or not a property is delegated is neither the API or the ABI of that property, and one should be able to recompile a library
     * in a way that makes some non-delegated properties delegated or vice versa, without any problems at compilation time or at runtime.
     * So this method should not be used in the compiler frontend in a way that would significantly alter the compilation result.
     *
     * This flag is needed for reflection however, that's why it's serialized to metadata and is exposed in this interface.
     */
//    val isDelegated: Boolean
}

//val PropertyDescriptorWithAccessors.accessors: List<VariableAccessorDescriptor>
//    get() = listOfNotNull(getter, setter)
