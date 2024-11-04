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
        return super.getComponent(myLocationField, checkValid)
    }

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
