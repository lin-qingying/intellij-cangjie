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

package org.cangnova.cangjie.dapdebugger.ui

import com.intellij.openapi.diagnostic.Logger
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import kotlinx.coroutines.runBlocking

/**
 * 仓颉断点处理器
 *
 * 处理断点的注册和注销
 */
class CangJieBreakpointHandler(
    private val debugProcess: CangJieDebugProcess
) : XBreakpointHandler<XLineBreakpoint<CangJieLineBreakpointProperties>>(
    CangJieLineBreakpointType::class.java
) {

    companion object {
        private val LOG = Logger.getInstance(CangJieBreakpointHandler::class.java)
    }

    override fun registerBreakpoint(breakpoint: XLineBreakpoint<CangJieLineBreakpointProperties>) {
        runBlocking {
            debugProcess.getBreakpointService()
                .registerBreakpoint(breakpoint)
                .onFailure { error ->
                    LOG.error("Failed to register breakpoint", error)
                }
        }
    }

    override fun unregisterBreakpoint(
        breakpoint: XLineBreakpoint<CangJieLineBreakpointProperties>,
        temporary: Boolean
    ) {
        runBlocking {
            debugProcess.getBreakpointService()
                .unregisterBreakpoint(breakpoint)
                .onFailure { error ->
                    LOG.error("Failed to unregister breakpoint", error)
                }
        }
    }
}