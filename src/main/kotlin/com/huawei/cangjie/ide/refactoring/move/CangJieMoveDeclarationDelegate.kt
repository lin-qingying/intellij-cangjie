package com.huawei.cangjie.ide.refactoring.move

import com.huawei.cangjie.psi.CjNamedDeclaration
import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap

sealed interface CangJieMoveDeclarationDelegate {
    fun collectConflicts(
        moveTarget: CangJieMoveTarget,
        internalUsages: MutableSet<UsageInfo>,
        conflicts: MultiMap<PsiElement, String>
    ) {
    }

    fun getContainerChangeInfo(
        originalDeclaration: CjNamedDeclaration,
        moveTarget: CangJieMoveTarget
    ): MoveContainerChangeInfo

    fun findInternalUsages(moveSource: CangJieMoveSource): List<UsageInfo> = emptyList()

    object TopLevel : CangJieMoveDeclarationDelegate {
        override fun getContainerChangeInfo(
            originalDeclaration: CjNamedDeclaration,
            moveTarget: CangJieMoveTarget
        ): MoveContainerChangeInfo {
            val sourcePackage = MoveContainerInfo.Package(originalDeclaration.getContainingCjFile().packageFqName)
            val targetPackage = moveTarget.targetContainerFqName?.let { MoveContainerInfo.Package(it) }
                ?: MoveContainerInfo.UnknownPackage
            return MoveContainerChangeInfo(sourcePackage, targetPackage)
        }
    }

}
