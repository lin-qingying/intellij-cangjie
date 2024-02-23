package com.huawei.cangjie.descriptors

import com.intellij.openapi.util.TextRange


interface UnboundDiagnostic {
    val factory: DiagnosticFactory<*>
    val severity: Severity
    val textRanges: List<TextRange>
    val isValid: Boolean
}