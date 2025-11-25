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
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.xdebugger.XDebuggerManager
import org.cangnova.cangjie.protodebugger.core.CangJieDebugProcess
import org.cangnova.cangjie.protodebugger.core.DebuggerDriverFacade
import org.cangnova.cangjie.protodebugger.memory.Address
import org.cangnova.cangjie.protodebugger.memory.MemoryViewFacade
import org.cangnova.cangjie.protodebugger.memory.vfs.MemoryViewFile

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
    // 尝试从 VirtualFile 获取
    getData(CommonDataKeys.VIRTUAL_FILE)?.let { file ->
        if (file is MemoryViewFile<*>) return file
    }

    // 尝试从 Editor 获取
    getData(CommonDataKeys.EDITOR)?.let { editor ->
        val file = FileDocumentManager.getInstance().getFile(editor.document)
        if (file is MemoryViewFile<*>) return file
    }

    // 尝试从 PSI File 获取
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
 * 需要配合 Editor 使用，从当前行文本中解析地址。
 * 支持的地址格式：
 * - `0000000000401000:` (16位十六进制)
 * - `401000:` (8位十六进制)
 * - `DEADBEEF:` (大小写不敏感)
 *
 * @return 光标处的地址，如果无法解析或不在内存视图中返回 null
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
 * 从编辑器中提取光标处的地址
 *
 * @param editor 编辑器实例
 * @return 解析出的地址，如果无法解析返回 null
 */
private fun getAddressFromEditor(editor: Editor): Address? {
    val offset = editor.caretModel.offset
    val lineNumber = editor.document.getLineNumber(offset)
    val lineStart = editor.document.getLineStartOffset(lineNumber)
    val lineEnd = editor.document.getLineEndOffset(lineNumber)
    val lineText = editor.document.getText(TextRange(lineStart, lineEnd))

    // 匹配地址格式：8-16位十六进制数字 + 冒号
    // 例如：0000000000401000:  48 8B 45 F8    mov rax, [rbp-8]
    val match = Regex("""([0-9A-Fa-f]{8,16}):""").find(lineText)
    return match?.let {
        try {
            Address.Companion.Factory.fromLong(it.groupValues[1].toLong(16))
        } catch (e: NumberFormatException) {
            null
        }
    }
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