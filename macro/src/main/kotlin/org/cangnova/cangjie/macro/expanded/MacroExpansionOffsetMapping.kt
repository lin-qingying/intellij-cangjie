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
    val expansionRegions: List<ExpansionRegion>,
    @SerializedName("lineMappings")
    val lineMappings: List<LineMapping> = emptyList()
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
     * 行映射条目
     *
     * 记录展开文件与原始源文件之间的逐行映射关系。
     *
     * @param originalLine 源文件行号（1-based）
     * @param expandedLine 展开文件行号（1-based）
     * @param isMacroExpansion 是否属于宏展开区域
     */
    data class LineMapping(
        @SerializedName("originalLine")
        val originalLine: Int,
        @SerializedName("expandedLine")
        val expandedLine: Int,
        @SerializedName("isMacroExpansion")
        val isMacroExpansion: Boolean
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

    /**
     * 展开文件行号 → 源文件行号
     *
     * @param expandedLine 展开文件中的行号（1-based）
     * @return 对应的源文件行号，无映射时返回 null
     */
    fun expandedLineToOriginalLine(expandedLine: Int): Int? {
        return lineMappings.find { it.expandedLine == expandedLine }?.originalLine
    }

    /**
     * 源文件行号 → 展开文件行号
     *
     * 对于非宏区域，返回对应的展开行号。
     * 对于宏调用行，返回展开区域的起始行号。
     *
     * @param originalLine 源文件中的行号（1-based）
     * @return 对应的展开文件行号，无映射时返回 null
     */
    fun originalLineToExpandedLine(originalLine: Int): Int? {
        return lineMappings.find { it.originalLine == originalLine }?.expandedLine
    }

    /**
     * 展开文件文本偏移 → 源文件文本偏移
     *
     * 基于行映射和列保持不变的假设，将展开文件中的字符偏移映射到源文件中。
     * 对于宏展开区域内的偏移，映射到宏调用所在行的起始位置。
     *
     * @param expandedOffset 展开文件中的字符偏移（0-based）
     * @param expandedFileText 展开文件的完整文本
     * @param originalFileText 源文件的完整文本
     * @return 映射后的源文件偏移，无法映射时返回 null
     */
    fun mapExpandedOffsetToOriginal(
        expandedOffset: Int,
        expandedFileText: String,
        originalFileText: String
    ): Int? {
        if (lineMappings.isEmpty()) return null

        // 计算展开文件中的行号和列号
        val expandedLine = expandedFileText.lineNumberAtOffset(expandedOffset)
        val expandedLineStart = expandedFileText.lineStartOffset(expandedLine)
        val expandedCol = expandedOffset - expandedLineStart

        // 查找对应的源文件行号
        val originalLine = expandedLineToOriginalLine(expandedLine) ?: return null
        val originalLineStart = originalFileText.lineStartOffset(originalLine)

        // 检查是否在宏展开区域内
        val region = findExpansionRegion(expandedLine)
        return if (region != null) {
            // 宏展开区域：映射到宏调用的起始列（originalCol）
            val macroCallCol = region.originalCol - 1  // 1-based → 0-based
            (originalLineStart + macroCallCol).coerceAtMost(originalFileText.length)
        } else {
            // 非宏区域：列保持不变
            (originalLineStart + expandedCol).coerceAtMost(originalFileText.length)
        }
    }

    /**
     * 展开文件文本范围 → 源文件文本范围
     *
     * 将展开文件中的文本范围映射到源文件中。
     *
     * @param expandedStartOffset 展开文件中的起始偏移（0-based）
     * @param expandedEndOffset 展开文件中的结束偏移（0-based）
     * @param expandedFileText 展开文件的完整文本
     * @param originalFileText 源文件的完整文本
     * @return 映射后的源文件 Pair(startOffset, endOffset)，无法映射时返回 null
     */
    fun mapExpandedRangeToOriginal(
        expandedStartOffset: Int,
        expandedEndOffset: Int,
        expandedFileText: String,
        originalFileText: String
    ): Pair<Int, Int>? {
        val expandedStartLine = expandedFileText.lineNumberAtOffset(expandedStartOffset)
        val region = findExpansionRegion(expandedStartLine)

        if (region != null) {
            // 在宏展开区域内：整个范围映射到宏调用处
            val originalLine = region.originalLine
            val originalLineStart = originalFileText.lineStartOffset(originalLine)
            val macroCallCol = region.originalCol - 1  // 1-based → 0-based
            val macroStart = (originalLineStart + macroCallCol).coerceAtMost(originalFileText.length)
            // 映射到宏调用行末尾
            val originalLineEnd = originalFileText.lineEndOffset(originalLine)
            return Pair(macroStart, originalLineEnd.coerceAtMost(originalFileText.length))
        }

        // 非宏区域：分别映射起止偏移
        val mappedStart = mapExpandedOffsetToOriginal(expandedStartOffset, expandedFileText, originalFileText)
            ?: return null
        val mappedEnd = mapExpandedOffsetToOriginal(expandedEndOffset, expandedFileText, originalFileText)
            ?: return null
        return Pair(mappedStart, mappedEnd)
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

/**
 * 计算文本偏移对应的行号（1-based）
 */
private fun String.lineNumberAtOffset(offset: Int): Int {
    var line = 1
    for (i in 0 until offset.coerceAtMost(length)) {
        if (this[i] == '\n') line++
    }
    return line
}

/**
 * 计算某行的起始偏移（0-based）
 *
 * @param line 行号（1-based）
 */
private fun String.lineStartOffset(line: Int): Int {
    if (line <= 1) return 0
    var currentLine = 1
    for (i in indices) {
        if (this[i] == '\n') {
            currentLine++
            if (currentLine == line) return i + 1
        }
    }
    return length
}

/**
 * 计算某行的结束偏移（0-based, exclusive of newline）
 *
 * @param line 行号（1-based）
 */
private fun String.lineEndOffset(line: Int): Int {
    var currentLine = 1
    for (i in indices) {
        if (this[i] == '\n') {
            if (currentLine == line) return i
            currentLine++
        }
    }
    return length
}
