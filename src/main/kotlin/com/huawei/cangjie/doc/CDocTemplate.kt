package com.huawei.cangjie.doc

import com.intellij.lang.documentation.DocumentationMarkup.*

open class CDocTemplate : Template<StringBuilder> {
    val definition = Placeholder<StringBuilder>()

    val description = Placeholder<StringBuilder>()

    val deprecation = Placeholder<StringBuilder>()

    val containerInfo = Placeholder<StringBuilder>()

    override fun StringBuilder.apply() {
        append(DEFINITION_START)
        insert(definition)
        append(DEFINITION_END)

        if (!deprecation.isEmpty()) {
            append(SECTIONS_START)
            insert(deprecation)
            append(SECTIONS_END)
        }

        insert(description)

        if (!containerInfo.isEmpty()) {
            append("<div class='bottom'>")
            insert(containerInfo)
            append("</div>")
        }
    }

    sealed class DescriptionBodyTemplate : Template<StringBuilder> {
        class CangJie : DescriptionBodyTemplate() {
            val content = Placeholder<StringBuilder>()
            val sections = Placeholder<StringBuilder>()
            override fun StringBuilder.apply() {
                val computedContent = buildString { insert(content) }
                if (computedContent.isNotBlank()) {
                    append(CONTENT_START)
                    append(computedContent)
                    append(CONTENT_END)
                }

                append(SECTIONS_START)
                insert(sections)
                append(SECTIONS_END)
            }
        }


    }

    class NoDocTemplate : CDocTemplate() {

        val error = Placeholder<StringBuilder>()

        override fun StringBuilder.apply() {
            insert(error  )
        }
    }
}
