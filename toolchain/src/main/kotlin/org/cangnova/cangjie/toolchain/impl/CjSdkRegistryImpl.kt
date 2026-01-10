/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.toolchain.impl

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import org.cangnova.cangjie.toolchain.api.CjSdk
import org.cangnova.cangjie.toolchain.api.CjSdkDetector
import org.cangnova.cangjie.toolchain.api.CjSdkRegistry
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

/**
 * SDK 注册中心的持久化状态
 */
internal data class CjSdkRegistryState(
    /**
     * 已注册的 SDK 路径列表
     * 存储格式: Map<SDK ID, SDK 根路径>
     */
    var sdkPaths: MutableMap<String, String> = mutableMapOf(),

    /**
     * SDK 自定义名称
     * 存储格式: Map<SDK ID, 自定义名称>
     */
    var customNames: MutableMap<String, String> = mutableMapOf()
)

/**
 * SDK 注册中心的默认实现
 *
 * 使用 IntelliJ Platform 的持久化机制存储 SDK 信息
 */
@State(
    name = "CangJieSdkRegistry",
    storages = [Storage("cangjie-sdks.xml")]
)
internal class CjSdkRegistryImpl : CjSdkRegistry, PersistentStateComponent<CjSdkRegistryState> {

    private var state = CjSdkRegistryState()
    private val sdkCache = ConcurrentHashMap<String, CjSdk>()
    private val detector: CjSdkDetector
        get() = CjSdkDetector.getInstance() ?: error("No SDK detector registered")

    init {
        // 初始化时加载所有已注册的 SDK
        reloadSdksFromState()
    }

    override fun getState(): CjSdkRegistryState = state

    override fun loadState(state: CjSdkRegistryState) {
        this.state = state
        reloadSdksFromState()
    }

    override fun getAllSdks(): List<CjSdk> {
        return sdkCache.values.toList()
    }

    override fun getSdk(id: String): CjSdk? {
        return sdkCache[id]
    }

    override fun getSdkByPath(homePath: Path): CjSdk? {
        val normalizedPath = homePath.toAbsolutePath().normalize().toString()
        return sdkCache.values.find {
            it.homePath.toAbsolutePath().normalize().toString() == normalizedPath
        }
    }

    override fun registerSdk(sdk: CjSdk): Boolean {
        if (sdkCache.containsKey(sdk.id)) {
            return false
        }

        sdkCache[sdk.id] = sdk
        state.sdkPaths[sdk.id] = sdk.homePath.toString()
        return true
    }

    override fun registerSdkPath(homePath: Path, customName: String?): CjSdk? {
        // 检查是否已注册
        val existingSdk = getSdkByPath(homePath)
        if (existingSdk != null) {
            return existingSdk
        }

        // 创建 SDK 实例
        val sdk = detector.createSdk(homePath, customName) ?: return null

        // 注册 SDK
        if (!registerSdk(sdk)) {
            return null
        }

        // 保存自定义名称
        if (customName != null) {
            state.customNames[sdk.id] = customName
        }

        return sdk
    }

    override fun unregisterSdk(id: String): Boolean {
        if (!sdkCache.containsKey(id)) {
            return false
        }

        sdkCache.remove(id)
        state.sdkPaths.remove(id)
        state.customNames.remove(id)
        return true
    }

    override fun isSdkRegistered(id: String): Boolean {
        return sdkCache.containsKey(id)
    }

    override fun isSdkPathRegistered(homePath: Path): Boolean {
        return getSdkByPath(homePath) != null
    }

    override fun refreshAllSdks() {
        reloadSdksFromState()
    }

    override fun refreshSdk(id: String): CjSdk? {
        val pathStr = state.sdkPaths[id] ?: return null
        val homePath = Paths.get(pathStr)
        val customName = state.customNames[id]

        val sdk = detector.createSdk(homePath, customName) ?: return null
        sdkCache[id] = sdk
        return sdk
    }

    /**
     * 从持久化状态重新加载所有 SDK
     */
    private fun reloadSdksFromState() {
        sdkCache.clear()

        for ((id, pathStr) in state.sdkPaths) {
            try {
                val homePath = Paths.get(pathStr)
                val customName = state.customNames[id]
                detector.createSdk(homePath, customName)?.let { sdk ->
                    sdkCache[id] = sdk
                }
            } catch (e: Exception) {
                // 忽略无效的路径
            }
        }
    }
}
