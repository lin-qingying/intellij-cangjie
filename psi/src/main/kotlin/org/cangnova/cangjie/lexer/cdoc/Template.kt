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

/**
 * A template that expands inside [TOuter]
 */
interface Template<in TOuter> {
    fun TOuter.apply()
}

/**
 * A placeholder that is inserted inside [TOuter]
 */
open class Placeholder<TOuter> {
    private var contentStack = mutableListOf<(TOuter.(Exec) -> Unit)>()

    var meta: String = ""

    operator fun invoke(meta: String = "", content: TOuter.(Exec) -> Unit) {
        this.contentStack.add(content)
        this.meta = meta
    }

    inner class Exec(var depth: Int, val outer: TOuter) {

        fun inherit() {
            depth--
            contentStack.getOrNull(depth)?.invoke(outer, this@Exec)
            depth++
        }
    }

    fun isEmpty() = contentStack.isEmpty()

    fun apply(destination: TOuter) {
        val top = contentStack.lastOrNull()
        val exec = Exec(contentStack.lastIndex, destination)
        top?.invoke(destination, exec)
    }
}

/**
 * A placeholder that is also a template
 */
open class TemplatePlaceholder<TTemplate> {
    private var content: TTemplate.() -> Unit = { }
    operator fun invoke(content: TTemplate.() -> Unit) {
        this.content = content
    }

    fun apply(template: TTemplate) {
        template.content()
    }
}
fun <TOuter, TTemplate : Template<TOuter>> TOuter.insert(template: TTemplate, build: TTemplate.() -> Unit) {
    template.build()
    with(template) { apply() }
}

fun <TTemplate : Template<TOuter>, TOuter> TOuter.insert(template: TTemplate, placeholder: TemplatePlaceholder<TTemplate>) {
    placeholder.apply(template)
    with(template) { apply() }
}

/**
 * Inserts placeholder
 */
fun <TOuter> TOuter.insert(placeholder: Placeholder<TOuter>): Unit = placeholder.apply(this)
