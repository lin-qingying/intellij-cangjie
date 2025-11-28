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

package org.cangnova.cangjie.debugger.protobuf.memory.editor

import com.intellij.icons.AllIcons
import com.intellij.ide.FileIconProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.debugger.protobuf.memory.vfs.MemoryViewFile
import javax.swing.Icon

/**
 * 内存视图文件图标提供者
 *
 * 为内存视图文件提供自定义图标。
 */
class MemoryViewFileIconProvider : FileIconProvider {

    /**
     * 获取文件图标
     *
     * @param file 虚拟文件
     * @param flags 图标标志
     * @param project 项目（可选）
     * @return 图标，如果不是内存视图文件返回 null
     */
    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? {
        // 检查是否是内存视图文件
        if (file !is MemoryViewFile<*>) {
            return null
        }

        // 基础图标
        val baseIcon = AllIcons.FileTypes.Text

        // 如果文件已修改，添加修改标记
//        if (file.isModified()) {
//            val layered = LayeredIcon(2)
//            layered.setIcon(baseIcon, 0)
//            layered.setIcon(AllIcons.General.Modified, 1, 8, 0)  // 右上角显示修改标记
//            return layered
//        }

        return baseIcon
    }
}