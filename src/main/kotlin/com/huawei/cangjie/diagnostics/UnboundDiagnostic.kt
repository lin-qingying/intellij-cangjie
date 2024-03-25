package com.huawei.cangjie.diagnostics

import com.huawei.cangjie.descriptors.DiagnosticFactory
import com.intellij.openapi.util.TextRange


interface UnboundDiagnostic {
    val factory: DiagnosticFactory<*>
    val severity: Severity
    val textRanges: List<TextRange>
    val isValid: Boolean
}