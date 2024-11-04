package com.linqingying.cangjie.diagnostics

/*

interface DiagnosticSink {
    fun report(diagnostic: Diagnostic)


    interface DiagnosticsCallback {
        fun callback(diagnostic: Diagnostic?)
    }

    companion object {
        val DO_NOTHING: DiagnosticSink = object : DiagnosticSink {
            override fun report(diagnostic: Diagnostic) {
            }

            override fun wantsDiagnostics(): Boolean {
                return false
            }
        }

        val THROW_EXCEPTION: DiagnosticSink = object : DiagnosticSink {
            override fun report(diagnostic: Diagnostic) {
                if (diagnostic.severity == Severity.ERROR) {
                    val psiFile: PsiFile = diagnostic.psiFile
                    val textRanges: List<TextRange> = diagnostic.textRanges
                    val diagnosticText: String =
                        DefaultErrorMessages.render(diagnostic)
                    throw java.lang.IllegalStateException(
                        diagnostic.factory.name + ": " + diagnosticText + " " + PsiDiagnosticUtils
                            .atLocation(psiFile, textRanges[0])
                    )
                }
            }

            override fun wantsDiagnostics(): Boolean {
                return true
            }
        }
    }

    */
/**
     * use [.setCallbackIfNotSet] instead
     * @param callback
     *//*

    @Deprecated("")
    fun setCallback(callback: DiagnosticsCallback) {
        setCallbackIfNotSet(callback)
    }

    fun setCallbackIfNotSet(callback: DiagnosticsCallback): Boolean {
        return false
    }

    fun resetCallback() {}

    fun wantsDiagnostics(): Boolean
}


*/
