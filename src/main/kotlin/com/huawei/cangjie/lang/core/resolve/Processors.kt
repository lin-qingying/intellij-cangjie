package com.huawei.cangjie.lang.core.resolve

import com.huawei.cangjie.lang.core.psi.ext.CjElement
import com.huawei.cangjie.lang.core.psi.ext.CjNamedElement
import com.intellij.util.SmartList


/**
 *ScopeEntry是一些在某些代码范围中可见的PsiElement。
 *[ScopeEntry]处理两种情况：
 **别名
 **延迟解析实际元素
 */
interface ScopeEntry {
    val name: String
    val element: CjElement

    val namespaces: Set<Namespace>
//    val subst: Substitution get() = emptySubstitution
    fun doCopyWithNs(namespaces: Set<Namespace>): ScopeEntry
}

interface CjResolveProcessorBase<in T : ScopeEntry> {

    fun process(entry: T): Boolean


    val names: Set<String>?

    fun acceptsName(name: String): Boolean {
        val names = names
        return names == null || name in names
    }
}
private class ResolveVariantsCollector(
    private val referenceName: String,
    val result: MutableList<CjElement> = SmartList(),
) : CjResolveProcessorBase<ScopeEntry> {
    override val names: Set<String> = setOf(referenceName)

    override fun process(entry: ScopeEntry): Boolean {
//        if (entry.name == referenceName) {
//            val element = entry.element
//            if (element !is CjDocAndAttributeOwner || element.existsAfterExpansionSelf) {
//                result += element
//            }
//        }
        return false
    }
}
data class SimpleScopeEntry(
    override val name: String,
    override val element: CjElement,
    override val namespaces: Set<Namespace>,
//    override val subst: Substitution = emptySubstitution
) : ScopeEntry {
    override fun doCopyWithNs(namespaces: Set<Namespace>): ScopeEntry = copy(namespaces = namespaces)
}

typealias CjResolveProcessor = CjResolveProcessorBase<ScopeEntry>

fun collectResolveVariants(referenceName: String?, f: (CjResolveProcessor) -> Unit): List<CjElement> {
    if (referenceName == null) return emptyList()
    val processor = ResolveVariantsCollector(referenceName)
    f(processor)
    return processor.result
}
fun CjResolveProcessor.processAll(elements: List<CjNamedElement>, namespaces: Set<Namespace>): Boolean {
    return elements.any { process(it, namespaces) }
}
fun CjResolveProcessor.process(e: CjNamedElement, namespaces: Set<Namespace>): Boolean {
    val name = e.name ?: return false
    return process(name, namespaces, e)
}
fun CjResolveProcessor.process(name: String, namespaces: Set<Namespace>, e: CjElement): Boolean =
    process(SimpleScopeEntry(name, e, namespaces))
