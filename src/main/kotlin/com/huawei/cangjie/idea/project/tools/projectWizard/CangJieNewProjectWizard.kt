package com.huawei.cangjie.idea.project.tools.projectWizard

import com.huawei.cangjie.idea.project.tools.projectWizard.wizard.CangJieNewProjectWizardUIBundle
import com.intellij.ide.projectWizard.NewProjectWizardCollector.BuildSystem.logBuildSystemChanged
import com.intellij.ide.projectWizard.NewProjectWizardCollector.BuildSystem.logBuildSystemFinished
import com.intellij.ide.wizard.*
import com.intellij.ide.wizard.GitNewProjectWizardData.Companion.gitData
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.builder.Align.Companion.FILL
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JLabel

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
        private val label = JLabel("仓颉项目创建向导")

        override fun setupUI(builder: Panel) {
            with(builder) {
                row {
                    cell(label)
                        .align(FILL)
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
