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
 */

package org.cangnova.cangjie.macro.expanded

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.diagnostic.Logger
import java.io.File

/**
 * 宏展开偏移映射
 *
 * 记录展开文件中每个宏展开区域的位置信息，用于：
 * - 将展开文件的分析结果（类型、诊断）映射回原始文件
 * - 跳转时将展开区域内的导航请求重定向到原始宏调用处
 *
 * 映射数据来源于编译器输出的行号前缀（如 `3.1`）和 `Emitted by MacroCall` 标记。
 *
 * 映射数据通过 JSON 文件持久化到磁盘（位于展开文件旁，后缀 `.mapping.json`），
 * IDE 重启后可直接从磁盘恢复，无需重新处理 `.macrocall` 文件。
 *
 * @param sourceFilePath 原始源文件路径
 * @param expandedFilePath 展开后清洁文件的路径
 * @param expansionRegions 所有展开区域列表
 */
data class MacroExpansionOffsetMapping(
    @SerializedName("sourceFilePath")
    val sourceFilePath: String,
    @SerializedName("expandedFilePath")
    val expandedFilePath: String,
    @SerializedName("regions")
    val expansionRegions: List<ExpansionRegion>
) {

    /**
     * 单个宏展开区域
     *
     * @param macroName 宏名称（如 "abc"、"Derive"）
     * @param originalLine 原始文件中宏调用的行号（1-based）
     * @param originalCol 原始文件中宏调用的列号（1-based）
     * @param expandedStartLine 展开文件中该区域的起始行号（1-based, inclusive）
     * @param expandedEndLine 展开文件中该区域的结束行号（1-based, inclusive）
     */
    data class ExpansionRegion(
        @SerializedName("macroName")
        val macroName: String,
        @SerializedName("originalLine")
        val originalLine: Int,
        @SerializedName("originalCol")
        val originalCol: Int,
        @SerializedName("expandedStartLine")
        val expandedStartLine: Int,
        @SerializedName("expandedEndLine")
        val expandedEndLine: Int
    )

    /**
     * 检查展开文件中的某行是否在宏展开区域内
     *
     * @param expandedLine 展开文件中的行号（1-based）
     * @return 如果该行在展开区域内，返回对应的 [ExpansionRegion]；否则返回 null
     */
    fun findExpansionRegion(expandedLine: Int): ExpansionRegion? {
        return expansionRegions.find {
            expandedLine in it.expandedStartLine..it.expandedEndLine
        }
    }

    /**
     * 查找原始文件中某行的所有展开区域
     *
     * @param originalLine 原始文件中的行号（1-based）
     * @return 所有对应的展开区域
     */
    fun findRegionsByOriginalLine(originalLine: Int): List<ExpansionRegion> {
        return expansionRegions.filter { it.originalLine == originalLine }
    }

    companion object {
        private val log = Logger.getInstance(MacroExpansionOffsetMapping::class.java)
        private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

        private const val MAPPING_FILE_SUFFIX = ".mapping.json"

        /**
         * 根据展开文件路径计算映射文件路径
         */
        @JvmStatic
        fun mappingFilePathFor(expandedFilePath: String): String = expandedFilePath + MAPPING_FILE_SUFFIX

        /**
         * 将映射数据序列化并写入 JSON 文件
         *
         * @param mapping 要持久化的映射数据
         * @return 写入成功返回 true
         */
        @JvmStatic
        fun saveToDisk(mapping: MacroExpansionOffsetMapping): Boolean {
            val mappingFilePath = mappingFilePathFor(mapping.expandedFilePath)
            return try {
                val file = File(mappingFilePath)
                file.parentFile?.mkdirs()
                file.writeText(gson.toJson(mapping), Charsets.UTF_8)
                true
            } catch (e: Exception) {
                log.warn("写入偏移映射文件失败: $mappingFilePath", e)
                false
            }
        }

        /**
         * 从 JSON 文件加载映射数据
         *
         * @param expandedFilePath 展开文件路径（映射文件路径由此推导）
         * @return 反序列化后的映射数据，文件不存在或解析失败时返回 null
         */
        @JvmStatic
        fun loadFromDisk(expandedFilePath: String): MacroExpansionOffsetMapping? {
            val mappingFilePath = mappingFilePathFor(expandedFilePath)
            val file = File(mappingFilePath)
            if (!file.exists()) return null
            return try {
                val json = file.readText(Charsets.UTF_8)
                gson.fromJson(json, MacroExpansionOffsetMapping::class.java)
            } catch (e: Exception) {
                log.warn("读取偏移映射文件失败: $mappingFilePath", e)
                null
            }
        }

        /**
         * 删除映射文件
         *
         * @param expandedFilePath 展开文件路径
         */
        @JvmStatic
        fun deleteFromDisk(expandedFilePath: String) {
            try {
                File(mappingFilePathFor(expandedFilePath)).delete()
            } catch (e: Exception) {
                log.debug("删除偏移映射文件失败: ${mappingFilePathFor(expandedFilePath)}", e)
            }
        }
    }
}
