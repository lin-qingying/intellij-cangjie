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

package org.cangnova.cangjie.cli.messages

import com.intellij.openapi.util.io.FileUtil
import org.cangnova.cangjie.utils.descendantRelativeTo
import java.io.File

/**
 * 消息渲染器接口
 *
 * 定义了如何渲染编译器消息的接口。
 * 提供了多种预定义的渲染器实现。
 */
interface MessageRenderer {

    /**
     * 渲染序言（在所有消息之前）
     */
    fun renderPreamble(): String

    /**
     * 渲染单条消息
     *
     * @param severity 消息严重级别
     * @param message 消息内容
     * @param location 消息位置（可选）
     * @return 渲染后的消息字符串
     */
    fun render(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?
    ): String

    /**
     * 渲染使用说明
     */
    fun renderUsage(usage: String): String

    /**
     * 渲染结论（在所有消息之后）
     */
    fun renderConclusion(): String

    /**
     * 渲染器名称
     */
    val name: String

    companion object {
        /**
         * 属性键，用于系统属性配置
         */
        const val PROPERTY_KEY = "org.cangnova.cangjie.cliMessageRenderer"

        /**
         * XML 格式渲染器
         */

        val XML: MessageRenderer = XmlMessageRenderer()

        /**
         * 无路径渲染器
         *
         * 不显示文件路径的纯文本渲染器。
         */

        val WITHOUT_PATHS: MessageRenderer = object : PlainTextMessageRenderer() {
            override fun getPath(location: CompilerMessageSourceLocation): String? = null

            override val name: String
                get() = "Pathless"
        }

        /**
         * 完整路径渲染器
         *
         * 显示完整绝对路径的纯文本渲染器。
         */

        val PLAIN_FULL_PATHS: MessageRenderer = object : PlainTextMessageRenderer() {
            override fun getPath(location: CompilerMessageSourceLocation): String =
                location.path

            override val name: String
                get() = "FullPath"
        }

        /**
         * 相对路径渲染器
         *
         * 显示相对于当前工作目录的路径。
         */

        val PLAIN_RELATIVE_PATHS: MessageRenderer = object : PlainTextMessageRenderer() {
            private val cwd = File(".").absoluteFile

            override fun getPath(location: CompilerMessageSourceLocation): String =
                File(location.path).descendantRelativeTo(cwd).path

            override val name: String
                get() = "RelativePath"
        }

        /**
         * 系统无关相对路径渲染器
         *
         * 显示相对路径，并转换为系统无关格式（使用正斜杠）。
         */

        val SYSTEM_INDEPENDENT_RELATIVE_PATHS: MessageRenderer = object : PlainTextMessageRenderer() {
            private val cwd = File(".").absoluteFile

            override fun getPath(location: CompilerMessageSourceLocation): String =
                FileUtil.toSystemIndependentName(
                    File(location.path).descendantRelativeTo(cwd).path
                )

            override val name: String
                get() = "SystemIndependentRelativePath"
        }

        /**
         * Gradle 风格渲染器
         */

        val GRADLE_STYLE: MessageRenderer = GradleStyleMessageRenderer()

        /**
         * Xcode 风格渲染器
         */

        val XCODE_STYLE: MessageRenderer = XcodeStyleMessageRenderer()
    }
}
