package com.huawei.cangjie.ide.annotator

import com.huawei.cangjie.openapiext.isUnitTestMode
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.annotations.TestOnly

abstract class AnnotatorBase : Annotator {

    final override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (isEnabled(javaClass)) {
            annotateInternal(element, holder)
        }
    }

    protected abstract fun annotateInternal(element: PsiElement, holder: AnnotationHolder)

    companion object {
        private val enabledAnnotators: MutableSet<Class<out AnnotatorBase>> = ContainerUtil.newConcurrentSet()

        @TestOnly
        fun enableAnnotator(annotatorClass: Class<out AnnotatorBase>, parentDisposable: Disposable) {
            enabledAnnotators += annotatorClass
            Disposer.register(parentDisposable) { enabledAnnotators -= annotatorClass }
        }

        fun isEnabled(annotatorClass: Class<out AnnotatorBase>): Boolean {
            return !isUnitTestMode || annotatorClass in enabledAnnotators
        }
    }
}
