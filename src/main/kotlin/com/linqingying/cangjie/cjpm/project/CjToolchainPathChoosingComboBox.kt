package com.linqingying.cangjie.cjpm.project

import com.linqingying.cangjie.cjpm.project.CjToolchainPathChoosingComboBox.Companion.LOG
import com.linqingying.cangjie.cjpm.toolchain.CjToolchainBase
import com.linqingying.cangjie.ide.project.settings.ui.addTextChangeListener
import com.intellij.openapi.application.AppUIExecutor
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
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
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

    private val editor: BasicComboBoxEditor = object : BasicComboBoxEditor() {
        override fun createEditorComponent(): ExtendableTextField = ExtendableTextField()
    }
    private val pathTextField: ExtendableTextField
        get() = childComponent.editor.editorComponent as ExtendableTextField
    private val busyIconExtension: ExtendableTextComponent.Extension =
        ExtendableTextComponent.Extension { AnimatedIcon.Default.INSTANCE }
    var selectedPath: Path?
        get() = pathTextField.text?.toPathOrNull()
        set(value) {
            pathTextField.text = value?.toString().orEmpty()
        }

    init {
        ComboboxSpeedSearch(childComponent)
        childComponent.editor = editor
        childComponent.isEditable = true

        addActionListener {

            val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            FileChooser.chooseFile(descriptor, null, null) { file ->
                childComponent.selectedItem = file.pathAsPath
            }
        }

        pathTextField.addTextChangeListener { onTextChanged() }
    }

    private fun setBusy(busy: Boolean) {
        if (busy) {
            pathTextField.addExtension(busyIconExtension)
        } else {
            pathTextField.removeExtension(busyIconExtension)
        }
        repaint()
    }


    /**。
     *使用[toolchainObtainer]获取池上的工具链列表，然后填充组合框并在EDT上调用[callback]。
     */
//    @Suppress("MemberVisibilityCanBePrivate")
//    fun addToolchainsAsync(toolchainObtainer: () -> List<Path>, callback: () -> Unit) {
//        setBusy(true)
//        ApplicationManager.getApplication().executeOnPooledThread {
//            var toolchains = emptyList<Path>()
//            try {
//                toolchains = toolchainObtainer()
//            } finally {
//                val executor = AppUIExecutor.onUiThread(ModalityState.any()).expireWith(this)
//                executor.execute {
//                    setBusy(false)
//                    val oldSelectedPath = selectedPath
//                    childComponent.removeAllItems()
//                    toolchains.forEach(childComponent::addItem)
//                    selectedPath = oldSelectedPath
//                    callback()
//                }
//            }
//        }
//    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun <T> addToolchainsAsync(toolchainObtainer: () -> List<T>, callback: () -> Unit) {
        setBusy(true)
        ApplicationManager.getApplication().executeOnPooledThread {
            var toolchains = emptyList<T>()
            try {
                toolchains = toolchainObtainer()
            } finally {
                val executor = AppUIExecutor.onUiThread(ModalityState.any()).expireWith(this)
                executor.execute {
                    setBusy(false)
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
                }
            }
        }
    }

    fun <T> addToolchainsAsync(toolchainObtainer: () -> List<T>) {
        addToolchainsAsync(toolchainObtainer) {}
    }

//    fun addToolchainsAsync(toolchainObtainer: () -> List<Path>) {
//        addToolchainsAsync(toolchainObtainer) {}
//    }

}


fun String.toPath(): Path = Paths.get(this)
fun String.toPathOrNull(): Path? = pathOrNull(this::toPath)
private inline fun pathOrNull(block: () -> Path): Path? {
    return try {
        block()
    } catch (e: InvalidPathException) {
        LOG.warn(e)
        null
    }
}

val VirtualFile.pathAsPath: Path get() = Paths.get(path)
