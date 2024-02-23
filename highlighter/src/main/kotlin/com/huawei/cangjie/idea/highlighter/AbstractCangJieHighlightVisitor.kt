package com.huawei.cangjie.idea.highlighter

import com.huawei.cangjie.descriptors.Diagnostic
import com.huawei.cangjie.descriptors.InvalidModuleException
import com.huawei.cangjie.idea.highlighter.suspender.CangJieHighlightingSuspender
import com.huawei.cangjie.psi.CjFile
import com.intellij.codeInsight.daemon.impl.Divider
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightVisitor
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingLevelManager
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Predicates
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.CommonProcessors

abstract class AbstractCangJieHighlightVisitor : HighlightVisitor {
    private var afterAnalysisVisitor: Array<AfterAnalysisHighlightingVisitor>? = null

    @Volatile
    private var attempt = 0

    override fun suitableForFile(file: PsiFile) = file is CjFile
    override fun visit(element: PsiElement) {
        afterAnalysisVisitor?.forEach(element::accept)
    }


    override fun analyze(
        psiFile: PsiFile,
        updateWholeFile: Boolean,
        holder: HighlightInfoHolder,
        action: Runnable
    ): Boolean {
        val file = psiFile as? CjFile ?: return false
        val highlightingLevelManager = HighlightingLevelManager.getInstance(file.project)
        if (highlightingLevelManager.runEssentialHighlightingOnly(file)) {
            return true
        }


        try {
            analyze(file, holder)

            action.run()

            attempt = 0

        } catch (e: Throwable) {
            val unwrappedException = (e as? ProcessCanceledException)?.cause as? InvalidModuleException ?: e
            if (unwrappedException is ControlFlowException) {
                throw e
            }

            if (unwrappedException is InvalidModuleException) {
                val currentAttempt = attempt
                if (currentAttempt < ATTEMPT_THRESHOLD) {
                    attempt = currentAttempt + 1
                    throw e
                }
            }
            if (CangJieHighlightingSuspender.getInstance(file.project).suspend(file.virtualFile)) {
                throw unwrappedException
            } else {
                LOG.warn(unwrappedException)
            }

        } finally {
            afterAnalysisVisitor = null
        }
        return true

    }


    private fun analyze(file: CjFile, holder: HighlightInfoHolder) {
        val dividedElements: List<Divider.DividedElements> = ArrayList()
        Divider.divideInsideAndOutsideAllRoots(
            file, file.textRange, file.textRange, Predicates.alwaysTrue(),
            CommonProcessors.CollectProcessor(dividedElements)
        )
        //TODO：为了检查元素是否属于文件
        //由于某种未知原因 analyzeWithAllCompilerChecks可能会返回不属于该文件的PsiElement
        val elements = dividedElements.flatMap(Divider.DividedElements::inside).toSet()
        //即时注释诊断：在前端报告后立即显示诊断
//不要创建快速修复程序，因为它可能需要一些解决方案
        val highlightInfoByDiagnostic = HashMap<Diagnostic, HighlightInfo>()
        // 使用描述符呈现实时诊断可能会导致递归
//        fun checkIfDescriptor(candidate: Any?): Boolean =
//            candidate is DeclarationDescriptor || candidate is Collection<*> && candidate.any(::checkIfDescriptor)



//        val shouldHighlightErrors = file.shouldHighlightErrors()
//        val isInjectedCode = isIgnoredInjectedCode(holder)

    }


    companion object {
        private const val ATTEMPT_THRESHOLD = 10
        private val LOG = Logger.getInstance(AbstractCangJieHighlightVisitor::class.java)

    }
}