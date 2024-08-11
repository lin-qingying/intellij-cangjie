package com.huawei.cangjie.diagnostics.rendering

object CommonRenderers {
    @JvmField
    val EMPTY = Renderer<Any> { "" }

    @JvmField
    val STRING = Renderer<String> { it }



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
