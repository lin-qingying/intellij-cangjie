package com.linqingying.cangjie.ide.actions

import com.linqingying.cangjie.cjpm.project.configurable.CjpmConfigurable
import com.linqingying.cangjie.utils.showSettingsDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

 class CjpmEditSettingsAction : AnAction(), DumbAware{
     override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

     override fun update(e: AnActionEvent) {
         super.update(e)
         e.presentation.isEnabledAndVisible = e.project != null
     }

     override fun actionPerformed(e: AnActionEvent) {
         e.project?.showSettingsDialog<CjpmConfigurable>()
     }
 }
