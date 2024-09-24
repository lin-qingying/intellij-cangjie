package com.linqingying.lsp.impl.navigation

import com.intellij.codeWithMe.ClientId
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.model.psi.ImplicitReferenceProvider
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.linqingying.lsp.util.getOffsetInDocument
import com.linqingying.lsp.util.getRangeInDocument
import com.linqingying.lsp.impl.LspServerManagerImpl
import org.eclipse.lsp4j.LocationLink

internal class LspImplicitReferenceProvider :
    ImplicitReferenceProvider {
    override fun getImplicitReference(
        element: PsiElement,
        offsetInElement: Int
    ): PsiSymbolReference? {
        val psiFile = element as? PsiFile ?: return null
        val virtualFile = psiFile.virtualFile ?: return null

        if (virtualFile is VirtualFileWindow) {
            return null
        }

        val currentActionHolder = ApplicationManager.getApplication().getService(CurrentActionHolder::class.java)
            ?: throw RuntimeException("Cannot find service ${CurrentActionHolder::class.java.name} (classloader=${CurrentActionHolder::class.java.classLoader}, client=${ClientId.currentOrNull})")

        if (!currentActionHolder.runningGoToDeclarationAction) {
            return null
        }

        val project = psiFile.project
        val lspServers = LspServerManagerImpl.getInstanceImpl(project).getServersWithThisFileOpen(virtualFile)

        if (lspServers.isEmpty()) {
            return null
        }

        val document = FileDocumentManager.getInstance().getCachedDocument(virtualFile) ?: return null

        val lspServerAndLocationLinksList = mutableListOf<LspServerAndLocationLinks>()

        for (lspServer in lspServers) {
            if (!lspServer.supportsGotoDefinition() ||
                !lspServer.descriptor.lspGoToDefinitionSupport
            ) {
                continue
            }

            val definitions = lspServer.requestExecutor.getElementDefinitions(virtualFile, offsetInElement)
            if (definitions.isNotEmpty()) {
                lspServerAndLocationLinksList.add(LspServerAndLocationLinks(lspServer, definitions))
            }
        }

        if (lspServerAndLocationLinksList.isEmpty()) {
            return null
        }

        val locationLinks = mutableListOf<LocationLink>()
        for (serverAndLinks in lspServerAndLocationLinksList) {
            locationLinks.addAll(serverAndLinks.locationLinks)
        }

        val hasValidLinks = locationLinks.any { link ->
            val range = link.originSelectionRange ?: return@any false
            val endPosition = range.end ?: return@any false
            val offset = getOffsetInDocument(document, endPosition)
            offset != null && offset > offsetInElement
        }

        var unionRange: TextRange? = null
        val validLinksByServer = mutableListOf<LspServerAndLocationLinks>()

        for (serverAndLinks in lspServerAndLocationLinksList) {
            val validLinks = mutableListOf<LocationLink>()

            for (link in serverAndLinks.locationLinks) {
                val range = link.originSelectionRange?.let { getRangeInDocument(document, it) }
                    ?: TextRange(offsetInElement, offsetInElement)

                if (!hasValidLinks || range.endOffset > offsetInElement) {
                    if (unionRange == null) {
                        unionRange = range
                    } else {
                        unionRange = unionRange.union(range)
                    }
                    validLinks.add(link)
                }
            }

            if (validLinks.isNotEmpty()) {
                validLinksByServer.add(LspServerAndLocationLinks(serverAndLinks.lspServer, validLinks))
            }
        }

        return if (unionRange == null) {
            null
        } else {
            LspSymbolReference(psiFile, unionRange, validLinksByServer)
        }
    }
}


private const val TARGET_PRESENTABLE_TEXT_MAX_LENGTH: Int = 20 

