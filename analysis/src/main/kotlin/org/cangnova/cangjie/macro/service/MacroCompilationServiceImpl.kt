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

package org.cangnova.cangjie.macro.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.macro.compiler.CompilerMacroCompilationProvider
import org.cangnova.cangjie.result.CjResult
import java.nio.file.Path

/**
 * 宏编译服务实现
 *
 * 通过编译器 `cjc-frontend --compile-macro` 实现宏声明的编译。
 * 宏展开前需要先编译宏声明，生成 `.cjo` 文件。
 */
@Service(Service.Level.PROJECT)
internal class MacroCompilationServiceImpl(private val project: Project) : MacroCompilationService {

    private val logger = Logger.getInstance(MacroCompilationServiceImpl::class.java)

    private val compilationProvider = CompilerMacroCompilationProvider(project)

    override suspend fun compileMacros(
        file: VirtualFile,
        options: MacroCompilationOptions
    ): CjResult<MacroCompilationResult, MacroCompilationError> {
        logger.info("开始编译宏声明: ${file.path}")
        return compilationProvider.compileMacros(file, options)
    }

    override suspend fun compileAllMacrosInProject(
        options: MacroCompilationOptions
    ): CjResult<MacroCompilationResult, MacroCompilationError> {
        logger.info("开始编译项目中的所有宏声明")
        return compilationProvider.compileAllMacrosInProject(options)
    }

    override fun needsRecompilation(file: VirtualFile): Boolean {
        return compilationProvider.needsRecompilation(file)
    }

    override fun isAvailable(): Boolean {
        return compilationProvider.isAvailable()
    }

    override fun getOutputDirectory(file: VirtualFile): Path? {
        return compilationProvider.getOutputDirectory(file)
    }
}
