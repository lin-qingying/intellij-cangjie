package org.cangnova.cangjie.ide.base.analysisApiPlatform

import com.intellij.injected.editor.DocumentWindow
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.WriteActionListener
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.impl.PsiModificationTrackerImpl
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import com.intellij.psi.impl.PsiTreeChangeEventImpl.PsiEventType
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtilBase
import com.intellij.psi.util.parentOfType
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.KotlinAnalysisInWriteActionListener
import org.cangnova.cangjie.analysis.api.platform.analysisMessageBus
import org.cangnova.cangjie.analysis.api.platform.modification.CaElementModificationType
import org.cangnova.cangjie.analysis.api.platform.modification.CaSourceModificationLocality
import org.cangnova.cangjie.analysis.api.platform.modification.CaSourceModificationService
import org.cangnova.cangjie.analysis.api.platform.modification.publishGlobalSourceOutOfBlockModificationEvent
import org.cangnova.cangjie.analysis.api.util.withPsiEntry
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import java.util.WeakHashMap

/**
 * 对齐 Kotlin K2 的 PSI tree-change 宿主链。
 *
 * 该服务在单次写动作内记住已经处理过的文件和是否已经发过 module/global out-of-block 事件，
 * 以避免在密集编辑期间重复命中 analysis invalidation 链。
 */
@OptIn(CaPlatformInterface::class)
@Service(Service.Level.PROJECT)
class CangJieIdeOutOfBlockModificationService(private val project: Project) : Disposable {
    private val fileProcessingLimit: Int by lazy(LazyThreadSafetyMode.PUBLICATION) {
        Registry.intValue(FILE_PROCESSING_LIMIT_KEY, DEFAULT_FILE_PROCESSING_LIMIT)
    }

    private val threadLocalContext = ThreadLocal.withInitial { TreeChangeHandlingContext() }

    init {
        ApplicationManagerEx.getApplicationEx().addWriteActionListener(ContextRemovalWriteActionListener(), this)

        project.analysisMessageBus
            .connect(this)
            .subscribe(KotlinAnalysisInWriteActionListener.TOPIC, ContextRemovalAnalysisInWriteActionListener())
    }

    private fun handleTreeChangeEvent(event: PsiTreeChangeEventImpl) {
        val context = threadLocalContext.get()
        if (context.state == TreeChangeProcessingState.GlobalEventPublished) {
            return
        }

        if (preprocessEvent(event, context)) {
            return
        }

        val rootElement = event.parent ?: return
        if (preprocessRootElement(event, rootElement, context)) {
            return
        }

        val containingFile = rootElement.containingFile
        if (containingFile == null) {
            processElementModification(event, rootElement, containingFile = null, processingState = null)
            return
        }

        if (containingFile is CjCodeFragment && containingFile.context?.isValid == false) {
            return
        }

        when (val currentState = context.state) {
            is TreeChangeProcessingState.Accepting -> {
                when (currentState.fileStates[containingFile]) {
                    null -> {
                        if (currentState.fileCount < fileProcessingLimit) {
                            currentState.fileStates[containingFile] = TreeChangeFileState.Processing
                            currentState.fileCount += 1
                            processElementModification(event, rootElement, containingFile, currentState)
                        } else {
                            publishGlobalModificationEvent(context)
                        }
                    }

                    TreeChangeFileState.Processing -> {
                        processElementModification(event, rootElement, containingFile, currentState)
                    }

                    TreeChangeFileState.ModuleEventPublished -> {}
                }
            }

            TreeChangeProcessingState.GlobalEventPublished -> {}
        }
    }

    private fun preprocessEvent(event: PsiTreeChangeEventImpl, context: TreeChangeHandlingContext): Boolean {
        val eventCode = event.code
        if (!PsiModificationTrackerImpl.canAffectPsi(event) ||
            event.isGenericChange ||
            eventCode == PsiEventType.BEFORE_CHILD_ADDITION
        ) {
            return true
        }

        if (event.isGlobalChange()) {
            publishGlobalModificationEvent(context)
            return true
        }

        return false
    }

    private fun preprocessRootElement(
        event: PsiTreeChangeEventImpl,
        rootElement: PsiElement,
        context: TreeChangeHandlingContext,
    ): Boolean {
        val eventCode = event.code

        if (eventCode != PsiEventType.BEFORE_CHILD_REPLACEMENT && isInjectionChange(rootElement)) {
            publishGlobalModificationEvent(context)
            return true
        }

        if (!rootElement.isPhysical) {
            return true
        }

        return false
    }

    private fun processElementModification(
        event: PsiTreeChangeEventImpl,
        rootElement: PsiElement,
        containingFile: PsiFile?,
        processingState: TreeChangeProcessingState.Accepting?,
    ) {
        val modificationType = when (event.code) {
            PsiEventType.CHILD_ADDED -> CaElementModificationType.ElementAdded
            PsiEventType.CHILD_REMOVED -> {
                val removedElement = event.child
                    ?: errorWithAttachment("A ${PsiEventType.CHILD_REMOVED} PSI tree change event should have a child element") {
                        withEntry("psiTreeChangeEvent", event.toString()) { it }
                        withPsiEntry("rootElement", rootElement)
                    }
                CaElementModificationType.ElementRemoved(removedElement)
            }

            else -> CaElementModificationType.Unknown
        }

        val child = when (event.code) {
            PsiEventType.CHILD_REMOVED -> rootElement
            PsiEventType.BEFORE_CHILD_REPLACEMENT -> event.oldChild
            else -> event.child
        }
        val targetElement = child ?: rootElement

        val sourceModificationService = CaSourceModificationService.getInstance(project)
        val locality = sourceModificationService.detectLocality(targetElement, modificationType)
        sourceModificationService.handleInvalidation(targetElement, locality)

        if (containingFile != null && processingState != null && locality is CaSourceModificationLocality.OutOfBlock) {
            processingState.fileStates[containingFile] = TreeChangeFileState.ModuleEventPublished
        }
    }

    private fun isInjectionChange(rootElement: PsiElement): Boolean {
        val injectionHost = rootElement.parentOfType<PsiLanguageInjectionHost>()
        if (injectionHost == null) {
            return false
        }

        @Suppress("DEPRECATION")
        val injectedDocuments = InjectedLanguageUtilBase.getCachedInjectedDocuments(rootElement.containingFile)
        if (injectedDocuments.isEmpty()) {
            return false
        }

        val textRange = rootElement.textRange
        return injectedDocuments.any { it.containsInjectionAt(textRange) }
    }

    private fun DocumentWindow.containsInjectionAt(textRange: TextRange): Boolean =
        hostRanges.any { textRange.intersects(it) }

    private fun publishGlobalModificationEvent(context: TreeChangeHandlingContext) {
        project.publishGlobalSourceOutOfBlockModificationEvent()
        context.state = TreeChangeProcessingState.GlobalEventPublished
    }

    override fun dispose() {
    }

    class OutOfBlockTreeChangePreprocessor(private val project: Project) : PsiTreeChangePreprocessor {
        override fun treeChanged(event: PsiTreeChangeEventImpl) {
            if (project.isDefault) {
                return
            }

            getInstance(project).handleTreeChangeEvent(event)
        }
    }

    private inner class ContextRemovalWriteActionListener : WriteActionListener {
        override fun writeActionStarted(action: Class<*>) {
            threadLocalContext.remove()
        }

        override fun writeActionFinished(action: Class<*>) {
            threadLocalContext.remove()
        }
    }

    private inner class ContextRemovalAnalysisInWriteActionListener : KotlinAnalysisInWriteActionListener {
        override fun onEnteringAnalysisInWriteAction() {
            threadLocalContext.remove()
        }

        override fun afterLeavingAnalysisInWriteAction() {
            threadLocalContext.remove()
        }
    }

    companion object {
        const val FILE_PROCESSING_LIMIT_KEY: String = "cangjie.analysis.treeChangePreprocessor.fileProcessingLimit"

        const val DEFAULT_FILE_PROCESSING_LIMIT: Int = 5

        fun getInstance(project: Project): CangJieIdeOutOfBlockModificationService =
            project.getService(CangJieIdeOutOfBlockModificationService::class.java)
    }
}

private class TreeChangeHandlingContext(
    var state: TreeChangeProcessingState = TreeChangeProcessingState.Accepting(),
)

private sealed class TreeChangeFileState {
    data object Processing : TreeChangeFileState()

    data object ModuleEventPublished : TreeChangeFileState()
}

private sealed class TreeChangeProcessingState {
    class Accepting : TreeChangeProcessingState() {
        var fileCount: Int = 0

        val fileStates: MutableMap<PsiFile, TreeChangeFileState> = WeakHashMap()
    }

    data object GlobalEventPublished : TreeChangeProcessingState()
}

private fun PsiTreeChangeEventImpl.isGlobalChange(): Boolean = when (code) {
    PsiEventType.PROPERTY_CHANGED -> propertyName === PsiTreeChangeEvent.PROP_UNLOADED_PSI
    PsiEventType.CHILD_MOVED -> oldParent is PsiDirectory || newParent is PsiDirectory
    else -> parent is PsiDirectory
}
