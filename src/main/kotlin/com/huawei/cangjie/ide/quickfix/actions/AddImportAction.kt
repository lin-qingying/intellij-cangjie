//package com.huawei.cangjie.ide.quickfix.actions
//
//import com.intellij.application.options.editor.AutoImportOptionsConfigurable
//
//import com.intellij.codeInsight.CodeInsightWorkspaceSettings
//import com.intellij.codeInsight.actions.OptimizeImportsProcessor
//
//import com.intellij.codeInsight.hint.QuestionAction
//import com.intellij.codeInsight.navigation.hidePopupIfDumbModeStarts
//import com.intellij.ide.util.DefaultPsiElementCellRenderer
//
//import com.intellij.openapi.application.ApplicationManager
//import com.intellij.openapi.application.ReadAction
//import com.intellij.openapi.command.WriteCommandAction
//import com.intellij.openapi.diagnostic.Logger
//import com.intellij.openapi.editor.Editor
//import com.intellij.openapi.editor.ScrollType
//import com.intellij.openapi.module.Module
//import com.intellij.openapi.options.ShowSettingsUtil
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.ui.popup.JBPopup
//import com.intellij.openapi.ui.popup.JBPopupFactory
//import com.intellij.openapi.ui.popup.PopupStep
//import com.intellij.openapi.ui.popup.util.BaseListPopupStep
//import com.intellij.openapi.util.ThrowableComputable
//import com.intellij.psi.PsiDocumentManager
//import com.intellij.psi.PsiReference
//import com.intellij.psi.search.GlobalSearchScope
//
//import com.intellij.psi.statistics.StatisticsManager
//import com.intellij.ui.popup.list.GroupedItemsListRenderer
//import com.intellij.util.IncorrectOperationException
//import com.intellij.util.concurrency.annotations.RequiresReadLock
//import com.intellij.util.containers.ContainerUtil
//import java.awt.BorderLayout
//import java.util.*
//import java.util.function.Function
//import java.util.stream.Collectors
//import javax.accessibility.AccessibleContext
//import javax.swing.Icon
//import javax.swing.JList
//import javax.swing.JPanel
//import javax.swing.ListCellRenderer
//
//
//
//
//
//class AddImportAction(
//    private val myProject: Project,
//    private val myReference: PsiReference,
//    editor: Editor,
//    vararg targetClasses: PsiClass
//) : QuestionAction {
//    private val myTargetClasses: Array<PsiClass> = targetClasses
//    private val myEditor = editor
//
//    override fun execute(): Boolean {
//        PsiDocumentManager.getInstance(myProject).commitAllDocuments()
//
//        if (!myReference.element.isValid) {
//            return false
//        }
//
//        for (myTargetClass in myTargetClasses) {
//            if (!myTargetClass.isValid()) {
//                return false
//            }
//        }
//
//        if (myTargetClasses.size == 1) {
//            addImport(myReference, myTargetClasses[0])
//        } else {
//            chooseClassAndImport()
//        }
//        return true
//    }
//
//    private fun chooseClassAndImport() {
//        CodeInsightUtil.sortIdenticalShortNamedMembers(myTargetClasses, myReference)
//
//        val step: BaseListPopupStep<PsiClass> =
//            object : BaseListPopupStep<Any?>(QuickFixBundle.message("class.to.import.chooser.title"), myTargetClasses) {
//                private val names: Map<PsiClass, String> = Arrays.stream(myTargetClasses)
//                    .collect(
//                        Collectors.toMap<T, K?, U>(
//                            Function<T, K?> { t: T? -> t },
//                            Function<T, U> { t: T ->
//                                val name: String = t.getQualifiedName()
//                                name ?: ""
//                            })
//                    )
//
//                override fun isAutoSelectionEnabled(): Boolean {
//                    return false
//                }
//
//                override fun isSpeedSearchEnabled(): Boolean {
//                    return true
//                }
//
//                override fun onChosen(selectedValue: PsiClass?, finalChoice: Boolean): PopupStep<*>? {
//                    if (selectedValue == null) {
//                        return FINAL_CHOICE
//                    }
//
//                    if (finalChoice) {
//                        return doFinalStep {
//                            PsiDocumentManager.getInstance(myProject).commitAllDocuments()
//                            addImport(myReference, selectedValue)
//                        }
//                    }
//
//                    return getExcludesStep(myProject, selectedValue.getQualifiedName())
//                }
//
//                override fun hasSubstep(selectedValue: PsiClass?): Boolean {
//                    return true
//                }
//
//                override fun getTextFor(value: PsiClass): String {
//                    return names.getOrDefault(value, "")
//                }
//
//                override fun getIconFor(aValue: PsiClass): Icon {
//                    return ReadAction.compute<T, E>(ThrowableComputable<T, E> { aValue.getIcon(0) })
//                }
//            }
//        val popup: JBPopup = JBPopupFactory.getInstance().createListPopup(
//            myProject, step
//        ) { superRenderer: ListCellRenderer<*> ->
//            val baseRenderer = superRenderer as GroupedItemsListRenderer<Any>
//            val psiRenderer: ListCellRenderer<Any> = DefaultPsiElementCellRenderer()
//            ListCellRenderer { list: JList<*>?, value: Any, index: Int, isSelected: Boolean, cellHasFocus: Boolean ->
//                baseRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
//                val panel: JPanel = object : JPanel(BorderLayout()) {
//                    private val myAccessibleContext: AccessibleContext? =
//                        baseRenderer.accessibleContext
//
//                    override fun getAccessibleContext(): AccessibleContext {
//                        if (myAccessibleContext == null) {
//                            return@ListCellRenderer super.getAccessibleContext()
//                        }
//                        return@ListCellRenderer myAccessibleContext
//                    }
//                }
//                panel.add(baseRenderer.nextStepLabel, BorderLayout.EAST)
//                panel.add(psiRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus))
//                panel
//            }
//        }
//
//        hidePopupIfDumbModeStarts(popup, myProject)
//        popup.showInBestPositionFor(myEditor)
//    }
//
//    private fun addImport(ref: PsiReference, targetClass: PsiClass) {
//        getInstance.getInstance(myProject).withAlternativeResolveEnabled(Runnable {
//            if (!ref.element.isValid || !targetClass.isValid()) {
//                return@withAlternativeResolveEnabled
//            }
//            StatisticsManager.getInstance().incUseCount(JavaStatisticsManager.createInfo(null, targetClass))
//            WriteCommandAction.runWriteCommandAction(
//                myProject, QuickFixBundle.message("add.import"), null,
//                { doAddImport(ref, targetClass) },
//                ref.element.containingFile
//            )
//        })
//    }
//
//    private fun doAddImport(ref: PsiReference, targetClass: PsiClass) {
//        try {
//            bindReference(ref, targetClass)
//            if (CodeInsightWorkspaceSettings.getInstance(myProject).isOptimizeImportsOnTheFly) {
//                val document = myEditor.document
//                val psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(document)
//                OptimizeImportsProcessor(myProject, psiFile!!).runWithoutProgress()
//            }
//        } catch (e: IncorrectOperationException) {
//            LOG.error(e)
//        }
//        myEditor.scrollingModel.scrollToCaret(ScrollType.RELATIVE)
//    }
//
//    protected fun bindReference(ref: PsiReference, targetClass: PsiClass) {
//        ref.bindToElement(targetClass)
//    }
//
//    companion object {
//        private val LOG = Logger.getInstance(
//            AddImportAction::class.java
//        )
//
//        @RequiresReadLock
//        fun create(
//            editor: Editor,
//            module: Module,
//            reference: PsiReference,
//            className: String
//        ): AddImportAction? {
//            val project = module.project
//            return getInstance.getInstance(project)
//                .computeWithAlternativeResolveEnabled<AddImportAction, RuntimeException>(
//                    ThrowableComputable<AddImportAction, RuntimeException> {
//                        val scope = GlobalSearchScope.moduleWithLibrariesScope(module)
//                        val aClass: PsiClass = JavaPsiFacade.getInstance(project).findClass(className, scope)
//                            ?: return@computeWithAlternativeResolveEnabled null
//                        AddImportAction(project, reference, editor, aClass)
//                    })
//        }
//
//        fun getExcludesStep(project: Project, qname: String?): PopupStep<*>? {
//            if (qname == null) return PopupStep.FINAL_CHOICE
//
//            val toExclude = getAllExcludableStrings(qname)
//
//            return object : BaseListPopupStep<String>(null, toExclude) {
//                override fun getTextFor(value: String): String {
//                    return JavaBundle.message("exclude.0.from.auto.import", value)
//                }
//
//                override fun onChosen(selectedValue: String, finalChoice: Boolean): PopupStep<*>? {
//                    if (finalChoice && selectedValue != null) {
//                        excludeFromImport(project, selectedValue)
//                    }
//
//                    return super.onChosen(selectedValue, finalChoice)
//                }
//            }
//        }
//
//        fun excludeFromImport(project: Project, prefix: String) {
//            ApplicationManager.getApplication().invokeLater {
//                if (project.isDisposed) return@invokeLater
//                val configurable = AutoImportOptionsConfigurable(project)
//                ShowSettingsUtil.getInstance().editConfigurable(project, configurable) {
//                    val options: JavaAutoImportOptions = ContainerUtil.findInstance(
//                        configurable.configurables,
//                        JavaAutoImportOptions::class.java
//                    )
//                    options.addExcludePackage(prefix)
//                }
//            }
//        }
//
//        fun getAllExcludableStrings(qname: String): List<String> {
//            var qname = qname
//            val toExclude: MutableList<String> = ArrayList()
//            while (true) {
//                toExclude.add(qname)
//                val i = qname.lastIndexOf('.')
//                if (i < 0 || i == qname.indexOf('.')) break
//                qname = qname.substring(0, i)
//            }
//            return toExclude
//        }
//    }
//}
