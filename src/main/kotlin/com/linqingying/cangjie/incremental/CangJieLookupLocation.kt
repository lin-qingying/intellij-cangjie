package com.linqingying.cangjie.incremental

import com.linqingying.cangjie.diagnostics.DiagnosticUtils.getLineAndColumnInPsiFile
import com.linqingying.cangjie.incremental.components.LocationInfo
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.incremental.components.Position
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.doNotAnalyze


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
