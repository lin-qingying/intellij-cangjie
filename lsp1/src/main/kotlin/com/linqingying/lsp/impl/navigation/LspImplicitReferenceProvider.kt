package com.linqingying.lsp.impl.navigation

import com.linqingying.lsp.api.customization.requests.util.getOffsetInDocument
import com.linqingying.lsp.api.customization.requests.util.getRangeInDocument
import com.linqingying.lsp.impl.LspServerManagerImpl
import com.linqingying.lsp.impl.requests.LspRequestExecutorImpl
import com.intellij.codeWithMe.ClientId
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.model.psi.ImplicitReferenceProvider
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class LspImplicitReferenceProvider: ImplicitReferenceProvider {


    override fun getImplicitReference(element: PsiElement, offsetInElement: Int):  PsiSymbolReference? {


        val file = element as? PsiFile ?: return null
        val virtualFile = file.virtualFile ?: return null

        if (virtualFile is VirtualFileWindow) return null

        val service = ApplicationManager.getApplication().getService(CurrentActionHolder::class.java)
            ?: throw RuntimeException("Cannot find service ${CurrentActionHolder::class.java.name} (classloader=${CurrentActionHolder::class.java.classLoader}, client=${ClientId.Companion.currentOrNull})")

        if (!service.runningGoToDeclarationAction) return null

        val servers = LspServerManagerImpl.getInstanceImpl(file.project).getServersWithThisFileOpen(virtualFile)
        if (servers.isEmpty()) return null

        val document = FileDocumentManager.getInstance().getCachedDocument(virtualFile) ?: return null

        val serverAndLocationLinks = servers.toList()
            .filter { it.descriptor.lspGoToDefinitionSupport }
            .mapNotNull { server ->
                server.requestExecutor.getElementDefinitions(virtualFile, offsetInElement).takeIf { it.isNotEmpty() }?.let { definitions ->
                    LspServerAndLocationLinks(server, definitions)
                }
            }

        if (serverAndLocationLinks.isEmpty()) return null

        val locationLinks = serverAndLocationLinks.flatMap { it.locationLinks }
        val hasLinkWithEndOffsetGreaterThanOffsetInElement = locationLinks.any { link ->
            link.originSelectionRange?.let { range ->
               getOffsetInDocument(document, range.end)?.let { offset ->
                    offset > offsetInElement
                }
            } ?: false
        }
////            serverAndLocationLink.locationLinks.map { link ->
////                link.originSelectionRange?.let { range ->
////                  getRangeInDocument(document, range)
////                } ?: TextRange(offsetInElement, offsetInElement)
////            }.filter { range ->
////                !hasLinkWithEndOffsetGreaterThanOffsetInElement || range.endOffset > offsetInElement
////            }.takeIf { it.isNotEmpty() }?.let { ranges ->
////                LspServerAndLocationLinks(serverAndLocationLink.lspServer, ranges)
////            }
        val filteredServerAndLocationLinks = serverAndLocationLinks.mapNotNull { serverAndLocationLink ->

            serverAndLocationLink.locationLinks. filter{ link ->
                val range = link.originSelectionRange.let { range ->
                    range?.let { range ->
                        getRangeInDocument(document, range)
                    } ?: TextRange(offsetInElement, offsetInElement)
                }
                !hasLinkWithEndOffsetGreaterThanOffsetInElement || range.endOffset > offsetInElement
            }.takeIf { it.isNotEmpty() }?.let { ranges ->
                LspServerAndLocationLinks(serverAndLocationLink.lspServer, ranges)
            }
        }



  val  textRange = filteredServerAndLocationLinks.flatMap { it.locationLinks }.map { link ->
            link.originSelectionRange?.let { range ->
                getRangeInDocument(document, range)
            } ?: TextRange(offsetInElement, offsetInElement)
        }.filter { range ->
            !hasLinkWithEndOffsetGreaterThanOffsetInElement || range.endOffset > offsetInElement
        }.takeIf { it.isNotEmpty() }?.let { ranges ->
            ranges.reduce { acc, range ->
                acc.union(range)
            }
        } ?: TextRange(offsetInElement, offsetInElement)

        return LspSymbolReference(file, textRange, filteredServerAndLocationLinks)
    }
}
private const val TARGET_PRESENTABLE_TEXT_MAX_LENGTH: Int = 20

