/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve

import com.google.common.collect.ImmutableMap
import org.cangnova.cangjie.diagnostics.Diagnostic
import org.cangnova.cangjie.diagnostics.Diagnostics
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.types.CangJieType
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.slicedMap.ReadOnlySlice
import org.cangnova.cangjie.resolve.binding.slicedMap.WritableSlice

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


    override val diagnostics: Diagnostics
        get() = CompositeDiagnostics(delegates.map { it.diagnostics })

    override fun <K : Any, V> get(slice: ReadOnlySlice<K, V>, key: K): V? {
        return delegates.asSequence().map { it[slice, key] }.firstOrNull { it != null }
    }

    override fun <K : Any, V> getKeys(slice: WritableSlice<K, V>): Collection<K> {
        return delegates.flatMap { it.getKeys(slice) }
    }

    override fun <K : Any, V> getSliceContents(slice: ReadOnlySlice<K, V>): ImmutableMap<K, V> {
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
