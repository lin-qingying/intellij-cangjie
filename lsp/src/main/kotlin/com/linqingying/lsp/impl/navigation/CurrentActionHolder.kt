package com.linqingying.lsp.impl.navigation

import com.intellij.openapi.components.Service

@Service(Service.Level.APP)
  class CurrentActionHolder {
    var runningGoToDeclarationAction: Boolean = false
}
