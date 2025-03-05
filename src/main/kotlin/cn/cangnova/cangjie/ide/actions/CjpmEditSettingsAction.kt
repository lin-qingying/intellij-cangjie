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

package cn.cangnova.cangjie.ide.actions

import cn.cangnova.cangjie.cjpm.project.configurable.CjpmConfigurable
import cn.cangnova.cangjie.utils.showSettingsDialog
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
