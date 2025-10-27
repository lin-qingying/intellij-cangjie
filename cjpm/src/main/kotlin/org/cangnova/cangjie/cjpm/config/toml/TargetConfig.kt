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

package org.cangnova.cangjie.cjpm.project.model.toml

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * 多后端、多平台隔离配置类
 */
@Serializable
data class TargetConfig(
    /** 额外编译命令选项 */
    @field:JsonProperty("compile-option")
    val compileOption: String? = null,

    /** 额外全局编译命令选项 */
    @field:JsonProperty("override-compile-option")
    val overrideCompileOption: String? = null,

    /** 链接器透传选项 */
    @field:JsonProperty("link-option")
    val linkOption: String? = null,

    /** 源码依赖配置项 */
    val dependencies: Map<String, DependencyConfig>? = null,

    /** 测试阶段依赖配置项 */
    @field:JsonProperty("test-dependencies")
    val testDependencies: Map<String, DependencyConfig>? = null,

    /** 仓颉二进制库依赖 */
    @field:JsonProperty("bin-dependencies")
    val binDependencies: BinDependenciesConfig? = null,

    /** 交叉编译时的宏包控制项 */
    @field:JsonProperty("compile-macros-for-target")
    @Serializable(with = CompileMacrosSerializer::class)
    val compileMacrosForTarget: CompileMacros? = null,

    /** debug 模式下的特定配置 */
    val debug: TargetConfig? = null,

    /** release 模式下的特定配置 */
    val release: TargetConfig? = null
)

/**
 * 二进制依赖配置
 */
@Serializable
data class BinDependenciesConfig(
    /** 二进制依赖路径列表 */
    @field:JsonProperty("path-option")
    val pathOption: List<String>? = null,

    /** 包选项配置，key为包名，value为路径 */
    @field:JsonProperty("package-option")
    val packageOption: Map<String, String>? = null
)

/**
 * 编译宏配置，可以是单个字符串或字符串列表
 */
@Serializable(with = CompileMacrosSerializer::class)
sealed class CompileMacros {
    data class Single(val value: String) : CompileMacros()
    data class Multiple(val values: List<String>) : CompileMacros()
}

/**
 * 编译宏序列化器
 */
object CompileMacrosSerializer : KSerializer<CompileMacros> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CompileMacros")

    override fun serialize(encoder: Encoder, value: CompileMacros) {
        val jsonEncoder = encoder as? JsonEncoder ?: throw IllegalStateException("Expected JSON encoder")
        when (value) {
            is CompileMacros.Single -> jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))
            is CompileMacros.Multiple -> jsonEncoder.encodeJsonElement(JsonArray(value.values.map { JsonPrimitive(it) }))
        }
    }

    override fun deserialize(decoder: Decoder): CompileMacros {
        val jsonDecoder = decoder as? JsonDecoder ?: throw IllegalStateException("Expected JSON decoder")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> CompileMacros.Single(element.content)
            is JsonArray -> CompileMacros.Multiple(element.map { (it as JsonPrimitive).content })
            else -> throw IllegalStateException("Unexpected JSON element type: ${element::class}")
        }
    }
} 