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

package org.cangnova.cangjie.debugger.protobuf.memory.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.fileEditor.impl.EditorWindowHolder
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ComponentUtil
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil
import org.cangnova.cangjie.debugger.configurable.isProtoDebuggerEngine
import org.cangnova.cangjie.debugger.protobuf.core.CangJieStackFrame
import org.cangnova.cangjie.debugger.protobuf.core.ProtoDebugProcess
import org.cangnova.cangjie.debugger.protobuf.memory.Address
import org.cangnova.cangjie.debugger.protobuf.memory.AddressRange
import org.cangnova.cangjie.debugger.protobuf.memory.toOpenFileDescriptor
import org.cangnova.cangjie.debugger.protobuf.memory.vfs.DisasmFileType
import org.cangnova.cangjie.debugger.protobuf.messages.ProtoDebuggerBundle
import java.awt.datatransfer.StringSelection
import javax.swing.JSplitPane.HORIZONTAL_SPLIT

/**
 * 跳转到地址 Action
 *
 * 允许用户输入一个十六进制地址并在当前内存视图中跳转到该地址。
 * - 如果在十六进制视图中，跳转到该地址的十六进制表示
 * - 如果在反汇编视图中，跳转到该地址的反汇编代码
 * - 只在 MemoryViewFile 中显示和启用
 */
class GoToAddressAction : AnAction(ProtoDebuggerBundle.message("proto.action.goToAddress")), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // 获取当前的 MemoryViewFile
        val memoryFile = e.getMemoryViewFile() ?: return
        val store = memoryFile.store

        // 获取调试进程和 facade
        val debugProcess = e.getCangJieDebugProcess() ?: return
        val memoryFacade = debugProcess.facade.memoryViewFacade

        // 弹出输入对话框
        val addressText = Messages.showInputDialog(
            project,
            ProtoDebuggerBundle.message("proto.action.goToAddress.prompt"),
            ProtoDebuggerBundle.message("proto.action.goToAddress"),
            null,
            "0x0000000000000000",
            null
        ) ?: return

        // 解析地址
        val address = try {
            parseAddress(addressText)
        } catch (e: NumberFormatException) {
            Messages.showErrorDialog(
                project,
                ProtoDebuggerBundle.message("proto.action.goToAddress.invalidFormat", addressText),
                ProtoDebuggerBundle.message("proto.action.goToAddress")
            )
            return
        }

        // 判断是否为十六进制视图（需要对齐）还是反汇编视图（不需要对齐）
        val isHexView = store.fileType !is DisasmFileType

        // 十六进制视图需要对齐到512字节边界，反汇编视图直接使用原始地址
        val targetAddress = if (isHexView) {
            address.alignDown(0x200UL)
        } else {
            address
        }

        val position = store.createAddressPosition(targetAddress)

        // 打开/跳转到该地址（在当前类型的视图中）
        val fileEditorManager = FileEditorManager.getInstance(project)
        fileEditorManager.openEditor(position.toOpenFileDescriptor(project), true)

        // 只有十六进制内存视图需要加载数据范围
        // 反汇编视图会自动按需加载指令
        if (isHexView) {
            memoryFacade.debuggerFacade.executeCommand {
                store.loadRange(
                    AddressRange.of(
                        targetAddress, targetAddress + 512
                    )

                )
                store.scrollTo(address)

            }
        }
    }

    override fun update(e: AnActionEvent) {
        // 只在内存视图文件中显示和启用
        val isMemoryView = e.isMemoryView()
        if (e.project?.isProtoDebuggerEngine == false) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        e.presentation.isEnabled = isMemoryView
        e.presentation.isVisible = isMemoryView
    }

    private fun parseAddress(text: String): Address {
        val cleaned = text.trim().removePrefix("0x").removePrefix("0X")
        val value = cleaned.toULong(16)
        return Address.Companion.Factory.fromULong(value)
    }
}


/**
 * 复制地址 Action
 *
 * 从内存视图编辑器中复制当前上下文的地址到剪贴板。
 * 支持两种场景：
 * 1. 在编辑器内容区域右键：复制光标处的地址
 * 2. 在行号区域右键：复制点击行号对应的地址
 */
class CopyAddressAction : AnAction(ProtoDebuggerBundle.message("proto.action.memory.copyAddress")), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        // 获取当前上下文的地址（支持编辑器内容区域和行号区域）
        val address = e.getAddressFromContext() ?: return


        // 格式化地址为十六进制字符串（带 0x 前缀）
        val addressText = "0x%016X".format(address.asLong)

        // 复制到剪贴板
        val stringSelection = StringSelection(addressText)
        CopyPasteManager.getInstance().setContents(stringSelection)
    }

    override fun update(e: AnActionEvent) {
        // 只检查是否在内存视图中
        val isMemoryView = e.isMemoryView()
        if (e.project?.isProtoDebuggerEngine == false) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        e.presentation.isEnabled = isMemoryView
        e.presentation.isVisible = isMemoryView
    }
}


/**
 * 反汇编函数 Action
 */
class DisassembleFunctionAction : AnAction(ProtoDebuggerBundle.message("proto.action.disassembleFunction")), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

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
        // 如果当前是在内存视图文件中，隐藏此 Action
        if (e.isMemoryView()) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        if (e.project?.isProtoDebuggerEngine == false) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        // 获取当前调试会话
        val session = DebuggerUIUtil.getSession(e)
        if (session == null) {
            e.presentation.isEnabledAndVisible = false
        }
        val frame = session?.currentStackFrame as? CangJieStackFrame

        // 只有在调试会话中且有栈帧且有反汇编位置时才启用此 Action
        val hasDisassembly = frame?.disassemblyPosition != null

        e.presentation.isEnabled = hasDisassembly

        // 可选：根据是否有源码位置动态更新 Action 的描述文本
        if (frame?.sourcePosition != null && hasDisassembly) {
            e.presentation.description =
                ProtoDebuggerBundle.message("proto.action.disassembleFunction.description.splitView")
        } else if (hasDisassembly) {
            e.presentation.description =
                ProtoDebuggerBundle.message("proto.action.disassembleFunction.description.disasmOnly")
        } else {
            e.presentation.description =
                ProtoDebuggerBundle.message("proto.action.disassembleFunction.description.noDisasm")
        }
    }
}

/**
 * 查看内存 Action
 *
 * 从栈帧的程序计数器地址打开内存视图
 */
class ViewMemoryAction : AnAction(ProtoDebuggerBundle.message("proto.action.viewMemory")), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val session: XDebugSession = DebuggerUIUtil.getSession(e) ?: return
        val debugProcess = session.debugProcess as? ProtoDebugProcess ?: return

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
        memoryFacade.debuggerFacade.executeCommand {

            hexStore.loadRange(
                AddressRange.of(
                    alignedAddress, alignedAddress + 512
                )
            )
            hexStore.scrollTo(alignedAddress)
        }
    }

    override fun update(e: AnActionEvent) {
        // 如果当前是在内存视图文件中，隐藏此 Action
        if (e.isMemoryView()) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        if (e.project?.isProtoDebuggerEngine == false) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val session = DebuggerUIUtil.getSession(e)
        if (session == null) {
            e.presentation.isEnabledAndVisible = false
        }
        val debugProcess = session?.debugProcess as? org.cangnova.cangjie.debugger.protobuf.core.ProtoDebugProcess
        val frame = session?.currentStackFrame as? CangJieStackFrame

        e.presentation.isEnabled = debugProcess != null && frame != null
    }
}




