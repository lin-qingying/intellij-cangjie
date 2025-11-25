package org.cangnova.cangjie.protodebugger.settings

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.xdebugger.XDebugSession
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import org.cangnova.cangjie.protodebugger.core.CangJieDebugProcess
import org.cangnova.cangjie.protodebugger.services.DisasmService

