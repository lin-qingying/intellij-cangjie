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

package org.cangnova.cangjie.debugger.toolchain

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.HttpRequests
import java.io.IOException

/**
 * 调试器索引获取工具
 *
 * 从远程 JSON 文件获取调试器下载信息
 */
object DebuggerIndexFetcher {

    private val LOG = Logger.getInstance(DebuggerIndexFetcher::class.java)

    /**
     * 索引 JSON URL
     */
    private const val INDEX_URL =
        "https://gitee.com/Lin_Qing_Ying/intellij-cangjie-index/raw/master/debugger/index.json"

    /**
     * 缓存的索引数据
     */
    @Volatile
    private var cachedIndex: Map<String, DebuggerFileInfo>? = null

    /**
     * 缓存时间戳
     */
    @Volatile
    private var cacheTimestamp: Long = 0

    /**
     * 缓存有效期（5分钟）
     */
    private const val CACHE_DURATION_MS = 5 * 60 * 1000L

    /**
     * 调试器文件信息
     */
    data class DebuggerFileInfo(
        val version: String,
        val download: String,
        val sha1: String
    )

    /**
     * 获取调试器文件信息
     *
     * @param fileName 文件名
     * @return 文件信息，如果不存在则返回 null
     */
    fun getFileInfo(fileName: String): DebuggerFileInfo? {
        return try {
            val index = getIndex()
            index[fileName]
        } catch (e: Exception) {
            LOG.error("Failed to get file info for: $fileName", e)
            null
        }
    }

    /**
     * 获取索引数据（带缓存）
     */
    private fun getIndex(): Map<String, DebuggerFileInfo> {
        val now = System.currentTimeMillis()

        // 检查缓存是否有效
        cachedIndex?.let { cache ->
            if (now - cacheTimestamp < CACHE_DURATION_MS) {
                return cache
            }
        }

        // 缓存失效或不存在，重新获取
        synchronized(this) {
            // 双重检查
            cachedIndex?.let { cache ->
                if (now - cacheTimestamp < CACHE_DURATION_MS) {
                    return cache
                }
            }

            // 从远程获取
            val index = fetchIndexFromRemote()
            cachedIndex = index
            cacheTimestamp = now
            return index
        }
    }

    /**
     * 从远程获取索引数据
     */
    private fun fetchIndexFromRemote(): Map<String, DebuggerFileInfo> {
        LOG.info("Fetching debugger index from: $INDEX_URL")

        try {
            val jsonContent = HttpRequests.request(INDEX_URL)
                .productNameAsUserAgent()
                .readString()

            return parseIndex(jsonContent)
        } catch (e: IOException) {
            LOG.error("Failed to fetch debugger index", e)
            throw e
        }
    }

    /**
     * 解析索引 JSON
     */
    private fun parseIndex(jsonContent: String): Map<String, DebuggerFileInfo> {
        val gson = Gson()
        val jsonObject = gson.fromJson(jsonContent, JsonObject::class.java)

        val result = mutableMapOf<String, DebuggerFileInfo>()

        for ((fileName, element) in jsonObject.entrySet()) {
            if (element.isJsonObject) {
                val obj = element.asJsonObject
                val version = obj.get("version")?.asString ?: continue
                val download = obj.get("download")?.asString ?: continue
                val sha1 = obj.get("sha1")?.asString ?: continue

                result[fileName] = DebuggerFileInfo(version, download, sha1)
            }
        }

        LOG.info("Parsed ${result.size} debugger files from index")
        return result
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        synchronized(this) {
            cachedIndex = null
            cacheTimestamp = 0
        }
    }
}