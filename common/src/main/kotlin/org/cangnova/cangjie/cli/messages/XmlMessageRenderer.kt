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

package org.cangnova.cangjie.cli.messages

import com.intellij.openapi.util.text.StringUtil

/**
 * XML 消息渲染器
 *
 * 将编译器消息渲染为 XML 格式。
 * 适合需要机器可读输出的场景。
 */
class XmlMessageRenderer : MessageRenderer {

    override fun renderPreamble(): String = "<MESSAGES>"

    override fun render(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?
    ): String = buildString {
        val tagName = severity.presentableName
        append("<").append(tagName)

        if (location != null) {
            append(" path=\"").append(e(location.path)).append("\"")
            append(" line=\"").append(location.line).append("\"")
            append(" column=\"").append(location.column).append("\"")
        }

        append(">")
        append(e(message))
        append("</").append(tagName).append(">\n")
    }

    override fun renderUsage(usage: String): String {
        return render(CompilerMessageSeverity.STRONG_WARNING, usage, null)
    }

    override fun renderConclusion(): String = "</MESSAGES>"

    override val name: String
        get() = "XML"

    companion object {
        /**
         * 转义 XML 实体
         */
        private fun e(str: String): String = StringUtil.escapeXmlEntities(str)
    }
}
