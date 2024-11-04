package com.linqingying.cangjie.cli.messages

data class CompilerMessageLocationWithRange private constructor(
    override val path: String,
    override val line: Int,
    override val column: Int,
    override val lineEnd: Int,
    override val columnEnd: Int,
    override val lineContent: String?
) : CompilerMessageSourceLocation {
    override fun toString(): String =
        path + (if (line != -1 || column != -1) " ($line:$column)" else "")

    companion object {
        @JvmStatic
        fun create(
            path: String?,
            lineStart: Int,
            columnStart: Int,
            lineEnd: Int?,
            columnEnd: Int?,
            lineContent: String?
        ): CompilerMessageLocationWithRange? =
            if (path == null) null else CompilerMessageLocationWithRange(path, lineStart, columnStart, lineEnd ?: -1, columnEnd ?: -1, lineContent)

        private val serialVersionUID: Long = 8228357578L
    }
}
