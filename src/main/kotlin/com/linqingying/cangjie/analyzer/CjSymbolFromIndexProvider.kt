package com.linqingying.cangjie.analyzer

import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjFile
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope


class CjSymbolFromIndexProvider private constructor(
    private val useSiteFile: CjFile,
    private val scope: GlobalSearchScope,
) {

    private val project: Project = useSiteFile.project

    companion object {
        fun createForElement(useSiteCjElement: CjElement): CjSymbolFromIndexProvider = analyze(useSiteCjElement) {
            CjSymbolFromIndexProvider(useSiteCjElement.getContainingCjFile(), analysisScope)
        }
    }
}
