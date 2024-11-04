package com.linqingying.cangjie.resolve

enum class TopDownAnalysisMode(val isLocalDeclarations: Boolean) {
    LocalDeclarations(true),
    TopLevelDeclarations(false)
}
