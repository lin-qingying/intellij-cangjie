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



package org.cangnova.cangjie.formatter

import org.cangnova.cangjie.lang.CangJieLanguage
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings


class CangJieObsoleteCodeStyle : CangJiePredefinedCodeStyle(CODE_STYLE_TITLE, CangJieLanguage) {
    override val codeStyleId: String = CODE_STYLE_ID

    override fun apply(settings: CodeStyleSettings) {
        Companion.apply(settings)
    }

    companion object {
        val INSTANCE = CangJieObsoleteCodeStyle()

        const val CODE_STYLE_ID = "CANGJIE_OLD_DEFAULTS"
        const val CODE_STYLE_SETTING = "obsolete"
        val CODE_STYLE_TITLE
            @NlsContexts.ListItem
            get() = CangJieFormatterBundle.message("list.item.cangjie.obsolete.intellij.idea.codestyle")

        fun apply(settings: CodeStyleSettings) {
            applyToCangJieCustomSettings(settings.cangjieCustomSettings)
            applyToCommonSettings(settings.cangjieCommonSettings)
        }

        fun applyToCangJieCustomSettings(cangjieCustomSettings: CangJieCodeStyleSettings, modifyCodeStyle: Boolean = true) {
            cangjieCustomSettings.apply {
                if (modifyCodeStyle) {
                    CODE_STYLE_DEFAULTS = CODE_STYLE_ID
                }

                CONTINUATION_INDENT_IN_PARAMETER_LISTS = true
                CONTINUATION_INDENT_IN_ARGUMENT_LISTS = true
                CONTINUATION_INDENT_FOR_EXPRESSION_BODIES = true
                CONTINUATION_INDENT_FOR_CHAINED_CALLS = true
                CONTINUATION_INDENT_IN_SUPERTYPE_LISTS = true
                CONTINUATION_INDENT_IN_IF_CONDITIONS = true
                CONTINUATION_INDENT_IN_ELVIS = true
                WRAP_EXPRESSION_BODY_FUNCTIONS = CodeStyleSettings.DO_NOT_WRAP
                IF_RPAREN_ON_NEW_LINE = false
            }
        }

        fun applyToCommonSettings(commonSettings: CommonCodeStyleSettings, modifyCodeStyle: Boolean = true) {
            commonSettings.apply {
                CALL_PARAMETERS_WRAP = CodeStyleSettings.DO_NOT_WRAP
                CALL_PARAMETERS_LPAREN_ON_NEXT_LINE = false
                CALL_PARAMETERS_RPAREN_ON_NEXT_LINE = false

                METHOD_PARAMETERS_WRAP = CodeStyleSettings.DO_NOT_WRAP
                METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE = false
                METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE = false

                EXTENDS_LIST_WRAP = CodeStyleSettings.DO_NOT_WRAP
                METHOD_CALL_CHAIN_WRAP = CodeStyleSettings.DO_NOT_WRAP
                ASSIGNMENT_WRAP = CodeStyleSettings.DO_NOT_WRAP
            }

            if (modifyCodeStyle && commonSettings is CangJieCommonCodeStyleSettings) {
                commonSettings.CODE_STYLE_DEFAULTS = CODE_STYLE_ID
            }
        }
    }
}
