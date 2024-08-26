package com.huawei.cangjie.ide.refactoring.move.changePackage

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.ide.highlighter.namedUnwrappedElement
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

//        fun collectUsages(cangjieToLightElements: Map<CjNamedDeclaration, List<PsiNamedElement>>, result: MutableCollection<UsageInfo>) {
//            cangjieToLightElements.values.flatten().flatMapTo(result) { lightElement ->
//                val searchScope = getSearchScope(lightElement) ?: return@flatMapTo emptyList()
//                val elementName = lightElement.name ?: return@flatMapTo emptyList()
//                val newFqName = StringUtil.getQualifiedName(newContainerName, elementName)
//
//                val foundReferences = HashSet<PsiReference>()
//                val results = ReferencesSearch
//                    .search(lightElement, searchScope)
//                    .mapNotNullTo(ArrayList()) { ref ->
//                        if (foundReferences.add(ref) && elementsToMove.none { it.isAncestor(ref.element) }) {
//                            CangJieMoveRenameUsage.createIfPossible(ref, lightElement, addImportToOriginalFile = true, isInternal = false)
//                        } else null
//                    }
//
//                val name = lightElement.cangjieFqName?.quoteIfNeeded()?.asString()
//                if (name != null) {
//                    fun searchForCangJieNameUsages(results: ArrayList<UsageInfo>) {
//                        TextOccurrencesUtil.findNonCodeUsages(
//                            lightElement,
//                            searchScope,
//                            name,
//                            descriptor.searchInCommentsAndStrings,
//                            descriptor.searchInNonCode,
//                            FqName(newFqName).quoteIfNeeded().asString(),
//                            results
//                        )
//                    }
//
//                    val facadeContainer = lightElement.parent as? CjLightClassForFacade
//                    if (facadeContainer != null) {
//                        val oldFqNameWithFacade = StringUtil.getQualifiedName(facadeContainer.qualifiedName, elementName)
//                        val newFqNameWithFacade = StringUtil.getQualifiedName(
//                            StringUtil.getQualifiedName(newContainerName, facadeContainer.name),
//                            elementName
//                        )
//
//                        TextOccurrencesUtil.findNonCodeUsages(
//                            lightElement,
//                            searchScope,
//                            oldFqNameWithFacade,
//                            descriptor.searchInCommentsAndStrings,
//                            descriptor.searchInNonCode,
//                            FqName(newFqNameWithFacade).quoteIfNeeded().asString(),
//                            results
//                        )
//
//                        ArrayList<UsageInfo>().also { searchForCangJieNameUsages(it) }.forEach { cangjieNonCodeUsage ->
//                            if (results.none { it.intersectsWith(cangjieNonCodeUsage) }) {
//                                results.add(cangjieNonCodeUsage)
//                            }
//                        }
//                    } else {
//                        searchForCangJieNameUsages(results)
//                    }
//                }
//
//                MoveClassHandler.EP_NAME.extensions.filter { it !is MoveCangJieClassHandler }.forEach { handler ->
//                    handler.preprocessUsages(results)
//                }
//
//                results
//            }
//        }

        val usages = ArrayList<UsageInfo>()
//        val moveCheckerInfo = CangJieMoveConflictCheckerInfo(
//            project,
//            elementsToMove,
//            descriptor.moveTarget,
//            elementsToMove.first(),
//            allElementsToMove = descriptor.allElementsToMove
//        )
//        for ((sourceFile, cangjieToLightElements) in cangjieToLightElementsBySourceFile) {
//            val internalUsages = LinkedHashSet<UsageInfo>()
//            val externalUsages = LinkedHashSet<UsageInfo>()
//
//            if (moveEntireFile) {
//                val changeInfo = MoveContainerChangeInfo(
//                    MoveContainerInfo.Package(sourceFile.packageFqName),
//                    descriptor.moveTarget.targetContainerFqName?.let { MoveContainerInfo.Package(it) } ?: MoveContainerInfo.UnknownPackage
//                )
//                internalUsages += sourceFile.getInternalReferencesToUpdateOnPackageNameChange(changeInfo)
//            } else {
//                cangjieToLightElements.keys.forEach {
//                    val packageNameInfo = descriptor.delegate.getContainerChangeInfo(it, descriptor.moveTarget)
//                    internalUsages += it.getInternalReferencesToUpdateOnPackageNameChange(packageNameInfo)
//                }
//            }
//
//            internalUsages += descriptor.delegate.findInternalUsages(descriptor.moveSource)
//            collectUsages(cangjieToLightElements, externalUsages)
//            if (descriptor.analyzeConflicts) {
//                conflicts.putAllValues(checkAllConflicts(moveCheckerInfo, internalUsages, externalUsages))
//                descriptor.delegate.collectConflicts(descriptor.moveTarget, internalUsages, conflicts)
//            }
//
//            usages += internalUsages
//            usages += externalUsages
//        }

        return UsageViewUtil.removeDuplicatedUsages(usages.toTypedArray())
    }

    override fun performRefactoring(usages: Array<out UsageInfo>) = doPerformRefactoring(usages.toList())
    internal fun doPerformRefactoring(usages: List<UsageInfo>) {
//        fun moveDeclaration(declaration: CjNamedDeclaration, moveTarget: CangJieMoveTarget): CjNamedDeclaration {
//            val targetContainer = moveTarget.getOrCreateTargetPsi(declaration)
//            descriptor.delegate.preprocessDeclaration(descriptor.moveTarget, declaration)
//            if (moveEntireFile) return declaration
//            return mover(declaration, targetContainer).apply {
//                addToBeShortenedDescendantsToWaitingSet()
//            }
//        }
//
//        val (oldInternalUsages, externalUsages) = usages.partition { it is CangJieMoveRenameUsage && it.isInternal }
//        val newInternalUsages = ArrayList<UsageInfo>()
//
//        markInternalUsages(oldInternalUsages)
//
//        val usagesToProcess = ArrayList(externalUsages)
//
//        try {
//            descriptor.delegate.preprocessUsages(project, descriptor.moveSource, usages)
//
//            val oldToNewElementsMapping = CollectionFactory.createCustomHashingStrategyMap<PsiElement, PsiElement>(ElementHashingStrategy)
//
//            val newDeclarations = ArrayList<CjNamedDeclaration>()
//
//            for ((sourceFile, kotlinToLightElements) in kotlinToLightElementsBySourceFile) {
//                for ((oldDeclaration, oldLightElements) in kotlinToLightElements) {
//                    val elementListener = transaction?.getElementListener(oldDeclaration)
//
//                    val newDeclaration = moveDeclaration(oldDeclaration, descriptor.moveTarget)
//                    newDeclarations += newDeclaration
//
//                    oldToNewElementsMapping[oldDeclaration] = newDeclaration
//                    oldToNewElementsMapping[sourceFile] = newDeclaration.containingCjFile
//
//                    elementListener?.elementMoved(newDeclaration)
//                    for ((oldElement, newElement) in oldLightElements.asSequence().zip(newDeclaration.toLightElements().asSequence())) {
//                        oldToNewElementsMapping[oldElement] = newElement
//                    }
//
//                    if (descriptor.openInEditor) {
//                        EditorHelper.openInEditor(newDeclaration)
//                    }
//                }
//
//                if (descriptor.deleteSourceFiles && sourceFile.declarations.isEmpty()) {
//                    sourceFile.delete()
//                }
//            }
//
//            val internalUsageScopes: List<CjElement> = if (moveEntireFile) {
//                newDeclarations.asSequence().map { it.containingCjFile }.distinct().toList()
//            } else {
//                newDeclarations
//            }
//            internalUsageScopes.forEach { newInternalUsages += restoreInternalUsages(it, oldToNewElementsMapping) }
//
//            usagesToProcess += newInternalUsages
//            nonCodeUsages = postProcessMoveUsages(usagesToProcess, oldToNewElementsMapping).toTypedArray()
//            performDelayedRefactoringRequests(project)
//        } catch (e: IncorrectOperationException) {
//            nonCodeUsages = null
//            RefactoringUIUtil.processIncorrectOperation(myProject, e)
//        } finally {
//            cleanUpInternalUsages(newInternalUsages + oldInternalUsages)
//        }
    }

    override fun getCommandName(): String = CangJieBundle.message("command.move.declarations")

}
