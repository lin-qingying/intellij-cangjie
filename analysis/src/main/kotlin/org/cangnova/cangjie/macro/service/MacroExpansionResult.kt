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

package org.cangnova.cangjie.macro.service

/**
 * 宏展开结果
 *
 * 表示单个宏表达式的展开结果，包含展开后的代码文本、Token 列表和诊断信息
 */
data class MacroExpansionResult(
    /**
     * 源文件路径
     */
    val filePath: String,

    /**
     * 宏表达式在源文件中的起始偏移量
     */
    val startOffset: Int,

    /**
     * 宏表达式在源文件中的结束偏移量
     */
    val endOffset: Int,

    /**
     * 展开后的代码文本
     */
    val expandedText: String,

    /**
     * 展开后的 Token 列表
     */
    val tokens: List<MacroToken> = emptyList(),

    /**
     * 展开过程中的诊断信息
     */
    val diagnostics: List<MacroDiagnostic> = emptyList(),

    /**
     * 展开结果的来源
     */
    val source: ExpansionSource
)

/**
 * 宏 Token
 *
 * 表示宏展开后的单个 Token
 */
data class MacroToken(
    /**
     * Token 类型
     */
    val type: MacroTokenType,

    /**
     * Token 文本
     */
    val text: String,

    /**
     * Token 在展开后代码中的偏移量
     */
    val offset: Int,

    /**
     * Token 长度
     */
    val length: Int
)

/**
 * 宏 Token 类型
 */
enum class MacroTokenType {
    /** 标识符 */
    IDENTIFIER,
    /** 关键字 */
    KEYWORD,
    /** 字面量 */
    LITERAL,
    /** 运算符 */
    OPERATOR,
    /** 分隔符 */
    DELIMITER,
    /** 注释 */
    COMMENT,
    /** 空白 */
    WHITESPACE,
    /** 其他 */
    OTHER
}

/**
 * 宏展开诊断信息
 */
data class MacroDiagnostic(
    /**
     * 诊断级别
     */
    val level: DiagnosticLevel,

    /**
     * 诊断消息
     */
    val message: String,

    /**
     * 相关位置的偏移量（可选）
     */
    val offset: Int? = null,

    /**
     * 相关位置的长度（可选）
     */
    val length: Int? = null
)

/**
 * 诊断级别
 */
enum class DiagnosticLevel {
    /** 错误 */
    ERROR,
    /** 警告 */
    WARNING,
    /** 信息 */
    INFO
}

/**
 * 展开结果来源
 */
enum class ExpansionSource {
    /** 通过编译器展开 */
    COMPILER,
    /** 来自缓存 */
    CACHE
}

/**
 * 宏展开选项
 */
data class MacroExpansionOptions(
    /**
     * 是否递归展开嵌套宏
     */
    val recursive: Boolean = true,

    /**
     * 是否使用缓存
     */
    val useCache: Boolean = true,

    /**
     * 超时时间（毫秒），0 表示无超时
     */
    val timeoutMs: Long = 10000L,

    /**
     * 是否在展开前自动编译宏声明
     *
     * 如果启用，在执行 `--debug-macro` 之前会先执行 `--compile-macro` 编译宏声明
     */
    val autoCompileMacros: Boolean = true,

    /**
     * 是否强制重新编译宏（忽略缓存）
     *
     * 仅在 autoCompileMacros 为 true 时有效
     */
    val forceRecompile: Boolean = false
) {
    companion object {
        /**
         * 默认选项（启用自动编译）
         */
        val DEFAULT = MacroExpansionOptions()

        /**
         * 不自动编译的选项
         */
        val NO_AUTO_COMPILE = MacroExpansionOptions(autoCompileMacros = false)
    }
}
