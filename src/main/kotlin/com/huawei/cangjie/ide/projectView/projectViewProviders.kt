package com.huawei.cangjie.ide.projectView

import com.huawei.cangjie.ide.AbstractCangJieIconProvider
import com.huawei.cangjie.psi.CjClassBody
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.CjTypeStatement
import com.huawei.cangjie.psi.psiUtil.parentsWithSelf
import com.intellij.ide.projectView.SelectableTreeStructureProvider
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.FileNodeWithNestedFileNodes
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement

class CangJieExpandNodeProjectViewProvider: TreeStructureProvider, DumbAware {
    override fun modify(
        parent: AbstractTreeNode<*>,
        children: Collection<AbstractTreeNode<*>>,
        settings: ViewSettings
    ): MutableCollection<AbstractTreeNode<*>> {
        val result = ArrayList<AbstractTreeNode<out Any>>()

        for (child in children) {
            val value = child.value
            val cjFile = value?.asCjFile()
            val nestedFileNodes = (child as? FileNodeWithNestedFileNodes)?.nestedFileNodes ?: emptyList()

            if (cjFile != null) {
                val mainClass = AbstractCangJieIconProvider.getSingleClass(cjFile)
                if (mainClass != null) {
                    CjTypeStatement
                    result.add(CjTypeStatementTreeNode(cjFile.project, mainClass, settings, nestedFileNodes))
                } else {
                    result.add(CjFileTreeNode(cjFile.project, cjFile, settings, nestedFileNodes))
                }
            } else {

                    result.add(child)

            }

        }

        return result
    }
    private fun Any.asCjFile(): CjFile? = when (this) {
        is CjFile -> this
//        is CjLightClassForFacade -> files.singleOrNull()
//        is CjLightClass -> cangjieOrigin?.containingFile as? CjFile
        else -> null
    }
}

class CangJieSelectInProjectViewProvider(private val project: Project) : SelectableTreeStructureProvider, DumbAware {
    override fun modify(
        parent: AbstractTreeNode<*>,
        children: MutableCollection<AbstractTreeNode<*>>,
        settings: ViewSettings?
    ): MutableCollection<AbstractTreeNode<*>> {
        return ArrayList(children)

    }

    private fun PsiElement.isSelectable(): Boolean = when (this) {
        is CjFile -> true
        is CjDeclaration -> parent is CjFile || ((parent as? CjClassBody)?.parent as? CjTypeStatement)?.isSelectable() ?: false
        else -> false
    }

    private fun fileInRoots(file: VirtualFile?): Boolean {
        val index = ProjectRootManager.getInstance(project).fileIndex
        return file != null && (index.isInSourceContent(file) || index.isInLibraryClasses(file) || index.isInLibrarySource(file))
    }
    override fun getTopLevelElement(element: PsiElement ): PsiElement? {
        if (!element.isValid) return null
        val file = element.containingFile as? CjFile ?: return null

        val virtualFile = file.virtualFile
        if (!fileInRoots(virtualFile)) return file

        var current = element.parentsWithSelf.firstOrNull { it.isSelectable() }

        if (current is CjFile) {
            val declaration = current.declarations.singleOrNull()
            val nameWithoutExtension = virtualFile?.nameWithoutExtension ?: file.name
            if (declaration is CjTypeStatement && nameWithoutExtension == declaration.name) {
                current = declaration
            }
        }

        return current ?: file
    }
}
