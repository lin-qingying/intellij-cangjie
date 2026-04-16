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

import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.debugger.protobuf.memory.vfs.DisasmFileType
import org.cangnova.cangjie.debugger.protobuf.memory.vfs.HexdumpFileType
import org.cangnova.cangjie.debugger.protobuf.memory.vfs.MemoryViewFile


/**
 * 内存视图标签标题提供者
 *
 * 为内存视图文件提供友好的标签标题。
 */
class MemoryViewTabTitleProvider : EditorTabTitleProvider {

    /**
     * 获取编辑器标签标题
     *
     * @param project 项目
     * @param file 虚拟文件
     * @return 标签标题，如果不是内存视图文件返回 null
     */
    override fun getEditorTabTitle(project: Project, file: VirtualFile): String? {
        // 检查是否是内存视图文件
        if (file !is MemoryViewFile<*>) {
            return null
        }

        return when (file.fileType) {
            HexdumpFileType -> {

                "Memory [Hex] "

            }

            DisasmFileType -> {
                // 显示当前指令地址
                "Disassembly"
            }

            else -> ""
        }
    }

    /**
     * 格式化大小
     *
     * @param size 字节大小
     * @return 格式化的大小字符串
     */
    private fun formatSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }
}