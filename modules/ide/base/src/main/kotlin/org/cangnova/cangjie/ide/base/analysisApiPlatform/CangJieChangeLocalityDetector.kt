package org.cangnova.cangjie.ide.base.analysisApiPlatform

import com.intellij.codeInsight.daemon.ChangeLocalityDetector
import com.intellij.codeInsight.daemon.impl.HighlightingPsiUtil
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.modification.CaSourceModificationService
import org.cangnova.cangjie.lang.CangJieLanguage

/**
 * 对齐 Kotlin K2 的 dirty-scope 检测入口。
 *
 * 这里只负责把 IDE 高亮器的局部重算边界对齐到 analysis-api 的 in-block 失效粒度，
 * 具体是否属于 in-block / out-of-block 仍由 [CaSourceModificationService] 判定。
 */
@OptIn(CaPlatformInterface::class)
class CangJieChangeLocalityDetector : ChangeLocalityDetector {
    override fun getChangeHighlightingDirtyScopeFor(changedElement: PsiElement): PsiElement? {
        if (changedElement.language != CangJieLanguage) {
            return null
        }
        if (HighlightingPsiUtil.hasReferenceInside(changedElement)) {
            return null
        }

        return CaSourceModificationService.getInstance(changedElement.project)
            .ancestorAffectedByInBlockModification(changedElement)
    }
}
