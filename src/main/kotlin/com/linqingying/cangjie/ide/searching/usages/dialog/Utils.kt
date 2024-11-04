package com.linqingying.cangjie.ide.searching.usages.dialog

import com.linqingying.cangjie.ide.searching.findUsages.CangJieFindUsagesSupport
import com.linqingying.cangjie.psi.CjNamedDeclaration
import com.intellij.ui.SimpleColoredComponent

object Utils {

    fun configureLabelComponent(
        coloredComponent: SimpleColoredComponent,
        declaration: CjNamedDeclaration
    ) {
        @Suppress("HardCodedStringLiteral") val renderedDeclaration: String? =
            CangJieFindUsagesSupport. tryRenderDeclarationCompactStyle(declaration)
        if (renderedDeclaration != null) {
            coloredComponent.append(renderedDeclaration)
        }
    }
}
