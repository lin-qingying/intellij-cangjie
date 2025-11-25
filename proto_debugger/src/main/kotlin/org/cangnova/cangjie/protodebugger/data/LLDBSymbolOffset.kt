package org.cangnova.cangjie.protodebugger.data

import com.intellij.util.text.nullize

data class LLDBSymbolOffset(val symbolName: String, val offset: Long) {
    companion object {
        private val ANGLE_BRACKETS_RE: Regex = Regex("<(.*?)(?:\\+([\\d]+|0x[0-9a-fA-F]+))?>")


        @JvmStatic
        fun parseAngleBrackets(
            s: String,
            defaultFunctionName: String?
        ): LLDBSymbolOffset {
            ANGLE_BRACKETS_RE.matchEntire(s)?.let { result ->
                val (_, symbolName, offsetString) = result.destructured
                val offset = if (offsetString.isNotBlank()) offsetString.toLong() else 0L
                val functionName = symbolName.nullize() ?: defaultFunctionName ?: ""

                return LLDBSymbolOffset(functionName, offset)
            } ?: throw IllegalArgumentException("Couldn't parse <symbol+offset>: '$s'")

        }
    }


}
