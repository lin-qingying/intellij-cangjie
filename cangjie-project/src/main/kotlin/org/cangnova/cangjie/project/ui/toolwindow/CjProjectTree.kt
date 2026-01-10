/*
 * Copyright 2026 LinQingYing. and contributors.
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
import org.cangnova.cangjie.project.model.CjDependency
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.model.CjWorkspace
import org.cangnova.cangjie.project.service.CjProjectsService
import javax.swing.Icon

/**
 * 仓颉项目树形视图
 *
 * 显示项目层次结构: Root → Project → Workspace → Module
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
    fun updateProjects(projects: CjProject) {
        treeStructure.updateProjects(projects)
        // 强制刷新整个树结构，递归清除所有子节点的缓存
        structureModel.invalidateAsync(treeStructure.rootElement, true)
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

    fun updateProjects(projects: CjProject) {
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

    private var projects: CjProject = CjProjectsService.getInstance(project).noProjectMarker

    fun updateProjects(newProjects: CjProject) {
        projects = newProjects
        cleanUpCache()
    }

    override fun buildChildren(): Array<SimpleNode> {
        // 直接返回所有项目作为根节点的子节点
        return listOf(CjProjectNode(myProject, this, projects)).toTypedArray()
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
    private val modules: List<CjModule>
) : CachingSimpleNode(project, parent) {

    override fun buildChildren(): Array<SimpleNode> {
        return modules.map { ModuleNode(myProject, this, it) }.toTypedArray()
    }

    override fun update(presentation: PresentationData) {
        presentation.presentableText = workspace.name
        presentation.setIcon(AllIcons.Nodes.Folder)
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
        val nodes = mutableListOf<SimpleNode>()

        if (project.isWorkspace) {
            // 如果是工作空间项目，添加工作空间节点
            project.workspace?.let { workspace ->
                nodes.add(WorkspaceNode(myProject, this, workspace, workspace.modules))
            }
        } else {
            // 如果是单模块项目，直接添加模块节点
            project.module?.let { module ->
                nodes.add(ModuleNode(myProject, this, module))
            }
        }

        return nodes.toTypedArray()
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
        val nodes = mutableListOf<SimpleNode>()

        // 如果模块有依赖,添加依赖节点
        val dependencies = module.dependencies
        if (dependencies.isNotEmpty()) {
            nodes.add(DependenciesGroupNode(myProject, this, dependencies))
        }

        return nodes.toTypedArray()
    }

    override fun update(presentation: PresentationData) {
        presentation.presentableText = module.name
        presentation.setIcon(AllIcons.Nodes.Package)
        presentation.tooltip = "Module: ${module.rootDir.path}"
    }

    override fun getName(): String = module.name
}

/**
 * 依赖组节点
 */
private class DependenciesGroupNode(
    project: Project,
    parent: SimpleNode,
    private val dependencies: List<CjDependency>
) : CachingSimpleNode(project, parent) {

    override fun buildChildren(): Array<SimpleNode> {
        return dependencies.map { DependencyNode(myProject, this, it) }.toTypedArray()
    }

    override fun update(presentation: PresentationData) {
        presentation.presentableText = "Dependencies"
        presentation.setIcon(AllIcons.Nodes.PpLibFolder)
        presentation.tooltip = "${dependencies.size} dependencies"
    }

    override fun getName(): String = "Dependencies"
}

/**
 * 依赖节点
 */
private class DependencyNode(
    project: Project,
    parent: SimpleNode,
    private val dependency: CjDependency
) : CachingSimpleNode(project, parent) {

    override fun buildChildren(): Array<SimpleNode> {
        // 依赖是叶子节点
        return emptyArray()
    }

    override fun update(presentation: PresentationData) {
        presentation.presentableText = getDependencyText()
        presentation.setIcon(getDependencyIcon())
        presentation.tooltip = getDependencyTooltip()
    }

    private fun getDependencyText(): String {

        return when (dependency) {
            is CjDependency.Library -> {
                val group = dependency.group?.let { "$it:" } ?: ""
                "$group${dependency.name}:${dependency.versionReq}"
            }

            is CjDependency.Path -> {
                "${dependency.name} (${dependency.path})"
            }

            is CjDependency.Git -> {
                val ref = dependency.ref.let { "@$it" }
                "${dependency.name}$ref"
            }

            is CjDependency.Stdlib -> {
                "${dependency.name}:${dependency.versionReq}"
            }

            is CjDependency.Binary -> {
                "${dependency.name} (${dependency.cjoPath.fileName})"
            }
        }
    }

    private fun getDependencyIcon(): Icon {
        return when (dependency) {
            is CjDependency.Library -> AllIcons.Nodes.PpLib
            is CjDependency.Path -> AllIcons.Nodes.Module
            is CjDependency.Git -> AllIcons.Vcs.Vendors.Github
            is CjDependency.Stdlib -> AllIcons.Nodes.PpJdk
            is CjDependency.Binary -> AllIcons.FileTypes.Archive  // 二进制文件图标
        }
    }

    private fun getDependencyTooltip(): String {
        return when (dependency) {
            is CjDependency.Library -> {
                buildString {
                    append("Library: ${dependency.id}")
                    if (dependency.registry != null) {
                        append("\nRegistry: ${dependency.registry}")
                    }
                    append("\nScope: ${dependency.scope}")
                    if (dependency.optional) append("\nOptional: true")
                    if (!dependency.transitive) append("\nTransitive: false")
                }
            }

            is CjDependency.Path -> {
                "Path dependency: ${dependency.path}\nScope: ${dependency.scope}"
            }

            is CjDependency.Git -> {
                buildString {
                    append("Git: ${dependency.url}")
                    dependency.ref.let { append("\nRef: $it") }
                    append("\nScope: ${dependency.scope}")
                }
            }

            is CjDependency.Stdlib -> {
                "Stdlib dependency: ${dependency.name}:${dependency.versionReq}"
            }

            is CjDependency.Binary -> {
                buildString {
                    append("Binary dependency: ${dependency.name}")
                    append("\nCJO: ${dependency.cjoPath}")
                    dependency.libPath?.let { append("\nLib: $it") }
                    dependency.target?.let { append("\nTarget: $it") }
                    append("\nScope: ${dependency.scope}")
                }
            }
        }
    }

    override fun getName(): String = dependency.name
}