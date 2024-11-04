package com.linqingying.cangjie.cli.messages
import java.io.Serializable
interface CompilerMessageSourceLocation : Serializable {
    val path: String
    val line: Int
    val column: Int
    // NOTE: Seems that the end-of-location data do not belong here conceptually, and now causes confusion with other usages
    // TODO: consider removing it and switching REPL/Scripting diagnostis to the higher-level entities (KtDiagnostics)
    val lineEnd: Int get() = -1
    val columnEnd: Int get() = -1
    val lineContent: String? // related to the (start) line/column only, used to show start position in the console output
}
