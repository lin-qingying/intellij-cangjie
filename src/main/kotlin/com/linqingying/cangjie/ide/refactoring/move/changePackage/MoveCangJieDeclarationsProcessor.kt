/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package com.linqingying.cangjie.ide.refactoring.move.changePackage

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.highlighter.namedUnwrappedElement
import com.linqingying.cangjie.ide.refactoring.move.CangJieMoveSource
import com.linqingying.cangjie.ide.refactoring.move.CangJieMover
import com.linqingying.cangjie.ide.refactoring.move.MoveDeclarationsDescriptor
import com.linqingying.cangjie.ide.refactoring.move.getTargetModule
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.CjNamedDeclaration
import com.linqingying.cangjie.resolve.scopes.projectScope
import com.linqingying.cangjie.utils.module
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
