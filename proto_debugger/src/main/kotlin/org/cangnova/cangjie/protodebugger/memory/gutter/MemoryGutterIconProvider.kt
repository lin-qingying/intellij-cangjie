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
 */

package org.cangnova.cangjie.protodebugger.memory.gutter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.breakpoints.XBreakpointProperties

import org.cangnova.cangjie.protodebugger.memory.Address
import org.cangnova.cangjie.protodebugger.memory.MemoryViewFacade
import org.cangnova.cangjie.protodebugger.memory.vfs.MemoryViewFile
import java.awt.datatransfer.StringSelection
import javax.swing.Icon

/**
 * 内存视图Gutter图标提供者
 *
 * 在内存视图的Gutter（行号旁边的区域）显示图标：
 * 1. 断点图标（可点击切换）
 * 2. PC（程序计数器）指示器
 * 3. 书签标记
 * 4. 修改标记
 * 5. 监视点（Watchpoint）
 * 6. 栈帧位置
 */
class MemoryGutterIconProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
//        // 只处理内存视图文件
//        val virtualFile = element.containingFile?.virtualFile
//        if (virtualFile !is MemoryViewFile) {
//            return null
//        }
//
//        // 解析地址
//        val address = extractAddress(element.text) ?: return null
//        val project = element.project
//
//        // 获取内存视图服务和调试器状态
//        val memoryService = project.getService(MemoryViewService::class.java)
//        val facade = memoryService.getFacade()
//        val debuggerManager = XDebuggerManager.getInstance(project)
//
//        // 检查各种标记状态
//        val debugState = DebuggerStateManager.getInstance(project)
//
//        // 优先级: PC > 断点 > 监视点 > 书签 > 修改标记
//
//        // 1. 检查是否是当前执行点（PC）
//        if (debugState.isProgramCounter(address)) {
//            return createLineMarkerInfo(
//                element,
//                ProgramCounterGutterIconRenderer(address, 0)
//            )
//        }
//
//        // 2. 检查栈帧位置
//        val frameIndex = debugState.getFrameIndex(address)
//        if (frameIndex > 0) {
//            return createLineMarkerInfo(
//                element,
//                ProgramCounterGutterIconRenderer(address, frameIndex)
//            )
//        }
//
//        // 3. 检查断点
//        if (debugState.hasBreakpoint(address)) {
//            val editor = FileEditorManager.getInstance(project).selectedTextEditor
//            return createLineMarkerInfo(
//                element,
//                BreakpointGutterIconRenderer(address, editor, element)
//            )
//        }
//
//        // 4. 检查监视点
//        if (debugState.hasWatchpoint(address)) {
//            return createLineMarkerInfo(
//                element,
//                WatchpointGutterIconRenderer(address, element)
//            )
//        }
//
//        // 5. 检查书签
//        val bookmark = debugState.getBookmark(address)
//        if (bookmark != null) {
//            return createLineMarkerInfo(
//                element,
//                BookmarkGutterIconRenderer(address, bookmark.description)
//            )
//        }
//
//        // 6. 检查修改标记
//        if (facade.isAddressModified(address)) {
//            return createLineMarkerInfo(
//                element,
//                ModifiedGutterIconRenderer(address)
//            )
//        }

        return null
    }

    private fun extractAddress(text: String): Address? {
        val match = Regex("""([0-9A-Fa-f]{8,16}):""").find(text) ?: return null
        val addressStr = match.groupValues[1]
        return try {
            Address.Companion.Factory.fromLong(addressStr.toLong(16))
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun createLineMarkerInfo(
        element: PsiElement,
        renderer: MemoryGutterIconRenderer
    ): LineMarkerInfo<PsiElement> {
        return LineMarkerInfo(
            element,
            element.textRange,
            renderer.icon,
            { renderer.tooltipText },
            { mouseEvent, _ ->
                renderer.clickAction?.let { action ->
                    val dataContext = com.intellij.ide.DataManager.getInstance().getDataContext(mouseEvent.component)
                    val event = AnActionEvent.createFromDataContext(
                        ActionPlaces.EDITOR_GUTTER,
                        null,
                        dataContext
                    )
                    action.actionPerformed(event)
                }
            },
            renderer.alignment,
            { renderer.tooltipText ?: "" }
        )
    }
}

/**
 * 内存Gutter图标渲染器基类
 */
abstract class MemoryGutterIconRenderer : GutterIconRenderer() {
    abstract val address: Address
    abstract override fun getIcon(): Icon
    abstract override fun getTooltipText(): String?
    abstract override fun getClickAction(): AnAction?

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MemoryGutterIconRenderer) return false
        return address == other.address && javaClass == other.javaClass
    }

    override fun hashCode(): Int {
        return 31 * address.hashCode() + javaClass.hashCode()
    }
}

/**
 * 断点Gutter渲染器
 */
class BreakpointGutterIconRenderer(
    override val address: Address,
    private val editor: Editor?,
    private val element: PsiElement
) : MemoryGutterIconRenderer() {

    override fun getIcon(): Icon = AllIcons.Debugger.Db_set_breakpoint

    override fun getTooltipText(): String {
        return "Memory breakpoint at 0x${"%016X".format(address.asLong)}\nClick to remove"
    }

    override fun getClickAction(): AnAction {
        return object : AnAction("Toggle Memory Breakpoint") {
            override fun actionPerformed(e: AnActionEvent) {
                val project = element.project
                val debugState = DebuggerStateManager.getInstance(project)

                if (debugState.hasBreakpoint(address)) {
                    // 移除断点
                    debugState.removeBreakpoint(address)
                    showNotification(project, "Breakpoint removed at 0x${"%016X".format(address.asLong)}")
                } else {
                    // 添加断点
                    debugState.addBreakpoint(address, MemoryBreakpointProperties())
                    showNotification(project, "Breakpoint added at 0x${"%016X".format(address.asLong)}")
                }

                // 刷新编辑器
                editor?.component?.repaint()
            }
        }
    }

    override fun getAlignment(): Alignment = Alignment.RIGHT
}

/**
 * PC（程序计数器）指示器
 */
class ProgramCounterGutterIconRenderer(
    override val address: Address,
    private val frameIndex: Int = 0
) : MemoryGutterIconRenderer() {

    override fun getIcon(): Icon {
        return if (frameIndex == 0) {
            AllIcons.Debugger.ThreadCurrent
        } else {
            AllIcons.Debugger.ThreadRunning
        }
    }

    override fun getTooltipText(): String {
        return if (frameIndex == 0) {
            "Current execution point: 0x${"%016X".format(address.asLong)}"
        } else {
            "Stack frame $frameIndex: 0x${"%016X".format(address.asLong)}"
        }
    }

    override fun getClickAction(): AnAction? {
        return if (frameIndex > 0) {
            object : AnAction("Switch to Frame $frameIndex") {
                override fun actionPerformed(e: AnActionEvent) {
                    val project = e.project ?: return
                    val debugState = DebuggerStateManager.getInstance(project)
                    debugState.switchToFrame(frameIndex)
                }
            }
        } else null
    }

    override fun getAlignment(): Alignment = Alignment.LEFT
}

/**
 * 监视点Gutter渲染器
 */
class WatchpointGutterIconRenderer(
    override val address: Address,
    private val element: PsiElement
) : MemoryGutterIconRenderer() {

    override fun getIcon(): Icon = AllIcons.Debugger.Db_watch

    override fun getTooltipText(): String {
        val project = element.project
        val debugState = DebuggerStateManager.getInstance(project)
        val watchpoint = debugState.getWatchpoint(address)

        return buildString {
            append("Memory watchpoint at 0x${"%016X".format(address.asLong)}")
            watchpoint?.let {
                append("\n")
                if (it.watchRead) append("Read ")
                if (it.watchWrite) append("Write ")
                if (it.watchExecute) append("Execute")
                append("\nSize: ${it.size} bytes")
            }
            append("\nClick to configure")
        }
    }

    override fun getClickAction(): AnAction {
        return object : AnAction("Configure Watchpoint") {
            override fun actionPerformed(e: AnActionEvent) {
                val project = element.project
                val debugState = DebuggerStateManager.getInstance(project)

                // 显示监视点配置对话框
                val dialog = WatchpointConfigDialog(project, address)
                if (dialog.showAndGet()) {
                    debugState.updateWatchpoint(address, dialog.getWatchpointProperties())
                }
            }
        }
    }

    override fun getAlignment(): Alignment = Alignment.RIGHT
}

/**
 * 书签Gutter渲染器
 */
class BookmarkGutterIconRenderer(
    override val address: Address,
    private val description: String?
) : MemoryGutterIconRenderer() {

    override fun getIcon(): Icon = AllIcons.Actions.SetDefault

    override fun getTooltipText(): String {
        return buildString {
            append("Bookmark at 0x${"%016X".format(address.asLong)}")
            description?.let {
                append("\n$it")
            }
        }
    }

    override fun getClickAction(): AnAction {
        return object : AnAction("Edit Bookmark") {
            override fun actionPerformed(e: AnActionEvent) {
                val project = e.project ?: return
                val newDescription = Messages.showInputDialog(
                    project,
                    "Enter bookmark description:",
                    "Edit Bookmark",
                    Messages.getQuestionIcon(),
                    description ?: "",
                    null
                )

                if (newDescription != null) {
                    val debugState = DebuggerStateManager.getInstance(project)
                    debugState.updateBookmark(address, newDescription)
                }
            }
        }
    }

    override fun getAlignment(): Alignment = Alignment.RIGHT
}

/**
 * 修改标记Gutter渲染器
 */
class ModifiedGutterIconRenderer(
    override val address: Address
) : MemoryGutterIconRenderer() {

    override fun getIcon(): Icon = AllIcons.Actions.Diff

    override fun getTooltipText(): String {
        return "Memory modified at 0x${"%016X".format(address.asLong)}\nClick to view changes"
    }

    override fun getClickAction(): AnAction {
        return object : AnAction("View Changes") {
            override fun actionPerformed(e: AnActionEvent) {
//                val project = e.project ?: return
//                val memoryService = project.getService(MemoryViewService::class.java)
//                val facade = memoryService.getFacade()
//
//                // 显示修改详情
//                val changes = facade.getMemoryChanges(address)
//                if (changes.isNotEmpty()) {
//                    val message = buildString {
//                        append("Memory changes at 0x${"%016X".format(address.asLong)}:\n\n")
//                        changes.forEach { change ->
//                            append("Offset +${change.offset}: ")
//                            append("${"%02X".format(change.oldValue)} → ${"%02X".format(change.newValue)}\n")
//                        }
//                    }
//                    Messages.showInfoMessage(project, message, "Memory Changes")
//                }
            }
        }
    }

    override fun getAlignment(): Alignment = Alignment.RIGHT
}

/**
 * Gutter Action Group - 右键菜单
 */
class MemoryGutterActionGroup : DefaultActionGroup(), DumbAware  {

    init {
        add(ToggleBreakpointAction())
        add(AddWatchpointAction())
        add(AddBookmarkAction())
        add(Separator.getInstance())
        add(GutterCopyAddressAction())
        add(Separator.getInstance())

    }

    /**
     * 切换断点
     */
    class ToggleBreakpointAction : AnAction("Toggle Breakpoint", "Toggle memory breakpoint at current address", AllIcons.Debugger.Db_set_breakpoint) {
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            val editor = e.getData(CommonDataKeys.EDITOR) ?: return
            val address = getAddressAtCaret(editor, project) ?: return

            val debugState = DebuggerStateManager.getInstance(project)
            if (debugState.hasBreakpoint(address)) {
                debugState.removeBreakpoint(address)
            } else {
                debugState.addBreakpoint(address, MemoryBreakpointProperties())
            }
        }
    }

    /**
     * 添加监视点
     */
    class AddWatchpointAction : AnAction("Add Watchpoint...", "Add memory watchpoint", AllIcons.Debugger.Db_watch) {
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            val editor = e.getData(CommonDataKeys.EDITOR) ?: return
            val address = getAddressAtCaret(editor, project) ?: return

            val dialog = WatchpointConfigDialog(project, address)
            if (dialog.showAndGet()) {
                val debugState = DebuggerStateManager.getInstance(project)
                debugState.addWatchpoint(address, dialog.getWatchpointProperties())
            }
        }
    }

    /**
     * 添加书签
     */
    class AddBookmarkAction : AnAction("Add Bookmark...", "Add bookmark at current address", AllIcons.Actions.SetDefault) {
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            val editor = e.getData(CommonDataKeys.EDITOR) ?: return
            val address = getAddressAtCaret(editor, project) ?: return

            val description = Messages.showInputDialog(
                project,
                "Enter bookmark description:",
                "Add Bookmark",
                Messages.getQuestionIcon()
            )

            if (description != null) {
                val debugState = DebuggerStateManager.getInstance(project)
                debugState.addBookmark(address, description)
            }
        }
    }

    /**
     * 从 Gutter 复制地址
     */
    class GutterCopyAddressAction : AnAction("Copy Address", "Copy current address to clipboard", AllIcons.Actions.Copy) {
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            val editor = e.getData(CommonDataKeys.EDITOR) ?: return
            val address = getAddressAtCaret(editor, project) ?: return

            val addressStr = "0x${"%016X".format(address.asLong)}"
            val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(addressStr), null)

            showNotification(project, "Address copied: $addressStr")
        }
    }





    companion object {
        /**
         * 获取光标处的地址
         */
        private fun getAddressAtCaret(editor: Editor, project: Project): Address? {
            val offset = editor.caretModel.offset
            val lineNumber = editor.document.getLineNumber(offset)
            val lineStart = editor.document.getLineStartOffset(lineNumber)
            val lineEnd = editor.document.getLineEndOffset(lineNumber)
            val lineText = editor.document.getText(TextRange(lineStart, lineEnd))

            val match = Regex("""([0-9A-Fa-f]{8,16}):""").find(lineText)
            return match?.let {
                try {
                    Address.Companion.Factory.fromLong(it.groupValues[1].toLong(16))
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }
    }
}

/**
 * 内存断点属性
 */
class MemoryBreakpointProperties : XBreakpointProperties<MemoryBreakpointProperties>() {
    var condition: String? = null
    var logMessage: String? = null
    var enabled: Boolean = true

    override fun getState(): MemoryBreakpointProperties? = this
    override fun loadState(state: MemoryBreakpointProperties) {
        condition = state.condition
        logMessage = state.logMessage
        enabled = state.enabled
    }
}

/**
 * 监视点属性
 */
data class WatchpointProperties(
    val watchRead: Boolean = false,
    val watchWrite: Boolean = true,
    val watchExecute: Boolean = false,
    val size: Int = 1
)

/**
 * 书签信息
 */
data class MemoryBookmark(
    val address: Address,
    val description: String?
)

/**
 * 内存变更信息
 */
data class MemoryChange(
    val offset: Int,
    val oldValue: Byte,
    val newValue: Byte
)

/**
 * 监视点配置对话框（简化版）
 */
class WatchpointConfigDialog(project: Project, private val address: Address) :
    com.intellij.openapi.ui.DialogWrapper(project) {

    private var watchRead = false
    private var watchWrite = true
    private var watchExecute = false
    private var size = 1

    init {
        title = "Configure Watchpoint"
        init()
    }

    override fun createCenterPanel(): javax.swing.JComponent {
        return javax.swing.JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)

            add(javax.swing.JLabel("Address: 0x${"%016X".format(address.asLong)}"))
            add(javax.swing.JCheckBox("Watch Read").apply {
                isSelected = watchRead
                addActionListener { watchRead = isSelected }
            })
            add(javax.swing.JCheckBox("Watch Write").apply {
                isSelected = watchWrite
                addActionListener { watchWrite = isSelected }
            })
            add(javax.swing.JCheckBox("Watch Execute").apply {
                isSelected = watchExecute
                addActionListener { watchExecute = isSelected }
            })
        }
    }

    fun getWatchpointProperties(): WatchpointProperties {
        return WatchpointProperties(watchRead, watchWrite, watchExecute, size)
    }
}

/**
 * 调试器状态管理器 - 管理内存视图的调试状态
 */
class DebuggerStateManager private constructor(private val project: Project) {

    val breakpoints = mutableMapOf<Address, MemoryBreakpointProperties>()
    val watchpoints = mutableMapOf<Address, WatchpointProperties>()
    val bookmarks = mutableMapOf<Address, MemoryBookmark>()
    private var programCounter: Address? = null
    private val stackFrames = mutableMapOf<Int, Address>()
    private val sourceLocations = mutableMapOf<Address, com.intellij.openapi.fileEditor.OpenFileDescriptor>()

    fun isProgramCounter(address: Address): Boolean = programCounter == address

    fun getFrameIndex(address: Address): Int {
        return stackFrames.entries.firstOrNull { it.value == address }?.key ?: -1
    }

    fun switchToFrame(frameIndex: Int) {
        // 切换到指定栈帧
        val address = stackFrames[frameIndex] ?: return
        programCounter = address
        navigateToAddress(address)
    }

    fun hasBreakpoint(address: Address): Boolean = breakpoints.containsKey(address)

    fun addBreakpoint(address: Address, properties: MemoryBreakpointProperties) {
        breakpoints[address] = properties
    }

    fun removeBreakpoint(address: Address) {
        breakpoints.remove(address)
    }

    fun hasWatchpoint(address: Address): Boolean = watchpoints.containsKey(address)

    fun getWatchpoint(address: Address): WatchpointProperties? = watchpoints[address]

    fun addWatchpoint(address: Address, properties: WatchpointProperties) {
        watchpoints[address] = properties
    }

    fun updateWatchpoint(address: Address, properties: WatchpointProperties) {
        watchpoints[address] = properties
    }

    fun getBookmark(address: Address): MemoryBookmark? = bookmarks[address]

    fun addBookmark(address: Address, description: String?) {
        bookmarks[address] = MemoryBookmark(address, description)
    }

    fun updateBookmark(address: Address, description: String?) {
        bookmarks[address] = MemoryBookmark(address, description)
    }

    fun getSourceLocation(address: Address): com.intellij.openapi.fileEditor.OpenFileDescriptor? {
        return sourceLocations[address]
    }

    private fun navigateToAddress(address: Address) {
//        val memoryService = project.getService(MemoryViewService::class.java)
//        val facade = memoryService.getFacade()
//        val position = facade.createAddressPosition(address)
//        position.createNavigatable(project).navigate(true)
    }

    companion object {
        private val instances = mutableMapOf<Project, DebuggerStateManager>()

        fun getInstance(project: Project): DebuggerStateManager {
            return instances.getOrPut(project) { DebuggerStateManager(project) }
        }
    }
}

/**
 * 显示通知
 */
private fun showNotification(project: Project, message: String) {
    ApplicationManager.getApplication().invokeLater {
        com.intellij.openapi.ui.Messages.showInfoMessage(project, message, "Memory View")
    }
}

/**
 * 内存视图统计信息
 */
data class MemoryViewStatistics(
    val totalRange: Long,
    val loadedBytes: Long,
    val loadedPercentage: Int,
    val modifiedBytes: Long,
    val breakpointCount: Int,
    val watchpointCount: Int,
    val bookmarkCount: Int
)



