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

package org.cangnova.cangjie.debugger.process

import com.intellij.openapi.diagnostic.Logger
import org.cangnova.cangjie.debugger.exception.PortException
import java.io.IOException
import java.net.ServerSocket

/**
 * 端口管理器
 *
 * 管理端口的分配和检测
 */
class PortManager {

    companion object {
        private val LOG = Logger.getInstance(PortManager::class.java)
    }

    /**
     * 查找可用端口
     */
    fun findAvailablePort(start: Int, end: Int): Result<Int> {
        for (port in start..end) {
            if (isPortAvailable(port)) {
                LOG.info("Found available port: $port")
                return Result.success(port)
            }
        }

        return Result.failure(
            PortException("No available ports in range $start-$end")
        )
    }

    /**
     * 检查端口是否可用
     */
    fun isPortAvailable(port: Int): Boolean {
        return try {
            ServerSocket(port).use { true }
        } catch (e: IOException) {
            false
        }
    }

    /**
     * 获取随机可用端口
     */
    fun getRandomAvailablePort(): Result<Int> {
        return try {
            ServerSocket(0).use { socket ->
                val port = socket.localPort
                LOG.info("Got random available port: $port")
                Result.success(port)
            }
        } catch (e: IOException) {
            Result.failure(PortException("Failed to get random port", e))
        }
    }
}