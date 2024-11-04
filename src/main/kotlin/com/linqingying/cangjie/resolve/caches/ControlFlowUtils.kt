package com.linqingying.cangjie.resolve.caches

import com.linqingying.cangjie.resolve.controlFlow.ControlFlowInformationProviderImpl
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.ide.projectStructure.languageVersionSettings
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.psi.NotNullablePsiCopyableUserDataProperty
import com.linqingying.cangjie.resolve.DelegatingBindingTrace
import com.linqingying.cangjie.resolve.lazy.ResolveSession
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
