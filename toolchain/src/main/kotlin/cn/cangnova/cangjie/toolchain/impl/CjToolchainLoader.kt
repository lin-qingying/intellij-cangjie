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

package cn.cangnova.cangjie.toolchain.impl

import cn.cangnova.cangjie.toolchain.api.CjToolchain
import cn.cangnova.cangjie.toolchain.api.CjToolchainProvider
import java.nio.file.Path
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap

/**
 * CangJie工具链加载器
 *
 * 用于发现和加载系统中可用的工具链
 */
object CjToolchainLoader {
    
    private val providers = ConcurrentHashMap<String, CjToolchainProvider>()
    private val toolchains = ConcurrentHashMap<String, CjToolchain>()
    
    init {
        loadProviders()
    }
    
    /**
     * 获取所有注册的工具链提供者
     *
     * @return 工具链提供者列表
     */
    fun getProviders(): List<CjToolchainProvider> {
        return providers.values.toList()
    }
    
    /**
     * 根据ID获取工具链提供者
     *
     * @param id 提供者ID
     * @return 工具链提供者，如果不存在则返回null
     */
    fun getProvider(id: String): CjToolchainProvider? {
        return providers[id]
    }
    
    /**
     * 获取所有可用的工具链
     *
     * @return 工具链列表
     */
    fun getToolchains(): List<CjToolchain> {
        if (toolchains.isEmpty()) {
            detectToolchains()
        }
        return toolchains.values.toList()
    }
    
    /**
     * 根据路径创建工具链
     *
     * @param homePath 工具链主目录
     * @return 工具链实例，如果路径无效则返回null
     */
    fun createToolchain(homePath: Path): CjToolchain? {
        for (provider in providers.values) {
            val toolchain = provider.createToolchain(homePath)
            if (toolchain != null) {
                return toolchain
            }
        }
        return null
    }
    
    /**
     * 重新检测系统中可用的工具链
     *
     * @return 检测到的工具链列表
     */
    fun detectToolchains(): List<CjToolchain> {
        toolchains.clear()
        
        for (provider in providers.values) {
            val detected = provider.detectToolchains()
            for (toolchain in detected) {
                val key = "${provider.id}:${toolchain.homePath}"
                toolchains[key] = toolchain
            }
        }
        
        return toolchains.values.toList()
    }
    
    /**
     * 注册工具链提供者
     *
     * @param provider 工具链提供者
     */
    fun registerProvider(provider: CjToolchainProvider) {
        providers[provider.id] = provider
    }
    
    /**
     * 加载所有注册的工具链提供者
     */
    private fun loadProviders() {
        val serviceLoader = ServiceLoader.load(CjToolchainProvider::class.java)
        for (provider in serviceLoader) {
            providers[provider.id] = provider
        }
        
        // 确保至少有官方提供者
        if (!providers.containsKey("official")) {
            registerProvider(OfficialCjToolchainProvider())
        }
    }
} 