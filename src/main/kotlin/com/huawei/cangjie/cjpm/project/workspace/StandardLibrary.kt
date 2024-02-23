package com.huawei.cangjie.cjpm.project.workspace

import CjpmWorkspaceData

//
data class StandardLibrary(
    val workspaceData: CjpmWorkspaceData,
    val isHardcoded: Boolean,
    val isPartOfCjpmProject: Boolean = false
)


object AutoInjectedCrates {
    const val STD: String = "std"
    const val CORE: String = "core"
    const val TEST: String = "test"
}