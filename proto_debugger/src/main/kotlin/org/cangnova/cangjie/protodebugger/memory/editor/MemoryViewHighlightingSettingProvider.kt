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
 * which allows users to freely use, modified, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.protodebugger.memory.editor

import com.intellij.codeInsight.daemon.impl.analysis.DefaultHighlightingSettingProvider
import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.protodebugger.memory.vfs.MemoryViewFile

/**
 * 内存视图高亮设置提供者
 *
 * 控制内存视图文件的语法检查和高亮行为。
 */
class MemoryViewHighlightingSettingProvider : DefaultHighlightingSettingProvider() {

    /**
     * 获取默认设置
     *
     * @param project 项目
     * @param file 虚拟文件
     * @return 高亮设置，如果不是内存视图文件返回 null
     */
    override fun getDefaultSetting(project: Project, file: VirtualFile): FileHighlightingSetting? {
        // 检查是否是内存视图文件
        if (file !is MemoryViewFile<*>) {
            return null
        }

        // 内存视图文件跳过所有检查
        // 因为它们不是真正的代码文件，不需要语法检查
        return FileHighlightingSetting.SKIP_INSPECTION
    }
}