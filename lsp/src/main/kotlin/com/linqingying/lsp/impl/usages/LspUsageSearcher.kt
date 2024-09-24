package com.linqingying.lsp.impl.usages

import com.intellij.find.usages.api.PsiUsage
import com.intellij.find.usages.api.Usage
import com.intellij.find.usages.api.UsageSearchParameters
import com.intellij.find.usages.api.UsageSearcher
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiManager
import com.intellij.util.AbstractQuery
import com.intellij.util.Processor
import com.intellij.util.Query
import com.linqingying.lsp.api.LspServer
import com.linqingying.lsp.util.getRangeInDocument
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.ReferenceContext
import org.eclipse.lsp4j.ReferenceParams
import org.eclipse.lsp4j.services.LanguageServer

private const val FIND_REFERENCES_TIMEOUT_MS: Int = 60000

  class LspUsageSearcher : UsageSearcher {
    override fun collectSearchRequest(parameters: UsageSearchParameters): Query<Usage>? {
        val searchTarget = parameters.target
        val lspSearchTarget = searchTarget as? LspSearchTarget ?: return null
        return LspReferencesQuery(lspSearchTarget)
    }
}

private class LspReferencesQuery(val searchTarget: LspSearchTarget) :
    AbstractQuery<Usage>() {
    private fun processReferences(lspServer: LspServer, references: List<Location>, consumer: Processor<in Usage>) {
        val psiManager = PsiManager.getInstance(lspServer.project)

        references.forEach { location ->
            val uri = location.uri
            val virtualFile = lspServer.descriptor.findFileByUri(uri) ?: return@forEach
            val psiFile = psiManager.findFile(virtualFile) ?: return@forEach
            val document = psiFile.viewProvider.document ?: return@forEach
            val textRange = getRangeInDocument(document, location.range) ?: return@forEach

            consumer.process(PsiUsage.textUsage(psiFile, textRange))
        }
    }

    override fun processResults(consumer: Processor<in Usage>): Boolean {
        searchTarget.lspServers.forEach { lspServer ->
            val documentIdentifier = lspServer.getDocumentIdentifier(searchTarget.file)
            val referenceParams = ReferenceParams(documentIdentifier, searchTarget.position, ReferenceContext(true))

            val references = lspServer.sendRequestSync(60000) { languageServer: LanguageServer ->
                languageServer.textDocumentService.references(referenceParams)
            } as? List<Location> ?: return@forEach

            if (references.isNotEmpty()) {
                runReadAction {
                    processReferences(lspServer, references, consumer)
                }
            }
        }
        return true
    }
}
