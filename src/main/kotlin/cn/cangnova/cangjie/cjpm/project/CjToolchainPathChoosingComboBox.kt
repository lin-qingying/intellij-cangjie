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

package cn.cangnova.cangjie.cjpm.project

import com.intellij.openapi.actionSystem.ActionUpdateThread.EDT
import cn.cangnova.cangjie.cjpm.project.CjToolchainPathChoosingComboBox.Companion.LOG
import cn.cangnova.cangjie.cjpm.toolchain.CjToolchainBase
import cn.cangnova.cangjie.ide.project.settings.ui.addTextChangeListener

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.ComboBoxWithWidePopup
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.ComboboxSpeedSearch.installOn
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import cn.cangnova.cangjie.utils.runTaskOnEdt
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.plaf.basic.BasicComboBoxEditor

/**。
 *带有浏览按钮的组合框，用于选择工具链的路径，也能够显示进度指示器。
 *要切换进度指示器可见性，请使用[setBusy]方法。
 */
class CjToolchainPathChoosingComboBox(onTextChanged: () -> Unit = {}) :
    ComponentWithBrowseButton<ComboBoxWithWidePopup<Path>>(
        ComboBoxWithWidePopup(), null
    ) {

    companion object {
        val LOG = Logger.getInstance(CjToolchainPathChoosingComboBox::class.java)
    }

    // 创建一个BasicComboBoxEditor对象，用于编辑ComboBox中的文本
    private val editor: BasicComboBoxEditor = object : BasicComboBoxEditor() {
        override fun createEditorComponent(): ExtendableTextField = ExtendableTextField()
    }

    // 获取ComboBox中的文本框
    private val pathTextField: ExtendableTextField
        get() = childComponent.editor.editorComponent as ExtendableTextField

    // 创建一个ExtendableTextComponent.Extension对象，用于显示忙碌图标
    private val busyIconExtension: ExtendableTextComponent.Extension =
        ExtendableTextComponent.Extension { AnimatedIcon.Default.INSTANCE }

    // 获取或设置ComboBox中选中的路径
    var selectedPath: Path?
        get() = pathTextField.text?.toPathOrNull()
        set(value) {
            pathTextField.text = value?.toString().orEmpty()
        }

    init {
        // 创建一个ComboboxSpeedSearch对象，用于快速搜索ComboBox中的文本
        installOn(childComponent)
        // 设置ComboBox的编辑器为editor
        childComponent.editor = editor
        // 设置ComboBox为可编辑
        childComponent.isEditable = true

        // 添加一个ActionListener，用于打开文件选择器
        addActionListener {

            val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            FileChooser.chooseFile(descriptor, null, null) { file ->
                childComponent.selectedItem = file.pathAsPath
            }
        }

        // 添加一个TextChangeListener，用于监听文本框中的文本变化
        pathTextField.addTextChangeListener { onTextChanged() }
    }

    // 设置ComboBox的忙碌状态
    private fun setBusy(busy: Boolean) {
        if (busy) {
            pathTextField.addExtension(busyIconExtension)
        } else {
            pathTextField.removeExtension(busyIconExtension)
        }
//        pathTextField.removeExtension(busyIconExtension)

        repaint()
    }


    @Suppress("MemberVisibilityCanBePrivate")
    // 异步添加工具链
    fun <T> addToolchainsAsync(toolchainObtainer: () -> List<T>, callback: () -> Unit) {
        setBusy(true)
        ApplicationManager.getApplication().executeOnPooledThread {
            var toolchains = emptyList<T>()
            try {
                toolchains = toolchainObtainer()
            } finally {


                val oldSelectedPath = selectedPath
                childComponent.removeAllItems()

                toolchains.forEach {

                    when (it) {
                        is CjToolchainBase -> childComponent.addItem(it.location)
                        is Path -> childComponent.addItem(it)
                        is String -> childComponent.addItem(it.toPath())
                    }

                }
                selectedPath = oldSelectedPath
                callback()
                setBusy(false)

            }
        }

    }

    // 异步添加工具链，不执行回调函数
    fun <T> addToolchainsAsync(toolchainObtainer: () -> List<T>) {
        addToolchainsAsync(toolchainObtainer) {}
    }

    // 添加单个工具链并更改选中的路径
    fun addSingleToolchainAndSelect(toolchain: Any) {
//        addToolchainsAsync({ listOf(toolchain) })
        val pathToAdd = when (toolchain) {
            is CjToolchainBase -> toolchain.location
            is Path -> toolchain
            is String -> toolchain.toPath()
            else -> throw IllegalArgumentException("Unsupported toolchain type")
        }

        // 添加路径并选中它
        childComponent.addItem(pathToAdd)
        selectedPath = pathToAdd
    }

}


// 将字符串转换为Path对象
fun String.toPath(): Path = Paths.get(this)

// 将字符串转换为Path对象，如果转换失败则返回null
fun String.toPathOrNull(): Path? = pathOrNull(this::toPath)

// 将字符串转换为Path对象，如果转换失败则记录日志并返回null
private inline fun pathOrNull(block: () -> Path): Path? {
    return try {
        block()
    } catch (e: InvalidPathException) {
        LOG.warn(e)
        null
    }
}

// 获取VirtualFile对象的路径，并转换为Path对象
val VirtualFile.pathAsPath: Path get() = Paths.get(path)
