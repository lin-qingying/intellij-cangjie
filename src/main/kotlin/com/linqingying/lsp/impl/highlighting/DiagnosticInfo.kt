package com.linqingying.lsp.impl.highlighting

import com.linqingying.lsp.api.customization.LspDiagnosticsSupport
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.util.TextRange
import org.eclipse.lsp4j.Diagnostic

class DiagnosticInfo(
    val diagnosticsSupport: LspDiagnosticsSupport,
    val diagnostic: Diagnostic,
    val textRange: TextRange,
    val quickFixes: List<IntentionAction>
) {


    override operator fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DiagnosticInfo) return false

        if (diagnosticsSupport != other.diagnosticsSupport) return false
        if (diagnostic != other.diagnostic) return false
        if (textRange != other.textRange) return false
        if (quickFixes != other.quickFixes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = diagnosticsSupport.hashCode()
        result = 31 * result + diagnostic.hashCode()
        result = 31 * result + textRange.hashCode()
        result = 31 * result + quickFixes.hashCode()
        return result
    }

    override fun toString(): String =
        "DiagnosticInfo(diagnosticsSupport=$diagnosticsSupport, diagnostic=$diagnostic, textRange=$textRange, quickFixes=$quickFixes)"
}
