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
    @JsonProperty("compile-option")
    val compileOption: String? = null,

    /** 额外全局编译命令选项 */
    @JsonProperty("override-compile-option")
    val overrideCompileOption: String? = null,

    /** 链接器透传选项 */
    @JsonProperty("link-option")
    val linkOption: String? = null,

    /** 源码依赖配置项 */
    val dependencies: Map<String, DependencyConfig>? = null,

    /** 测试阶段依赖配置项 */
    @JsonProperty("test-dependencies")
    val testDependencies: Map<String, DependencyConfig>? = null,

    /** 仓颉二进制库依赖 */
    @JsonProperty("bin-dependencies")
    val binDependencies: BinDependenciesConfig? = null,

    /** 交叉编译时的宏包控制项 */
    @JsonProperty("compile-macros-for-target")
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
    @JsonProperty("path-option")
    val pathOption: List<String>? = null,

    /** 包选项配置，key为包名，value为路径 */
    @JsonProperty("package-option")
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