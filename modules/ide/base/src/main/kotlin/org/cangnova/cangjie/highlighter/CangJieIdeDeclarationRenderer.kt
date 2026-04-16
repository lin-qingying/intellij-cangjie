package org.cangnova.cangjie.highlighter

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationNameRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.impl.CaDeclarationRendererForSource
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.name.Name

internal class CangJieIdeDeclarationRenderer(
    private var highlightingManager: CangJieIdeDescriptorRendererHighlightingManager<CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes> = CangJieIdeDescriptorRendererHighlightingManager.NO_HIGHLIGHTING,
    private val rootSymbol: CaDeclarationSymbol? = null
) {


    fun createNameRenderer(): CaDeclarationNameRenderer {
        return CaDeclarationNameRenderer { analysisSession, name, symbol, declarationRenderer, printer ->


            TODO("Not yet implemented")
        }
    }

    internal val renderer = CaDeclarationRendererForSource.WITH_SHORT_NAMES.with {
        nameRenderer = createNameRenderer()


    }
}