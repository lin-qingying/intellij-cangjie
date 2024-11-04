package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.descriptors.CallableMemberDescriptor
import com.linqingying.cangjie.descriptors.DescriptorVisibilities

fun findMemberWithMaxVisibility(descriptors: Collection<CallableMemberDescriptor>): CallableMemberDescriptor {
    assert(descriptors.isNotEmpty())

    var descriptor: CallableMemberDescriptor? = null
    for (candidate in descriptors) {
        if (descriptor == null) {
            descriptor = candidate
            continue
        }

        val result = DescriptorVisibilities.compare(descriptor.visibility, candidate.visibility)
        if (result != null && result < 0) {
            descriptor = candidate
        }
    }
    return descriptor!!
}

