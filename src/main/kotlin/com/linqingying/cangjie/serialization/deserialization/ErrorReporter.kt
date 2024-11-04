package com.linqingying.cangjie.serialization.deserialization

import com.linqingying.cangjie.descriptors.CallableMemberDescriptor
import com.linqingying.cangjie.descriptors.ClassDescriptor

interface ErrorReporter {
    fun reportIncompleteHierarchy(
        descriptor:  ClassDescriptor,
        unresolvedSuperClasses: List<String >
    )

    fun reportCannotInferVisibility(descriptor: CallableMemberDescriptor)

    companion object {
        val DO_NOTHING: ErrorReporter = object : ErrorReporter {
            override fun reportIncompleteHierarchy(
                descriptor: ClassDescriptor,
                unresolvedSuperClasses: List<String >
            ) {
            }

            override fun reportCannotInferVisibility(descriptor:  CallableMemberDescriptor) {
            }
        }
    }
}
