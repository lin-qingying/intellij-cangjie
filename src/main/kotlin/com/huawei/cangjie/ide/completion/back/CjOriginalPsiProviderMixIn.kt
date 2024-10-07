package com.huawei.cangjie.ide.completion.back

import com.huawei.cangjie.analyzer.CangJieAnalysisSession
import com.huawei.cangjie.analyzer.components.CangJieAnalysisSessionComponent
import com.huawei.cangjie.analyzer.components.CjAnalysisSessionMixIn
import com.huawei.cangjie.analyzer.lifetime.withValidityAssertion
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjFile

abstract class CangJieOriginalPsiProvider : CangJieAnalysisSessionComponent() {
    abstract fun getOriginalDeclaration(declaration: CjDeclaration): CjDeclaration?
    abstract fun getOriginalCjFile(file: CjFile): CjFile?

    abstract fun recordOriginalDeclaration(fakeDeclaration: CjDeclaration, originalDeclaration: CjDeclaration)
    abstract fun recordOriginalCjFile(fakeFile: CjFile, originalFile: CjFile)
}
class CangJieOriginalPsiProviderImpl(override val analysisSession: CangJieAnalysisSession) : CangJieOriginalPsiProvider() {
    override fun getOriginalDeclaration(declaration: CjDeclaration): CjDeclaration?  = null

    override fun getOriginalCjFile(file: CjFile): CjFile?  = null

    override fun recordOriginalDeclaration(fakeDeclaration: CjDeclaration, originalDeclaration: CjDeclaration) {

    }

    override fun recordOriginalCjFile(fakeFile: CjFile, originalFile: CjFile) {

    }
}

interface CjOriginalPsiProviderMixIn : CjAnalysisSessionMixIn {
    /**
     * Records [file] as an original file for [this].
     */
    fun CjFile.recordOriginalCjFile(file: CjFile) {
        withValidityAssertion { analysisSession.originalPsiProvider.recordOriginalCjFile(this, file) }
    }
    /**
     * Records [declaration] as an original declaration for [this].
     */
    fun CjDeclaration.recordOriginalDeclaration(declaration: CjDeclaration) {
        withValidityAssertion { analysisSession.originalPsiProvider.recordOriginalDeclaration(this, declaration) }
    }
}
