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

package cn.cangnova.cangjie.ide.notifications



import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import java.util.function.Function

abstract class CjNotificationProvider(
    protected val project: Project
) : EditorNotificationProvider {

    protected abstract val VirtualFile.disablingKey: String

    final override fun collectNotificationData(
        project: Project,
        file: VirtualFile
    ): Function<in FileEditor, out CjEditorNotificationPanel?> {
        return Function { editor -> createNotificationPanel(file, editor, project) }
    }

    protected abstract fun createNotificationPanel(
        file: VirtualFile,
        editor: FileEditor,
        project: Project
    ): CjEditorNotificationPanel?

    protected fun updateAllNotifications() {
        EditorNotifications.getInstance(project).updateAllNotifications()
    }

    protected fun disableNotification(file: VirtualFile) {
        PropertiesComponent.getInstance(project).setValue(file.disablingKey, true)
    }

    protected fun isNotificationDisabled(file: VirtualFile): Boolean =
        PropertiesComponent.getInstance(project).getBoolean(file.disablingKey)
}

class CjEditorNotificationPanel(@Suppress("unused") private val debugId: String) : EditorNotificationPanel() {
    override fun getActionPlace(): String = NOTIFICATION_PANEL_PLACE

    companion object {
        const val NOTIFICATION_PANEL_PLACE = "CjEditorNotificationPanel"
    }
}
