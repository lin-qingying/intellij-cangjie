/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.project.ui.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.project.Project
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.CachingSimpleNode
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.ui.treeStructure.SimpleTree
import com.intellij.util.ui.tree.TreeUtil
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.model.CjWorkspace
import javax.swing.Icon

/**
 * 仓颉项目树形视图
 *
 * 显示项目层次结构: Root → Workspace → Project → Module
 */
class CjProjectTree(private val project: Project) : SimpleTree(), DataProvider, Disposable {

    private val treeStructure: CjProjectTreeStructure = CjProjectTreeStructure(project)
    private val structureModel: StructureTreeModel<CjProjectTreeStructure>
    private val asyncModel: AsyncTreeModel

    init {
        structureModel = StructureTreeModel(treeStructure, this)
        asyncModel = AsyncTreeModel(structureModel, this)
        model = asyncModel

        isRootVisible = false
        showsRootHandles = true

        TreeUtil.installActions(this)
    }

    /**
     * 更新显示的项目列表
     */
    fun updateProjects(projects: List<CjProject>) {
        treeStructure.updateProjects(projects)
        structureModel.invalidate()
    }

    override fun getData(dataId: String): Any? {
        if (CjProjectToolWindow.SELECTED_CJ_PROJECT.`is`(dataId)) {
            val selectedNode = selectedNode as? CjProjectNode
            return selectedNode?.project
        }
        return null
    }

    override fun dispose() {
        // Cleanup resources if needed
    }
}

/**
 * 树结构定义
 */
private class CjProjectTreeStructure(private val project: Project) : AbstractTreeStructure() {

    private val rootElement = RootNode(project)

    fun updateProjects(projects: List<CjProject>) {
        rootElement.updateProjects(projects)
    }

    override fun getRootElement(): Any = rootElement

    override fun getChildElements(element: Any): Array<Any> {
        return when (element) {
            is SimpleNode -> element.children.map { it as Any }.toTypedArray()
            else -> emptyArray()
        }
    }

    override fun getParentElement(element: Any): Any? {
        return (element as? SimpleNode)?.parent
    }

    override fun createDescriptor(element: Any, parentDescriptor: NodeDescriptor<*>?): NodeDescriptor<*> {
        return when (element) {
            is SimpleNode -> element
            else -> object : NodeDescriptor<Any>(project, parentDescriptor) {
                override fun update(): Boolean {
                    return false
                }

                override fun getElement(): Any = element
            }
        }
    }

    override fun commit() {}

    override fun hasSomethingToCommit(): Boolean = false
}

/**
 * 根节点
 */
private class RootNode(project: Project) : CachingSimpleNode(project, null) {

    private var projects: List<CjProject> = emptyList()

    fun updateProjects(newProjects: List<CjProject>) {
        projects = newProjects
        cleanUpCache()
    }

    override fun buildChildren(): Array<SimpleNode> {
        // 按工作空间分组
        val workspaceProjects = projects.filter { it.workspace != null }
            .groupBy { it.workspace!! }
        val standaloneProjects = projects.filter { it.workspace == null }

        val nodes = mutableListOf<SimpleNode>()

        // 添加工作空间节点
        workspaceProjects.forEach { (workspace, projectsInWorkspace) ->
            nodes.add(WorkspaceNode(myProject, this, workspace, projectsInWorkspace))
        }

        // 添加独立项目节点
        standaloneProjects.forEach { project ->
            nodes.add(CjProjectNode(myProject, this, project))
        }

        return nodes.toTypedArray()
    }

    override fun getName(): String = "Root"
}

/**
 * 工作空间节点
 */
private class WorkspaceNode(
    project: Project,
    parent: SimpleNode,
    private val workspace: CjWorkspace,
    private val projects: List<CjProject>
) : CachingSimpleNode(project, parent) {

    override fun buildChildren(): Array<SimpleNode> {
        return projects.map { CjProjectNode(myProject, this, it) }.toTypedArray()
    }

    override fun update(presentation: PresentationData) {
        presentation.presentableText = workspace.name
        presentation.setIcon(AllIcons.Nodes.ModuleGroup)
        presentation.tooltip = "Workspace: ${workspace.rootDir.path}"
    }

    override fun getName(): String = workspace.name
}

/**
 * 项目节点
 */
private class CjProjectNode(
    intellijProject: Project,
    parent: SimpleNode,
    val project: CjProject
) : CachingSimpleNode(intellijProject, parent) {

    override fun buildChildren(): Array<SimpleNode> {
        return this.project.modules.map { ModuleNode(myProject, this, it) }.toTypedArray()
    }

    override fun update(presentation: PresentationData) {
        presentation.presentableText = project.name
        presentation.setIcon(getProjectIcon())
        presentation.tooltip = "Project: ${project.rootDir.path}"
    }

    private fun getProjectIcon(): Icon {
        return if (project.isValid) {
            AllIcons.Nodes.Module
        } else {
            AllIcons.Nodes.ExceptionClass
        }
    }

    override fun getName(): String = project.name
}

/**
 * 模块节点
 */
private class ModuleNode(
    project: Project,
    parent: SimpleNode,
    private val module: CjModule
) : CachingSimpleNode(project, parent) {

    override fun buildChildren(): Array<SimpleNode> {
        // 模块是叶子节点
        return emptyArray()
    }

    override fun update(presentation: PresentationData) {
        presentation.presentableText = module.name
        presentation.setIcon(AllIcons.Nodes.Package)
        presentation.tooltip = "Module: ${module.rootDir.path}"
    }

    override fun getName(): String = module.name
}