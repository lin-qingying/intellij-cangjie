package com.linqingying.lsp.impl.navigation

import com.intellij.openapi.components.Service

@Service
class CurrentActionHolder {
    var runningGoToDeclarationAction: Boolean = false
}
