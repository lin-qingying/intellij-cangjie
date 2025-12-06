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


package org.cangnova.cangjie.ide.formatter

import org.cangnova.cangjie.lang.CangJieLanguage
import com.intellij.application.options.codeStyle.CommenterForm
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.psi.codeStyle.CodeStyleConfigurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider
import com.intellij.psi.codeStyle.DisplayPriority
import com.intellij.ui.IdeBorderFactory
import com.intellij.util.ui.JBInsets

import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

class CangJieGenerationSettingsProvider : CodeStyleSettingsProvider() {
    override fun createConfigurable(settings: CodeStyleSettings, modelSettings: CodeStyleSettings): CodeStyleConfigurable {
        return CangJieCodeStyleGenerationConfigurable(settings)
    }

    override fun getConfigurableDisplayName(): String = ApplicationBundle.message("title.code.generation")
    override fun getPriority(): DisplayPriority = DisplayPriority.CODE_SETTINGS
    override fun hasSettingsPage() = false
    override fun getLanguage() = CangJieLanguage
}

class CangJieCodeStyleGenerationConfigurable(private val mySettings: CodeStyleSettings) : CodeStyleConfigurable {
    private val myCommenterForm: CommenterForm = CommenterForm(CangJieLanguage)

    override fun getDisplayName(): String = ApplicationBundle.message("title.code.generation")

    override fun createComponent(): JComponent {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = IdeBorderFactory.createEmptyBorder(JBInsets(0, 10, 10, 10))
            add(myCommenterForm.commenterPanel)
        }
    }

    override fun isModified(): Boolean {
        return myCommenterForm.isModified(mySettings)
    }

    override fun apply() {
        apply(mySettings)
    }

    override fun reset() {
        reset(mySettings)
    }

    override fun reset(settings: CodeStyleSettings) {
        myCommenterForm.reset(settings)
    }

    override fun apply(settings: CodeStyleSettings) {
        myCommenterForm.apply(settings)
    }
}
