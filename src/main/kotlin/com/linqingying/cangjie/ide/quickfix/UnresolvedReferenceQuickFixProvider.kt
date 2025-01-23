//// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
//package com.linqingying.cangjie.ide.quickfix
//
//import com.intellij.codeInsight.daemon.HighlightDisplayKey
//import com.intellij.codeInsight.daemon.QuickFixActionRegistrar
//import com.intellij.codeInsight.daemon.impl.HighlightInfo
//import com.intellij.codeInsight.daemon.impl.HighlightInfo.IntentionActionDescriptor
//import com.intellij.codeInsight.daemon.impl.UnresolvedReferenceQuickFixUpdaterImpl
//import com.intellij.codeInsight.intention.IntentionAction
//import com.intellij.openapi.application.ApplicationManager
//import com.intellij.openapi.editor.Editor
//import com.intellij.openapi.extensions.ExtensionPointName
//import com.intellij.openapi.fileEditor.FileEditorManager
//import com.intellij.openapi.project.DumbService
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.util.Condition
//import com.intellij.openapi.util.TextRange
//import com.intellij.psi.PsiFile
//import com.intellij.psi.PsiReference
//import com.intellij.util.ReflectionUtil
//import org.jetbrains.annotations.Nls
//
///**
// * Register implementation of this class as `com.intellij.codeInsight.unresolvedReferenceQuickFixProvider` extension to provide additional
// * quick fixes for 'Unresolved reference' problems.
// *
// *
// * For example, this line in the `plugin.xml` file:
// *
// *
// * `<codeInsight.unresolvedReferenceQuickFixProvider implementation="com.intellij.jarFinder.FindJarQuickFixProvider"/>`
// *
// * registers class `com.intellij.jarFinder.FindJarQuickFixProvider"` as an unresolved reference quick fix.
// *
// * @param <T> type of element you want register quick fixes for; for example, in Java language it may be [com.intellij.psi.PsiJavaCodeReferenceElement]
//</T> */
//abstract class UnresolvedReferenceQuickFixProvider<T : PsiReference?> {
//    abstract fun registerFixes(ref: T, registrar: QuickFixActionRegistrar)
//
//    abstract fun getReferenceClass(): Class<T>
//
//    companion object {
//        /**
//         * Call each registered [UnresolvedReferenceQuickFixProvider] for its quick fixes.
//         * Please don't use because it might be very expensive.
//         */
//        fun <T : PsiReference?> registerReferenceFixes(ref: T, registrar: QuickFixActionRegistrar) {
//            val dumbService = DumbService.getInstance(ref!!.element.project)
//            val referenceClass: Class<out PsiReference> = ref.javaClass
//            for (each in EP_NAME.extensionList) {
//                if (!dumbService.isUsableInCurrentContext(each)) {
//                    continue
//                }
//                if (ReflectionUtil.isAssignable(each.getReferenceClass(), referenceClass)) {
//                    (each as UnresolvedReferenceQuickFixProvider<T>).registerFixes(ref, registrar)
//                }
//            }
//        }
//
//        fun <T : PsiReference> registerReferenceFixes(ref: T, info: HighlightInfo) {
//            ApplicationManager.getApplication().assertIsNonDispatchThread()
//            ApplicationManager.getApplication().assertReadAccessAllowed()
//            val editor = getEditorFromPsiFile(ref.element.project, ref.element.containingFile)
//            val referenceElement = ref.element
//            if (referenceElement.project.isDisposed() || !referenceElement.containingFile.isValid() || editor?.isDisposed() == true
//                || DumbService.getInstance(referenceElement.project).isDumb || !referenceElement.isValid
//            ) {
//                // this will be restarted anyway on smart mode switch
//                return
//            }
//            val quickfixes = mutableListOf<IntentionActionDescriptor>()
//
//            registerReferenceFixes<T>(
//                ref, object : QuickFixActionRegistrarImpl(info) {
//                    override fun doRegister(
//                        action: IntentionAction,
//                        displayName: @Nls(capitalization = Nls.Capitalization.Sentence) String?,
//                        fixRange: TextRange?,
//                        key: HighlightDisplayKey?
//                    ) {
//                        quickfixes.add(
//                            IntentionActionDescriptor(
//                                action, null, displayName, null, key, myInfo.getProblemGroup(),
//                                myInfo.severity, fixRange
//                            )
//                        )
//                    }
//                })
//
//        }
//
//        fun <T : PsiReference> registerReferenceFixes(ref: T, builder: HighlightInfo.Builder) {
//            registerReferenceFixes(ref, builder.createUnconditionally())
//
//        }
//
//        private val EP_NAME =
//            ExtensionPointName.create<UnresolvedReferenceQuickFixProvider<*>>("com.linqingying.cangjie.ide.codeInsight.unresolvedReferenceQuickFixProvider")
//    }
//}
//
//open class QuickFixActionRegistrarImpl(val myInfo: HighlightInfo) : QuickFixActionRegistrar {
//    override fun register(action: IntentionAction) {
//        doRegister(action, null, null, null)
//    }
//
//    override fun register(fixRange: TextRange, action: IntentionAction, key: HighlightDisplayKey?) {
//        doRegister(action, HighlightDisplayKey.getDisplayNameByKey(key), fixRange, key)
//    }
//
//    open fun doRegister(
//        action: IntentionAction,
//        displayName: @Nls(capitalization = Nls.Capitalization.Sentence) String?,
//        fixRange: TextRange?,
//        key: HighlightDisplayKey?
//    ) {
//        myInfo.registerFix(action, null, displayName, fixRange, key)
//    }
//
//    override fun unregister(condition: Condition<in IntentionAction>) {
//        myInfo.unregisterQuickFix(condition)
//    }
//
//    override fun toString(): String {
//        return "QuickFixActionRegistrarImpl{myInfo=$myInfo}"
//    }
//}
//
//fun getEditorFromPsiFile(project: Project, psiFile: PsiFile): Editor? {
//    val fileEditorManager = FileEditorManager.getInstance(project)
//    val virtualFile = psiFile.virtualFile
//    val editors = fileEditorManager.getEditors(virtualFile)
//    return editors.firstOrNull() as? Editor
//}