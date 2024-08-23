package com.huawei.cangjie.diagnostics.rendering

import com.intellij.openapi.util.text.StringUtil
import java.io.PrintWriter
import java.io.StringWriter

object CommonRenderers {
    @JvmField
    val EMPTY = Renderer<Any> { "" }

    @JvmField
    val STRING = Renderer<String> { it }


    @JvmField
    val THROWABLE = Renderer<Throwable> {
        val writer = StringWriter()
        it.printStackTrace(PrintWriter(writer))
        StringUtil.first(writer.toString(), 2048, true)
    }

    @JvmStatic
    fun <T> commaSeparated(itemRenderer: DiagnosticParameterRenderer<T>) = ContextDependentRenderer<Collection<T>> { collection, context ->
        buildString {
            val iterator = collection.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                append(itemRenderer.render(next, context))
                if (iterator.hasNext()) {
                    append(", ")
                }
            }
        }
    }
}
