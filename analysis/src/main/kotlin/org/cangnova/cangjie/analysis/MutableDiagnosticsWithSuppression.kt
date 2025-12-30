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

package org.cangnova.cangjie.analysis

import org.cangnova.cangjie.diagnostics.Diagnostic
import org.cangnova.cangjie.diagnostics.DiagnosticSink
import org.cangnova.cangjie.diagnostics.Diagnostics
import org.cangnova.cangjie.diagnostics.DiagnosticsWithSuppression
import org.cangnova.cangjie.psi.CjAnnotated
import org.cangnova.cangjie.psi.psiUtil.CjStubbedPsiUtil
import com.intellij.openapi.util.CompositeModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.util.CachedValueImpl
import org.jetbrains.annotations.TestOnly

class MutableDiagnosticsWithSuppression(
    private val suppressCache: CangJieSuppressCache,
    private val delegateDiagnostics: Diagnostics,
) : Diagnostics {
    @Volatile
    private var diagnosticsCallback: DiagnosticSink.DiagnosticsCallback? = null
    private val diagnosticList = ArrayList<Diagnostic>()
    override fun setCallbackIfNotSet(callback: DiagnosticSink.DiagnosticsCallback): Boolean {
        return if (diagnosticsCallback == null) {
            diagnosticsCallback = callback
            delegateDiagnostics.setCallbackIfNotSet(callback)
            true
        } else false
    }

    //essential that this list is readonly
    fun getOwnDiagnostics(): List<Diagnostic> {
        return diagnosticList
    }

    override fun resetCallback() {
        diagnosticsCallback = null
        delegateDiagnostics.resetCallback()
    }

    private fun onTheFlyDiagnosticsCallback(diagnostic: Diagnostic): DiagnosticSink.DiagnosticsCallback? {
        val callback = diagnosticsCallback ?: return null
        // Due to a potential recursion in filter.invoke (via LazyAnnotations) do not try to report
        // diagnostic on-the-fly if it happened in annotations, and do not report any potentially suppressed elements
        var element: PsiElement? = diagnostic.psiElement
        while (element != null && element !is PsiFile) {
            val annotated = CjStubbedPsiUtil.getPsiOrStubParent(element, CjAnnotated::class.java, false)
            val annotationEntries = annotated?.annotationEntries
            if (annotationEntries?.isNotEmpty() == true) return null
            element = annotated?.parent
        }
        val filtered = suppressCache.filter.invoke(diagnostic)
        if (!filtered) return null
        return callback
    }

    fun report(diagnostic: Diagnostic) {
        onTheFlyDiagnosticsCallback(diagnostic)?.callback(diagnostic)

        diagnosticList.add(diagnostic)
        modificationTracker.incModificationCount()
    }


    fun clear() {
        diagnosticList.clear()
        modificationTracker.incModificationCount()
    }

    @TestOnly
    fun getReadonlyView(): DiagnosticsWithSuppression = readonlyView()
    private val cache = CachedValueImpl {
        val allDiagnostics = delegateDiagnostics.noSuppression().all() + diagnosticList
        CachedValueProvider.Result(DiagnosticsWithSuppression(suppressCache, allDiagnostics), modificationTracker)
    }
    override val modificationTracker = CompositeModificationTracker(delegateDiagnostics.modificationTracker)

    private fun readonlyView(): DiagnosticsWithSuppression = cache.value!!

    override fun all(): Collection<Diagnostic> = readonlyView().all()

    override fun forElement(psiElement: PsiElement): Collection<Diagnostic> = readonlyView().forElement(psiElement)

    override fun noSuppression(): Diagnostics = readonlyView().noSuppression()
}
