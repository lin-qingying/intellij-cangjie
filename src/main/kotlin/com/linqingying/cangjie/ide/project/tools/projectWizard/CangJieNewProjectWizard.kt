package com.linqingying.cangjie.ide.project.tools.projectWizard

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.cjpm.project.toPathOrNull
import com.linqingying.cangjie.icon.CangJieIcons
import com.linqingying.cangjie.ide.newProject.CjProjectGeneratorPeer
import com.linqingying.cangjie.ide.project.tools.projectWizard.wizard.CangJieModuleBuilder
import com.intellij.CommonBundle
import com.intellij.ide.wizard.*
import com.intellij.ide.wizard.GitNewProjectWizardData.Companion.gitData
import com.intellij.ide.wizard.language.LanguageGeneratorNewProjectWizard
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ui.configuration.*
import com.intellij.openapi.roots.ui.configuration.SdkListItem.*
import com.intellij.openapi.ui.*
import com.intellij.openapi.ui.validation.DialogValidation
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.Consumer
import com.jetbrains.rd.util.getThrowableText
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import javax.swing.AbstractListModel
import javax.swing.ComboBoxModel
import javax.swing.Icon
import javax.swing.ListModel


class CangJieGeneratorNewProjectWizard : LanguageGeneratorNewProjectWizard {

    companion object {
        private const val GITIGNORE: String = ".gitignore"

        private fun createGitIgnoreFile(projectDir: Path, module: Module) {
            val directory = VfsUtil.createDirectoryIfMissing(projectDir.toString()) ?: return
            val existingFile = directory.findChild(GITIGNORE)
            if (existingFile != null) return
            val file = directory.createChildData(module, GITIGNORE)
            VfsUtil.saveText(file, "/build\n")
        }

    }

    override val icon: Icon = CangJieIcons.CANGJIE
    override val name: String = CangJieBundle.message("cangjie")

    override fun createStep(parent: NewProjectWizardStep): NewProjectWizardStep = Step(parent as NewProjectWizardLanguageStep)
    class Step(parent: NewProjectWizardLanguageStep) : AbstractNewProjectWizardStep(parent) {



        private val peer: CjProjectGeneratorPeer = CjProjectGeneratorPeer(parent.path.toPathOrNull() ?: Paths.get("."))
        override fun setupProject(project: Project) {

            val builder = CangJieModuleBuilder(
//                moduleName = moduleNameTextField.text,
//                organizationName = groupIdTextField.text,
//                projectType = (projectTypeComboBox.selectedItem as CangJieProjectTypeItem).type
            )
            val module = builder.commit(project)?.firstOrNull() ?: return
//
            ModuleRootModificationUtil.updateModel(module) { rootModel ->
                builder.configurationData = peer.settings


                try {
                    builder.createProject(rootModel)
                } catch (e: ConfigurationException) {
//                    捕获cjpm执行错误
//                    弹窗向用户报告错误

                    var message =
                        e.getThrowableText().replace("com.intellij.openapi.options.ConfigurationException: ", "")
                    message = message.substring(0, message.indexOf("stderr : "))
                    MessageDialogBuilder.yesNo(e.title, message)


                    return@updateModel
                }


                if (gitData?.git == true) runWriteAction {
                    createGitIgnoreFile(context.projectDirectory, module)
                }
            }


        }

        override fun setupUI(builder: Panel) {


            with(builder) {

                row {
                    cell(peer.component).align(Align.FILL)

                        .validationRequestor { peer.checkValid = Runnable(it) }
                        .validation(DialogValidation { peer.validate() })

                }
//                row(CangJieUiBundle.message("action.new.project.projecttype.title")) {
//                    cell(projectTypeComboBox)
//                        .columns(COLUMNS_MEDIUM)
//                        .component
//                }.bottomGap(BottomGap.SMALL)
//                row(CangJieUiBundle.message("action.new.project.modulename.title")) {
//                    cell(moduleNameTextField)
//                        .columns(COLUMNS_MEDIUM)
//                        //                        .validationOnApply {
////                            validateModuleName(moduleNameTextField.text)
////                        }
//                        .component
//                }.bottomGap(BottomGap.SMALL)
//                row(CangJieUiBundle.message("action.new.project.groupname.title")) {
//                    cell(groupIdTextField)
//                        .columns(COLUMNS_MEDIUM)
////                        .validationOnApply {
////                            validateGroupName(groupIdTextField.text)
////                        }
//                        .component
//                }.bottomGap(BottomGap.SMALL)
            }


        }
    }
}

class CangJieNewProjectWizard : LanguageNewProjectWizard {
    override val name = "CangJie"

    companion object {
        private const val GITIGNORE: String = ".gitignore"

        private fun createGitIgnoreFile(projectDir: Path, module: Module) {
            val directory = VfsUtil.createDirectoryIfMissing(projectDir.toString()) ?: return
            val existingFile = directory.findChild(GITIGNORE)
            if (existingFile != null) return
            val file = directory.createChildData(module, GITIGNORE)
            VfsUtil.saveText(file, "/build\n")
        }

    }


    override fun createStep(parent: NewProjectWizardLanguageStep): NewProjectWizardStep = Step(parent)


    class Step(parent: NewProjectWizardLanguageStep) : AbstractNewProjectWizardStep(parent) {

        private val peer: CjProjectGeneratorPeer = CjProjectGeneratorPeer(parent.path.toPathOrNull() ?: Paths.get("."))
        override fun setupProject(project: Project) {

            val builder = CangJieModuleBuilder(
//                moduleName = moduleNameTextField.text,
//                organizationName = groupIdTextField.text,
//                projectType = (projectTypeComboBox.selectedItem as CangJieProjectTypeItem).type
            )
            val module = builder.commit(project)?.firstOrNull() ?: return
//
            ModuleRootModificationUtil.updateModel(module) { rootModel ->
                builder.configurationData = peer.settings


                try {
                    builder.createProject(rootModel)
                } catch (e: ConfigurationException) {
//                    捕获cjpm执行错误
//                    弹窗向用户报告错误

                    var message =
                        e.getThrowableText().replace("com.intellij.openapi.options.ConfigurationException: ", "")
                    message = message.substring(0, message.indexOf("stderr : "))
                    MessageDialogBuilder.yesNo(e.title, message)


                    return@updateModel
                }


                if (gitData?.git == true) runWriteAction {
                    createGitIgnoreFile(context.projectDirectory, module)
                }
            }


        }

        override fun setupUI(builder: Panel) {


            with(builder) {

                row {
                    cell(peer.component).align(Align.FILL)

                        .validationRequestor { peer.checkValid = Runnable(it) }
                        .validation(DialogValidation { peer.validate() })

                }
//                row(CangJieUiBundle.message("action.new.project.projecttype.title")) {
//                    cell(projectTypeComboBox)
//                        .columns(COLUMNS_MEDIUM)
//                        .component
//                }.bottomGap(BottomGap.SMALL)
//                row(CangJieUiBundle.message("action.new.project.modulename.title")) {
//                    cell(moduleNameTextField)
//                        .columns(COLUMNS_MEDIUM)
//                        //                        .validationOnApply {
////                            validateModuleName(moduleNameTextField.text)
////                        }
//                        .component
//                }.bottomGap(BottomGap.SMALL)
//                row(CangJieUiBundle.message("action.new.project.groupname.title")) {
//                    cell(groupIdTextField)
//                        .columns(COLUMNS_MEDIUM)
////                        .validationOnApply {
////                            validateGroupName(groupIdTextField.text)
////                        }
//                        .component
//                }.bottomGap(BottomGap.SMALL)
            }


        }
    }

}


class CangJieSdkCombox : SdkComboBoxBase<CangJieSdkCombox.CangJieSdkItem> {
    val project: Project
    private var myOnNewSdkAdded: Consumer<Sdk>? = null


    constructor(
        project: Project,
        modelBuilder: SdkListModelBuilder,
        onNewSdkAdded: Consumer<Sdk>?
    ) : super(modelBuilder) {
        this.project = project

        myOnNewSdkAdded = Consumer<Sdk> { sdk: Sdk? ->
            onNewSdkAdded?.consume(sdk)
        }

        setRenderer(SdkListPresenter.create(
            this,
            { (this.model as CangJieSdkModel).innerModel }
        ) { item -> unwrapItem(item) })
        reloadModel()

    }

    companion object {
        private fun unwrapItem(item: CangJieSdkItem?): SdkListItem {
            var item: CangJieSdkItem? = item
            if (item == null) item = ProjectSdkComboBoxItem()
            if (item is InnerComboBoxItem) {
                return (item as InnerComboBoxItem?)?.item
                    ?: throw RuntimeException("Failed to unwrap " + item.javaClass.name + ": " + item)
            }
            throw RuntimeException("Failed to unwrap " + item.javaClass.name + ": " + item)
        }

        private fun wrapItem(item: SdkListItem): CangJieSdkItem {
            if (item is SdkItem) {
//                val a = item.sdk
                ApplicationManager.getApplication().runWriteAction {
                    ProjectJdkTable.getInstance().addJdk(item.sdk)
                }
//                添加到sdk列表
//                ProjectJdkTable.getInstance().addJdk(item.sdk)
                return ActualSdkInnerItem(item)
            }
            if (item is NoneSdkItem) {
                return NoneSdkComboBoxItem()
            }
            return if (item is ProjectSdkItem) {
                ProjectSdkComboBoxItem()
            } else InnerSdkComboBoxItem(item)
        }
    }

    private interface InnerComboBoxItem {
        val item: SdkListItem
    }


    private interface SelectableComboBoxItem

    class ProjectSdkComboBoxItem : CangJieSdkItem(),
        InnerComboBoxItem,
        SelectableComboBoxItem {
        override val item: SdkListItem
            get() = ProjectSdkItem()

        override fun hashCode(): Int {
            return 42
        }

        override fun equals(other: Any?): Boolean {
            return other is ProjectSdkComboBoxItem
        }
    }

//    private val myEditButton: JButton? = null

    //    private fun updateEditButton() {
//        if (myEditButton != null) {
//            val selectedItem: CangJieSdkItem? = selectedItem as CangJieSdkItem?
//            if (selectedItem is ProjectSdkComboBoxItem && project != null) {
//                myEditButton.setEnabled(
//                    ProjectStructureConfigurable.getInstance(project).getProjectsdksModel().getProjectSdk() != null
//                )
//            } else {
//                myEditButton.setEnabled(selectedItem != null && selectedItem.sdk != null)
//            }
//        }
//    }
    override fun setSelectedItem(anObject: Any?) {
        if (anObject is SdkListItem) {
            setSelectedItem((anObject as SdkListItem?)?.let { wrapItem(it) })
//            updateEditButton()

            return
        }

        if (anObject == null) {
            val innerModel: SdkListModel =
                (model as CangJieSdkModel).innerModel
            var candidate = innerModel.findProjectSdkItem()
            if (candidate == null) {
                candidate = innerModel.findNoneSdkItem()
            }
            if (candidate == null) {
                candidate = myModel.showProjectSdkItem()
            }
            setSelectedItem(candidate)
            return
        }

        if (anObject is Sdk) {

            myModel.reloadSdks()
            (anObject as Sdk?)?.let { (model as CangJieSdkModel).trySelectSdk(it) }
            return
        }

        if (anObject is InnerComboBoxItem) {
            val item: SdkListItem = anObject.item
            if (myModel.executeAction(this, item) { newItem: SdkListItem? ->
                    setSelectedItem(newItem)

//                    将新的sdk添加到sdk列表中
//                    ProjectJdkTable.getInstance().addJdk(newItem!!.)

                    if (newItem is SdkItem) {
                        myOnNewSdkAdded!!.consume(newItem.sdk)
                    }
                }) return
        }

        if (anObject is SelectableComboBoxItem) {
            super.setSelectedItem(anObject)
        }
    }

    //    override fun getSelectedItem(): CangJieSdkItem {
//        return super.getSelectedItem() as CangJieSdkItem
//    }
    override fun onModelUpdated(model: SdkListModel) {
        val previousSelection = selectedItem
        val newModel =
            CangJieSdkModel(model)
        newModel.selectedItem = previousSelection
        setModel(newModel)
    }

    override fun firePopupMenuWillBecomeVisible() {
        resolveSuggestionsIfNeeded()
        super.firePopupMenuWillBecomeVisible()

    }

    private fun resolveSuggestionsIfNeeded() {
        myModel.reloadActions()
        val dialogWrapper = DialogWrapper.findInstance(this)
        if (dialogWrapper == null) {
            Logger.getInstance(CangJieSdkCombox::class.java)
                .warn(
                    "Cannot find DialogWrapper parent for the CangJieSdkCombox $this, SDK search is disabled",
                    java.lang.RuntimeException()
                )
            return
        }
        myModel.detectItems(this, dialogWrapper.disposable)
    }

    open class ActualSdkComboBoxItem(override val sdk: Sdk) : CangJieSdkItem(), SelectableComboBoxItem {

        override fun toString(): String {
            return sdk.name
        }

        override val sdkName: String?
            get() = sdk.name

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val item = other as ActualSdkComboBoxItem
            return sdk == item.sdk
        }

        override fun hashCode(): Int {
            return Objects.hash(sdk)
        }
    }

    private class ActualSdkInnerItem(private val myItem: SdkItem) :
        ActualSdkComboBoxItem(
            myItem.sdk
        ),
        InnerComboBoxItem {

        override val item: SdkListItem
            get() = myItem
    }


    class NoneSdkComboBoxItem : CangJieSdkItem(), InnerComboBoxItem, SelectableComboBoxItem {
        override val item: SdkListItem
            get() = NoneSdkItem()

        override fun toString(): String {
            return "<None>"
        }

        override fun hashCode(): Int {
            return 42
        }

        override fun equals(other: Any?): Boolean {
            return other is NoneSdkComboBoxItem
        }
    }

    class InnerSdkComboBoxItem(override val item: SdkListItem) : CangJieSdkItem(),
        InnerComboBoxItem {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val item = other as InnerSdkComboBoxItem
            return this.item == item.item
        }

        override fun hashCode(): Int {
            return Objects.hash(item)
        }
    }

    class CangJieSdkModel(val innerModel: SdkListModel) : AbstractListModel<CangJieSdkItem>(),
        ComboBoxPopupState<CangJieSdkItem>,
        ComboBoxModel<CangJieSdkItem> {
        override fun getSize(): Int {
            return innerModel.items.size
        }

        override fun getElementAt(index: Int): CangJieSdkItem {
            return wrapItem(innerModel.items[index])
        }

        companion object {
            private fun wrapItem(item: SdkListItem): CangJieSdkItem {
                if (item is SdkItem) {
                    return ActualSdkInnerItem(item)
                }
                if (item is NoneSdkItem) {
                    return NoneSdkComboBoxItem()
                }
                return if (item is ProjectSdkItem) {
                    ProjectSdkComboBoxItem()
                } else InnerSdkComboBoxItem(item)
            }
        }

        override fun onChosen(selectedValue: CangJieSdkItem?): ListModel<CangJieSdkItem>? {
            if (selectedValue is InnerComboBoxItem) {
                val inner: SdkListModel? = innerModel.onChosen((selectedValue as InnerComboBoxItem).item)
                return if (inner == null) null else CangJieSdkModel(
                    inner
                )
            }
            return null
        }

        override fun hasSubstep(selectedValue: CangJieSdkItem?): Boolean {
            return if (selectedValue is InnerComboBoxItem) {
                innerModel.hasSubstep((selectedValue as InnerComboBoxItem).item)
            } else false
        }

        private var mySelectedItem: CangJieSdkItem? = null
        override fun setSelectedItem(anItem: Any?) {
            if (anItem !is CangJieSdkItem) return
            if (anItem !is InnerComboBoxItem) return
            val innerItem: SdkListItem = (anItem as InnerComboBoxItem).item
            if (!innerModel.items.contains(innerItem)) return
            mySelectedItem = anItem
            fireContentsChanged(this, -1, -1)
        }

        fun trySelectSdk(sdk: Sdk) {
            val item: SdkItem = innerModel.findSdkItem(sdk) ?: return
            setSelectedItem(wrapItem(item))
        }

        override fun getSelectedItem(): Any {
            return mySelectedItem ?: ProjectSdkComboBoxItem()
        }
    }

    abstract class CangJieSdkItem {
        open val sdk: Sdk?
            get() = null
        open val sdkName: String?
            get() = null
    }


}


class CangJieProjectTypeItem(val type: String, val description: String) {
    override fun toString(): String {
        return description
    }
}

fun ValidationInfoBuilder.validateSdk(sdk: Sdk?/*, sdkModel: ProjectSdksModel */): ValidationInfo? {
    return validateAndGetSdkValidationMessage(sdk/*, sdkModel*/)?.let { error(it) }
}

fun ValidationInfoBuilder.validateModuleName(moduleName: String?): ValidationInfo? {
    return validateAndGetModuleValidationMessage(moduleName)?.let { error(it) }
}

fun ValidationInfoBuilder.validateGroupName(groupName: String?): ValidationInfo? {
    return validateAndGetGroupNameValidationMessage(groupName)?.let { error(it) }
}

private fun validateAndGetGroupNameValidationMessage(groupName: String?): @NlsContexts.DialogMessage String? {
    if (groupName == null || groupName.isEmpty()) {
        return CangJieUiBundle.message("title.group.name.specified")
    }
    return null

}

private fun validateAndGetModuleValidationMessage(moduleName: String?): @NlsContexts.DialogMessage String? {
    if (moduleName == null || moduleName.isEmpty()) {
        return CangJieUiBundle.message("title.module.name.specified")
    }
    return null

}


private fun validateAndGetSdkValidationMessage(
    sdk: Sdk?,
    /*  sdkModel: ProjectSdksModel*/
): @NlsContexts.DialogMessage String? {
    if (sdk == null) {
        if (Messages.showDialog(
                CangJieUiBundle.message("prompt.confirm.project.no.sdk"),
                CangJieUiBundle.message("title.no.sdk.specified"),
                arrayOf(CommonBundle.getYesButtonText(), CommonBundle.getNoButtonText()), 1,
                Messages.getWarningIcon()
            ) != Messages.YES
        ) {
            return CangJieUiBundle.message("title.no.sdk.specified")
        }
    }

//    try {
//        sdkModel.apply(null, true)
//    } catch (e: ConfigurationException) {
//        //IDEA-98382 We should allow Next step if user has wrong SDK
//        if (Messages.showDialog(
//                e.message?.let { CangJieUiBundle.message("dialog.message.0.do.you.want.to.proceed", it) },
//                e.title, arrayOf(CommonBundle.getYesButtonText(), CommonBundle.getNoButtonText()), 1,
//                Messages.getWarningIcon()
//            ) != Messages.YES
//        ) {
//            return e.message ?: e.title
//        }
//    }
    return null
}



