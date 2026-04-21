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

package org.cangnova.cangjie.highlighter.visitor

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.HighlightRangeExtension
import com.intellij.codeInsight.daemon.impl.HighlightVisitor
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingLevelManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.readAction
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.createSmartPointer
import com.intellij.xml.util.XmlStringUtil
import com.intellij.platform.util.coroutines.childScope
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.cangnova.cangjie.CangJiePluginDisposable
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnostic
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.analysis.api.diagnostics.CaSeverity
import org.cangnova.cangjie.analysis.api.diagnostics.getDefaultMessageWithFactoryName
import org.cangnova.cangjie.analysis.injectionRequiresOnlyEssentialHighlighting
import org.cangnova.cangjie.analysis.isInjectedFileShouldBeAnalyzed
import org.cangnova.cangjie.psi.CjDeclarationContainer
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile

internal class CangJieDiagnosticHighlightVisitor : HighlightVisitor, HighlightRangeExtension {
    /**
     * 将诊断预先按 PSI 元素分组，visit 时按元素顺序逐个吐出，避免整文件闪烁。
     */
    private var diagnosticsMap: Map<PsiElement, List<HighlightInfo.Builder>> = emptyMap()
    private var holder: HighlightInfoHolder? = null
    private var coroutineScope: CoroutineScope? = null

    override fun suitableForFile(file: PsiFile): Boolean {
        return shouldHighlightDiagnostics(file)
    }

    override fun visit(element: PsiElement) {
        val diagnostics = diagnosticsMap[element] ?: return
        for (builder in diagnostics) {
            val info = builder.create() ?: continue
            holder!!.add(info)
        }
    }

    override fun analyze(
        file: PsiFile,
        updateWholeFile: Boolean,
        holder: HighlightInfoHolder,
        action: Runnable
    ): Boolean {
        this.holder = holder
        this.coroutineScope = CangJiePluginDisposable.getInstance(file.project)
            .coroutineScope
            .childScope(name = "${CangJieDiagnosticHighlightVisitor::class.simpleName}: ${file.name}")

        try {
            val contextFile = holder.contextFile as? CjFile
                ?: error("${CjFile::class.simpleName} files expected but got ${holder.contextFile::class.simpleName}")

            diagnosticsMap = analyzeFile(contextFile)
            action.run()
        } catch (e: Throwable) {
            if (Logger.shouldRethrow(e)) throw e
            // TODO: Port CangJieHighlightingSuspender to K2 to avoid the issue with infinite highlighting loop restart
            throw e
        } finally {
            // do not leak Editor, since CangJieDiagnosticHighlightVisitor is a project-level extension
            this.diagnosticsMap = emptyMap()
            this.coroutineScope?.cancel() // TODO
            this.coroutineScope = null
            this.holder = null
        }

        return true
    }

    private fun analyzeFile(file: CjFile): Map<PsiElement, List<HighlightInfo.Builder>> = analyze(file) {
        triggerCollectingDiagnostics(file)

        val diagnostics = file.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        val builders = diagnostics.mapNotNull { diagnostic ->
            val psi = diagnostic.psi ?: return@mapNotNull null
            val ranges = diagnostic.effectiveRanges()
            psi to ranges.map { range -> createHighlightInfo(diagnostic, range) }
        }

        val destination = LinkedHashMap<PsiElement, List<HighlightInfo.Builder>>(builders.size)
        for ((psi, psiBuilders) in builders) {
            destination.compute(psi) { _, old ->
                if (old == null) psiBuilders else old + psiBuilders
            }
        }

        destination
    }

    override fun clone(): HighlightVisitor {
        return CangJieDiagnosticHighlightVisitor()
    }

    companion object {
        fun shouldHighlightDiagnostics(file: PsiFile): Boolean {
            if (file !is CjFile || file.isCompiled) return false

            val viewProvider = file.viewProvider
            val isInjection = InjectedLanguageManager.getInstance(file.project).isInjectedViewProvider(viewProvider)
            if (isInjection && (!viewProvider.isInjectedFileShouldBeAnalyzed || file.injectionRequiresOnlyEssentialHighlighting)) {
                // do not highlight errors in injected code
                return false
            }

            val highlightingManager = HighlightingLevelManager.getInstance(file.project)
            return highlightingManager.shouldHighlight(file) && !highlightingManager.runEssentialHighlightingOnly(file)
        }
    }

    override fun isForceHighlightParents(file: PsiFile): Boolean = file is CjFile

    /**
     * 提前预热诊断缓存，减少首次进入编辑器时的停顿。
     * 与 Kotlin K2 的 visitor 一样，按声明容器递归触发 analysis-api 诊断计算。
     */
    private fun triggerCollectingDiagnostics(element: CjElement) {
        val pointer = element.createSmartPointer()
        coroutineScope!!.launch {
            readAction {
                val declaration = pointer.element as? CjElement ?: return@readAction
                analyze(declaration) {
                    declaration.diagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                }
            }
        }

        val declarations = when (element) {
            is CjFile -> element.declarations
            is CjDeclarationContainer -> element.declarations
            else -> null
        }
        declarations?.forEach { declaration ->
            if (declaration is CjElement) {
                triggerCollectingDiagnostics(declaration)
            }
        }
    }

    private fun CaDiagnosticWithPsi<*>.effectiveRanges(): List<TextRange> {
        val explicitRanges = textRanges.filterNot(TextRange::isEmpty)
        if (explicitRanges.isNotEmpty()) return explicitRanges

        val fallback = psi.textRange
        return if (fallback == null || fallback.isEmpty) {
            listOf(TextRange.EMPTY_RANGE)
        } else {
            listOf(fallback)
        }
    }

    private fun createHighlightInfo(
        diagnostic: CaDiagnosticWithPsi<*>,
        range: TextRange
    ): HighlightInfo.Builder {
        val message = diagnostic.renderMessage()
        val htmlMessage = XmlStringUtil.wrapInHtml(XmlStringUtil.escapeString(message).replace("\n", "<br>"))

        return HighlightInfo.newHighlightInfo(getHighlightInfoType(diagnostic))
            .range(range)
            .description(message)
            .escapedToolTip(htmlMessage)
    }

    private fun getHighlightInfoType(diagnostic: CaDiagnostic): HighlightInfoType = when {
        diagnostic.isUnresolvedDiagnostic() -> HighlightInfoType.WRONG_REF
        diagnostic.isDeprecatedDiagnostic() -> HighlightInfoType.DEPRECATED
        diagnostic.isUnusedElementDiagnostic() -> HighlightInfoType.UNUSED_SYMBOL
        else -> when (diagnostic.severity) {
            CaSeverity.ERROR -> HighlightInfoType.ERROR
            CaSeverity.WARNING -> HighlightInfoType.WARNING
            CaSeverity.INFO -> HighlightInfoType.INFORMATION
        }
    }

    private fun CaDiagnostic.renderMessage(): String {
        val application = ApplicationManager.getApplication()
        return if (application.isInternal || application.isUnitTestMode) {
            getDefaultMessageWithFactoryName()
        } else {
            defaultMessage
        }
    }

    private fun CaDiagnostic.isUnresolvedDiagnostic(): Boolean {
        val factory = factoryName.uppercase()
        return factory.contains("UNRESOLVED") || factory.contains("INVISIBLE_REFERENCE") || factory.contains("MISSING_STDLIB")
    }

    private fun CaDiagnostic.isDeprecatedDiagnostic(): Boolean {
        return factoryName.uppercase().contains("DEPRECATION")
    }

    private fun CaDiagnostic.isUnusedElementDiagnostic(): Boolean {
        val factory = factoryName.uppercase()
        return factory.contains("USELESS") || factory.contains("UNUSED")
    }
}
