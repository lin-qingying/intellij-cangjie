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

package org.cangnova.cangjie.cjpm.config.toml

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.cjpm.project.model.toml.CjpmTomlConfig
import java.io.File
import java.io.InputStream
import java.nio.file.Path

val TOML_MAPPER = ObjectMapper(TomlFactory()).apply {
    registerKotlinModule()
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

/**
 * CJPM TOML 配置文件解析器
 */
object CjpmTomlParser {
    /**
     * 从文件解析 TOML 配置
     *
     * @param file TOML 配置文件
     * @return CJPM TOML 配置对象
     * @throws com.fasterxml.jackson.core.JsonProcessingException 当解析失败时抛出
     */
    fun parse(file: File): CjpmTomlConfig {
        return TOML_MAPPER.readValue(file, CjpmTomlConfig::class.java)
    }

    /**
     * 从路径解析 TOML 配置
     *
     * @param path TOML 配置文件路径
     * @return CJPM TOML 配置对象
     * @throws com.fasterxml.jackson.core.JsonProcessingException 当解析失败时抛出
     */
    fun parse(path: Path): CjpmTomlConfig {
        return parse(path.toFile())
    }

    /**
     * 从字符串解析 TOML 配置
     *
     * @param content TOML 配置内容
     * @return CJPM TOML 配置对象
     * @throws com.fasterxml.jackson.core.JsonProcessingException 当解析失败时抛出
     */
    fun parse(content: String): CjpmTomlConfig {
        return TOML_MAPPER.readValue(content, CjpmTomlConfig::class.java)
    }

    /**
     * 从输入流解析 TOML 配置
     *
     * @param input TOML 配置输入流
     * @return CJPM TOML 配置对象
     * @throws com.fasterxml.jackson.core.JsonProcessingException 当解析失败时抛出
     * @throws java.io.IOException 当读取输入流失败时抛出
     */
    fun parse(input: InputStream): CjpmTomlConfig {
        return TOML_MAPPER.readValue(input, CjpmTomlConfig::class.java)
    }

    /**
     * 从 VirtualFile 解析 TOML 配置
     *
     * @param file TOML 配置文件
     * @return CJPM TOML 配置对象，解析失败时返回 null
     */
    fun parse(file: VirtualFile): CjpmTomlConfig? {
        return try {
            val content = String(file.contentsToByteArray(), Charsets.UTF_8)
            parse(content)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 将 TOML 配置对象序列化为字符串
     *
     * @param config CJPM TOML 配置对象
     * @return TOML 格式的字符串
     * @throws com.fasterxml.jackson.core.JsonProcessingException 当序列化失败时抛出
     */
    fun writeToString(config: CjpmTomlConfig): String {
        return TOML_MAPPER.writeValueAsString(config)
    }

    /**
     * 将 TOML 配置对象写入文件
     *
     * @param config CJPM TOML 配置对象
     * @param file 目标文件
     * @throws com.fasterxml.jackson.core.JsonProcessingException 当序列化失败时抛出
     * @throws java.io.IOException 当写入文件失败时抛出
     */
    fun writeToFile(config: CjpmTomlConfig, file: File) {
        TOML_MAPPER.writeValue(file, config)
    }
} 