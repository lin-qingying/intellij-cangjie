package com.huawei.cangjie.ide.lineMarkers

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.descriptors.CallableMemberDescriptor
import com.huawei.cangjie.descriptors.Modality
import com.huawei.cangjie.ide.lineMarkers.markers.*
import com.huawei.cangjie.ide.presentation.DeclarationByModuleRenderer
import com.huawei.cangjie.ide.search.ClassInheritorsSearch
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.isInheritable
import com.intellij.codeInsight.daemon.DaemonBundle
import com.intellij.codeInsight.daemon.NavigateAction
import com.intellij.codeInsight.daemon.impl.InheritorsLineMarkerNavigator
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.psi.util.PsiTreeUtil


class CangJieLineMarkerProvider : AbstractCangJieLineMarkerProvider() {

    override fun doCollectSlowLineMarkers(elements: List<PsiElement>, result: LineMarkerInfos) {
        val functions = hashSetOf<CjNamedFunction>()
        val properties = hashSetOf<CjNamedDeclaration>()
        val declarations = hashSetOf<CjNamedDeclaration>()

        for (leaf in elements) {
            ProgressManager.checkCanceled()
            if (/*leaf !is PsiIdentifier && */leaf.firstChild != null) continue
            val element = leaf.parent as? CjNamedDeclaration ?: continue
            if (!declarations.add(element)) continue

            when (element) {
                is CjTypeStatement -> {
                    collectInheritedClassMarker(element, result)
//                    collectHighlightingColorsMarkers(element, result)
                }
//                is CjClass -> {
//                    collectInheritedClassMarker(element, result)
//                    collectHighlightingColorsMarkers(element, result)
//                }
                is CjNamedFunction -> {
                    functions.add(element)
                    collectSuperDeclarationMarkers(element, result)
                }

                is CjProperty -> {
                    properties.add(element)
                    collectSuperDeclarationMarkers(element, result)
                }

                is CjParameter -> {
                    if (element.hasLetOrVar()) {
                        properties.add(element)
                        collectSuperDeclarationMarkers(element, result)
                    }
                }
            }
//            collectMultiplatformMarkers(element, result)
        }

//        collectOverriddenFunctions(functions, result)
//        collectOverriddenPropertyAccessors(properties, result)
    }
}

private fun isImplementsAndNotOverrides(
    descriptor: CallableMemberDescriptor,
    overriddenMembers: Collection<CallableMemberDescriptor>
): Boolean = descriptor.modality != Modality.ABSTRACT && overriddenMembers.all { it.modality == Modality.ABSTRACT }

private fun collectSuperDeclarationMarkers(declaration: CjDeclaration, result: LineMarkerInfos) {
    assert(declaration is CjNamedFunction || declaration is CjProperty || declaration is CjParameter)
    declaration as CjNamedDeclaration // implied by assert

//    if (!declaration.hasModifier(CjTokens.OVERRIDE_KEYWORD)) return

    val resolveWithParents = resolveDeclarationWithParents(declaration)
    if (resolveWithParents.overriddenDescriptors.isEmpty()) return

    val implements =
        isImplementsAndNotOverrides(resolveWithParents.descriptor!!, resolveWithParents.overriddenDescriptors)

    val anchor = declaration.nameAnchor()

    // NOTE: Don't store descriptors in line markers because line markers are not deleted while editing other files and this can prevent
    // clearing the whole BindingTrace.

    val gutter =
        if (implements) CangJieLineMarkerOptions.implementingOption else CangJieLineMarkerOptions.overridingOption
    if (!gutter.isEnabled) return

    val lineMarkerInfo = InheritanceMergeableLineMarkerInfo(
        anchor,
        anchor.textRange,
        gutter.icon!!,
        SuperDeclarationMarkerTooltip,
        SuperDeclarationMarkerNavigationHandler(),
        GutterIconRenderer.Alignment.RIGHT,
    ) { gutter.name }

    NavigateAction.setNavigateAction(
        lineMarkerInfo,
        if (declaration is CjNamedFunction)
            CangJieBundle.message("highlighter.action.text.go.to.super.method")
        else
            CangJieBundle.message("highlighter.action.text.go.to.super.property"),
        IdeActions.ACTION_GOTO_SUPER,
        gutter.icon
    )
    result.add(lineMarkerInfo)
}

private fun collectInheritedClassMarker(element: CjTypeStatement, result: LineMarkerInfos) {
    if (!element.isInheritable()) {
        return
    }
    val gutter =
        if (element.isInterface()) CangJieLineMarkerOptions.implementedOption else CangJieLineMarkerOptions.overriddenOption
    if (!gutter.isEnabled) return

//    val lightClass = element.toLightClass() ?: element.toFakeLightClass()
//
    if (ClassInheritorsSearch.search(element, false).findFirst() == null) return

    val anchor = element.nameIdentifier ?: element
    val icon = gutter.icon ?: return
    val lineMarkerInfo = InheritanceMergeableLineMarkerInfo(
        anchor,
        anchor.textRange,
        icon,
        SUBCLASSED_CLASS.tooltip,
        SUBCLASSED_CLASS.navigationHandler,
        GutterIconRenderer.Alignment.RIGHT
    ) { gutter.name }

    NavigateAction.setNavigateAction(
        lineMarkerInfo,
        if (element is CjInterface)
            CangJieBundle.message("highlighter.action.text.go.to.implementations")
        else
            CangJieBundle.message("highlighter.action.text.go.to.subclasses"),
        IdeActions.ACTION_GOTO_IMPLEMENTATION,
        gutter.icon
    )
    result.add(lineMarkerInfo)
}

private fun collectOverriddenPropertyAccessors(
    properties: Collection<CjNamedDeclaration>,
    result: LineMarkerInfos
) {


}

private fun collectMultiplatformMarkers(
    declaration: CjNamedDeclaration,
    result: LineMarkerInfos
) {

}


private fun collectOverriddenFunctions(functions: Collection<CjNamedFunction>, result: LineMarkerInfos) {

}

private fun CjNamedDeclaration.nameAnchor(): PsiElement =
    nameIdentifier ?: PsiTreeUtil.getDeepestVisibleFirst(this) ?: this

fun getCjClass(element: PsiElement?): CjTypeStatement? {
    return when {
        element == null -> null

        element is CjClass -> element
        element.parent is CjClass -> getCjClass(element.parent)
        else -> null
    }
}

val SUBCLASSED_CLASS: MarkerType = MarkerType(
    "SUBCLASSED_CLASS",
    { getCjClass(it)?.let(::getModuleSpecificSubclassedClassTooltip) },
    object : InheritorsLineMarkerNavigator() {
        override fun getMessageForDumbMode() = CangJieBundle.message("notification.navigation.to.overriding.classes")
    })

// Module-specific version of MarkerType.getSubclassedClassTooltip
fun getModuleSpecificSubclassedClassTooltip(cclass: CjTypeStatement): String {
    val processor = PsiElementProcessor.CollectElementsWithLimit(5, HashSet<CjTypeStatement>())

    if (processor.isOverflow) {
        return if (cclass.isInterface()) DaemonBundle.message("method.is.implemented.too.many") else DaemonBundle.message(
            "class.is.subclassed.too.many"
        )
    }

    val subclasses = processor.toArray(CjTypeStatement.EMPTY_ARRAY)
//    if (subclasses.isEmpty()) {
//        val functionalImplementations = PsiElementProcessor.CollectElementsWithLimit(2, HashSet<PsiFunctionalExpression>())
//        FunctionalExpressionSearch.search(cclass).forEach(PsiElementProcessorAdapter(functionalImplementations))
//        return if (functionalImplementations.collection.isNotEmpty())
//            CangJieBundle.message("highlighter.text.has.functional.implementations")
//        else
//            null
//    }

    val comparator = DeclarationByModuleRenderer().comparator
    val start =
        CangJieBundle.message(if (cclass.isInterface()) "tooltip.is.implemented.by" else "tooltip.is.subclassed.by")

    return CangJieGutterTooltipHelper.buildTooltipText(
        subclasses.sortedWith(comparator),
        start, true, IdeActions.ACTION_GOTO_IMPLEMENTATION
    )
}
