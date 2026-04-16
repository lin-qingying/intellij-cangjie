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
 */

package org.cangnova.cangjie.macro.compiler

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * 宏包声明定位器扩展点
 *
 * 用于查找项目中包含 `macro package` 声明的源码目录。
 * 实现者可选择不同策略（文本扫描、PSI 语义分析等）定位宏包目录。
 *
 * ## 调用约定
 *
 * [CompilerMacroCompilationProvider] 按 [priority] 降序依次调用所有注册的实现，
 * 采用第一个返回非 null 结果的实现。若所有实现均返回 null，则回退到
 * 内置的文本正则扫描逻辑。
 *
 * ## 返回值语义
 *
 * - **非空列表**：找到了宏包目录，使用这些目录进行编译
 * - **空列表**：分析成功但项目中没有宏包声明，不触发编译
 * - **null**：此实现无法处理（如 PSI 未就绪），框架将尝试下一个实现
 *
 * ## 注册方式
 *
 * ```xml
 * <extensions defaultExtensionNs="org.cangnova.cangjie">
 *     <macroDeclarationLocator
 *         implementation="com.example.MyMacroDeclarationLocator"/>
 * </extensions>
 * ```
 */
interface MacroDeclarationLocator {

    companion object {
        val EP_NAME: ExtensionPointName<MacroDeclarationLocator> =
            ExtensionPointName.create("org.cangnova.cangjie.macroDeclarationLocator")
    }

    /**
     * 优先级（值越大越先被调用）
     *
     * 较高优先级的实现通常提供更精确的结果（如语义分析），
     * 较低优先级作为备用（如文本扫描）。
     */
    val priority: Int get() = 0

    /**
     * 查找项目中所有包含 `macro package` 声明的源码目录路径
     *
     * @param project 当前项目
     * @param contextFile 触发宏展开的源文件（用于语义定位），null 表示全项目范围
     * @return 宏包目录路径列表；返回 null 表示此实现无法处理，框架将尝试下一个实现
     */
    fun findMacroPackageDirs(project: Project, contextFile: VirtualFile? = null): List<String>?
}
