package com.huawei.cangjie.ide.refactoring.move

import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.CjNamedDeclaration
import com.huawei.cangjie.psi.CjTypeStatement
import com.huawei.cangjie.psi.psiUtil.deleteSingle
import com.huawei.cangjie.psi.psiUtil.getElementTextWithContext
import com.huawei.cangjie.utils.CangJieExceptionWithAttachments
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
//                val container = originalElement.containingClassOrObject
//                if (container is CjObjectDeclaration
//                    && container.isCompanion()
//                    && container.declarations.singleOrNull() == originalElement
//                    && ReferencesSearch.search(container, LocalSearchScope(container.containingFile)).findAll().isEmpty()
//                ) {
//                    container.deleteSingle()
//                } else {
                    originalElement.deleteSingle()
//                }

            }
        }
    }
}
