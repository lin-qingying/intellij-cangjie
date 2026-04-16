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

package org.cangnova.cangjie.project

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic


abstract class CjProjectSettingsServiceBase<T :  CjProjectSettingsBase<T>>(
    val project: Project,
    state: T
) : SimplePersistentStateComponent<T>(state)  {



    fun modify(action: (T) -> Unit) {
        val oldState = state.copy()
        val newState = state.also(action)
        val event = createSettingsChangedEvent(oldState, newState)
        notifySettingsChanged(event)
    }

    protected abstract fun createSettingsChangedEvent(oldEvent: T, newEvent: T): SettingsChangedEventBase<T>
    protected open fun notifySettingsChanged(event: SettingsChangedEventBase<T>) {
        project.messageBus.syncPublisher(CANGJIE_SETTINGS_TOPIC).settingsChanged(event)

        if (event.affectsHighlighting) {
            DaemonCodeAnalyzer.getInstance(project).restart()
        }
    }



}
