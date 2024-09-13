package com.huawei.cangjie.resolve.caches

import com.huawei.cangjie.resolve.controlFlow.ControlFlowInformationProviderImpl
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.ide.projectStructure.languageVersionSettings
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.NotNullablePsiCopyableUserDataProperty
import com.huawei.cangjie.resolve.DelegatingBindingTrace
import com.huawei.cangjie.resolve.lazy.ResolveSession
import com.intellij.openapi.util.Key

fun analyzeControlFlow(resolveSession: ResolveSession, resolveElement: CjElement, trace: BindingTrace) {
    val controlFlowTrace = DelegatingBindingTrace(
        trace.bindingContext, "Element control flow resolve", resolveElement, allowSliceRewrite = true
    )
    ControlFlowInformationProviderImpl(
        resolveElement, controlFlowTrace, resolveElement.languageVersionSettings,/* resolveSession.platformDiagnosticSuppressor*/
    ) .checkDeclaration()
    controlFlowTrace.addOwnDataTo(trace, filter = null, commitDiagnostics = resolveElement.getContainingCjFile().reportControlFlowDiagnostics )
}
var CjFile.reportControlFlowDiagnostics : Boolean by NotNullablePsiCopyableUserDataProperty(Key.create("REPORT_CONTROL_FLOW_DIAGNOSTICS_IN_K1"), false)
