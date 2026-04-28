package com.intellij.codeInsight.daemon.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor

/**
 * 当前宿主产品编译面缺失 Kotlin K2 直接依赖的 `HighlightingPsiUtil.hasReferenceInside(...)`。
 *
 * 这里补齐同名入口，只承载 change-locality detector 需要的这一项语义：
 * 只要变更子树里出现任意 PSI reference，就关闭局部 dirty-scope 优化。
 */
object HighlightingPsiUtil {
    fun hasReferenceInside(changedElement: PsiElement): Boolean {
        var hasReference = false

        changedElement.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element.references.isNotEmpty()) {
                    hasReference = true
                    stopWalking()
                    return
                }

                super.visitElement(element)
            }
        })

        return hasReference
    }
}
