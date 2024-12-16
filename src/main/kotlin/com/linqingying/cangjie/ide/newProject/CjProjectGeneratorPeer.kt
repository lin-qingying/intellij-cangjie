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

package com.linqingying.cangjie.ide.newProject

import com.linqingying.cangjie.ide.newProject.ui.CjNewProjectPanel
import com.linqingying.cangjie.ide.newProject.ui.ConfigurationData

import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.platform.GeneratorPeerImpl
import com.intellij.ui.dsl.builder.panel
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JComponent

class CjProjectGeneratorPeer(cjpmProjectDir: Path = Paths.get(".")) : GeneratorPeerImpl<ConfigurationData>() {

    private val newProjectPanel = CjNewProjectPanel(showProjectTypeSelection = true, cjpmProjectDir) { checkValid?.run() }
    var checkValid: Runnable? = null

    override fun getSettings(): ConfigurationData = newProjectPanel.data

    override fun getComponent(myLocationField: TextFieldWithBrowseButton, checkValid: Runnable): JComponent {
        this.checkValid = checkValid
        return panel {
            newProjectPanel.attachTo(this)
        }

    }

    @Deprecated("Deprecated in Java")
    override fun getComponent(): JComponent = panel {
        newProjectPanel.attachTo(this)
    }


    override fun validate(): ValidationInfo? = try {
        newProjectPanel.validateSettings()
        null
    } catch (e: ConfigurationException) {
        ValidationInfo(e.message ?: "")
    }
}
