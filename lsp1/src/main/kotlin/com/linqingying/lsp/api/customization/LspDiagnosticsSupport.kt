package com.linqingying.lsp.api.customization
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.TextRange
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.DiagnosticTag
import org.jetbrains.annotations.ApiStatus


/**
 * Handles [Diagnostic](https://microsoft.github.io/language-server-protocol/specification#diagnostic) objects received from the LSP server.
 */
@ApiStatus.Experimental
open class LspDiagnosticsSupport {
    @RequiresReadLock
    @RequiresBackgroundThread
    open fun createAnnotation(holder: AnnotationHolder, diagnostic: Diagnostic, textRange: TextRange, quickFixes: List<IntentionAction>) {
        val severity = getHighlightSeverity(diagnostic) ?: return
        holder.newAnnotation(severity, getMessage(diagnostic))
            .tooltip(getTooltip(diagnostic))
            .range(textRange)
            .let {
                val highlightType = getSpecialHighlightType(diagnostic)
                if (highlightType != null) it.highlightType(highlightType) else it
            }
            .let {
                var builder = it
                quickFixes.forEach { fix -> builder = builder.withFix(fix) }
                builder
            }
            .create()
    }

    /**
     * Implementations may return `null` if this [diagnostic] should be ignored.
     */
    open fun getHighlightSeverity(diagnostic: Diagnostic): HighlightSeverity? =
        when (diagnostic.severity) {
            DiagnosticSeverity.Error -> HighlightSeverity.ERROR
            DiagnosticSeverity.Warning -> HighlightSeverity.WARNING
            else -> HighlightSeverity.WEAK_WARNING
        }

    @InspectionMessage
    open fun getMessage(diagnostic: Diagnostic): String = diagnostic.message

    @NlsContexts.Tooltip
    open fun getTooltip(diagnostic: Diagnostic): String = diagnostic.message

    open fun getSpecialHighlightType(diagnostic: Diagnostic): ProblemHighlightType? = when {
        diagnostic.tags?.contains(DiagnosticTag.Unnecessary) == true -> ProblemHighlightType.LIKE_UNUSED_SYMBOL
        diagnostic.tags?.contains(DiagnosticTag.Deprecated) == true -> ProblemHighlightType.LIKE_DEPRECATED
        else -> null
    }
}
