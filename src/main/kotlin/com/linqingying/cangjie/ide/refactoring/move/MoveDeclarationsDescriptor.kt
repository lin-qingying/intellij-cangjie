package com.linqingying.cangjie.ide.refactoring.move

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.refactoring.move.MoveCallback

class MoveDeclarationsDescriptor @JvmOverloads constructor(
    val project: Project,
    val moveSource: CangJieMoveSource,
    val moveTarget: CangJieMoveTarget,
    val delegate: CangJieMoveDeclarationDelegate,
    val searchInCommentsAndStrings: Boolean = true,
    val searchInNonCode: Boolean = true,
    val deleteSourceFiles: Boolean = false,
    val moveCallback: MoveCallback? = null,
    val openInEditor: Boolean = false,
    val allElementsToMove: List<PsiElement>? = null,
    val analyzeConflicts: Boolean = true,
    val searchReferences: Boolean = true
)
