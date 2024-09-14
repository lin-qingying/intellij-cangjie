package com.huawei.cangjie.psi.codeFragmentUtil

import com.huawei.cangjie.psi.CjCodeFragment
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjFile
import com.intellij.openapi.util.Key


fun CjElement.suppressDiagnosticsInDebugMode(): Boolean {
    return if (this is CjFile) {
        this.suppressDiagnosticsInDebugMode
    } else {
        val file = this.containingFile
        file is CjFile && file.suppressDiagnosticsInDebugMode
    }
}

var CjFile.suppressDiagnosticsInDebugMode: Boolean
    get() = when (this) {
        is CjCodeFragment -> true
        else -> getUserData(SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE) ?: false
    }
    set(skip) {
        putUserData(SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE, skip)
    }

val DEBUG_TYPE_REFERENCE_STRING: String = "DebugTypeCangJieRulezzzz"
val SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE: Key<Boolean> = Key.create<Boolean>("SUPPRESS_DIAGNOSTICS_IN_DEBUG_MODE")
