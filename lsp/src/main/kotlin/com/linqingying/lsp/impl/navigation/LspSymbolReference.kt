package com.linqingying.lsp.impl.navigation

import com.intellij.model.Symbol
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class LspSymbolReference(
    val psiFile: PsiFile,
    val rangeInFile: TextRange,
    val lspServerAndLocationLinks: List<LspServerAndLocationLinks>
) : PsiSymbolReference {

    override fun getElement(): PsiElement {
        return psiFile
    }

    override fun getRangeInElement(): TextRange {
        return this.rangeInFile

    }

    override fun resolveReference(): List<LspNavigatableSymbol> {
        val result = mutableListOf<LspNavigatableSymbol>()

        for (link in lspServerAndLocationLinks) {
            val lspServer = link.lspServer
            for (locationLink in link.locationLinks) {
                val uri = locationLink.targetUri ?: continue
                val virtualFile = lspServer.descriptor.findFileByUri(uri)

                if (virtualFile != null) {
                    result.add(LspNavigatableSymbol(virtualFile, locationLink))
                }
            }
        }

        return result
    }

    override fun resolvesTo(target: Symbol): Boolean {
        return false
    }
}
