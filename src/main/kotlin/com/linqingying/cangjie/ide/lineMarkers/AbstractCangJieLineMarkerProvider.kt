package com.linqingying.cangjie.ide.lineMarkers

import com.linqingying.cangjie.ide.lineMarkers.markers.CangJieLineMarkerOptions
import com.linqingying.cangjie.ide.base.projectStructure.RootKindFilter
import com.linqingying.cangjie.ide.base.projectStructure.matches
import com.linqingying.cangjie.psi.CjClassInitializer
import com.linqingying.cangjie.psi.CjFunction
import com.linqingying.cangjie.psi.CjProperty
import com.linqingying.cangjie.psi.CjVariable
import com.linqingying.cangjie.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.SeparatorPlacement
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
typealias LineMarkerInfos = MutableCollection<in LineMarkerInfo<*>>

abstract class AbstractCangJieLineMarkerProvider: LineMarkerProviderDescriptor() {
    override fun getName(): String = CangJieLineMarkersSharedBundle.message("highlighter.name.cangjie.line.markers")

    override fun getOptions(): Array<Option> = CangJieLineMarkerOptions.options

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiElement>? {
        if (DaemonCodeAnalyzerSettings.getInstance().SHOW_METHOD_SEPARATORS) {
            if (element.canHaveSeparator()) {
                val prevSibling = element.getPrevSiblingIgnoringWhitespaceAndComments()
                if (prevSibling.canHaveSeparator() &&
                    (element.wantsSeparator() || prevSibling?.wantsSeparator() == true)
                ) {
                    return createLineSeparatorByElement(element)
                }
            }
        }

        return null
    }


    private fun PsiElement?.canHaveSeparator(): Boolean =
        this is CjFunction
                || this is CjClassInitializer
                || (this is CjProperty && !isLocal)

                || (this is CjVariable && !isLocal)


    private fun PsiElement.wantsSeparator(): Boolean = this is CjFunction || StringUtil.getLineBreakCount(text) > 0

    private fun createLineSeparatorByElement(element: PsiElement): LineMarkerInfo<PsiElement> {
        val anchor = PsiTreeUtil.getDeepestFirst(element)

        val info = LineMarkerInfo(anchor, anchor.textRange)
        info.separatorColor = EditorColorsManager.getInstance().globalScheme.getColor(CodeInsightColors.METHOD_SEPARATORS_COLOR)
        info.separatorPlacement = SeparatorPlacement.TOP
        return info
    }

    final override fun collectSlowLineMarkers(elements: List<PsiElement>, result: LineMarkerInfos) {
        if (elements.isEmpty()) return
        if (CangJieLineMarkerOptions.options.none { option -> option.isEnabled }) return

        val first = elements.first()
        if (DumbService.getInstance(first.project).isDumb || !RootKindFilter.projectAndLibrarySources.matches(first)) return

        doCollectSlowLineMarkers(elements, result)
    }

    abstract fun doCollectSlowLineMarkers(elements: List<PsiElement>, result: LineMarkerInfos)

}
