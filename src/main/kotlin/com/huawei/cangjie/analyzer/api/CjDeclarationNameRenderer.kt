package com.huawei.cangjie.analyzer.api

import com.huawei.cangjie.analyzer.CjAnalysisSession
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.name.SpecialNames
import com.huawei.cangjie.renderer.render

interface CjDeclarationNameRenderer {
    context(CjAnalysisSession, CjDeclarationRenderer)
    fun renderName(symbol: CjNamedSymbol, printer: PrettyPrinter): Unit =
        renderName(symbol.name, symbol, printer)

    context(CjAnalysisSession, CjDeclarationRenderer)
    fun renderName(name: Name, symbol: CjNamedSymbol?, printer: PrettyPrinter)

    object QUOTED : CjDeclarationNameRenderer {
        context(CjAnalysisSession, CjDeclarationRenderer)
        override fun renderName(name: Name, symbol: CjNamedSymbol?, printer: PrettyPrinter) {
            if (symbol is CjClassOrObjectSymbol && symbol.classKind == CjClassKind.COMPANION_OBJECT && symbol.name == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
                return
            printer.append(name.render())
        }
    }

    object UNQUOTED : CjDeclarationNameRenderer {
        context(CjAnalysisSession, CjDeclarationRenderer)
        override fun renderName(name: Name, symbol: CjNamedSymbol?, printer: PrettyPrinter) {
            if (symbol is CjClassOrObjectSymbol && symbol.classKind == CjClassKind.COMPANION_OBJECT && symbol.name == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
                return
            printer.append(name.asString())
        }
    }
}
