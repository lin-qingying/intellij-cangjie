package com.huawei.cangjie.incremental

import com.huawei.cangjie.diagnostics.DiagnosticUtils.getLineAndColumnInPsiFile
import com.huawei.cangjie.incremental.components.LocationInfo
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.incremental.components.Position
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.doNotAnalyze


class CangJieLookupLocation(val element: CjElement) : LookupLocation {
    val cachedLocation : LocationInfo? by lazy {
        val containingCjFile = element.getContainingCjFile()

        if (containingCjFile.doNotAnalyze != null)
            null
        else
            object : LocationInfo {
                override val filePath = containingCjFile.virtualFilePath

                override val position: Position
                    get() = getLineAndColumnInPsiFile(containingCjFile, element.textRange).let { Position(it.line, it.column) }
            }
    }

    override val location: LocationInfo?
        get() = cachedLocation
}
