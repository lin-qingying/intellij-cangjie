package com.linqingying.cangjie.ide.structureView

import com.linqingying.cangjie.ide.codeinsight.CangJieCodeInsightBundle
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.containingTypeStatement
import com.linqingying.cangjie.psi.psiUtil.isPropertyParameter
import com.intellij.icons.AllIcons
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.ui.IconManager
import com.intellij.ui.PlatformIcons


open class CangJieStructureViewModel(cjFile: CjFile, editor: Editor?, rootElement : StructureViewTreeElement) :
    StructureViewModelBase(cjFile, editor, rootElement),
    StructureViewModel.ElementInfoProvider {

    init {
        withSorters(CangJieVisibilitySorter, Sorter.ALPHA_SORTER)
    }

    override fun isSuitable(element: PsiElement?): Boolean =
        element is CjDeclaration &&
                element !is CjPropertyAccessor &&
                element !is CjFunctionLiteral &&
                !((element is CjProperty || element is CjFunction) && !element.topLevelDeclaration && element.containingTypeStatement !is CjNamedDeclaration)

    private val CjDeclaration.topLevelDeclaration: Boolean
        get() = parent is CjFile || parent is CjBlockExpression

    override fun getFilters() = FILTERS

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean {
        val value = element.value
        return (value is CjTypeStatement && value !is CjEnumEntry) || value is CjFile
    }

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean {
        // Local declarations can in any other declaration
        return false
    }

    companion object {
        private val FILTERS = arrayOf(PropertiesFilter, PublicElementsFilter)
    }
}

object CangJieVisibilitySorter : VisibilitySorter() {
    override fun getComparator() = Comparator<Any> { a1, a2 -> a1.accessLevel() - a2.accessLevel() }

    private fun Any.accessLevel() = (this as? AbstractCangJieStructureViewElement)?.accessLevel ?: Int.MAX_VALUE

    override fun getName() = ID

    const val ID = "CANGJIE_VISIBILITY_SORTER"
}

object PublicElementsFilter : Filter {
    override fun isVisible(treeNode: TreeElement): Boolean {
        return (treeNode as? AbstractCangJieStructureViewElement)?.isPublic ?: true
    }

    override fun getPresentation(): ActionPresentation {
        return ActionPresentationData(
            CangJieCodeInsightBundle.message("show.non.public"),
            null,
            IconManager.getInstance().getPlatformIcon(PlatformIcons.Private)
        )
    }

    override fun getName() = ID

    override fun isReverted() = true

    const val ID = "CANGJIE_SHOW_NON_PUBLIC"
}

object PropertiesFilter : Filter {
    override fun isVisible(treeNode: TreeElement): Boolean {
        val element = (treeNode as? AbstractCangJieStructureViewElement)?.element
        val isProperty = element is CjProperty && element.isMember || element is CjParameter && element.isPropertyParameter()
        return !isProperty
    }

    override fun getPresentation(): ActionPresentation {
        return ActionPresentationData(
            CangJieCodeInsightBundle.message("show.properties"), null, AllIcons.Nodes.Property)
    }

    override fun getName() = ID

    override fun isReverted() = true

    const val ID = "CANGJIE_SHOW_PROPERTIES"
}
