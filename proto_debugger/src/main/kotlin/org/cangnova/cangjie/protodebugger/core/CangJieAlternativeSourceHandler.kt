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

package org.cangnova.cangjie.protodebugger.core

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.XAlternativeSourceHandler
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XSuspendContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.protodebugger.memory.toOpenFileDescriptor

/**
 * 仓颉替代源处理器
 *
 * 用于处理调试时的源代码映射，提供反汇编视图与源代码视图的切换。
 *
 * 核心功能：
 * - 当源代码不可用时自动显示反汇编视图
 * - 支持源代码与反汇编视图之间的切换
 * - 从 CangJieStackFrame 获取预先计算的源代码位置和反汇编位置
 *
 * @param facade 调试器驱动门面，用于访问调试器会话信息
 */
class CangJieAlternativeSourceHandler(
    private val facade: DebuggerDriverFacade
) : XAlternativeSourceHandler {

    companion object {
        private val LOG = Logger.getInstance(CangJieAlternativeSourceHandler::class.java)
    }

    private val _alternativeSourceKindState = MutableStateFlow(false)

    /**
     * 获取替代源类型的状态流
     *
     * @return StateFlow<Boolean> 表示是否启用替代源视图
     */
    override fun getAlternativeSourceKindState(): StateFlow<Boolean> {
        return _alternativeSourceKindState
    }

    /**
     * 判断是否首选替代源类型
     *
     * @param suspendContext 当前暂停上下文
     * @return true 如果首选替代源（如反汇编视图），false 否则
     */
    override fun isAlternativeSourceKindPreferred(suspendContext: XSuspendContext): Boolean {
        val activeThread = suspendContext.activeExecutionStack?.topFrame
        val sourcePosition = activeThread?.sourcePosition

        // 在以下情况下首选替代源（反汇编）视图：
        // 1. 没有源代码位置信息
        // 2. 源代码文件不是仓颉文件
        // 3. 当前已经在替代源模式下
        if (sourcePosition == null) {
            return true // 首选反汇编视图
        }

        val sourceFile = sourcePosition.file
        if (sourceFile.fileType != CangJieFileType.INSTANCE) {
            return true // 非仓颉文件，首选反汇编视图
        }

        // 检查是否为系统库或编译器生成的代码
        if (isSystemOrGeneratedCode(sourcePosition)) {
            return true // 系统代码，首选反汇编视图
        }

        return false // 默认首选源代码视图
    }

    /**
     * 获取替代源位置
     *
     * 从栈帧获取替代视图位置：
     * - 如果当前显示源代码，返回反汇编位置（从 stackFrame 获取）
     * - 如果当前显示反汇编，返回源代码位置（从 stackFrame 获取）
     *
     * @param frame 当前栈帧
     * @return 替代源位置，如果无可用的替代位置则返回 null
     */
    override fun getAlternativePosition(frame: XStackFrame): XSourcePosition? {
        // 检查是否为 CangJieStackFrame
        if (frame !is CangJieStackFrame) {
            LOG.debug("Frame is not CangJieStackFrame, cannot provide alternative position")
            return null
        }

        val disassemblyPosition = frame.disassemblyPosition



        return disassemblyPosition

    }

    /**
     * 设置替代源类型状态
     *
     * @param enabled 是否启用替代源模式
     */
    fun setAlternativeSourceKindEnabled(enabled: Boolean) {
        _alternativeSourceKindState.value = enabled
    }

    /**
     * 切换替代源类型状态
     */
    fun toggleAlternativeSourceKind() {
        _alternativeSourceKindState.value = !_alternativeSourceKindState.value
    }

    /**
     * 判断是否为系统或编译器生成的代码
     *
     * @param sourcePosition 源代码位置
     * @return true 如果是系统或生成代码
     */
    private fun isSystemOrGeneratedCode(sourcePosition: XSourcePosition): Boolean {
        val filePath = sourcePosition.file.path

        // 检查是否为系统库路径
        val systemPaths = listOf(
            "/usr/", "/lib/", "/System/", "/Windows/",
            "/Program Files/", "/Program Files (x86)/"
        )

        return systemPaths.any { systemPath ->
            filePath.contains(systemPath)
        }
    }
}