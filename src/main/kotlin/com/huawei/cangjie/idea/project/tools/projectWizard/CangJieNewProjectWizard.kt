package com.huawei.cangjie.idea.project.tools.projectWizard

import com.huawei.cangjie.idea.project.tools.projectWizard.wizard.NewProjectWizardModuleBuilder
import com.huawei.cangjie.lang.sdk.CangJieSdkType
import com.intellij.CommonBundle
import com.intellij.ide.wizard.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.observable.properties.ObservableProperty
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.roots.ui.configuration.*
import com.intellij.openapi.roots.ui.configuration.SdkListItem.*
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.*
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsContexts
import com.intellij.testFramework.requireIs
import com.intellij.ui.dsl.builder.Align.Companion.FILL
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.Consumer
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.util.*
import javax.swing.*

class CangJieNewProjectWizard : LanguageNewProjectWizard {
    override val name = "CangJie"

    companion object {

        fun generateProject(
            project: Project,

            projectModuleName: String?,
            groupName: String?,
            projectTypeStr: String?,
            sdk: Sdk?
        ) {
            NewProjectWizardModuleBuilder().apply {
//                wizardContext

                projectSdk = sdk
                moduleName = projectModuleName
                organizationName = groupName
                projectType = projectTypeStr


//                wizardContext.projectJdk = sdk
//                wizardContext.projectName = projectName
//                wizardContext.

            }

                .commit(project, null, null)

        }
    }

    override fun createStep(parent: NewProjectWizardLanguageStep): NewProjectWizardStep = Step(parent)


    class Step(parent: NewProjectWizardLanguageStep) : AbstractNewProjectWizardStep(parent) {
        //        private val label = JLabel("仓颉项目创建向导")


        //        仓颉Sdk 下拉框
        private val cangJieSdkComboBox: CangJieSdkCombox


        private val sdkListModelBuilder: SdkListModelBuilder


        //        项目类型
        private val projectTypeComboBox: ComboBox<CangJieProjectTypeItem> = ComboBox<CangJieProjectTypeItem>().apply {

//            添加条目
            addItem(CangJieProjectTypeItem("executable", "可执行程序"))
            addItem(CangJieProjectTypeItem("static", "静态库"))
            addItem(CangJieProjectTypeItem("dynamic", "动态库"))


        }


        //        模块名
        private val moduleNameTextField: JTextField = JTextField().apply {

        }


        //        组织名
        private val groupIdTextField: JTextField = JTextField().apply {

        }


        override fun setupProject(project: Project) {


            if ((cangJieSdkComboBox.selectedItem as CangJieSdkCombox.CangJieSdkItem).sdk == null) {
                return
            }

            generateProject(
                project,
                projectModuleName = moduleNameTextField.text,
                groupName = groupIdTextField.text,
                projectTypeStr = (projectTypeComboBox.selectedItem as CangJieProjectTypeItem).type,

                sdk = (cangJieSdkComboBox.selectedItem as CangJieSdkCombox.CangJieSdkItem).sdk
            )


        }


        init {
            val project = ProjectManager.getInstance().defaultProject
            val model = ProjectSdksModel()
            model.reset(project)


//            初始化仓颉sdk下拉框
            sdkListModelBuilder = SdkListModelBuilder(
                project,
                model,
                { type -> type is CangJieSdkType },
                {
//                添加sdk
                        type ->


                    type is CangJieSdkType
                },
                null
            )
//            sdkListModelBuilder.showProjectSdkItem()

            cangJieSdkComboBox = CangJieSdkCombox(project, sdkListModelBuilder, null).apply {
                reloadModel()

//                选择第一个sdk
                if (itemCount > 0) {
                    setSelectedItem(getItemAt(0))

                }


            }


//            cangjieSdkLabel = LabeledComponent.create(cangJieSdkComboBox, "仓颉SDK")


//            cangjieSdkLabel.labelLocation = "West"
//            cangjieSdkLabel.component = cangJieSdkComboBox


//            设置模块和组织名为必填
//            moduleNameTextField.addFocusListener(object : FocusAdapter() {
//                override fun focusLost(e: FocusEvent?) {
//                    if (moduleNameTextField.text.isEmpty()) {
//                        JOptionPane.showMessageDialog(null, "This field is required")
//                    }
//                }
//            })

        }

        override fun setupUI(builder: Panel) {


            with(builder) {
                row("CangJie Sdk:") {
//                    val sdksModel = ProjectSdksModel()
//
//                    Disposer.register(context.disposable) {
//                        sdksModel.disposeUIResources()
//                    }

                    cell(cangJieSdkComboBox)
                        .validationOnApply {
                            validateSdk(
                                (cangJieSdkComboBox.selectedItem as CangJieSdkCombox.CangJieSdkItem).sdk
//                                ,    sdksModel
                            )
                        }
//                        .onApply {
//                            context.projectJdk =
//                                (cangJieSdkComboBox.selectedItem as CangJieSdkCombox.CangJieSdkItem).sdk
//                        }
                        .columns(COLUMNS_MEDIUM)
                        .component
                }.bottomGap(BottomGap.SMALL)
                row("Project Type:") {
                    cell(projectTypeComboBox)
                        .columns(COLUMNS_MEDIUM)
                        .component
                }.bottomGap(BottomGap.SMALL)
                row("Module Name:") {
                    cell(moduleNameTextField)
                        .columns(COLUMNS_MEDIUM)
                        //                        .validationOnApply {
//                            validateModuleName(moduleNameTextField.text)
//                        }
                        .component
                }.bottomGap(BottomGap.SMALL)
                row("Group Name:") {
                    cell(groupIdTextField)
                        .columns(COLUMNS_MEDIUM)
//                        .validationOnApply {
//                            validateGroupName(groupIdTextField.text)
//                        }
                        .component
                }.bottomGap(BottomGap.SMALL)
            }


        }


    }


}

interface BuildSystemCangJieNewProjectWizard : NewProjectWizardMultiStepFactory<CangJieNewProjectWizard.Step> {
    companion object {
        var EP_NAME =
            ExtensionPointName<BuildSystemCangJieNewProjectWizard>("com.intellij.newProjectWizard.CangJie.buildSystem")
    }
}

interface BuildSystemCangJieNewProjectWizardData : BuildSystemNewProjectWizardData {

    companion object {

        val KEY =
            Key.create<BuildSystemCangJieNewProjectWizardData>(BuildSystemCangJieNewProjectWizardData::class.java.name)

        @JvmStatic
        val NewProjectWizardStep.CangJieBuildSystemData: BuildSystemCangJieNewProjectWizardData?
            get() = data.getUserData(KEY)
    }
}
//fun NewProjectWizardStep.setupKmpWizardLinkUI(builder: Panel) {
//    builder.row {
//        text(
//            CangJieNewProjectWizardUIBundle.message("project.wizard.new.project.cangjie.comment"),
//            action = HyperlinkEventAction {
//                context.requestSwitchTo(NewProjectWizardModuleBuilder.MODULE_BUILDER_ID) { }
//            })
//            .applyToComponent { foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND }
//
//        topGap(TopGap.SMALL)
//        bottomGap(BottomGap.SMALL)
//    }
//}

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
                    ?: throw RuntimeException("Failed to unwrap " + item.javaClass.getName() + ": " + item)
            }
            throw RuntimeException("Failed to unwrap " + item.javaClass.getName() + ": " + item)
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



