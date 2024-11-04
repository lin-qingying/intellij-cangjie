package com.linqingying.cangjie.cli.messages



@JvmDefaultWithCompatibility
interface MessageCollector {
    fun clear()

    fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation? = null)


    fun hasErrors(): Boolean

    companion object {
        val NONE: MessageCollector = object : MessageCollector {
            override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
                // Do nothing
            }

            override fun clear() {
                // Do nothing
            }

            override fun hasErrors(): Boolean = false
        }
    }
}
