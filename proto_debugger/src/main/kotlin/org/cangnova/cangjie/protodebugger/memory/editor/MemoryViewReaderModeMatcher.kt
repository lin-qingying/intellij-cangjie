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

package org.cangnova.cangjie.protodebugger.memory.editor

import com.intellij.codeInsight.actions.ReaderModeMatcher
import com.intellij.codeInsight.actions.ReaderModeProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.protodebugger.memory.vfs.MemoryViewFile

/**
 * 内存视图阅读模式匹配器
 *
 * 控制内存视图文件是否进入阅读模式。
 */
class MemoryViewReaderModeMatcher : ReaderModeMatcher {

    /**
     * 检查文件是否应该在阅读模式下打开
     *
     * @param project 项目
     * @param file 虚拟文件
     * @param editor 编辑器（可选）
     * @param mode 阅读模式
     * @return 是否匹配阅读模式：true=匹配，false=不匹配，null=不关心
     */
    override fun matches(
        project: Project,
        file: VirtualFile,
        editor: Editor?,
        mode: ReaderModeProvider.ReaderMode
    ): Boolean  {
        // 检查是否是内存视图文件
        if (file !is MemoryViewFile<*>) {
            return false
        }

        // 内存视图文件永远不使用阅读模式
        // 因为用户需要编辑和交互
        return true
    }
}