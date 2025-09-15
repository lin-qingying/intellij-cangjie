package org.cangnova.cangjie.cjpm.project.model.impl

import com.intellij.openapi.externalSystem.ui.ExternalSystemIconProvider
import org.cangnova.cangjie.icon.CangJieIcons
import org.cangnova.cangjie.icon.CjpmIcons
import javax.swing.Icon

class CjpmExternalSystemIconProvider: ExternalSystemIconProvider {
    override val reloadIcon: Icon get() = CjpmIcons.RELOAD_ICON

}