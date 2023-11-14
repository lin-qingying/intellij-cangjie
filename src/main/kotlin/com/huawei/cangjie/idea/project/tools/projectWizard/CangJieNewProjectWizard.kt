package com.huawei.cangjie.idea.project.tools.projectWizard

import com.huawei.cangjie.lang.sdk.CangJieSdkType
import com.intellij.ide.wizard.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ui.configuration.*
import com.intellij.openapi.roots.ui.configuration.SdkListItem.*
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.ComboBoxPopupState
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.Align.Companion.FILL
import com.intellij.ui.dsl.builder.Panel
import com.intellij.util.Consumer
import java.util.*
import javax.swing.AbstractListModel
import javax.swing.ComboBoxModel
import javax.swing.JButton
import javax.swing.ListModel

class CangJieNewProjectWizard : LanguageNewProjectWizard {
    override val name = "CangJie"

    override fun createStep(parent: NewProjectWizardLanguageStep): NewProjectWizardStep = Step(parent)


    //    class Step(parent: NewProjectWizardLanguageStep) :
//        AbstractNewProjectWizardMultiStep<Step, BuildSystemCangJieNewProjectWizard>(
//            parent,
//            BuildSystemCangJieNewProjectWizard.EP_NAME
//        ),
//        LanguageNewProjectWizardData by parent,
//        BuildSystemCangJieNewProjectWizardData {
//
//        override val self = this
//        override val label = "Build 系统:"
//        override val buildSystemProperty by ::stepProperty
//        override var buildSystem by ::step
//
//        override fun createAndSetupSwitcher(builder: Row): SegmentedButton<String> {
//            return super.createAndSetupSwitcher(builder)
//                .whenItemSelectedFromUi { logBuildSystemChanged() }
//        }
//
//        override fun setupProject(project: Project) {
//            super.setupProject(project)
//
//            logBuildSystemFinished()
//        }
//
//        init {
//            data.putUserData(BuildSystemCangJieNewProjectWizardData.KEY, this)
//        }
//    }
    class Step(parent: NewProjectWizardLanguageStep) : AbstractNewProjectWizardStep(parent) {
//        private val label = JLabel("仓颉项目创建向导")


        //        仓颉Sdk 下拉框
        private val cangJieSdkComboBox: CangJieSdkCombox
        private val cangjieSdkLabel: LabeledComponent<CangJieSdkCombox>

        private val sdkListModelBuilder: SdkListModelBuilder

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

//                为添加sdk按钮绑定事件
                addActionListener {
                    val selectedSdk = selectedItem as CangJieSdkCombox.CangJieSdkItem?
                    if (selectedSdk != null) {
                        if (selectedSdk.sdk == null) {

// 调用Sdktype添加sdk
                            ApplicationManager.getApplication().invokeLater {

                                SdkPopupFactory.newBuilder().withProject(project)
                                    .withSdkTypeFilter { type: SdkTypeId? -> type is CangJieSdkType }
                                    .buildEditorNotificationPanelHandler()
                            }
                        }
                    }

                }


            }




            cangjieSdkLabel = LabeledComponent.create(cangJieSdkComboBox, "仓颉SDK")


            cangjieSdkLabel.labelLocation = "West"
//            cangjieSdkLabel.component = cangJieSdkComboBox


        }

        override fun setupUI(builder: Panel) {
            with(builder) {
                row {
//                    cell(label)
//                        .align(FILL)
                    cell(cangjieSdkLabel).align(FILL)
                }
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
            if (onNewSdkAdded != null) {
                onNewSdkAdded.consume(sdk)
            }
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

    private val myEditButton: JButton? = null

    //    private fun updateEditButton() {
//        if (myEditButton != null) {
//            val selectedItem: CangJieSdkItem? = selectedItem as CangJieSdkItem?
//            if (selectedItem is ProjectSdkComboBoxItem && project != null) {
//                myEditButton.setEnabled(
//                    ProjectStructureConfigurable.getInstance(project).getProjectJdksModel().getProjectSdk() != null
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

    open class ActualSdkComboBoxItem(val jdk: Sdk) : CangJieSdkItem(), SelectableComboBoxItem {

        override fun toString(): String {
            return jdk.name
        }

        override val sdkName: String?
            get() = jdk.name

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val item = other as ActualSdkComboBoxItem
            return jdk == item.jdk
        }

        override fun hashCode(): Int {
            return Objects.hash(jdk)
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
        val sdk: Sdk?
            get() = null
        open val sdkName: String?
            get() = null
    }


}

