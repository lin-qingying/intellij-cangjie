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

package cn.cangnova.cangjie.cjpm.project.toolwindow

import cn.cangnova.cangjie.icon.CjpmIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.CachingSimpleNode
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.ui.treeStructure.SimpleTree
import com.intellij.ui.treeStructure.SimpleTreeStructure
import cn.cangnova.cangjie.messages.CangJieBundle
import cn.cangnova.cangjie.cjpm.project.model.CjpmProject
import cn.cangnova.cangjie.cjpm.project.model.impl.workingDirectory
import cn.cangnova.cangjie.cjpm.project.toolwindow.CjpmProjectTreeStructure.CjpmSimpleNode
import cn.cangnova.cangjie.cjpm.project.workspace.CjpmWorkspace
import cn.cangnova.cangjie.cjpm.project.workspace.PackageOrigin
import cn.cangnova.cangjie.ide.run.cjpm.CjpmCommandLine
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeSelectionModel

open class CjpmProjectsTree : SimpleTree() {


    val selectedProject: CjpmProject?
        get() {
            val path = selectionPath ?: return null
            if (path.pathCount < 2) return null
            val treeNode = path.getPathComponent(1) as? DefaultMutableTreeNode ?: return null
            return (treeNode.userObject as? CjpmSimpleNode.Project)?.cjpmProject
        }

    init {
        isRootVisible = false
        showsRootHandles = true
        emptyText.text = "There are no Cjpm projects to display."
        selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        @Suppress("LeakingThis")
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount < 2 || !SwingUtilities.isLeftMouseButton(e)) return
                val tree = e.source as? CjpmProjectsTree ?: return
                val node = tree.selectionModel.selectionPath
                    ?.lastPathComponent as? DefaultMutableTreeNode ?: return
//                val target = (node.userObject as? CjpmSimpleNode.Target)?.target ?: return
//                val command = target.launchCommand()
//                if (command == null) {
//                    LOG.warn("Can't create launch command for `${target.name}` target")
//                    return
//                }
//                val cjpmProject = selectedProject ?: return
//                // Capitalize command name to be consistent with line market providers
//                // TODO: not to use `capitalized` here
//                val configurationName = "${command.capitalized()} ${target.name}"
//                run(CjpmCommandLine.forTarget(target, command), cjpmProject, configurationName)
            }
        })
    }

    protected open fun run(commandLine: CjpmCommandLine, project: CjpmProject, name: String) {
        commandLine.run(project, name)
    }

    companion object {
        private val LOG: Logger = logger<CjpmProjectsTree>()
    }
}

class CjpmProjectTreeStructure(
    tree: CjpmProjectsTree,
    parentDisposable: Disposable,
    private var cjpmProjects: List<CjpmProject> = emptyList()
) : SimpleTreeStructure() {


    private var root = CjpmSimpleNode.Root(cjpmProjects)
    private val treeModel = StructureTreeModel(this, parentDisposable)

    override fun getRootElement(): Any = root

    init {
        tree.model = AsyncTreeModel(treeModel, parentDisposable)
    }

    fun updateCjpmProjects(cjpmProjects: List<CjpmProject>) {
        this.cjpmProjects = cjpmProjects
        root = CjpmSimpleNode.Root(cjpmProjects)
        treeModel.invalidate()
    }

    sealed class CjpmSimpleNode(parent: SimpleNode?) : CachingSimpleNode(parent) {
        abstract fun toTestString(): String

        class WorkspaceMember(val pkg: CjpmWorkspace.Package, parent: SimpleNode) : CjpmSimpleNode(parent) {

            init {
                icon = CjpmIcons.ICON
            }

            override fun buildChildren(): Array<SimpleNode> = arrayOf()
            override fun getName(): String = pkg.name
            override fun toTestString(): String = "WorkspaceMember($name)"
        }

        class Project(val cjpmProject: CjpmProject, parent: SimpleNode) : CjpmSimpleNode(parent) {

            init {
                icon = CjpmIcons.ICON
            }

            override fun buildChildren(): Array<SimpleNode> {
                val (ourPackage, workspaceMembers) = cjpmProject.workspace
                    ?.packages
                    ?.filter { it.origin == PackageOrigin.WORKSPACE }
                    .orEmpty()
                    .sortedBy { it.name }
                    .partition { it.rootDirectory == cjpmProject.workingDirectory }
                val childrenNodes = mutableListOf<SimpleNode>()
//                ourPackage.mapTo(childrenNodes) { Targets(it.targets, this) }
                workspaceMembers.mapTo(childrenNodes) { WorkspaceMember(it, this) }
                return childrenNodes.toTypedArray()
            }

            override fun getName(): String = cjpmProject.presentableName

            override fun update(presentation: PresentationData) {
                var attrs = SimpleTextAttributes.REGULAR_ATTRIBUTES
                when (val status = cjpmProject.mergedStatus) {
                    is CjpmProject.UpdateStatus.UpdateFailed -> {
                        attrs = attrs.derive(SimpleTextAttributes.STYLE_WAVED, null, null, JBColor.RED)
                        presentation.tooltip = status.reason
                    }

                    is CjpmProject.UpdateStatus.NeedsUpdate -> {
                        attrs = attrs.derive(SimpleTextAttributes.STYLE_WAVED, null, null, JBColor.GRAY)
                        presentation.tooltip = CangJieBundle.message("tooltip.project.needs.update")
                    }

                    is CjpmProject.UpdateStatus.UpToDate -> {
                        presentation.tooltip = CangJieBundle.message("tooltip.project.up.to.date")
                    }
                }
                presentation.addText(cjpmProject.presentableName, attrs)
                presentation.setIcon(icon)
            }

            override fun toTestString(): String = "Project"
        }

        class Root(private val cjpmProjects: List<CjpmProject>) : CjpmSimpleNode(null) {
            override fun buildChildren(): Array<SimpleNode> =
                cjpmProjects.map { Project(it, this) }.sortedBy { it.name }.toTypedArray()

            override fun getName(): String = ""
            override fun toTestString(): String = "Root"
        }
    }
}