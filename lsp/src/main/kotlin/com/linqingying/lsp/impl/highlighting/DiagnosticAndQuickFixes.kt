package com.linqingying.lsp.impl.highlighting

import com.intellij.codeInsight.intention.IntentionAction
import org.eclipse.lsp4j.Diagnostic

class DiagnosticAndQuickFixes(val diagnostic: Diagnostic, val quickFixes: List<IntentionAction>) {


    operator fun component1(): Diagnostic =
        diagnostic


    operator fun component2(): List<IntentionAction> = quickFixes
    override operator fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DiagnosticAndQuickFixes) return false

        if (diagnostic != other.diagnostic) return false
        if (quickFixes != other.quickFixes) return false

        return true
    }


    //    fun copy(diagnostic: Diagnostic,  quickFixes: List<IntentionAction>): DiagnosticAndQuickFixes {
//        return DiagnosticAndQuickFixes(diagnostic, quickFixes)
//    }
    override fun hashCode(): Int {
        var result = diagnostic.hashCode()
        result = 31 * result + quickFixes.hashCode()
        return result
    }

    override fun toString(): String = "DiagnosticAndQuickFixes(diagnostic=$diagnostic, quickFixes=$quickFixes)"
}
