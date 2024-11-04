package com.linqingying.cangjie.cli.messages

// https://developer.apple.com/documentation/xcode/running-custom-scripts-during-a-build#Log-errors-and-warnings-from-your-script
class XcodeStyleMessageRenderer : MessageRenderer {

    override fun render(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?
    ): String {
        val xcodeSeverity = when (severity) {
            CompilerMessageSeverity.WARNING, CompilerMessageSeverity.STRONG_WARNING -> "warning"
            CompilerMessageSeverity.ERROR, CompilerMessageSeverity.EXCEPTION -> "error"
            CompilerMessageSeverity.LOGGING, CompilerMessageSeverity.OUTPUT, CompilerMessageSeverity.INFO -> "note"
        }

        return buildString {
            location?.apply {
                append("$path:$line:$column: ")
            }

            append("$xcodeSeverity: $message")
        }
    }

    override fun renderPreamble() = ""

    override fun renderUsage(usage: String) = usage

    override fun renderConclusion() = ""

    override fun getName() = "XcodeStyle"
}
