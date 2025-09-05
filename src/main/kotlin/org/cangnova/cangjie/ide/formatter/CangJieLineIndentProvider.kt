/*
 * Copyright 2024 LinQingYing. and contributors.
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



package org.cangnova.cangjie.ide.formatter

import org.cangnova.cangjie.ide.formatter.lineIndent.CangJieIndentationAdjuster
import org.cangnova.cangjie.ide.formatter.lineIndent.CangJieLangLineIndentProvider
import com.intellij.application.options.CodeStyle
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly


class CangJieLineIndentProvider : CangJieLangLineIndentProvider() {
    override fun getLineIndent(project: Project, editor: Editor, language: Language?, offset: Int): String? =
        if (useFormatter)
            null
        else
            super.getLineIndent(project, editor, language, offset)

    override fun indentionSettings(editor: Editor): CangJieIndentationAdjuster = object : CangJieIndentationAdjuster {
        val settings = CodeStyle.getSettings(editor)
        val commonSettings: CangJieCommonCodeStyleSettings get() = settings.cangjieCommonSettings
        val customSettings: CangJieCodeStyleSettings get() = settings.cangjieCustomSettings

        override val alignWhenMultilineFunctionParentheses: Boolean
            get() = commonSettings.ALIGN_MULTILINE_METHOD_BRACKETS

        override val alignWhenMultilineBinaryExpression: Boolean
            get() = commonSettings.ALIGN_MULTILINE_BINARY_OPERATION

        override val continuationIndentInElvis: Boolean
            get() = customSettings.CONTINUATION_INDENT_IN_ELVIS

        override val continuationIndentForExpressionBodies: Boolean
            get() = customSettings.CONTINUATION_INDENT_FOR_EXPRESSION_BODIES

        override val alignMultilineParameters: Boolean
            get() = commonSettings.ALIGN_MULTILINE_PARAMETERS

        override val alignMultilineParametersInCalls: Boolean
            get() = commonSettings.ALIGN_MULTILINE_PARAMETERS_IN_CALLS

        override val continuationIndentInArgumentLists: Boolean
            get() = customSettings.CONTINUATION_INDENT_IN_ARGUMENT_LISTS

        override val continuationIndentInParameterLists: Boolean
            get() = customSettings.CONTINUATION_INDENT_IN_PARAMETER_LISTS

        override val continuationIndentInIfCondition: Boolean
            get() = customSettings.CONTINUATION_INDENT_IN_IF_CONDITIONS

        override val continuationIndentForChainedCalls: Boolean
            get() = customSettings.CONTINUATION_INDENT_FOR_CHAINED_CALLS
    }

    companion object {
        @set:TestOnly
        var useFormatter: Boolean = false
    }
}
