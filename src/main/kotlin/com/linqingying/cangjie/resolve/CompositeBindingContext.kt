package com.linqingying.cangjie.resolve

import com.google.common.collect.ImmutableMap
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.diagnostics.Diagnostic
import com.linqingying.cangjie.diagnostics.Diagnostics
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.utils.slicedMap.ReadOnlySlice
import com.linqingying.cangjie.utils.slicedMap.WritableSlice
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement

class CompositeBindingContext private constructor(
    private val delegates: LinkedHashSet<BindingContext>
) : BindingContext {

    companion object {
        fun create(delegates: List<BindingContext>): BindingContext {
            if (delegates.isEmpty()) return BindingContext.EMPTY
            val delegatesSet = LinkedHashSet(delegates)
            if (delegatesSet.size == 1) return delegates.first()
            return CompositeBindingContext(delegatesSet)
        }
    }

    private class CompositeDiagnostics(
        private val delegates: List<Diagnostics>
    ) : Diagnostics {
        override fun iterator(): Iterator<Diagnostic> {
            return delegates.fold(emptySequence<Diagnostic>(), { r, t -> r + t.asSequence() }).iterator()
        }

        override val modificationTracker = ModificationTracker {
            delegates.fold(0L) { r, t -> r + t.modificationTracker.modificationCount }
        }

        override fun all(): Collection<Diagnostic> {
            return delegates.flatMap { it.all() }
        }

        override fun forElement(psiElement: PsiElement): Collection<Diagnostic> {
            return delegates.flatMap { it.forElement(psiElement) }
        }

        override fun isEmpty(): Boolean {
            return delegates.all { it.isEmpty() }
        }

        override fun noSuppression(): Diagnostics {
            return CompositeDiagnostics(delegates.map { it.noSuppression() })
        }
    }

    override fun getDiagnostics(): Diagnostics {
        return CompositeDiagnostics(delegates.map { it.diagnostics })

    }

    override fun <K, V> get(slice: ReadOnlySlice<K, V>?, key: K?): V? {
        return delegates.asSequence().map { it[slice, key] }.firstOrNull { it != null }
    }

    override fun <K, V> getKeys(slice: WritableSlice<K, V>?): Collection<K> {
        return delegates.flatMap { it.getKeys(slice) }
    }

    override fun <K, V> getSliceContents(slice: ReadOnlySlice<K, V>): ImmutableMap<K, V> {
        //we need intermediate map cause ImmutableMap doesn't support same entries obtained from different slices
        val map = hashMapOf<K, V>()
        delegates.forEach { map.putAll(it.getSliceContents(slice)) }
        return ImmutableMap.builder<K, V>().putAll(map).build()
    }


    override fun addOwnDataTo(trace: BindingTrace, commitDiagnostics: Boolean) {
        // Do nothing
    }


    override fun getType(expression: CjExpression): CangJieType? {
        return delegates.asSequence().map { it.getType(expression) }.firstOrNull { it != null }

    }


}
