package com.linqingying.cangjie.doc


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
