package com.linqingying.cangjie.ide.refactoring.move

import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.psi.CjNamedDeclaration
import com.linqingying.cangjie.psi.CjTypeStatement
import com.linqingying.cangjie.psi.psiUtil.deleteSingle
import com.linqingying.cangjie.psi.psiUtil.getElementTextWithContext
import com.linqingying.cangjie.utils.CangJieExceptionWithAttachments
import com.intellij.psi.search.searches.ReferencesSearch


interface CangJieMover : (CjNamedDeclaration, CjElement) -> CjNamedDeclaration {

    object Default : CangJieMover {
        override fun invoke(originalElement: CjNamedDeclaration, targetContainer: CjElement): CjNamedDeclaration {
            return when (targetContainer) {
                is CjFile -> {
                    val declarationContainer: CjElement = targetContainer
                    declarationContainer.add(originalElement) as CjNamedDeclaration
                }

                is CjTypeStatement -> targetContainer.addDeclaration(originalElement)
                else -> throw CangJieExceptionWithAttachments("Unexpected element")
                    .withAttachment("context", targetContainer.getElementTextWithContext())
            }.apply {

                    originalElement.deleteSingle()


            }
        }
    }
}
