package com.huawei.cangjie1.utils.exceptions


class SmartPrinter(appendable: Appendable, indent: String = DEFAULT_INDENT) {
    companion object {
        private const val DEFAULT_INDENT = "    "
    }

    private val printer = Printer(appendable, indent)

    private var notFirstPrint: Boolean = false

    fun print(vararg objects: Any) {
        if (notFirstPrint) {
            printer.printWithNoIndent(*objects)
        } else {
            printer.print(*objects)
        }
        notFirstPrint = true
    }

    fun println(vararg objects: Any) {
        if (notFirstPrint) {
            printer.printlnWithNoIndent(*objects)
        } else {
            printer.println(*objects)
        }
        notFirstPrint = false
    }

    fun pushIndent() {
        printer.pushIndent()
    }

    fun popIndent() {
        printer.popIndent()
    }

    fun getCurrentIndentInUnits() = printer.currentIndentLengthInUnits
    fun getIndentUnit() = printer.indentUnitLength

    override fun toString(): String = printer.toString()
}

inline fun SmartPrinter.withIndent(block: () -> Unit) {
    pushIndent()
    block()
    popIndent()
}
