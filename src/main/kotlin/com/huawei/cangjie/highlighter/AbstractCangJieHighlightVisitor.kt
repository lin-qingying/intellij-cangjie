package com.huawei.cangjie.highlighter

import com.huawei.cangjie.configurable.services.CangJieLanguageServerServices
import com.huawei.cangjie.configurable.services.Feature
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.diagnostics.Diagnostic
import com.huawei.cangjie.diagnostics.Errors
import com.huawei.cangjie.descriptors.InvalidModuleException
import com.huawei.cangjie.diagnostics.Severity
import com.huawei.cangjie.diagnostics.rendering.RenderingContext
import com.huawei.cangjie.diagnostics.rendering.parameters
import com.huawei.cangjie.highlighter.suspender.CangJieHighlightingSuspender
import com.huawei.cangjie.ide.statistics.compilationError.CangJieCompilationErrorFrequencyStatsCollector
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.CjNameReferenceExpression
import com.huawei.cangjie.psi.CjParameter
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.caches.analyzeWithAllCompilerChecks
import com.huawei.cangjie.utils.actionUnderSafeAnalyzeBlock
import com.intellij.codeInsight.daemon.impl.CollectHighlightsUtil
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightVisitor
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingLevelManager
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.resolve.FileContextUtil

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

//          TODO  静态分析
            if(CangJieLanguageServerServices.getInstance().astConfig.isFeatureEnabled(Feature.DIAGNOSTICS)){
                analyze(file, holder)

            }

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
//        val dividedElements: List<Divider.DividedElements> = ArrayList()
//        Divider.divideInsideAndOutsideAllRoots(
//            file, file.textRange, file.textRange, Predicates.alwaysTrue(),
//            CommonProcessors.CollectProcessor(dividedElements)
//        )

//        if(file.fileType is CangJieBuiltInFileType){
//            return
//        }
//if (file.moduleInfo !is CangJieModuleInfo){
//    return
//}

        val elements =
            CollectHighlightsUtil.getElementsInRange(file, file.textRange.startOffset, file.textRange.endOffset)

        //TODO：为了检查元素是否属于文件
        //由于某种未知原因 analyzeWithAllCompilerChecks可能会返回不属于该文件的PsiElement
//        val elements1 = dividedElements.flatMap(Divider.DividedElements::inside).toSet()
        //即时注释诊断：在前端报告后立即显示诊断
//不要创建快速修复程序，因为它可能需要一些解决方案
        val highlightInfoByDiagnostic = HashMap<Diagnostic, HighlightInfo>()

        // 使用描述符呈现实时诊断可能会导致递归
        fun checkIfDescriptor(candidate: Any?): Boolean =
            candidate is DeclarationDescriptor || candidate is Collection<*> && candidate.any(::checkIfDescriptor)


        val shouldHighlightErrors = file.shouldHighlightErrors()
        val isInjectedCode = isIgnoredInjectedCode(holder)


        val analysisResult = if (shouldHighlightErrors) {
            file.analyzeWithAllCompilerChecks(
                {


                    val element = it.psiElement
                    if (element in elements &&
                        it !in highlightInfoByDiagnostic &&
                        !RenderingContext.parameters(it).any(::checkIfDescriptor)
                    ) {
                        annotateDiagnostic(
                            element,
                            holder,
                            listOf(it),
                            isInjectedCode,
                            highlightInfoByDiagnostic,
                            calculatingInProgress = true
                        )
                    }
                }
            )
        } else {
            file.analyzeWithAllCompilerChecks()
        }
        val bindingContext =
            file.actionUnderSafeAnalyzeBlock(
                {
                    analysisResult.throwIfError()
                    analysisResult.bindingContext
                },
                { BindingContext.EMPTY }
            )

        afterAnalysisVisitor = getAfterAnalysisVisitor(holder, bindingContext)
        //cleanUpCalculatingAnnotations(highlightInfoByTextRange)
        if (!shouldHighlightErrors) return
        val diagnostics = bindingContext.diagnostics
        diagnostics
            .filter { diagnostic ->
                diagnostic.psiElement in elements
                        // has been processed earlier e.g. on-fly or for some reasons it could be duplicated diagnostics for the same factory
                        //  see [PsiCheckerTestGenerated$Checker.testRedeclaration]
                        && diagnostic !in highlightInfoByDiagnostic
            }
            .groupBy { it.psiElement }
            .forEach { (psiElement, diagnostics) ->
                // annotate diagnostics those were not possible to report (and therefore render) on-the-fly
                annotateDiagnostic(
                    psiElement,
                    holder,
                    diagnostics,
                    isInjectedCode,
                    highlightInfoByDiagnostic,
                    calculatingInProgress = false
                )
            }

        // apply quick fixes for all diagnostics grouping by element
        highlightInfoByDiagnostic.keys
            .groupBy { it.psiElement }
            .forEach {
                annotateDiagnostic(
                    it.key,
                    holder,
                    it.value,
                    isInjectedCode,
                    highlightInfoByDiagnostic,
                    calculatingInProgress = false
                )
            }
        CangJieCompilationErrorFrequencyStatsCollector.recordCompilationErrorsHappened(
            diagnostics.asSequence().filter { it.severity == Severity.ERROR }.map(Diagnostic::factoryName),
            file
        )


    }


    private fun annotateDiagnostic(
        element: PsiElement,
        holder: HighlightInfoHolder,
        diagnostics: List<Diagnostic>,
        isInjectedCode: Boolean,
        highlightInfoByDiagnostic: MutableMap<Diagnostic, HighlightInfo>? = null,
        calculatingInProgress: Boolean = true
    ) {
        if (element.getUserData(DO_NOT_HIGHLIGHT_KEY) != null) return
        assertBelongsToTheSameElement(element, diagnostics)
        if (element is CjNameReferenceExpression) {
            val unresolved = diagnostics.any { it.factory == Errors.UNRESOLVED_REFERENCE }
            element.putUserData(UNRESOLVED_KEY, if (unresolved) Unit else null)
        }

        val activeDiagnostics = if (!isInjectedCode) {
            diagnostics
        } else {
            diagnostics.filter { it.factoryName !in suppressedInjectedFilesDiagnostics }
        }

        if (activeDiagnostics.isNotEmpty()) {
            ElementAnnotator(element) { shouldSuppressUnusedParameter(it) }
                .registerDiagnosticsAnnotations(
                    holder,
                    activeDiagnostics,
                    highlightInfoByDiagnostic,
                    calculatingInProgress
                )
        }
    }

    protected open fun shouldSuppressUnusedParameter(parameter: CjParameter): Boolean = false

    private fun isIgnoredInjectedCode(holder: HighlightInfoHolder): Boolean {
        val file = holder.annotationSession.file


        return InjectedLanguageManager.getInstance(holder.project).isInjectedFragment(file)
                || file.getUserData(FileContextUtil.INJECTED_IN_ELEMENT) != null
    }

    companion object {
        internal fun assertBelongsToTheSameElement(element: PsiElement, diagnostics: Collection<Diagnostic>) {
            assert(diagnostics.all { it.psiElement == element })
        }

        private val suppressedInjectedFilesDiagnostics: Set<String> = setOf(
            "UNRESOLVED_REFERENCE",
            "NOTHING_TO_OVERRIDE"
        )


        fun getAfterAnalysisVisitor(holder: HighlightInfoHolder, bindingContext: BindingContext) =
            arrayOf<AfterAnalysisHighlightingVisitor>(
//            PropertiesHighlightingVisitor(holder, bindingContext),
            FunctionsHighlightingVisitor(holder, bindingContext),
//            VariablesHighlightingVisitor(holder, bindingContext),
//            TypeKindHighlightingVisitor(holder, bindingContext)
            )

        private const val ATTEMPT_THRESHOLD = 10
        private val LOG = Logger.getInstance(AbstractCangJieHighlightVisitor::class.java)
        private val DO_NOT_HIGHLIGHT_KEY = Key<Unit>("DO_NOT_HIGHLIGHT_KEY")
        private val UNRESOLVED_KEY = Key<Unit>("CangJieHighlightVisitor.UNRESOLVED_KEY")

    }
}
