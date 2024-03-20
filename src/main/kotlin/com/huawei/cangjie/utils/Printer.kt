package com.huawei.cangjie.utils

import java.io.IOException



class Printer private constructor(
    private val out: Appendable,
    private val maxBlankLines: Int,
    private val indentUnit: String,
    private var indent: String
) {
    private var blankLineCountIncludingCurrent = 0
    private var withholdIndentOnce = false
    private var length = 0

    constructor(out: Appendable, indentUnit: String) : this(out, Int.MAX_VALUE, indentUnit)

    @JvmOverloads
    constructor(
        out: Appendable,
        maxBlankLines: Int = Int.MAX_VALUE,
        indentUnit: String = DEFAULT_INDENTATION_UNIT
    ) : this(out, maxBlankLines, indentUnit, "")

    constructor(out: Appendable, parent: Printer) : this(out, parent.maxBlankLines, parent.indentUnit, parent.indent)

    private fun append(o: Any) {
        try {
            val string = o.toString()
            out.append(string)
            length += string.length
        } catch (e: IOException) {
        }
    }

    fun println(vararg objects: Any?): Printer {
        print(*objects)
        printLineSeparator()

        return this
    }

    private fun printLineSeparator() {
        if (blankLineCountIncludingCurrent <= maxBlankLines) {
            blankLineCountIncludingCurrent++
            append(LINE_SEPARATOR)
        }
    }

    fun print(vararg objects: Any?): Printer {
        if (withholdIndentOnce) {
            withholdIndentOnce = false
        } else if (objects.size > 0) {
            printIndent()
        }
        printWithNoIndent(objects)

        return this
    }

    fun printIndent() {
        append(indent)
    }

    fun printWithNoIndent(vararg objects: Any): Printer {
        for (`object` in objects) {
            blankLineCountIncludingCurrent = 0
            append(`object`)
        }

        return this
    }

    fun withholdIndentOnce(): Printer {
        withholdIndentOnce = true
        return this
    }

    fun printlnWithNoIndent(vararg objects: Any?): Printer {
        printWithNoIndent( objects)
        printLineSeparator()

        return this
    }

    fun pushIndent(): Printer {
        indent += indentUnit

        return this
    }

    fun popIndent(): Printer {
        check(indent.length >= indentUnit.length) { "No indentation to pop" }

        indent = indent.substring(indentUnit.length)

        return this
    }

    fun separated(separator: Any?, vararg items: Any?): Printer {
        for (i in items.indices) {
            if (i > 0) {
                printlnWithNoIndent(separator)
            }
            printlnWithNoIndent(items[i])
        }
        return this
    }

    fun separated(separator: Any?, items: Collection<*>): Printer {
        val iterator = items.iterator()
        while (iterator.hasNext()) {
            printlnWithNoIndent(iterator.next())
            if (iterator.hasNext()) {
                printlnWithNoIndent(separator)
            }
        }
        return this
    }

    val isEmpty: Boolean
        get() = length == 0

    override fun toString(): String {
        return out.toString()
    }

    val currentIndentLengthInUnits: Int
        get() = indent.length / indentUnit.length

    val indentUnitLength: Int
        get() = indentUnit.length

    companion object {
        private const val DEFAULT_INDENTATION_UNIT = "    "
        val LINE_SEPARATOR: String = System.getProperty("line.separator")
    }
}