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

package cn.cangnova.cangjie.ide.project.settings.ui


import cn.cangnova.cangjie.lang.CangJieFileType
import cn.cangnova.cangjie.psi.CjElement
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsContexts.DialogTitle
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.Alarm

import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.event.DocumentEvent

class UiDebouncer(
    private val parentDisposable: Disposable,
    private val delayMillis: Int = 200
) {
    private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, parentDisposable)

    /**
     * @param onUiThread: callback to be executed in EDT with **any** myModality state.
     * Use it only for UI updates
     */
    fun <T> run(onPooledThread: () -> T, onUiThread: (T) -> Unit) {
        if (Disposer.isDisposed(parentDisposable)) return
        alarm.cancelAllRequests()
        alarm.addRequest({
            val r = onPooledThread()
            invokeLater(ModalityState.any()) {
                if (!Disposer.isDisposed(parentDisposable)) {
                    onUiThread(r)
                }
            }
        }, delayMillis)
    }
}

//fun pathToDirectoryTextField(
//    disposable: Disposable,
//    @Suppress("UnstableApiUsage") @DialogTitle title: String,
//    onTextChanged: () -> Unit = {}
//): TextFieldWithBrowseButton =
//    pathTextField(
//        FileChooserDescriptorFactory.createSingleFolderDescriptor(),
//        disposable,
//        title,
//        onTextChanged
//    )

//fun pathToCjFileTextField(
//    disposable: Disposable,
//    @DialogTitle title: String,
//    project: Project,
//    onTextChanged: () -> Unit = {}
//): TextFieldWithBrowseButton =
//    pathTextField(
//        FileChooserDescriptorFactory
//            .createSingleFileDescriptor(CangJieFileType.INSTANCE )
//            .withRoots(project.guessProjectDir()),
//        disposable,
//        title,
//        onTextChanged
//    )

//fun pathTextField(
//    fileChooserDescriptor: FileChooserDescriptor,
//    disposable: Disposable,
//    @DialogTitle title: String,
//    onTextChanged: () -> Unit = {}
//): TextFieldWithBrowseButton {
//    val component = TextFieldWithBrowseButton(null, disposable)
//    component.addBrowseFolderListener(
//     null,
//        fileChooserDescriptor,
//        TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
//    )
//    component.childComponent.addTextChangeListener { onTextChanged() }
//    return component
//}

fun JTextField.addTextChangeListener(listener: (DocumentEvent) -> Unit) {
    document.addDocumentListener(
        object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                listener(e)
            }
        }
    )
}

fun selectElement(element: CjElement, editor: Editor) {
    val start = element.textRange.startOffset
    val unwrappedEditor = if (editor is CjIntentionInsideMacroExpansionEditor && element.containingFile != editor.psiFileCopy) {
        if (element.containingFile != editor.originalFile) return
        editor.originalEditor
    } else {
        editor
    }
    unwrappedEditor.caretModel.moveToOffset(start)
    unwrappedEditor.scrollingModel.scrollToCaret(ScrollType.RELATIVE)
    unwrappedEditor.selectionModel.setSelection(start, element.textRange.endOffset)
}

fun <T : JComponent> Row.fullWidthCell(component: T): Cell<T> {
    return cell(component).align(Align.FILL)

}

val JBTextField.trimmedText: String
    get() = text.trim()
