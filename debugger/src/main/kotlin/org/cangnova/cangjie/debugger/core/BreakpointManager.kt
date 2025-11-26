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

package org.cangnova.cangjie.debugger.core

import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import kotlinx.coroutines.flow.StateFlow

/**
 * 断点管理器接口
 *
 * 负责断点的生命周期管理，包括注册、注销、同步和状态更新
 */
interface BreakpointManager {

    /**
     * 所有断点
     */
    val breakpoints: StateFlow<List<ManagedBreakpoint>>

    /**
     * 注册断点
     */
    suspend fun registerBreakpoint(
        breakpoint: XLineBreakpoint<*>
    ): Result<ManagedBreakpoint>

    /**
     * 注销断点
     */
    suspend fun unregisterBreakpoint(
        breakpoint: XLineBreakpoint<*>
    ): Result<Unit>

    /**
     * 同步断点到调试适配器
     */
    suspend fun synchronize(): Result<Unit>

    /**
     * 更新断点状态
     */
    suspend fun updateBreakpointState(
        breakpointId: String,
        state: BreakpointState
    ): Result<Unit>
}

/**
 * 托管断点
 *
 * 包含IDE断点和服务器断点的映射关系
 */
data class ManagedBreakpoint(
    val id: String,
    val ideBreakpoint: XLineBreakpoint<*>,
    val state: BreakpointState,
    val serverBreakpoint: ServerBreakpoint?
)

/**
 * 断点状态
 */
sealed class BreakpointState {
    object Pending : BreakpointState()
    object Verified : BreakpointState()
    data class Failed(val reason: String) : BreakpointState()
}

/**
 * 服务器断点
 */
data class ServerBreakpoint(
    val id: Int,
    val verified: Boolean,
    val line: Int,
    val message: String?
)