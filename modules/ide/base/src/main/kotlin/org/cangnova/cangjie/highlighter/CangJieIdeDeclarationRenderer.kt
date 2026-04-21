package org.cangnova.cangjie.highlighter

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationNameRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.impl.CaDeclarationRendererForSource
import org.cangnova.cangjie.analysis.api.symbols.CaClassKind
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.name.Name

internal class CangJieIdeDeclarationRenderer(
    private var highlightingManager: CangJieIdeDescriptorRendererHighlightingManager<CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes> = CangJieIdeDescriptorRendererHighlightingManager.NO_HIGHLIGHTING,
    private val rootSymbol: CaDeclarationSymbol? = null
) {


    fun createNameRenderer(): CaDeclarationNameRenderer {
        return CaDeclarationNameRenderer { analysisSession, name, symbol, declarationRenderer, printer ->
            renderHighlightedName(name, symbol, printer)
        }
    }

    internal val renderer = CaDeclarationRendererForSource.WITH_SHORT_NAMES.with {
        nameRenderer = createNameRenderer()


    }

    /**
     * 声明渲染最终会进入文档 HTML 片段，因此这里先按符号语义生成带样式的字符串，
     * 再整体交给 PrettyPrinter，保持 analysis renderer 与 IDE 高亮职责分离。
     */
    private fun renderHighlightedName(
        name: Name,
        symbol: CaNamedSymbol?,
        printer: PrettyPrinter,
    ) {
        val renderedName = name.asString()
        val highlightedName = buildString {
            with(highlightingManager) {
                appendHighlighted(renderedName, attributeFor(symbol))
            }
        }
        printer.append(highlightedName)
    }

    private fun attributeFor(
        symbol: CaNamedSymbol?,
    ): CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes =
        with(highlightingManager) {
            when (symbol) {
                is CaClassSymbol -> when (symbol.classKind) {
                    CaClassKind.CLASS -> asClassName
                    CaClassKind.INTERFACE -> asInterfaceName
                    CaClassKind.STRUCT -> asStructName
                    CaClassKind.ENUM -> asEnumName
                }

                is CaTypeAliasSymbol -> asTypeAlias
                is CaTypeParameterSymbol -> asTypeParameterName
                is CaNamedFunctionSymbol -> asFunDeclaration
                is CaValueParameterSymbol -> asParameter
                is CaPropertySymbol -> asInstanceProperty
                is CaFieldSymbol -> asInstanceVariable
                is CaLocalVariableSymbol -> asLocalVarOrLet
                is CaPackageSymbol -> asPackageName
                else -> asInfo
            }
        }
}
