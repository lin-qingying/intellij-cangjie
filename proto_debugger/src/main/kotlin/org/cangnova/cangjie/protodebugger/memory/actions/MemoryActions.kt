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

import com.intellij.openapi.actionSystem.ActionPromoter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.fileEditor.impl.EditorWindowHolder
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ComponentUtil
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl
import kotlinx.coroutines.launch
import org.cangnova.cangjie.protodebugger.core.CangJieDebugProcess
import org.cangnova.cangjie.protodebugger.core.CangJieStackFrame
import org.cangnova.cangjie.protodebugger.memory.Address
import org.cangnova.cangjie.protodebugger.memory.AddressRange
import org.cangnova.cangjie.protodebugger.memory.toOpenFileDescriptor
import javax.swing.JSplitPane.HORIZONTAL_SPLIT

/**
 * 跳转到地址 Action
 */
class GoToAddressAction : AnAction("Go to Address..."), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        // TODO: 使用 e.getDebuggerDriverFacade() 获取 facade
        // 然后使用 facade.scope 来启动协程
//        val project = e.project ?: return
//        val facade = e.getDebuggerDriverFacade() ?: return
//
//        // 弹出输入对话框
//        val addressText = Messages.showInputDialog(
//            project,
//            "Enter memory address (hex):",
//            "Go to Address",
//            null,
//            "0x0000000000000000",
//            null
//        ) ?: return
//
//        try {
//            // 解析地址
//            val address = parseAddress(addressText)
//
//            // 使用 facade 的 scope 来加载并跳转
//            facade.executeCommandAsync {
//                val range = address.minus(256).rangeTo(address.plus(255))
//                facade.memoryViewFacade.loadRange(range)
//                facade.memoryViewFacade.scrollTo(address)
//            }
//        } catch (ex: Exception) {
//            Messages.showErrorDialog(
//                project,
//                "Invalid address format: ${ex.message}",
//                "Error"
//            )
//        }
    }

    private fun parseAddress(text: String): Address {
        val cleaned = text.trim().removePrefix("0x").removePrefix("0X")
        val value = cleaned.toLong(16)
        return Address.Companion.Factory.fromLong(value)
    }
}


/**
 * 复制地址 Action
 */
class CopyAddressAction : XDebuggerTreeActionBase(), ActionPromoter {
    override fun perform(
        node: XValueNodeImpl?,
        nodeName: String,
        e: AnActionEvent?
    ) {

    }

}


/**
 * 反汇编函数 Action
 */
class DisassembleFunctionAction : AnAction("Disassemble Function"), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val session: XDebugSession = DebuggerUIUtil.getSession(e) ?: return

        val project = session.project
        val fileEditorManager: FileEditorManager = FileEditorManager.getInstance(project)
        val frame = session.currentStackFrame ?: return
        if (frame !is CangJieStackFrame) return

        val sourcePosition = frame.sourcePosition
        val disassemblyPosition = frame.disassemblyPosition ?: return

        if (sourcePosition != null) {
            // 有源码位置：先打开源码文件，然后在分割窗口中打开反汇编视图
            // 1. 打开源码文件（不获取焦点）
            val sourceFileDescriptor = sourcePosition.toOpenFileDescriptor(project)
            val fileEditors = fileEditorManager.openEditor(sourceFileDescriptor, false)

            // 2. 查找源码编辑器所在的窗口
            val sourceEditorWindow = findEditorWindow(fileEditors)

            // 3. 在右侧分割窗口中打开反汇编视图
            sourceEditorWindow?.split(
                orientation = HORIZONTAL_SPLIT,        // 1 = 垂直分割（左右布局），0 = 水平分割（上下布局）
                forceSplit = false,     // 不强制分割
                virtualFile = disassemblyPosition.file,
                focusNew = true,        // 焦点移到新窗口
                fileIsSecondaryComponent = true
            )
            return
        }

        // 4. 打开反汇编视图并获取焦点
        val disassemblyFileDescriptor = disassemblyPosition.toOpenFileDescriptor(project)
        fileEditorManager.openEditor(disassemblyFileDescriptor, true)
    }

    /**
     * 查找编辑器所在的窗口
     */
    private fun findEditorWindow(fileEditors: List<FileEditor>): EditorWindow? {
        for (fileEditor in fileEditors) {
            val editorWindowHolder = ComponentUtil.getParentOfType(
                EditorWindowHolder::class.java, fileEditor.component
            )
            if (editorWindowHolder != null) {
                return editorWindowHolder.editorWindow
            }
        }


        return null
    }

    override fun update(e: AnActionEvent) {
        // 获取当前调试会话
        val session = DebuggerUIUtil.getSession(e)
        val frame = session?.currentStackFrame as? CangJieStackFrame

        // 只有在调试会话中且有栈帧且有反汇编位置时才启用此 Action
        val hasDisassembly = frame?.disassemblyPosition != null

        e.presentation.isEnabled = hasDisassembly

        // 可选：根据是否有源码位置动态更新 Action 的描述文本
        if (frame?.sourcePosition != null && hasDisassembly) {
            e.presentation.description = "Open source and disassembly in split view"
        } else if (hasDisassembly) {
            e.presentation.description = "Open disassembly view"
        } else {
            e.presentation.description = "No disassembly available"
        }
    }
}

/**
 * 查看内存 Action
 *
 * 从栈帧的程序计数器地址打开内存视图
 */
class ViewMemoryAction : AnAction("View Memory"), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val session: XDebugSession = DebuggerUIUtil.getSession(e) ?: return
        val debugProcess = session.debugProcess as? CangJieDebugProcess ?: return

        val project = session.project
        val fileEditorManager: FileEditorManager = FileEditorManager.getInstance(project)
        val frame = session.currentStackFrame ?: return
        if (frame !is CangJieStackFrame) return

        // 获取 MemoryViewFacade
        val memoryFacade = debugProcess.facade.memoryViewFacade

        // 获取栈帧的程序计数器地址
        val pcAddress = frame.frame.programCounter

        // 获取 HexStore 并加载数据
        val hexStore = memoryFacade.getHexStore()


        //            // 数据对齐，确保数据是以512字节边界对齐
//            // 示例: 0x0086 -> 0x0000, 0x0286 -> 0x0200
//            // 使用 alignDown 方法将地址向下对齐到512字节边界 (0x200 = 512)
        val alignedAddress = pcAddress.alignDown(0x200UL)

        val position = hexStore.createAddressPosition(alignedAddress)


        // 先打开内存视图文件（HEX视图）
        fileEditorManager.openEditor(position.toOpenFileDescriptor(project), true)


//        // 使用协程加载数据并滚动到目标地址
        memoryFacade.  debuggerFacade.executeCommand  {

            hexStore.loadRange(
                AddressRange.of(
                    alignedAddress, alignedAddress + 512
                )
            )
//            hexStore.scrollTo(alignedAddress)
        }
    }

    override fun update(e: AnActionEvent) {
        val session = DebuggerUIUtil.getSession(e)
        val debugProcess = session?.debugProcess as? org.cangnova.cangjie.protodebugger.core.CangJieDebugProcess
        val frame = session?.currentStackFrame as? CangJieStackFrame

        e.presentation.isEnabled = debugProcess != null && frame != null
    }
}




