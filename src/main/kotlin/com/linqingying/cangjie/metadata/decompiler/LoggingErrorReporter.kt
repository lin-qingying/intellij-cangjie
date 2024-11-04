package com.linqingying.cangjie.metadata.decompiler

import com.intellij.openapi.diagnostic.Logger
import com.linqingying.cangjie.descriptors.CallableMemberDescriptor
import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.serialization.deserialization.ErrorReporter

class LoggingErrorReporter(private val log: Logger) : ErrorReporter {
    override fun reportIncompleteHierarchy(descriptor: ClassDescriptor, unresolvedSuperClasses: List<String>) {
        // This is absolutely fine for the decompiler
    }

    override fun reportCannotInferVisibility(descriptor: CallableMemberDescriptor) {
        log.error("Could not infer visibility for $descriptor")
    }
}
