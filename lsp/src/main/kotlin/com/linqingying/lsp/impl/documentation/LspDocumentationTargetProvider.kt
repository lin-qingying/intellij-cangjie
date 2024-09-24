package com.linqingying.lsp.impl.documentation

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.model.Pointer
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiFile
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.linqingying.lsp.impl.LspServerManagerImpl
import com.linqingying.lsp.util.convertMarkupContentToHtml

import org.eclipse.lsp4j.MarkupContent


internal class LspDocumentationTargetProvider :
    DocumentationTargetProvider {
    override fun documentationTargets(
        psiFile: PsiFile,
        offset: Int
    ): List<DocumentationTarget> {
        val project = psiFile.project
        val injectedLanguageManager = InjectedLanguageManager.getInstance(project)
        val topLevelFile = injectedLanguageManager.getTopLevelFile(psiFile)
        val hostOffset = injectedLanguageManager.injectedToHost(psiFile, offset)
        val virtualFile = topLevelFile.virtualFile ?: return emptyList()

        val servers = LspServerManagerImpl.getInstanceImpl(project).getServersWithThisFileOpen(virtualFile)
        val documentationTargets = mutableListOf<LspDocumentationTarget>()

        for (server in servers) {
            if (server.descriptor.lspHoverSupport && server.supportsHover()) {
                val hoverInformation = server.requestExecutor.getHoverInformation(virtualFile, hostOffset, 300)
                if (hoverInformation != null) {
                    val textRange = hoverInformation.textRange
                    val text = textRange.substring(topLevelFile.text)
                    documentationTargets.add(LspDocumentationTarget(text, hoverInformation.markupContent, project))
                }
            }
        }

        return documentationTargets
    }
}

private class LspDocumentationTarget(
    val presentableText: @NlsSafe String?,
    val markupContent: MarkupContent,
    val project: Project
) : DocumentationTarget {

    @RequiresReadLock
    override fun computeDocumentation(): DocumentationResult {
//        return documentation(
//            convertMarkupContentToHtml(
//                this.markupContent
//            )
//        )

        return createLspDocumentationData(this.markupContent)
            .toQuickDocHtml(this.project)

    }

    override fun computePresentation(): TargetPresentation {
        val presentableText = this.presentableText ?: CodeInsightBundle.message("documentation.tool.window.title")
        return TargetPresentation.builder(presentableText).presentation()
    }

    override fun createPointer(): @org.jetbrains.annotations.NotNull Pointer<LspDocumentationTarget> {
        return Pointer.hardPointer(
            this
        )
    }
}

