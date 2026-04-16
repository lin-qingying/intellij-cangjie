/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.project.ui.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.DumbAwareAction

/**
 * 仓颉项目动作的基类
 *
 * 所有仓颉项目相关的动作都应该继承此类，以保持一致的行为。
 * 设置后台线程更新以避免阻塞 UI。
 */
abstract class CangJieProjectActionBase : DumbAwareAction() {

    /**
     * 指定动作更新在后台线程执行，避免阻塞 UI
     */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /**
     * 更新动作状态的通用逻辑
     * 子类可以重写此方法来实现特定的启用/禁用逻辑
     */
    protected open fun updatePresentation(e: AnActionEvent, presentation: Presentation) {
        val project = e.project
        presentation.isEnabled = project != null
    }

    final override fun update(e: AnActionEvent) {
        updatePresentation(e, e.presentation)
    }
}