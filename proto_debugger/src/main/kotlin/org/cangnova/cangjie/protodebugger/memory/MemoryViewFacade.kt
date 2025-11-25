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

package org.cangnova.cangjie.protodebugger.memory

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.xdebugger.XSourcePosition
import org.cangnova.cangjie.protodebugger.core.DebuggerDriverFacade
import org.cangnova.cangjie.protodebugger.memory.config.MemoryStoreConfig
import org.cangnova.cangjie.protodebugger.memory.provider.DisasmCellLoader
import org.cangnova.cangjie.protodebugger.memory.provider.HexCellLoader
import org.cangnova.cangjie.protodebugger.memory.state.*
import org.cangnova.cangjie.protodebugger.memory.vfs.DisasmFileType
import org.cangnova.cangjie.protodebugger.memory.vfs.HexdumpFileType

/**
 * 内存视图外观
 *
 * V3设计的统一入口点，职责：
 * - 管理双 Store（hexStore 和 disasmStore）
 * - 提供虚拟文件访问
 *
 * 注意：
 * - 数据加载由各个 Store 通过 Provider 自行管理
 * - 具体的状态操作（如选择、高亮、滚动等）应该通过获取 Store 实例后直接调用
 *
 * 这是对外的主要API，隐藏了内部复杂性。
 * 只能通过 DebuggerDriverFacade 获取实例。
 */
class MemoryViewFacade(
      val debuggerFacade: DebuggerDriverFacade,
    initialRange: AddressRange = Address(0x1000UL).rangeTo(Address(0x1FFFUL))
) : Disposable {

    // 项目引用
    val project: Project get() = debuggerFacade.project


    val scope get() = debuggerFacade.scope


    // 核心组件 - 双 Store 架构
    private val hexStore: MemoryStore<MemoryCell.DataCell>       // 十六进制视图的 Store
    private val disasmStore: MemoryStore<MemoryCell.InstructionCell>  // 反汇编视图的 Store



    init {
        // 注册清理 - 注册到调试会话的 processDisposable，确保调试会话结束时自动清理
        Disposer.register(debuggerFacade.facadeDisposable, this)

        // 创建 Providers
        val hexProvider = HexCellLoader(
            memoryService = debuggerFacade.memoryService,

        )
        val disasmProvider = DisasmCellLoader(
            disasmService = debuggerFacade.disasmService,
            instructionCount = 50
        )

        // 初始化双 Store（使用 Provider）
        hexStore = MemoryStore(MemoryStoreConfig.DEFAULT, hexProvider, this, HexdumpFileType)
        disasmStore = MemoryStore(MemoryStoreConfig.DEFAULT.copy(vfsDisableLazyLoad = true), disasmProvider, this, DisasmFileType)


    }

    // ==================== Store 访问 ====================

    /**
     * 获取十六进制视图的 Store
     *
     * 使用示例：
     * ```kotlin
     * val hexStore = facade.getHexStore()
     * hexStore.load(address)  // 加载数据
     * hexStore.scrollTo(address)
     * hexStore.highlight(range, HighlightType.MODIFIED)
     * ```
     */
    fun getHexStore(): MemoryStore<MemoryCell.DataCell> = hexStore

    /**
     * 获取反汇编视图的 Store
     *
     * 使用示例：
     * ```kotlin
     * val disasmStore = facade.getDisasmStore()
     * disasmStore.load(address)  // 加载反汇编数据
     * disasmStore.select(range, primary)
     * ```
     */
    fun getDisasmStore(): MemoryStore<MemoryCell.InstructionCell> = disasmStore

    // ==================== 文档访问 ====================


    /**
     * 获取当前状态（十六进制视图）
     */
    fun getState(): MemoryViewState<MemoryCell.DataCell> = hexStore.getState()

    /**
     * 获取反汇编状态
     */
    fun getDisasmState(): MemoryViewState<MemoryCell.InstructionCell> = disasmStore.getState()



    override fun dispose() {


    }


}

fun XSourcePosition.toOpenFileDescriptor(project: Project): OpenFileDescriptor {
    return if (this is  AddressPosition) {
        this
    } else {
        val navigatable = this.createNavigatable(project)
        return if (navigatable is OpenFileDescriptor) {
            navigatable
        } else {
            createOpenFileDescriptor(project, this)
        }
    }
}

fun createOpenFileDescriptor(project: Project, position: XSourcePosition): OpenFileDescriptor {
    return if (position.getOffset() != -1)
        OpenFileDescriptor(project, position.getFile(), position.getOffset())
    else
        OpenFileDescriptor(project, position.getFile(), position.getLine(), 0)
}