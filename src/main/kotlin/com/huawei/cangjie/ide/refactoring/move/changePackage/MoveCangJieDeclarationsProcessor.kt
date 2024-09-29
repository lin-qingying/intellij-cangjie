package com.huawei.cangjie.ide.refactoring.move.changePackage

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.highlighter.namedUnwrappedElement
import com.huawei.cangjie.ide.refactoring.move.CangJieMoveSource
import com.huawei.cangjie.ide.refactoring.move.CangJieMover
import com.huawei.cangjie.ide.refactoring.move.MoveDeclarationsDescriptor
import com.huawei.cangjie.ide.refactoring.move.getTargetModule
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.CjNamedDeclaration
import com.huawei.cangjie.resolve.scopes.projectScope
import com.huawei.cangjie.utils.module
import com.intellij.ide.IdeDeprecatedMessagesBundle
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.move.MoveMultipleElementsViewDescriptor
import com.intellij.refactoring.util.NonCodeUsageInfo
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewDescriptor
import com.intellij.usageView.UsageViewUtil
import com.intellij.util.containers.MultiMap
import kotlin.math.max
import kotlin.math.min


//执行移动重构
open class MoveCangJieDeclarationsProcessor(
    val descriptor: MoveDeclarationsDescriptor,
    val mover: CangJieMover = CangJieMover.Default,
    private val throwOnConflicts: Boolean = false
) : BaseRefactoringProcessor(descriptor.project) {
    companion object {
        const val REFACTORING_ID = "move.cangjie.declarations"
    }

    val project get() = descriptor.project
    private val conflicts = MultiMap<PsiElement, String>()

    private var nonCodeUsages: Array<NonCodeUsageInfo>? = null
    private val moveEntireFile = descriptor.moveSource is CangJieMoveSource.File
    private val elementsToMove = descriptor.moveSource.elementsToMove.filter { e ->
        e.parent != descriptor.moveTarget.getTargetPsiIfExists(e)
    }
    private val cangjieToLightElementsBySourceFile = elementsToMove
        .groupBy { it.getContainingCjFile() }
//        .mapValues { it.value.keysToMap { declaration -> declaration.toLightElements().ifEmpty { listOf(declaration) } } }

    override fun createUsageViewDescriptor(usages: Array<out UsageInfo>): UsageViewDescriptor {
        val targetContainerFqName = descriptor.moveTarget.targetContainerFqName?.let {
            if (it.isRoot) IdeDeprecatedMessagesBundle.message("default.package.presentable.name") else it.asString()
        } ?: IdeDeprecatedMessagesBundle.message("default.package.presentable.name")
        return MoveMultipleElementsViewDescriptor(elementsToMove.toTypedArray(), targetContainerFqName)
    }
    fun execute(usages: List<UsageInfo>) {
        execute(usages.toTypedArray())
    }
    override fun getRefactoringId() = REFACTORING_ID

    public override fun findUsages(): Array<UsageInfo> {
        if (!descriptor.searchReferences || elementsToMove.isEmpty()) return UsageInfo.EMPTY_ARRAY

        val newContainerName = descriptor.moveTarget.targetContainerFqName?.asString() ?: ""

        fun getSearchScope(element: PsiElement): GlobalSearchScope? {
            val projectScope = project.projectScope()
            val cjDeclaration = element.namedUnwrappedElement as? CjNamedDeclaration ?: return projectScope
            if (cjDeclaration.hasModifier(CjTokens.PRIVATE_KEYWORD)) return projectScope
            val moveTarget = descriptor.moveTarget
            val (oldContainer, newContainer) = descriptor.delegate.getContainerChangeInfo(cjDeclaration, moveTarget)
            val targetModule = moveTarget.getTargetModule(project) ?: return projectScope
            if (oldContainer != newContainer || cjDeclaration.module != targetModule) return projectScope
            // Check if facade class may change

            return null
        }

        fun UsageInfo.intersectsWith(usage: UsageInfo): Boolean {
            if (element?.containingFile != usage.element?.containingFile) return false
            val firstSegment = segment ?: return false
            val secondSegment = usage.segment ?: return false
            return max(firstSegment.startOffset, secondSegment.startOffset) <= min(
                firstSegment.endOffset,
                secondSegment.endOffset
            )
        }


        val usages = ArrayList<UsageInfo>()


        return UsageViewUtil.removeDuplicatedUsages(usages.toTypedArray())
    }

    override fun performRefactoring(usages: Array<out UsageInfo>) = doPerformRefactoring(usages.toList())
    internal fun doPerformRefactoring(usages: List<UsageInfo>) {

    }

    override fun getCommandName(): String = CangJieBundle.message("command.move.declarations")

}
