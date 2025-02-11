package com.linqingying.cangjie.ide.projectStructure.download

import com.intellij.execution.wsl.WslPath
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.BrowseFolderRunnable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapper.IdeModalityType
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.textFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import com.linqingying.cangjie.CangJieBundle
import org.jetbrains.annotations.Nls
import java.awt.Component
import java.awt.event.ItemEvent
import java.nio.file.Path
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.event.DocumentEvent

internal class SdkDownloadDialog(
    val project: Project?,
    private val parentComponent: Component?,

    private val mergedModel: SdkDownloaderMergedModel,
    okActionText: @NlsContexts.Button String = CangJieBundle.message("dialog.button.download.sdk"),
    val text: @Nls String? = null
) : DialogWrapper(project, parentComponent, false, IdeModalityType.IDE) {
    private lateinit var versionComboBox: ComboBox<SdkVersionItem>


    private var installDirTextField: TextFieldWithBrowseButton? = null
    private var installDirCombo: ComboBox<String>? = null
    private lateinit var installDirComponent: JComponent


    private var currentModel: SdkDownloaderModel? = null


    private lateinit var selectedItem: SdkItem
    private lateinit var selectedPath: String


    private val panel: DialogPanel = panel {
        if (text != null) {
            row {
                label(text)
            }
        }

        var archiveSizeCell: Cell<*>? = null

        row(CangJieBundle.message("dialog.row.sdk.version")) {
            versionComboBox =
                comboBox(listOf<SdkVersionItem>().toMutableList(), textListCellRenderer { it!!.sdkVersion }).align(
                    AlignX.FILL
                ).component
        }

        row(CangJieBundle.message("dialog.row.sdk.location")) {
            cell(setupContainer()).align(AlignX.FILL).apply {
                archiveSizeCell = comment("")
            }
        }
    }


    init {
        title = CangJieBundle.message("dialog.title.download.sdk")
        isResizable = false

        versionComboBox.onSelectionChange(::onVersionSelectionChange)

        setOKButtonText(okActionText)

        setModel(false/*mergedModel.projectWSLDistribution != null*/)
        init()
    }

    private fun setupContainer(): JComponent {
        if (mergedModel.hasWsl) {
            installDirCombo = ComboBox<String>().apply {
                isEditable = true
                initBrowsableEditor(
//                    BrowseFolderRunnable(
//                        CangJieBundle.message("dialog.title.select.path.to.install.sdk"),
//                        null,
//                        project,
//                        FileChooserDescriptorFactory.createSingleFolderDescriptor(),
//                        installDirCombo,
//                        TextComponentAccessor.STRING_COMBOBOX_WHOLE_TEXT
//                    )
                    BrowseFolderRunnable(
                        project,
                        FileChooserDescriptorFactory.createSingleFolderDescriptor()
                            .withTitle(CangJieBundle.message("dialog.title.select.path.to.install.sdk")),
                        installDirCombo,
                        TextComponentAccessor.STRING_COMBOBOX_WHOLE_TEXT
                    )
                    , disposable
                )
                addActionListener { onTargetPathChanged(editor.item as String) }
                installDirComponent = this
            }
            installDirTextField = null
        } else {
            installDirTextField = textFieldWithBrowseButton(
                project,
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
                    .withTitle(CangJieBundle.message("dialog.title.select.path.to.install.sdk"))
            )

                .apply {
                    onTextChange { onTargetPathChanged(it) }
                    textField.columns = 36
                    installDirComponent = this
                }
            installDirCombo = null
        }
        return installDirComponent
    }

    private fun setModel(forWsl: Boolean) {
        val model = mergedModel.selectModel(forWsl)
        if (currentModel === model) return

        val prevSelectedVersion = versionComboBox.selectedItem as? SdkVersionItem

        currentModel = model
        versionComboBox.model = DefaultComboBoxModel(model.versionGroups.toTypedArray())

        val newVersionItem = if (prevSelectedVersion != null) {
            model.versionGroups.singleOrNull { it.sdkVersion == prevSelectedVersion.sdkVersion }
        } else null


        onVersionSelectionChange(newVersionItem ?: model.defaultVersion)

    }


    private fun onTargetPathChanged(path: String) {
        @Suppress("NAME_SHADOWING") val path = FileUtil.expandUserHome(path)
        selectedPath = path


        setModel(WslPath.isWslUncPath(path))
    }

    private fun getSuggestedInstallDirs(newVersion: SdkItem): List<String> {
        return (listOf(null) + mergedModel.wslDistributions).mapTo(LinkedHashSet()) {
            SdkInstaller.getInstance().defaultInstallDir(newVersion, it).toString()
        }.map {
            FileUtil.getLocationRelativeToUserHome(it)
        }
    }

    private fun onVersionSelectionChange(it: SdkVersionItem?) {
        if (it == null) return
        versionComboBox.selectedItem = it
        val path =
            SdkInstaller.getInstance().defaultInstallDir(it.item/* mergedModel.projectWSLDistribution*/).toString()
        val relativePath = FileUtil.getLocationRelativeToUserHome(path)
        if (installDirTextField != null) {
            installDirTextField!!.text = relativePath
        } else {
            installDirCombo!!.model = CollectionComboBoxModel(getSuggestedInstallDirs(it.item), relativePath)
        }
        selectedPath = path
        selectedItem = it.item

    }

    override fun doValidate(): ValidationInfo? {
        super.doValidate()?.let { return it }

        val (_, error) = SdkInstaller.getInstance().validateInstallDir(selectedPath)
        return error?.let { ValidationInfo(error, installDirComponent) }
    }

    override fun createCenterPanel() = panel

    fun selectSdkAndPath(): Pair<SdkItem, Path>? {
        if (!showAndGet()) {
            return null
        }

        val (selectedFile) = SdkInstaller.getInstance().validateInstallDir(selectedPath)
        if (selectedFile == null) {
            return null
        }

        return selectedItem to selectedFile
    }

    private inline fun TextFieldWithBrowseButton.onTextChange(crossinline action: (String) -> Unit) {
        textField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                action(text)
            }
        })
    }

    private inline fun <reified T> ComboBox<T>.onSelectionChange(crossinline action: (T) -> Unit) {
        this.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) action(e.item as T)
        }
    }

}
