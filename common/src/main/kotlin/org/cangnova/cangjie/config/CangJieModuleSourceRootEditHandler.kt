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

package org.cangnova.cangjie.config

import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.roots.ui.configuration.ContentRootPanel
import com.intellij.openapi.roots.ui.configuration.ModuleSourceRootEditHandler
import com.intellij.ui.JBColor
import org.jetbrains.annotations.Nls
import org.jetbrains.jps.model.JpsElement
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import java.awt.Color
import javax.swing.Icon
import javax.swing.JComponent

/**
 * 仓颉源代码根编辑处理器基类
 *
 * 为 IntelliJ 的项目结构对话框提供源码根的 UI 展示和编辑功能。
 * 负责定义源码根的图标、颜色、分组名称等视觉元素。
 */
abstract class CangJieModuleSourceRootEditHandlerBase(
    rootType: JpsModuleSourceRootType<JpsElement>,
    private val typeName: String,
    private val groupTitle: String,
    private val rootIcon: Icon,
    private val groupColor: Color,
    private val folderIcon: Icon? = null,
    private val unmarkButtonText: String = "Unmark as $typeName"
) : ModuleSourceRootEditHandler<JpsElement>(rootType) {

    override fun getRootTypeName(): String = typeName

    override fun getRootIcon(): Icon = rootIcon

    override fun getRootsGroupTitle(): String = groupTitle

    override fun getRootsGroupColor(): Color = groupColor

    override fun getPropertiesString(properties: JpsElement): String? = null

    override fun createPropertiesEditor(
        folder: SourceFolder,
        parentComponent: JComponent,
        callback: ContentRootPanel.ActionCallback
    ): JComponent? = null

    /**
     * 返回源码根下文件夹的图标
     * 如果返回 null，将使用默认的文件夹图标
     */
    override fun getFolderUnderRootIcon(): Icon? = folderIcon

    /**
     * 返回"标记为源码根"操作的快捷键
     * 如果返回 null，将不设置快捷键
     */
    override fun getMarkRootShortcutSet(): CustomShortcutSet? = null

    /**
     * 返回"取消标记源码根"按钮的文本
     */
    override fun getUnmarkRootButtonText(): String = unmarkButtonText
}

/**
 * 仓颉生产源代码根编辑处理器
 */
internal class CangJieSourceRootEditHandler : CangJieModuleSourceRootEditHandlerBase(
    CangJieSourceRootType,
    "CangJie Source",
    "CangJie Sources",
    com.intellij.icons.AllIcons.Modules.SourceRoot,
    JBColor(Color(0x0A7700), Color(0x0E9A00)),  // 绿色
    folderIcon = com.intellij.icons.AllIcons.Nodes.Package,  // 源码根下的包图标
    unmarkButtonText = "Unmark as CangJie Source"
)

/**
 * 仓颉测试源代码根编辑处理器
 */
internal class CangJieTestSourceRootEditHandler : CangJieModuleSourceRootEditHandlerBase(
    CangJieTestSourceRootType,
    "CangJie Test Source",
    "CangJie Test Sources",
    com.intellij.icons.AllIcons.Modules.TestRoot,
    JBColor(Color(0x59A869), Color(0x5FAD65)),  // 浅绿色（测试用）
    folderIcon = com.intellij.icons.AllIcons.Nodes.Package,  // 使用普通包图标
    unmarkButtonText = "Unmark as CangJie Test Source"
)

/**
 * 仓颉资源根编辑处理器
 */
internal class CangJieResourceRootEditHandler : CangJieModuleSourceRootEditHandlerBase(
    CangJieResourceRootType,
    "CangJie Resource",
    "CangJie Resources",
    com.intellij.icons.AllIcons.Modules.ResourcesRoot,
    JBColor(Color(0x9AA7B0), Color(0x9AA7B0)),  // 灰蓝色
    folderIcon = com.intellij.icons.AllIcons.Nodes.ResourceBundle,
    unmarkButtonText = "Unmark as CangJie Resource"
)

/**
 * 仓颉测试资源根编辑处理器
 */
internal class CangJieTestResourceRootEditHandler : CangJieModuleSourceRootEditHandlerBase(
    CangJieTestResourceRootType,
    "CangJie Test Resource",
    "CangJie Test Resources",
    com.intellij.icons.AllIcons.Modules.TestResourcesRoot,
    JBColor(Color(0x9AA7B0), Color(0x9AA7B0)),  // 灰蓝色
    folderIcon = com.intellij.icons.AllIcons.Nodes.ResourceBundle,
    unmarkButtonText = "Unmark as CangJie Test Resource"
)
