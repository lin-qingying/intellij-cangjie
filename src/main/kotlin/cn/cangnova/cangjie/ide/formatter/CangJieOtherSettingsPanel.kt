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



package cn.cangnova.cangjie.ide.formatter

import cn.cangnova.cangjie.CangJieBundle
import cn.cangnova.cangjie.lang.CangJieLanguage
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.psi.codeStyle.CodeStyleSettings


import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import javax.swing.*

class CangJieOtherSettingsPanel(settings: CodeStyleSettings) : CodeStyleAbstractPanel(CangJieLanguage, null, settings) {
    private val cbTrailingComma = JCheckBox(CangJieBundle.message("formatter.checkbox.text.use.trailing.comma"))

    override fun getRightMargin() = throw UnsupportedOperationException()

    override fun createHighlighter(scheme: EditorColorsScheme) = throw UnsupportedOperationException()

    override fun getFileType() = throw UnsupportedOperationException()

    override fun getPreviewText(): String? = null

    override fun apply(settings: CodeStyleSettings) {
        settings.cangjieCustomSettings.ALLOW_TRAILING_COMMA = cbTrailingComma.isSelected
    }

    override fun isModified(settings: CodeStyleSettings): Boolean {
        return settings.cangjieCustomSettings.ALLOW_TRAILING_COMMA != cbTrailingComma.isSelected
    }

    override fun getPanel() = cPanel

    override fun resetImpl(settings: CodeStyleSettings) {
        cbTrailingComma.isSelected = settings.cangjieCustomSettings.ALLOW_TRAILING_COMMA
    }

    override fun getTabTitle(): String = CangJieBundle.message("formatter.title.other")

//    private val cjPanel = JPanel().apply {
//        layout = BoxLayout(this, BoxLayout.Y_AXIS)
//
//        add(
//            JBScrollPane(
//                JPanel(VerticalLayout(JBUI.scale(5))).apply {
//                    border = BorderFactory.createEmptyBorder(0, UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP)
//                    add(
//                        OptionGroup(CangJieBundle.message("formatter.title.trailing.comma")).apply {
//                            add(cbTrailingComma)
//                        }.createPanel()
//                    )
//                }
//            )
//        )
//    }


    private val cPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)

        add(
            JBScrollPane(
                JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    border = BorderFactory.createEmptyBorder(0, UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP)
                    add(JLabel(CangJieBundle.message("formatter.title.trailing.comma")))
                    add(cbTrailingComma)
                }
            )
        )
    }
}
