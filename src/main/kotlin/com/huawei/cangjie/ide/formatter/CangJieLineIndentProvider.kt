

package com.huawei.cangjie.ide.formatter

import com.huawei.cangjie.ide.formatter.lineIndent.CangJieIndentationAdjuster
import com.huawei.cangjie.ide.formatter.lineIndent.CangJieLangLineIndentProvider
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
