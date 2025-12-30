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

package org.cangnova.telemetry.sender

import org.cangnova.telemetry.TelemetrySettings
import org.cangnova.telemetry.api.TelemetryEvent
import org.cangnova.telemetry.data.TelemetryDataCollector
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * URL扩展函数，用于使用协程获取URL内容
 */
private suspend fun URL.fetchContent(): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
        (this@fetchContent.openConnection() as HttpURLConnection).run {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000

            val responseCode = responseCode
            if (responseCode in 200..299) {
                inputStream.bufferedReader().use { it.readText() }
            } else {
                throw Exception("HTTP error: $responseCode")
            }
        }
    }
}

/**
 * 遥测数据发送器，负责将收集到的遥测数据发送到服务器
 */
@Service
class TelemetryDataSender {
    private val logger = Logger.getInstance(TelemetryDataSender::class.java)
    private val settings = TelemetrySettings.getInstance()
    private val dataCollector = TelemetryDataCollector.getInstance()
    private val gson = createGson()
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        val thread = Thread(r, "Telemetry-Sender")
        thread.isDaemon = true
        thread
    }

    // 协程作用域
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 遥测服务器URL和API前缀
    private var TELEMETRY_SERVER_URL = ""
    private val PREFIX = "/api/telemetry"

    // 远程配置URL
    private val CONFIG_URL = "https://gitee.com/Lin_Qing_Ying/intellij-cangjie-index/raw/master/telemetry/index.json"

    // 隐私政策URL
    private var privacyPolicyUrl = ""

    // 系统唯一标识符
    private val systemId = generateSystemId()

    /**
     * 创建Gson实例，用于序列化JSON
     */
    private fun createGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
            .create()
    }

    /**
     * 初始化发送器，开始定期发送遥测数据
     */
    fun initialize() {
        if (!settings.telemetryEnabled) {
            logger.info("Telemetry is disabled, not initializing sender")
            return
        }

        // 尝试从远程获取配置
        fetchRemoteConfig()

        // 每5分钟发送一次数据
        scheduler.scheduleWithFixedDelay(
            { sendDataToServer() },
            5,
            5,
            TimeUnit.MINUTES
        )

        logger.info("Telemetry sender initialized")
    }

    /**
     * 从远程获取遥测服务器配置，使用协程异步执行
     */
    private fun fetchRemoteConfig() {
        // 使用协程在后台线程中执行网络请求
        coroutineScope.launch {
            try {
                logger.info("Fetching telemetry configuration from $CONFIG_URL")

                // 使用扩展函数获取配置
                val result = URL(CONFIG_URL).fetchContent()

                result.fold(
                    onSuccess = { configJson ->
                        if (configJson.isNotEmpty()) {
                            parseConfig(configJson)
                        }
                    },
                    onFailure = { error ->
                        logger.warn("Failed to fetch telemetry configuration", error)
                    }
                )
            } catch (e: Exception) {
                logger.warn("Error fetching telemetry configuration", e)
            }
        }
    }

    /**
     * 解析配置JSON
     */
    private fun parseConfig(jsonStr: String) {
        try {
            val jsonObject = JsonParser.parseString(jsonStr).asJsonObject

            // 获取遥测服务器URL
            if (jsonObject.has("telemetry-server")) {
                val serverUrl = jsonObject.get("telemetry-server").asString
                if (serverUrl.isNotEmpty()) {
                    TELEMETRY_SERVER_URL = serverUrl
                    logger.info("Updated telemetry server URL to: $TELEMETRY_SERVER_URL")
                }
            }

            // 获取隐私政策URL
            if (jsonObject.has("privacy-policy")) {
                val policyUrl = jsonObject.get("privacy-policy").asString
                if (policyUrl.isNotEmpty()) {
                    privacyPolicyUrl = policyUrl
                    logger.info("Updated privacy policy URL to: $privacyPolicyUrl")
                }
            }
        } catch (e: Exception) {
            logger.warn("Error parsing telemetry configuration", e)
        }
    }

    /**
     * 关闭发送器
     */
    fun shutdown() {
        scheduler.shutdown()
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow()
            }
        } catch (e: InterruptedException) {
            scheduler.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    /**
     * 获取完整的遥测服务器URL
     */
    private fun getTeleMetryServerUrl(): String {
        return TELEMETRY_SERVER_URL + PREFIX
    }

    /**
     * 获取隐私政策URL
     */
    fun getPrivacyPolicyUrl(): String {
        return privacyPolicyUrl
    }

    /**
     * 使用协程异步发送HTTP POST请求
     */
    private suspend fun sendPostRequestAsync(url: String, jsonPayload: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                (URL(url).openConnection() as HttpURLConnection).run {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000

                    outputStream.use { os ->
                        os.write(jsonPayload.toByteArray())
                        os.flush()
                    }

                    val responseCode = responseCode
                    if (responseCode in 200..299) {
                        inputStream.bufferedReader().use { it.readText() }
                    } else {
                        val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: "No error details"
                        throw Exception("HTTP error $responseCode: $errorResponse")
                    }
                }
            }
        }

    /**
     * 立即发送数据到服务器
     * @return 如果发送成功，则返回true；否则返回false
     */
    fun sendDataToServer(): Boolean {
        if (!settings.telemetryEnabled) {
            return false
        }

        val events = dataCollector.getAndClearEvents()
        if (events.isEmpty()) {
            logger.debug("No telemetry data to send")
            return true
        }

        // 如果服务器URL为空，无法发送数据
        if (TELEMETRY_SERVER_URL.isEmpty()) {
            logger.debug("Telemetry server URL is not configured, cannot send data")
            return false
        }

        // 创建有效载荷
        val payload = createPayload(events)
        val jsonPayload = gson.toJson(payload)

        // 记录发送的数据，用于调试
        logger.debug("Sending telemetry payload to ${getTeleMetryServerUrl()}: $jsonPayload")

        return try {
            val connection = URL(getTeleMetryServerUrl()).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true

            connection.outputStream.use { os ->
                os.write(jsonPayload.toByteArray())
                os.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                logger.info("Successfully sent ${events.size} telemetry events to ${getTeleMetryServerUrl()}")
                true
            } else {
                // 读取错误响应，用于调试
                val errorResponse =
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error details"
                logger.warn("Failed to send telemetry data to ${getTeleMetryServerUrl()}, server returned code: $responseCode, response: $errorResponse")
                false
            }
        } catch (e: Exception) {
            logger.warn("Error sending telemetry data to ${getTeleMetryServerUrl()}", e)
            false
        }
    }

    /**
     * 生成系统唯一标识符
     */
    private fun generateSystemId(): String {
        val osName = System.getProperty("os.name") ?: ""
        val osVersion = System.getProperty("os.version") ?: ""
        val userName = System.getProperty("user.name") ?: ""
        val computerName = System.getProperty("COMPUTERNAME") ?: System.getProperty("HOSTNAME") ?: ""

        // 组合系统信息并生成哈希值作为唯一标识符
        val systemInfo = "$osName-$osVersion-$userName-$computerName-${System.currentTimeMillis()}"
        return UUID.nameUUIDFromBytes(systemInfo.toByteArray()).toString()
    }

    /**
     * 创建要发送的有效载荷
     * @param events 要发送的事件
     * @return 包含元数据和事件的有效载荷
     */
    private fun createPayload(events: List<TelemetryEvent>): Map<String, Any> {
        val appInfo = ApplicationInfo.getInstance()

        return mapOf(
            "metadata" to mapOf(
                "systemId" to systemId,
                "pluginVersion" to PluginManagerCore.getPlugin(PluginId.getId("cn.cangnova.cangjie"))?.version,
                "ideVersion" to appInfo.fullVersion,
                "ideBuild" to appInfo.build.asString(),
                "os" to System.getProperty("os.name"),
                "osVersion" to System.getProperty("os.version"),
                "javaVersion" to System.getProperty("java.version"),
                "timestamp" to System.currentTimeMillis()
            ),
            "events" to events
        )
    }

    /**
     * 获取当前等待发送的事件数量
     * @return 等待发送的事件数量
     */
    fun getPendingEventsCount(): Int {
        return dataCollector.getPendingEventsCount()
    }

    /**
     * Instant类型适配器，用于将Instant转换为ISO-8601格式的字符串
     */
    private class InstantTypeAdapter : com.google.gson.JsonSerializer<Instant> {
        override fun serialize(
            src: Instant,
            typeOfSrc: java.lang.reflect.Type,
            context: com.google.gson.JsonSerializationContext
        ): com.google.gson.JsonElement {
            return com.google.gson.JsonPrimitive(src.toString())
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(): TelemetryDataSender = service()
    }
} 