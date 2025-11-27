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

package org.cangnova.cangjie.protodebugger.memory.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorGutterComponentEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.xdebugger.XDebuggerManager
import org.cangnova.cangjie.protodebugger.core.CangJieDebugProcess
import org.cangnova.cangjie.protodebugger.core.DebuggerDriverFacade
import org.cangnova.cangjie.protodebugger.memory.Address
import org.cangnova.cangjie.protodebugger.memory.MemoryViewFacade
import org.cangnova.cangjie.protodebugger.memory.vfs.HexdumpFileType
import org.cangnova.cangjie.protodebugger.memory.vfs.MemoryViewFile
import java.awt.Point

/**
 * AnActionEvent 扩展方法
 *
 * 提供便捷的方式从 AnActionEvent 获取调试器和内存视图相关的组件。
 */

// ==================== 内存视图文件相关 ====================

/**
 * 从 AnActionEvent 获取当前的 MemoryViewFile
 *
 * 此方法尝试从以下来源获取文件（按优先级）：
 * 1. CommonDataKeys.VIRTUAL_FILE (最直接)
 * 2. Editor 的 Document (如果在编辑器中)
 * 3. PSI File (如果有)
 *
 * @return MemoryViewFile 如果当前上下文是内存视图文件，否则返回 null
 *
 * @sample
 * ```kotlin
 * class MyMemoryAction : AnAction() {
 *     override fun actionPerformed(e: AnActionEvent) {
 *         val memoryFile = e.getMemoryViewFile() ?: return
 *         println("Viewing address range: ${memoryFile.addressRange}")
 *     }
 * }
 * ```
 */
fun AnActionEvent.getMemoryViewFile(): MemoryViewFile<*>? {
    // 尝试从 Editor 获取 (优先使用此方法，因为它在 BGT 和 EDT 上都是安全的)
    getData(CommonDataKeys.EDITOR)?.let { editor ->
        val file = FileDocumentManager.getInstance().getFile(editor.document)
        if (file is MemoryViewFile<*>) return file
    }

    // 尝试从 VirtualFile 获取 (在 BGT 模式下可能受限)
    getData(CommonDataKeys.VIRTUAL_FILE)?.let { file ->
        if (file is MemoryViewFile<*>) return file
    }

    // 尝试从 PSI File 获取 (在 BGT 模式下可能受限)
    getData(CommonDataKeys.PSI_FILE)?.let { psiFile ->
        if (psiFile.virtualFile is MemoryViewFile<*>) {
            return psiFile.virtualFile as MemoryViewFile<*>
        }
    }

    return null
}

/**
 * 检查当前上下文是否是内存视图
 *
 * @return 如果当前是内存视图返回 true，否则返回 false
 *
 * @sample
 * ```kotlin
 * class MyMemoryAction : AnAction() {
 *     override fun update(e: AnActionEvent) {
 *         // 只在内存视图中启用此 Action
 *         e.presentation.isEnabled = e.isMemoryView()
 *     }
 * }
 * ```
 */
fun AnActionEvent.isMemoryView(): Boolean {
    return getMemoryViewFile() != null
}

/**
 * 从 AnActionEvent 获取当前编辑器中光标处的地址
 *
 * 使用 MemoryStore 的偏移映射功能将光标位置映射到内存地址。
 * 支持十六进制视图和反汇编视图。
 *
 * @return 光���处的地址，如果无法解析或不在内存视图中返回 null
 *
 * @sample
 * ```kotlin
 * class JumpToAddressAction : AnAction() {
 *     override fun actionPerformed(e: AnActionEvent) {
 *         val address = e.getAddressAtCaret() ?: return
 *         println("Current address: 0x${address.asLong.toString(16)}")
 *     }
 * }
 * ```
 */
fun AnActionEvent.getAddressAtCaret(): Address? {
    val editor = getData(CommonDataKeys.EDITOR) ?: return null
    // 确保是在内存视图中
    getMemoryViewFile() ?: return null

    return getAddressFromEditor(editor)
}

/**
 * 从 AnActionEvent 获取当前上下文中的地址
 *
 * 支持两种场景：
 * 1. 在编辑器内容区域右键：使用光标位置
 * 2. 在行号区域右键：使用鼠标点击的行号
 *
 * @return 当前上下文的地址，如果无法解析或不在内存视图中返回 null
 *
 * @sample
 * ```kotlin
 * class CopyAddressAction : AnAction() {
 *     override fun actionPerformed(e: AnActionEvent) {
 *         val address = e.getAddressFromContext() ?: return
 *         println("Address: 0x${address.asLong.toString(16)}")
 *     }
 * }
 * ```
 */
fun AnActionEvent.getAddressFromContext(): Address? {
    val editor = getData(CommonDataKeys.EDITOR) ?: return null
    val file = getMemoryViewFile() ?: return null


        EditorGutterComponentEx.LOGICAL_LINE_AT_CURSOR.getData(dataContext)?.let {
            return file.store.getAddressForLine(it)
        }



    // 如果不是在行号区域右键，则使用光标位置
    return getAddressFromEditor(editor)
}

/**
 * 从编辑器中提取光标处的地址
 *
 * @param editor 编辑器实例
 * @return 解析出的地址，如果无法解析返回 null
 */
private fun getAddressFromEditor(editor: Editor): Address? {
    val offset = editor.caretModel.offset
    val file = FileDocumentManager.getInstance().getFile(editor.document) as? MemoryViewFile<*> ?: return null

    // 使用 MemoryStore 的 mapOffsetToAddress 方法来获取地址
    return file.store.mapOffsetToAddress(offset)
}

// ==================== 调试器相关 ====================

/**
 * 获取当前调试会话的 MemoryViewFacade
 *
 * 通过以下路径获取：
 * AnActionEvent → Project → XDebugSession → CangJieDebugProcess → DebuggerDriverFacade → MemoryViewFacade
 *
 * @return MemoryViewFacade 实例，如果当前没有活动的调试会话则返回 null
 *
 * @sample
 * ```kotlin
 * class LoadMemoryAction : AnAction() {
 *     override fun actionPerformed(e: AnActionEvent) {
 *         val facade = e.getMemoryViewFacade() ?: return
 *         facade.loadAddress(Address(0x401000UL))
 *     }
 * }
 * ```
 */
fun AnActionEvent.getMemoryViewFacade(): MemoryViewFacade? {
    val project = this.project ?: return null
    val debuggerManager = XDebuggerManager.getInstance(project)
    val currentSession = debuggerManager.currentSession ?: return null
    val debugProcess = currentSession.debugProcess as? CangJieDebugProcess ?: return null
    return debugProcess.facade.memoryViewFacade
}

/**
 * 获取当前调试会话的 DebuggerDriverFacade
 *
 * @return DebuggerDriverFacade 实例，如果当前没有活动的调试会话则返回 null
 *
 * @sample
 * ```kotlin
 * class MyDebugAction : AnAction() {
 *     override fun actionPerformed(e: AnActionEvent) {
 *         val facade = e.getDebuggerDriverFacade() ?: return
 *         // 访问各种调试服务
 *         facade.breakpointService.addLineBreakpoint(...)
 *         facade.steppingService.stepOver(...)
 *     }
 * }
 * ```
 */
fun AnActionEvent.getDebuggerDriverFacade(): DebuggerDriverFacade? {
    val project = this.project ?: return null
    val debuggerManager = XDebuggerManager.getInstance(project)
    val currentSession = debuggerManager.currentSession ?: return null
    val debugProcess = currentSession.debugProcess as? CangJieDebugProcess ?: return null
    return debugProcess.facade
}

/**
 * 获取当前调试会话的 CangJieDebugProcess
 *
 * @return CangJieDebugProcess 实例，如果当前没有活动的调试会话则返回 null
 *
 * @sample
 * ```kotlin
 * class MyDebugAction : AnAction() {
 *     override fun actionPerformed(e: AnActionEvent) {
 *         val process = e.getCangJieDebugProcess() ?: return
 *         // 访问调试进程的功能
 *         process.consoleManager.print("Hello, Debugger!")
 *     }
 * }
 * ```
 */
fun AnActionEvent.getCangJieDebugProcess(): CangJieDebugProcess? {
    val project = this.project ?: return null
    val debuggerManager = XDebuggerManager.getInstance(project)
    val currentSession = debuggerManager.currentSession ?: return null
    return currentSession.debugProcess as? CangJieDebugProcess
}

/**
 * 检查当前是否有活动的仓颉调试会话
 *
 * @return 如果有活动的仓颉调试会话返回 true，否则返回 false
 *
 * @sample
 * ```kotlin
 * class MyDebugAction : AnAction() {
 *     override fun update(e: AnActionEvent) {
 *         // 只在调试会话活动时启用 Action
 *         e.presentation.isEnabled = e.hasActiveDebugSession()
 *     }
 * }
 * ```
 */
fun AnActionEvent.hasActiveDebugSession(): Boolean {
    val project = this.project ?: return false
    val debuggerManager = XDebuggerManager.getInstance(project)
    val currentSession = debuggerManager.currentSession ?: return false
    return currentSession.debugProcess is CangJieDebugProcess
}