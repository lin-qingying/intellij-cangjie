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
) :
    PsiSymbolReference {
    override fun getElement(): PsiElement = psiFile

    override fun getRangeInElement(): TextRange = rangeInFile

    override fun resolveReference(): MutableCollection<out Symbol> {
        val result = mutableListOf<Symbol>()

        for (serverAndLocationLink in lspServerAndLocationLinks) {
            val server = serverAndLocationLink.lspServer
            val locationLinks = serverAndLocationLink.locationLinks

            for (locationLink in locationLinks) {
                val file = server.descriptor.findFileByUri(locationLink.targetUri)
                val navigatableSymbol = file?.let { LspNavigatableSymbol(it, locationLink) }

                navigatableSymbol?.let { result.add(it) }
            }
        }

        return result
    }


}
