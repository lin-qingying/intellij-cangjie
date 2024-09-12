package com.huawei.cangjie.descriptors

import com.huawei.cangjie.diagnostics.Diagnostic
import com.huawei.cangjie.diagnostics.DiagnosticSink
import com.huawei.cangjie.diagnostics.Diagnostics
import com.huawei.cangjie.diagnostics.DiagnosticsWithSuppression
import com.intellij.openapi.util.CompositeModificationTracker
import com.intellij.psi.PsiElement
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

    fun report(diagnostic: Diagnostic) {
//        onTheFlyDiagnosticsCallback(diagnostic)?.callback(diagnostic)

        diagnosticList.add(diagnostic)
        modificationTracker.incModificationCount()
    }

//    private fun onTheFlyDiagnosticsCallback(diagnostic: Diagnostic): DiagnosticSink.DiagnosticsCallback? {
//        val callback = diagnosticsCallback ?: return null
//        // Due to a potential recursion in filter.invoke (via LazyAnnotations) do not try to report
//        // diagnostic on-the-fly if it happened in annotations, and do not report any potentially suppressed elements
//        var element: PsiElement? = diagnostic.psiElement
//        while (element != null && element !is PsiFile) {
//            val annotated = CjStubbedPsiUtil.getPsiOrStubParent(element, CjAnnotated::class.java, false)
//            val annotationEntries = annotated?.annotationEntries
//            if (annotationEntries?.isNotEmpty() == true) return null
//            element = annotated?.parent
//        }
//        val filtered = suppressCache.filter.invoke(diagnostic)
//        if (!filtered) return null
//        return callback
//    }

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
