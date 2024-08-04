package com.linqingying.cangjie.cjpm.project.configurable

import com.linqingying.cangjie.CangJieBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel

class CjpmConfigurable(
    project: Project,
    private val isPlaceholder: Boolean
) : CjConfigurableBase(project, CangJieBundle.message("settings.cangjie.cjpm.name")) {
    override fun createPanel(): DialogPanel  = panel{}


}
