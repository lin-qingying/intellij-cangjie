package org.cangnova.cangjie.buildsystem.impl.cjpm.project.model.toml

import org.cangnova.cangjie.toolchain.tools.Cjpm
import java.io.File
import java.io.InputStream
import java.nio.file.Path

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
        return Cjpm.TOML_MAPPER.readValue(file, CjpmTomlConfig::class.java)
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
        return Cjpm.TOML_MAPPER.readValue(content, CjpmTomlConfig::class.java)
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
        return Cjpm.TOML_MAPPER.readValue(input, CjpmTomlConfig::class.java)
    }

    /**
     * 将 TOML 配置对象序列化为字符串
     *
     * @param config CJPM TOML 配置对象
     * @return TOML 格式的字符串
     * @throws com.fasterxml.jackson.core.JsonProcessingException 当序列化失败时抛出
     */
    fun writeToString(config: CjpmTomlConfig): String {
        return Cjpm.TOML_MAPPER.writeValueAsString(config)
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
        Cjpm.TOML_MAPPER.writeValue(file, config)
    }
} 