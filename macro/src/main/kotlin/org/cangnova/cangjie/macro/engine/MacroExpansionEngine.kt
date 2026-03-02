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

package org.cangnova.cangjie.macro.engine

import org.cangnova.cangjie.macro.messages.CangJieMacroBundle

/**
 * 宏展开引擎接口
 *
 * 每个宏展开引擎（Provider）实现此接口，向外描述自身的元信息。
 * 设计原则：**由来源者自行实现相关信息**，即引擎描述由引擎自身提供，
 * 而非集中定义在枚举中。
 *
 * ## 实现方式
 *
 * Provider 直接实现此接口（Provider = 引擎本身）：
 *
 * ```kotlin
 * class MyMacroProvider : MacroExpansionProvider, MacroExpansionEngine {
 *     override val engine: MacroExpansionEngine get() = this
 *     override val id = "my-engine"
 *     override val displayName = "My Engine"
 *     // ...
 * }
 * ```
 *
 * ## 内置引擎
 *
 * - [org.cangnova.cangjie.macro.compiler.CompilerFrontendEngine]
 *   — 通过 `cjc-frontend --debug-macro` 展开宏（文件级，生成 `.macrocall`）
 * - [org.cangnova.cangjie.macro.server.LspMacroServerEngine]
 *   — 通过 `LSPMacroServer` 常驻进程展开宏（管道 + FlatBuffers）
 */
interface MacroExpansionEngine {

    /**
     * 引擎唯一标识符
     *
     * 用于配置持久化、日志、注册表键等。
     * 示例：`"cjc-frontend"`、`"lsp-macro-server"`
     */
    val id: String

    /**
     * 用户可见的引擎显示名称
     *
     * 显示在 UI、日志和错误信息中。
     */
    val displayName: String

    /**
     * 引擎的简要描述
     *
     * 说明该引擎的工作方式、适用场景及局限性。
     */
    val description: String

    /**
     * 引擎使用的可执行文件名（不含路径和平台后缀）
     *
     * 示例：`"cjc-frontend"`、`"LSPMacroServer"`
     */
    val executableName: String

    /**
     * 与宏展开进程的通信协议
     */
    val protocol: EngineProtocol

    /**
     * 是否支持流式（增量）宏展开
     *
     * `true` 表示引擎作为常驻服务运行，可以处理多次请求（如 LSPMacroServer）。
     * `false` 表示每次展开都需要启动新进程（如 cjc-frontend）。
     */
    val supportsStreaming: Boolean get() = false

    /**
     * 展开前是否需要先编译宏包动态库
     *
     * 为 `true` 时，展开前需要先执行 `cjc --compile-macro`
     * 生成 `lib-macro_<pkg>.dll/.so/.dylib`。
     */
    val requiresMacroCompilation: Boolean get() = true

    /**
     * 展开粒度
     *
     * 描述该引擎能展开的最小单元。
     */
    val expansionGranularity: ExpansionGranularity get() = ExpansionGranularity.FILE
}

/**
 * 宏展开引擎通信协议
 */
enum class EngineProtocol(private val key: String) {
    /**
     * 通过文件通信
     *
     * 编译器将结果写入 `<source>.macrocall` 文本文件，IDE 读取该文件。
     * 代表引擎：`cjc-frontend --debug-macro`
     */
    FILE("macro.engine.protocol.file"),

    /**
     * 通过匿名管道 + FlatBuffers 二进制协议通信
     *
     * 引擎作为常驻进程，通过管道接收请求并返回结果（FlatBuffers 序列化）。
     * 代表引擎：`LSPMacroServer`
     */
    PIPE_FLATBUFFERS("macro.engine.protocol.pipe.flatbuffers"),

    /**
     * 通过 LSP（Language Server Protocol）通信
     *
     * 引擎集成在 LSP Server 中，通过 LSP 扩展消息展开宏。
     * 代表引擎：`LSPServer`（需要 LSP4IJ）
     */
    LSP("macro.engine.protocol.lsp");

    val displayName: String get() = CangJieMacroBundle.message(key)
}

/**
 * 宏展开粒度
 */
enum class ExpansionGranularity(private val key: String) {
    /** 文件级：一次展开整个文件中所有宏 */
    FILE("macro.engine.granularity.file"),

    /** 宏调用级：可以单独展开指定的宏调用 */
    MACRO_CALL("macro.engine.granularity.macro.call");

    val displayName: String get() = CangJieMacroBundle.message(key)
}
