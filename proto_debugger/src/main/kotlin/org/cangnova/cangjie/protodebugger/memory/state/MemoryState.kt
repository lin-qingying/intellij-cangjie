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

package org.cangnova.cangjie.protodebugger.memory.state

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.AccessToken
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteIntentReadAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ex.DocumentEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.ui.EditorNotifications
import com.intellij.util.SlowOperations
import com.intellij.util.SlowOperations.allowSlowOperations
import com.intellij.util.ui.EDT
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.impl.XSourcePositionEx
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.cangnova.cangjie.protodebugger.memory.*
import org.cangnova.cangjie.protodebugger.memory.config.MemoryStoreConfig
import org.cangnova.cangjie.protodebugger.memory.editor.MemoryViewFileEditor
import org.cangnova.cangjie.protodebugger.memory.provider.MemoryCellLoader
import org.cangnova.cangjie.protodebugger.memory.rendering.CellDiff
import org.cangnova.cangjie.protodebugger.memory.rendering.SnapshotRenderer
import org.cangnova.cangjie.protodebugger.memory.vfs.MemoryViewVirtualFileSystem
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference


/**
 * 视图设置
 */
data class ViewSettings(
    val bytesPerLine: Int = 16,
    val showAscii: Boolean = true,
    val addressFormat: AddressFormat = AddressFormat.HEX,
    val dataFormat: DataFormat = DataFormat.HEX
) {
    enum class AddressFormat {
        HEX, DECIMAL, OCTAL
    }

    enum class DataFormat {
        HEX, DECIMAL, BINARY, ASCII
    }

    companion object {
        fun default(): ViewSettings = ViewSettings()
    }
}

/**
 * 内存视图的全局不可变状态
 *
 * 简化的Redux状态模型：
 * - 只保留核心的 Matrix 和 Settings
 * - 移除了 viewport、selection、highlights 等UI相关状态
 *
 * 注意：每个 Store 实例管理一个特定类型的 Matrix：
 * - HexStore 使用 MemoryMatrix<MemoryCell.DataCell>
 * - DisasmStore 使用 MemoryMatrix<MemoryCell.InstructionCell>
 */
data class MemoryViewState<T : MemoryCell>(
    val matrix: MemoryMatrix<T>,
    val settings: ViewSettings,
    val version: Long = 0
) {

    companion object {
        /**
         * 初始状态
         */
        fun <T : MemoryCell> initial(): MemoryViewState<T> = MemoryViewState(
            matrix = MemoryMatrix(),
            settings = ViewSettings.default(),
        )
    }
}

/**
 * 内存动作 - 所有可能的状态变更操作
 *
 * Redux模式：状态变化通过分发动作来触发
 *
 * 简化版本：只保留核心操作
 */
sealed class MemoryAction {
    /**
     * 加载地址范围
     */
    data class LoadRange(val range: AddressRange, val cells: List<MemoryCell>) : MemoryAction()

    /**
     * 更新设置
     */
    data class UpdateSettings(val settings: ViewSettings) : MemoryAction()

    /**
     * 清空所有数据
     */
    object Clear : MemoryAction()
}

/**
 * Reducer - 纯函数状态转换器
 *
 * 根据当前状态和动作，计算新的状态。
 * 这是Redux的核心：纯函数、不可变、可预测。
 */
object MemoryReducer {
    /**
     * 状态归约函数
     *
     * @param state 当前状态
     * @param action 要执行的动作
     * @return 新的状态
     */
    fun <T : MemoryCell> reduce(state: MemoryViewState<T>, action: MemoryAction): MemoryViewState<T> {
        return when (action) {
            is MemoryAction.LoadRange -> {
                // 直接设置到 matrix（由于泛型约束，只能设置正确类型的 cell）
                @Suppress("UNCHECKED_CAST")
                action.cells.forEach { cell ->
                    state.matrix.setCell(cell as T)
                }
                // 返回原状态（matrix已就地修改）
                state.copy(version = state.version + 1)
            }

            is MemoryAction.UpdateSettings -> {
                state.copy(settings = action.settings)
            }

            is MemoryAction.Clear -> {
                // 3. 清空 matrix（如果 MemoryMatrix 提供 clear 方法）
                state.matrix.clear()

                MemoryViewState.initial()
            }
        }
    }
}

/**
 * 状态监听器
 */
fun interface StateListener<T : MemoryCell> {
    /**
     * 状态变化回调
     *
     * @param oldState 旧状态
     * @param newState 新状态
     */
    fun onStateChanged(oldState: MemoryViewState<T>, newState: MemoryViewState<T>)
}

/**
 * Store - 特定类型 MemoryCell 的完整管理器
 *
 * 职责：
 * 1. 状态管理（Redux pattern）- 持有状态，分发动作，通知监听器
 * 2. 数据加载 - 使用 Provider 从数据源加载数据
 * 3. 查询功能 - 查询特定地址范围的数据
 * 4. 视图控制 - 滚动、选择、高亮等视图操作
 *
 * 设计原则：
 * - 每个 Store 管理一种类型的 Cell（DataCell 或 InstructionCell）
 * - 使用 Provider 接口解耦数据源
 * - 提供类型安全的 API
 * - 使用 CAS 保证线程安全
 * - 通过 Redux 模式保证状态可预测
 *
 * @param T 单元格类型（DataCell 或 InstructionCell）
 * @param provider 数据提供者，用于从数据源加载数据
 * @param centerAddress 初始中心地址
 */
class MemoryStore<T : MemoryCell>(
    val config: MemoryStoreConfig,
    private val provider: MemoryCellLoader<T>,
    val facade: MemoryViewFacade,
    val fileType: FileType

) : Disposable {
    init {
        Disposer.register(facade, this)
    }

    private val state = AtomicReference(MemoryViewState.initial<T>())
    private val listeners = CopyOnWriteArrayList<StateListener<T>>()
    // 创建虚拟文件系统


    val virtualFile = MemoryViewVirtualFileSystem.getInstance().getOrCreateFile<T>(this)


    // 文档 - 直接基于 Matrix 的简化架构
    // 延迟初始化 document
    val document: DocumentEx by lazy {
        runReadAction {
            FileDocumentManager.getInstance().getDocument(virtualFile) as? DocumentEx
                ?: throw IllegalStateException("Null or non-DocumentEx document")
        }
    }

    /**
     * 安全地读取文档内容
     *
     * 在读操作中执行给定的代码块
     *
     * @param block 要执行的读操作
     * @return 读操作的结果
     */
    fun <R> readDocument(block: () -> R): R {
        return runReadAction(block)
    }

    /**
     * 安全地修改文档内容
     *
     * 在写操作中执行给定的代码块，自动处理文档和虚拟文件的可写状态：
     * 1. 保存 virtualFile 的原始只读状态
     * 2. 将 virtualFile 设置为可写
     * 3. 将 document 设置为可写
     * 4. 执行 block 中的操作
     * 5. 将 document 恢复为只读
     * 6. 将 virtualFile 恢复为原始状态
     *
     * @param block 要执行的写操作
     */
    fun writeDocument(block: () -> Unit) {
        WriteCommandAction.runWriteCommandAction(facade.project) {
            // 1. 保存 virtualFile 的原始只读状态
            val originalReadOnly = !virtualFile.isWritable

            try {
                // 2. 将 virtualFile 设置为可写
                if (originalReadOnly) {
                    virtualFile.isWritable = true
                }

                // 3. 将 document 设置为可写
                document.setReadOnly(false)

                // 4. 执行 block 中的操作
                block()

            } finally {
                // 5. 将 document 恢复为只读
                document.setReadOnly(true)

                // 6. 将 virtualFile 恢复为原始状态
                if (originalReadOnly) {
                    virtualFile.isWritable = false
                }
            }
        }
    }

    // 快照渲染器 - 用于差异计算和增量更新
    private val renderer = SnapshotRenderer()

    // 行号到地址的映射表（用于所有视图类型）
    // 键: 行号, 值: 地址
    // 十六进制视图: 每行映射到该行的起始地址
    // 反汇编视图: 每条指令映射到指令地址，符号标题行和空行不映射
    private val lineToAddressMap = AtomicReference<Map<Int, Address>>(emptyMap())

    init {
        // 订阅状态变化，自动更新 Document
        subscribe { oldState, newState ->
            // 版本变化时触发渲染
            if (oldState.version != newState.version) {
                renderToDocument(newState)
            }
        }
    }

    /**
     * 渲染状态到 Document
     *
     * 使用 SnapshotRenderer 计算差异，然后更新 Document
     */
    private fun renderToDocument(state: MemoryViewState<T>) {
        val diff = renderer.render(state)

        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(facade.project) {
                if (diff == null) {
                    // 初次渲染：全量更新
                    fullRenderToDocument(state)
                } else if (diff.hasChanges()) {
                    // 增量更新：只更新变化的部分
                    incrementalRenderToDocument(state, diff)
                }
            }
        }
    }

    /**
     * 全量渲染到 Document
     */
    private fun fullRenderToDocument(state: MemoryViewState<T>) {
        val text = renderCellsToText(state.matrix.getCellsInRange(state.matrix.getAddressRange()))
        writeDocument {
            document.setText(text)
        }
    }

    /**
     * 增量渲染到 Document
     *
     * 目前简化实现：直接全量更新
     * TODO: 实现真正的增量更新，只修改变化的行
     */
    private fun incrementalRenderToDocument(state: MemoryViewState<T>, diff: CellDiff) {
        // 简化实现：直接全量更新
        // 后续可以优化为只更新变化的行
        fullRenderToDocument(state)
    }

    /**
     * 将单元格列表渲染为文本
     *
     * 根据 fileType 选择不同的渲染策略
     */
    private fun renderCellsToText(cells: List<T>): String {
        if (cells.isEmpty()) return ""

        val settings = state.get().settings
        val sb = StringBuilder()

        // 按行分组（每行 bytesPerLine 个字节）
        val sortedCells = cells.sortedBy { it.address.value }

        when (val firstCell = sortedCells.firstOrNull()) {
            is MemoryCell.DataCell -> {
                // 十六进制视图渲染
                renderHexView(sortedCells.filterIsInstance<MemoryCell.DataCell>(), settings, sb)
            }

            is MemoryCell.InstructionCell -> {
                // 反汇编视图渲染
                renderDisasmView(sortedCells.filterIsInstance<MemoryCell.InstructionCell>(), sb)
            }

            else -> {}
        }

        return sb.toString()
    }

    /**
     * 渲染十六进制视图
     *
     * 格式化输出十六进制内存数据，包括：
     * - 十六进制数据（每行 bytesPerLine 个字节）
     * - ASCII 可视化（可选）
     *
     * 输出格式示例（bytesPerLine=16, showAscii=true）：
     * ```
     * 48 65 6C 6C 6F 20 57 6F 72 6C 64 21 00 00 00 00  |Hello World!....|
     * 54 68 69 73 20 69 73 20 61 20 74 65 73 74       |This is a test  |
     * ```
     *
     * @param cells 数据单元格列表
     * @param settings 视图设置
     * @param sb 字符串构建器
     */
    private fun renderHexView(cells: List<MemoryCell.DataCell>, settings: ViewSettings, sb: StringBuilder) {
        if (cells.isEmpty()) return

        val bytesPerLine = settings.bytesPerLine

        // 先按地址排序
        val sortedCells = cells.sortedBy { it.address.value }

        // 按行分组（每行 bytesPerLine 个字节）
        val groupedByLine = sortedCells.groupBy { it.address.value / bytesPerLine.toULong() }

        // 构建行号到地址的映射表
        val lineToAddressMapBuilder = mutableMapOf<Int, Address>()
        var currentLine = 0

        // 按行号排序后渲染
        groupedByLine.keys.sorted().forEach { lineIndex ->
            val lineCells = groupedByLine[lineIndex] ?: return@forEach

            // 计算该行的起始地址
            val lineStartAddress = Address(lineIndex * bytesPerLine.toULong())

            // 记录行号到地址的映射（每行映射到该行的起始地址）
            lineToAddressMapBuilder[currentLine] = lineStartAddress

            // 构建单元格地址到数据的快速查找映射
            val cellMap = lineCells.associateBy { it.address }

            // 渲染十六进制数据
            renderHexData(lineStartAddress, bytesPerLine, cellMap, settings, sb)

            // 渲染 ASCII 列（如果启用）
            if (settings.showAscii) {
                renderAsciiColumn(lineStartAddress, bytesPerLine, cellMap, sb)
            }

            sb.append("\n")
            currentLine++
        }

        // 更新映射表
        lineToAddressMap.set(lineToAddressMapBuilder)
    }

    /**
     * 渲染十六进制数据列
     *
     * @param lineStartAddress 行起始地址
     * @param bytesPerLine 每行字节数
     * @param cellMap 地址到单元格的映射
     * @param settings 视图设置
     * @param sb 字符串构建器
     */
    private fun renderHexData(
        lineStartAddress: Address,
        bytesPerLine: Int,
        cellMap: Map<Address, MemoryCell.DataCell>,
        settings: ViewSettings,
        sb: StringBuilder
    ) {
        for (i in 0 until bytesPerLine) {
            val cellAddress = Address(lineStartAddress.value + i.toULong())
            val cell = cellMap[cellAddress]

            if (cell != null) {
                // 根据 dataFormat 格式化数据
                val formatted = when (settings.dataFormat) {
                    ViewSettings.DataFormat.HEX -> String.format("%02X", cell.value.toInt() and 0xFF)
                    ViewSettings.DataFormat.DECIMAL -> String.format("%3d", cell.value.toInt() and 0xFF)
                    ViewSettings.DataFormat.BINARY -> String.format(
                        "%8s",
                        Integer.toBinaryString(cell.value.toInt() and 0xFF)
                    ).replace(' ', '0')

                    ViewSettings.DataFormat.ASCII -> {
                        val b = cell.value.toInt() and 0xFF
                        if (b in 32..126) " ${b.toChar()}" else " ."
                    }
                }
                sb.append(formatted).append(" ")
            } else {
                // 空单元格：根据格式填充相应长度的空格
                val padding = when (settings.dataFormat) {
                    ViewSettings.DataFormat.HEX -> "   "
                    ViewSettings.DataFormat.DECIMAL -> "    "
                    ViewSettings.DataFormat.BINARY -> "         "
                    ViewSettings.DataFormat.ASCII -> "   "
                }
                sb.append(padding)
            }
        }
    }

    /**
     * 渲染 ASCII 可视化列
     *
     * @param lineStartAddress 行起始地址
     * @param bytesPerLine 每行字节数
     * @param cellMap 地址到单元格的映射
     * @param sb 字符串构建器
     */
    private fun renderAsciiColumn(
        lineStartAddress: Address,
        bytesPerLine: Int,
        cellMap: Map<Address, MemoryCell.DataCell>,
        sb: StringBuilder
    ) {
        sb.append(" |")
        for (i in 0 until bytesPerLine) {
            val cellAddress = Address(lineStartAddress.value + i.toULong())
            val cell = cellMap[cellAddress]

            if (cell != null) {
                val b = cell.value.toInt() and 0xFF
                // 可打印 ASCII 字符直接显示，其他显示为 '.'
                sb.append(if (b in 32..126) b.toChar() else '.')
            } else {
                sb.append(' ')
            }
        }
        sb.append("|")
    }

    /**
     * 渲染反汇编视图
     */
    /**
     * 渲染反汇编视图
     *
     * 格式化输出反汇编指令，包括：
     * - 符号信息（函数名+偏移量）
     * - 指令文本
     * - 注释信息
     *
     * 输出格式示例：
     * ```
     * main+0x0:
     *     mov    rdi, rsp
     *     call   0x1234                ; some_function
     *     add    rsp, 0x10
     *
     * other_function+0x0:
     *     push   rbp
     *     mov    rbp, rsp
     * ```
     *
     * @param cells 反汇编指令单元格列表
     * @param sb 字符串构建器
     */
    private fun renderDisasmView(cells: List<MemoryCell.InstructionCell>, sb: StringBuilder) {
        // 先按地址排序
        val sortedCells = cells.sortedBy { it.address.value }

        // 按符号分组（保持原顺序）
        val groupedBySymbol = sortedCells.groupBy { it.symbol }

        // 为了保持地址顺序，需要按第一个指令的地址排序分组
        val sortedGroups = groupedBySymbol.entries
            .sortedBy { (_, cells) -> cells.firstOrNull()?.address?.value ?: ULong.MAX_VALUE }

        // 构建行号到地址的映射表
        val lineToAddressMapBuilder = mutableMapOf<Int, Address>()
        var currentLine = 0

        // 渲染每个符号组
        sortedGroups.forEachIndexed { index, (symbol, groupCells) ->
            // 非第一组前添加空行分隔
            if (index > 0) {
                sb.append("\n")
                currentLine++ // 空行不映射地址
            }

            // 显示符号标题（如果有符号）
            if (symbol != null) {
                sb.append("$symbol:\n")
                currentLine++ // 符号标题行不映射地址
            }

            // 渲染组内的所有指令（使用4空格缩进）
            groupCells.forEach { cell ->
                // 记录行号到地址的映射
                lineToAddressMapBuilder[currentLine] = cell.address

                sb.append("    ${cell.disassembly}\n")
                currentLine++
            }
        }

        // 更新映射表
        lineToAddressMap.set(lineToAddressMapBuilder)
    }

    // ==================== 导航和地址查询 ====================


    /**
     * 根据行号获取地址
     *
     * 根据当前渲染格式计算行号对应的起始地址
     *
     * @param line 文档中的行号
     * @return 对应的内存地址
     */
    fun getAddressForLine(line: Int): Address? {
        // 统一使用映射表查询（十六进制视图和反汇编视图都已构建映射表）
        return lineToAddressMap.get()[line]
    }

    /**
     * 从文档偏移映射到地址
     *
     * 根据当前渲染格式计算文档偏移对应的内存地址
     *
     * @param offset 文档偏移
     * @return 对应的内存地址，如果无法映射返回null
     */
    fun mapOffsetToAddress(offset: Int): Address? {
        if (document.textLength == 0) return null

        val line = document.getLineNumber(offset.coerceIn(0, document.textLength - 1))
        return getAddressForLine(line)
    }

    /**
     * 从地址映射到文档偏移
     *
     * 根据当前渲染格式计算内存地址对应的文档偏移
     *
     * @param address 内存地址
     * @return 对应的文档偏移，如果无法映射返回null
     */
    fun mapAddressToOffset(address: Address): Int? {
        if (document.textLength == 0) return null

        // 统一使用映射表查询
        val map = lineToAddressMap.get()
        if (map.isEmpty()) return null

        // 查找精确匹配或最接近的地址
        val lineNumber = map.entries.find { (_, addr) -> addr == address }?.key
            ?: map.entries
                .filter { (_, addr) -> addr.value <= address.value }
                .maxByOrNull { (_, addr) -> addr.value }
                ?.key
            ?: return null

        // 确保行号在有效范围内
        val safeLineNumber = lineNumber.coerceIn(0, document.lineCount - 1)
        return document.getLineStartOffset(safeLineNumber)
    }
    // ==================== 状态管理（Redux Core） ====================

    /**
     * 分发动作
     *
     * 这是修改状态的唯一方式。
     *
     * @param action 要执行的动作
     */
    fun dispatch(action: MemoryAction) {
        while (true) {
            val oldState = state.get()
            val newState = MemoryReducer.reduce(oldState, action)

            if (state.compareAndSet(oldState, newState)) {
                // CAS成功，通知监听器
                notifyListeners(oldState, newState)
                break
            }
            // CAS失败，重试
        }
    }

    /**
     * 批量分发动作
     *
     * @param actions 动作列表
     */
    fun dispatchBatch(actions: List<MemoryAction>) {
        actions.forEach { dispatch(it) }
    }

    /**
     * 选择器：获取状态切片
     *
     * @param selector 选择函数
     * @return 选择的结果
     */
    fun <R> select(selector: (MemoryViewState<T>) -> R): R {
        return selector(state.get())
    }

    /**
     * 获取当前完整状态
     */
    fun getState(): MemoryViewState<T> = state.get()

    /**
     * 订阅状态变化
     *
     * @param listener 监听器
     * @return Disposable对象，用于取消订阅
     */
    fun subscribe(listener: StateListener<T>): Disposable {
        listeners.add(listener)
        return Disposable { listeners.remove(listener) }
    }

    /**
     * 通知所有监听器
     */
    private fun notifyListeners(oldState: MemoryViewState<T>, newState: MemoryViewState<T>) {
        if (oldState !== newState) {
            listeners.forEach { it.onStateChanged(oldState, newState) }
        }
    }

    override fun dispose() {
        // 1. 先清空监听器（防止后续操作触发回调）
        listeners.clear()

        // 2. 清空当前 matrix 的数据（重要！）
        state.get().matrix.clear()

        // 3. 清空映射表
        lineToAddressMap.set(emptyMap())

        // 4. 清空 renderer 的快照缓存
        renderer.clear()


        // 5. 清空文档并更新 UI
        val application = ApplicationManager.getApplication()

        val updateUI = {
            // 先清空文档
//            try {
//                writeDocument {
//                    document.setText("")
//                }
//            } catch (e: Exception) {
//                // 忽略异常
//            }

            // 再更新 UI 通知
            if (!facade.project.isDisposed) {
                FileEditorManager.getInstance(facade.project)
                    .updateFilePresentation(virtualFile)

                WriteIntentReadAction.run {
                    FileEditorManager.getInstance(facade.project).closeFile(virtualFile)
                    runWriteAction {
                        virtualFile.setWritable(true);
                        virtualFile.delete(this);
                        virtualFile.setValid(false);
                    }
                }
                EditorNotifications.getInstance(facade.project)
                    .updateNotifications(virtualFile)


            }
        }

        if (application.isDispatchThread) {
            updateUI()
        } else {
            application.invokeLater(updateUI)
        }
    }

    /**
     * 获取指定地址的源位置
     *
     * 支持调试器框架的地址导航。
     *
     * @param address 内存地址
     * @return XSourcePositionEx 实例
     */
    fun createAddressPosition(address: Address): XSourcePositionEx {
        return AddressPosition(
            facade.project,
            virtualFile,
            address,
            WeakReference(this)
        )
    }

    // ==================== 数据加载====================

    /**
     * 从指定地址加载数据
     *
     * 使用 Provider 从数据源加载数据，然后更新状态
     *
     * @param address 起始地址
     */
    suspend fun load(address: Address) {
        // 1. 从 Provider 加载数据
        val cells = provider.load(address)

        // 2. 计算加载的范围
        if (cells.isNotEmpty()) {
            val minAddr = cells.minOf { it.address.value }
            val maxAddr = cells.maxOf { it.address.value }
            val range = Address(minAddr).rangeTo(Address(maxAddr))

            // 3. 更新状态
            dispatch(MemoryAction.LoadRange(range, cells))
        }
    }

    /**
     * 直接加载地址范围的数据
     *
     * @param range 地址范围
     * @param cells 单元格列表
     */
    suspend fun loadRange(range: AddressRange) {
        // 1. 从 Provider 加载数据
        val cells = provider.loadRange(range)

        // 2. 计算加载的范围
        if (cells.isNotEmpty()) {
            val minAddr = cells.minOf { it.address.value }
            val maxAddr = cells.maxOf { it.address.value }
            val range = Address(minAddr).rangeTo(Address(maxAddr))

            // 3. 更新状态
            dispatch(MemoryAction.LoadRange(range, cells))
        }
    }


    // ==================== 查询功能 ====================

    /**
     * 获取指定地址的单元格
     *
     * @param address 地址
     * @return 单元格，如果不存在返回 null
     */
    fun getCell(address: Address): T? {
        return state.get().matrix.getCell(address)
    }

    /**
     * 获取地址范围内的所有单元格
     *
     * @param range 地址范围
     * @return 单元格列表
     */
    fun getCellsInRange(range: AddressRange): List<T> {
        return state.get().matrix.getCellsInRange(range)
    }

    /**
     * 检查地址是否已加载
     *
     * @param address 地址
     * @return 是否已加载
     */
    fun isAddressLoaded(address: Address): Boolean {
        return getCell(address) != null
    }

    /**
     * 获取矩阵覆盖的地址范围
     *
     * @return 地址范围
     */
    fun getAddressRange(): AddressRange {
        return state.get().matrix.getAddressRange()
    }

    // ==================== 视图控制 ====================

    /**
     * 更新视图设置
     *
     * @param settings 新的设置
     */
    fun updateSettings(settings: ViewSettings) {
        dispatch(MemoryAction.UpdateSettings(settings))
    }

    /**
     * 滚动到指定地址
     *
     * 将内存视图滚动到指定地址，使其在可视区域中居中显示。
     * 如果地址尚未加载，会触发数据加载。
     *
     * @param alignedAddress 要滚动到的地址（应该已对齐）
     */
    fun scrollTo(alignedAddress: Address) {
        // 1. 检查地址是否已加载，如果未加载则先加载数据
        if (!isAddressLoaded(alignedAddress)) {
            facade.scope.launch {
                load(alignedAddress)
                // 加载完成后再滚动
                scrollToAddress(alignedAddress)
            }
        } else {
            // 地址已加载，直接滚动
            scrollToAddress(alignedAddress)
        }
    }

    /**
     * 执行实际的滚动操作
     *
     * 内部方法，将编辑器滚动到指定地址对应的文档位置
     *
     * @param address 目标地址
     */
    private fun scrollToAddress(address: Address) {
        // 1. 将地址映射到文档偏移
        val offset = mapAddressToOffset(address) ?: return

        // 2. 在EDT上执行滚动操作
        ApplicationManager.getApplication().invokeLater {
            // 3. 通过FileEditorManager找到对应的编辑器
            val fileEditorManager = FileEditorManager.getInstance(facade.project)
            val fileEditors = fileEditorManager.getEditors(virtualFile)

            // 4. 找到MemoryViewFileEditor并滚动
            for (fileEditor in fileEditors) {
                if (fileEditor is MemoryViewFileEditor<*>) {
                    val editor = fileEditor.editor
                    // 移动光标到目标位置
                    editor.caretModel.moveToOffset(offset)
                    // 滚动使光标居中显示
                    editor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.CENTER)
                    break
                }
            }
        }
    }

    fun getAddressAtSourcePosition(position: XSourcePosition): Address? {
        if(position.file != virtualFile) return null
        return getAddressForLine(position.line)

    }


}

/**
 * 地址位置实现
 *
 * 实现 XSourcePositionEx 接口，支持调试器框架的地址导航。
 */
class AddressPosition(
    private val project: Project,
    private val virtualFile: VirtualFile,
    private val address: Address,
    private val storeRef: WeakReference<MemoryStore<*>>
) :
    OpenFileDescriptor(project, virtualFile),
    XSourcePositionEx {

    /**
     * 位置更新流
     *
     * 用于异步加载地址数据。
     */
    override val positionUpdateFlow: Flow<Boolean> = flow {
        val store = storeRef.get() ?: return@flow
        if (!store.isAddressLoaded(address)) {
            store.load(address)
            emit(true)
        }
    }

    /**
     * 创建导航器
     */
    override fun createNavigatable(project: Project): Navigatable = this
    override fun navigate(requestFocus: Boolean) {
        super.navigate(requestFocus)
    }

    /**
     * 获取列号
     *
     * 内存视图中列号始终为 0。
     */
    override fun getColumn(): Int = 0

    /**
     * 获取行号
     *
     * 返回地址在文档中对应的行号。
     */
    override fun getLine(): Int {

        // 尝试获取地址对应的文档偏移
        val offset = storeRef.get()?.mapAddressToOffset(address)
        if (offset != null) {
            return storeRef.get()?.readDocument {
                storeRef.get()?.document?.getLineNumber(offset)
            } ?: 0
        }
        return 0
    }

    /**
     * 获取偏移
     *
     * 返回地址在文档中对应的偏移。
     */
    override fun getOffset(): Int {
        return storeRef.get()?.mapAddressToOffset(address) ?: 0
    }
}
