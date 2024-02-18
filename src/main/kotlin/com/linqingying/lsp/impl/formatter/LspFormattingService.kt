package com.linqingying.lsp.impl.formatter


import com.intellij.formatting.service.AsyncDocumentFormattingService
import com.intellij.formatting.service.AsyncFormattingRequest
import com.intellij.formatting.service.FormattingService
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.lang.LanguageFormatting
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile


import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.linqingying.lsp.api.customization.requests.util.applyTextEdits
import com.linqingying.lsp.impl.LspBundle
import com.linqingying.lsp.impl.LspServerImpl
import com.linqingying.lsp.impl.LspServerManagerImpl
import com.linqingying.lsp.impl.requests.LspFormattingRequest
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NotNull
import java.util.logging.Level


private class LspFormattingService :
    AsyncDocumentFormattingService() {
    override fun canFormat(psiFile: PsiFile): Boolean {
        val project = psiFile.project
        val virtualFile = psiFile.virtualFile
        if (virtualFile == null || virtualFile is VirtualFileWindow || !virtualFile.isInLocalFileSystem) {
            return false
        }
        return V(project, virtualFile) != null
    }

    override fun createFormattingTask(formattingRequest: AsyncFormattingRequest): FormattingTask? { /* compiled code */

        val project = formattingRequest.context.project
        val virtualFile = formattingRequest.context.virtualFile
        if (virtualFile == null || virtualFile is VirtualFileWindow || !virtualFile.isInLocalFileSystem) {
            return null
        }
        val lspServerImpl = V(project, virtualFile) ?: return null
        return LspFormattingTask(lspServerImpl, virtualFile, formattingRequest)

    }

    override fun getFeatures(): Set<FormattingService.Feature> = emptySet()

    override fun getName(): @NotNull @Nls String = LspBundle.message("lsp.based.formatter")
    override fun getNotificationGroupId(): String = "LSP window/showMessage"

    private fun V(
        project: Project,
        file: VirtualFile
    ): LspServerImpl? {
        return LspServerManagerImpl.getInstanceImpl(project)
            .findServer {
                val lspFormattingSupport = it.descriptor.lspFormattingSupport
                if (lspFormattingSupport == null || !it.hasFormattingRelatedCapabilities() || !it.descriptor.isSupportedFile(
                        file
                    )
                ) {
                    return@findServer false
                }
                val psiFile = PsiManager.getInstance(project).findFile(file)
                val formattingModelBuilder = psiFile?.let { LanguageFormatting.INSTANCE.forContext(it) }
                val hasFormattingSupport = formattingModelBuilder != null


                val doesServerWantToFormat = it.doesServerExplicitlyWantToFormatThisFile(file)
                return@findServer lspFormattingSupport.shouldFormatThisFileExclusivelyByServer(
                    file,
                    hasFormattingSupport,
                    doesServerWantToFormat
                )
            }

    }


    private class LspFormattingTask(
        val lspServer: LspServerImpl,
        val file: VirtualFile,
        val formattingRequest: AsyncFormattingRequest
    ) : FormattingTask {

        override fun cancel(): Boolean = true

        override fun isRunUnderProgress(): Boolean = true

        override fun run() {
            val text = formattingRequest.documentText
            val codeStyleSettings = formattingRequest.context.codeStyleSettings
            val edits = lspServer.requestExecutor.sendRequestSync(
                LspFormattingRequest(
                    lspServer,
                    file,
                    codeStyleSettings
                )
            ) ?: emptyList()

            if (edits.isEmpty()) {
                formattingRequest.onTextReady(null.toString())
            } else {
                val document = DocumentImpl(text, false, true)
                applyTextEdits(document, edits)
                formattingRequest.onTextReady(document.text)
            }
        }
    }
}

