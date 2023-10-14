package com.huawei.cangjie.ide.presentation

import com.huawei.cangjie.lang.core.psi.CjFunction
import com.huawei.cangjie.lang.core.psi.ext.CjElement
import com.huawei.cangjie.lang.core.psi.ext.CjNamedElement
import com.huawei.cangjie.lang.core.psi.ext.functionName
import com.huawei.cangjie.lang.core.psi.impl.CjItemImpl
import com.intellij.ide.projectView.PresentationData
import com.intellij.navigation.ItemPresentation

fun getPresentation(psi: CjElement): ItemPresentation {
    val location = run {
        val mod = psi.containingMod
//        "(in ${mod.qualifiedName ?: mod.modName ?: psi.containingFile.name})"
        "(in ${mod.modName ?: psi.containingFile.name})"
    }

    val name = presentableName(psi)
    return PresentationData(name, location, psi.getIcon(0), null)
}
private fun presentableName(psi: CjElement): String? {
    return when (psi) {
        is CjFunction -> psi.functionName
        is CjNamedElement -> psi.name

        else -> null
    }
}
