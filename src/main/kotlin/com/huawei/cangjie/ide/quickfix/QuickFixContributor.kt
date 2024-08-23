package com.huawei.cangjie.ide.quickfix


import com.intellij.openapi.extensions.ExtensionPointName


interface QuickFixContributor {
    companion object {
        val EP_NAME: ExtensionPointName<QuickFixContributor> =
            ExtensionPointName.create("com.huawei.cangjie.quickFixContributor")
    }

    fun registerQuickFixes(quickFixes: QuickFixes)
}
