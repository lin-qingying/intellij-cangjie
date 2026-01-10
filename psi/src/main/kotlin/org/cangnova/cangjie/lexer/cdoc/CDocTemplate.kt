/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.lexer.cdoc

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
            insert(error)
        }
    }
}
