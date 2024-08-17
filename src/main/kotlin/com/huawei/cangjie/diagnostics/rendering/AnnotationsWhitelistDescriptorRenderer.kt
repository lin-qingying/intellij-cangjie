package com.huawei.cangjie.diagnostics.rendering

import com.huawei.cangjie.descriptors.DeclarationDescriptor

data class DeclarationWithDiagnosticComponents(
    val declaration: DeclarationDescriptor,
//    val diagnosticComponents: PlatformSpecificDiagnosticComponents
) : Iterable<Any> {
    override fun iterator() =
        sequenceOf(declaration/*, diagnosticComponents*/).iterator()
}
