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

package org.cangnova.cangjie.dapdebugger.service

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.cangnova.cangjie.dapdebugger.core.*
import org.cangnova.cangjie.dapdebugger.exception.BreakpointException
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 断点服务
 *
 * 管理断点的注册、同步和状态更新
 */
class BreakpointService(
    private val project: Project,
    private val adapter: DebugAdapter
) : BreakpointManager {

    companion object {
        private val LOG = Logger.getInstance(BreakpointService::class.java)
    }

    private val _breakpoints = MutableStateFlow<List<ManagedBreakpoint>>(emptyList())
    override val breakpoints: StateFlow<List<ManagedBreakpoint>> = _breakpoints.asStateFlow()

    // 断点映射: IDE断点 -> 托管断点
    private val breakpointMap = ConcurrentHashMap<XLineBreakpoint<*>, ManagedBreakpoint>()

    // 文件断点分组: 文件路径 -> 断点列表
    private val fileBreakpoints = ConcurrentHashMap<String, MutableSet<ManagedBreakpoint>>()

    override suspend fun registerBreakpoint(
        breakpoint: XLineBreakpoint<*>
    ): Result<ManagedBreakpoint> {
        return withContext(Dispatchers.IO) {
            try {
                val file = breakpoint.sourcePosition?.file ?: run {
                    return@withContext Result.failure(
                        BreakpointException("Breakpoint has no source position")
                    )
                }

                val managedBp = ManagedBreakpoint(
                    id = UUID.randomUUID().toString(),
                    ideBreakpoint = breakpoint,
                    state = BreakpointState.Pending,
                    serverBreakpoint = null
                )

                breakpointMap[breakpoint] = managedBp

                val filePath = file.path
                fileBreakpoints.computeIfAbsent(filePath) { ConcurrentHashMap.newKeySet() }
                    .add(managedBp)

                updateBreakpointsList()

                LOG.info("Registered breakpoint: ${managedBp.id} at $filePath:${breakpoint.line}")

                // 同步到服务器
                synchronizeFile(filePath)

                Result.success(managedBp)
            } catch (e: Exception) {
                LOG.error("Failed to register breakpoint", e)
                Result.failure(BreakpointException("Failed to register breakpoint", e))
            }
        }
    }

    override suspend fun unregisterBreakpoint(
        breakpoint: XLineBreakpoint<*>
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val managedBp = breakpointMap.remove(breakpoint) ?: run {
                    return@withContext Result.success(Unit)
                }

                val file = breakpoint.sourcePosition?.file
                if (file != null) {
                    fileBreakpoints[file.path]?.remove(managedBp)
                    synchronizeFile(file.path)
                }

                updateBreakpointsList()

                LOG.info("Unregistered breakpoint: ${managedBp.id}")
                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Failed to unregister breakpoint", e)
                Result.failure(BreakpointException("Failed to unregister breakpoint", e))
            }
        }
    }

    override suspend fun synchronize(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Synchronizing all breakpoints")

                fileBreakpoints.keys.forEach { filePath ->
                    synchronizeFile(filePath).getOrThrow()
                }

                LOG.info("All breakpoints synchronized")
                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Failed to synchronize breakpoints", e)
                Result.failure(BreakpointException("Failed to synchronize breakpoints", e))
            }
        }
    }

    override suspend fun updateBreakpointState(
        breakpointId: String,
        state: BreakpointState
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val managedBp = breakpointMap.values.find { it.id == breakpointId } ?: run {
                    return@withContext Result.failure(
                        BreakpointException("Breakpoint not found: $breakpointId")
                    )
                }

                val updated = managedBp.copy(state = state)
                breakpointMap[managedBp.ideBreakpoint] = updated

                updateBreakpointsList()

                // 更新UI
                ApplicationManager.getApplication().invokeLater {
                    updateBreakpointPresentation(updated)
                }

                LOG.debug("Updated breakpoint state: $breakpointId -> $state")
                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Failed to update breakpoint state", e)
                Result.failure(BreakpointException("Failed to update breakpoint state", e))
            }
        }
    }

    private suspend fun synchronizeFile(filePath: String): Result<Unit> {
        return try {
            val breakpoints = fileBreakpoints[filePath] ?: emptySet()

            val specs = breakpoints
                .filter { it.ideBreakpoint.isEnabled }
                .map { bp ->
                    BreakpointSpec(
                        line = bp.ideBreakpoint.line + 1, // DAP uses 1-based lines
                        column = 0,
                        condition = bp.ideBreakpoint.conditionExpression?.expression,
                        logMessage = null
                    )
                }

            val source = SourceFile(
                path = filePath,
                name = File(filePath).name
            )

            val results = adapter.setBreakpoints(source, specs).getOrThrow()

            // 更新断点状态
            breakpoints.zip(results).forEach { (managedBp, result) ->
                val newState = if (result.verified) {
                    BreakpointState.Verified
                } else {
                    BreakpointState.Failed(result.message ?: "Unknown error")
                }

                val serverBp = ServerBreakpoint(
                    id = result.id,
                    verified = result.verified,
                    line = result.line,
                    message = result.message
                )

                val updated = managedBp.copy(
                    state = newState,
                    serverBreakpoint = serverBp
                )

                breakpointMap[managedBp.ideBreakpoint] = updated
            }

            updateBreakpointsList()

            LOG.debug("Synchronized ${breakpoints.size} breakpoints in $filePath")
            Result.success(Unit)
        } catch (e: Exception) {
            LOG.error("Failed to synchronize file: $filePath", e)
            Result.failure(e)
        }
    }

    private fun updateBreakpointsList() {
        _breakpoints.value = breakpointMap.values.toList()
    }

    private fun updateBreakpointPresentation(breakpoint: ManagedBreakpoint) {
        val icon = when (breakpoint.state) {
            is BreakpointState.Pending -> AllIcons.Debugger.Db_set_breakpoint
            is BreakpointState.Verified -> AllIcons.Debugger.Db_verified_breakpoint
            is BreakpointState.Failed -> AllIcons.Debugger.Db_invalid_breakpoint
        }

        val errorMessage = when (val state = breakpoint.state) {
            is BreakpointState.Failed -> state.reason
            else -> null
        }

        XDebuggerManager.getInstance(project).breakpointManager.updateBreakpointPresentation(
            breakpoint.ideBreakpoint,
            icon,
            errorMessage
        )
    }
}