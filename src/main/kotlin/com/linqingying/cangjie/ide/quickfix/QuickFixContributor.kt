package com.linqingying.cangjie.ide.quickfix


import com.intellij.openapi.extensions.ExtensionPointName


interface QuickFixContributor {
    companion object {
        val EP_NAME: ExtensionPointName<QuickFixContributor> =
            ExtensionPointName.create("com.linqingying.cangjie.quickFixContributor")
    }

    fun registerQuickFixes(quickFixes: QuickFixes)
}
