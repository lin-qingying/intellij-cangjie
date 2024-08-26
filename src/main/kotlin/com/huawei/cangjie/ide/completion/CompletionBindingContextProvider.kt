package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.analyzer.analyzeInContext
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.ide.cache.trackers.CangJieCodeBlockModificationListener
import com.huawei.cangjie.ide.cache.trackers.PureCangJieCodeBlockModificationListener
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.siblings
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.CompositeBindingContext
import com.huawei.cangjie.resolve.ResolutionFacade
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.getDataFlowInfoBefore
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.resolve.scopes.getResolutionScope
import com.huawei.cangjie.psi.psiUtil.anyDescendantOfType
import com.huawei.cangjie.utils.firstIsInstance
import com.huawei.cangjie.utils.firstIsInstanceOrNull
import com.huawei.cangjie.psi.psiUtil.parentsWithSelf
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.parentsOfType
import org.jetbrains.annotations.TestOnly
import java.lang.ref.SoftReference

@Service(Service.Level.PROJECT)
class CompletionBindingContextProvider(project: Project) {
    private val LOG = Logger.getInstance(CompletionBindingContextProvider::class.java)

    @get:TestOnly
    var TEST_LOG: StringBuilder? = null

    companion object {
        fun getInstance(project: Project): CompletionBindingContextProvider = project.service()

        var ENABLED = true
    }

    private data class PsiElementData(val element: PsiElement, val level: Int)

    private class CompletionData(
        val container: CjExpression,
        val prevStatement: CjExpression?,
        val psiElementsBeforeAndAfter: List<PsiElementData>,
        val bindingContext: BindingContext,
        val moduleDescriptor: ModuleDescriptor,
        val statementResolutionScope: LexicalScope,
        val statementDataFlowInfo: DataFlowInfo,
        val debugText: String
    ) {
        init {
            require(container is CjBlockExpression || container is CjDeclarationWithBody) {
                "Container was of class ${container::class}"
            }
        }
    }

    private class DataHolder {
        private var reference: SoftReference<CompletionData>? = null

        var data: CompletionData?
            get() = reference?.get()
            set(value) {
                reference = value?.let { SoftReference(it) }
            }
    }

    private var prevCompletionDataCache: CachedValue<DataHolder> =
        CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result.create(
                    DataHolder(),
                    CangJieCodeBlockModificationListener.getInstance(project).cangjieOutOfCodeBlockTracker
                )
            },
            false
        )

    private fun log(message: String) {
        TEST_LOG?.append(message)
        LOG.debug(message)
    }

    private fun _getBindingContext(position: PsiElement, resolutionFacade: ResolutionFacade): BindingContext {
        val (inStatement, container) =
            position.findStatementInBlock()
                ?: position.findSingleExpressionBody()
                ?: (null to null)

        val prevStatement =
            inStatement?.siblings(forward = false, withItself = false)?.firstIsInstanceOrNull<CjExpression>()
        val modificationScope =
            inStatement?.let { PureCangJieCodeBlockModificationListener.getInsideCodeBlockModificationScope(it)?.element }

        val psiElementsBeforeAndAfter =
            modificationScope?.let { collectPsiElementsBeforeAndAfter(modificationScope, inStatement) }

        val prevCompletionData = prevCompletionDataCache.value.data
        when {
            prevCompletionData == null ->
                log("No up-to-date data from previous completion\n")

            container != prevCompletionData.container ->
                log("Not in the same container\n")

            prevStatement != prevCompletionData.prevStatement ->
                log("Previous statement is not the same\n")

            psiElementsBeforeAndAfter != prevCompletionData.psiElementsBeforeAndAfter ->
                log("PSI-tree has changed inside current scope\n")

            prevCompletionData.moduleDescriptor != resolutionFacade.moduleDescriptor ->
                log("ModuleDescriptor has been reset")

            inStatement.isTooComplex() ->
                log("Current statement is too complex to use optimization\n")

            else -> {
                log("Statement position is the same - analyzing only one statement:\n${inStatement.text.prependIndent("    ")}\n")
                LOG.debug("Reusing data from completion of \"${prevCompletionData.debugText}\"")

                //TODO: expected type?
                val statementContext = inStatement.analyzeInContext(
                    scope = prevCompletionData.statementResolutionScope,
                    contextExpression = container,
                    dataFlowInfo = prevCompletionData.statementDataFlowInfo,
                    isStatement = true
                )
                // we do not update prevCompletionDataCache because the same data should work
                return CompositeBindingContext.create(listOf(statementContext, prevCompletionData.bindingContext))
            }
        }

        val bindingContext =
            resolutionFacade.analyze(
                position.parentsWithSelf.firstIsInstance<CjElement>(),
                BodyResolveMode.PARTIAL_FOR_COMPLETION
            )
        prevCompletionDataCache.value.data = if (container != null && modificationScope != null) {
            val resolutionScope = inStatement.getResolutionScope(bindingContext, resolutionFacade)
            val dataFlowInfo = bindingContext.getDataFlowInfoBefore(inStatement)
            CompletionData(
                container,
                prevStatement,
                psiElementsBeforeAndAfter!!,
                bindingContext,
                resolutionFacade.moduleDescriptor,
                resolutionScope,
                dataFlowInfo,
                debugText = position.text
            )
        } else {
            null
        }

        return bindingContext
    }

    private fun collectPsiElementsBeforeAndAfter(scope: PsiElement, statement: CjExpression): List<PsiElementData> {
        return ArrayList<PsiElementData>().apply { addElementsInTree(scope, 0, statement) }
    }

    private fun MutableList<PsiElementData>.addElementsInTree(
        root: PsiElement,
        initialLevel: Int,
        skipSubtree: PsiElement
    ) {
        if (root == skipSubtree) return
        add(PsiElementData(root, initialLevel))
        var child = root.firstChild
        while (child != null) {
            if (child !is PsiWhiteSpace && child !is PsiComment && child !is PsiErrorElement) {
                addElementsInTree(child, initialLevel + 1, skipSubtree)
            }
            child = child.nextSibling
        }
    }

    fun getBindingContext(position: PsiElement, resolutionFacade: ResolutionFacade): BindingContext = if (ENABLED) {
        _getBindingContext(position, resolutionFacade)
    } else {
        resolutionFacade.analyze(
            position.parentsWithSelf.firstIsInstance<CjElement>(),
            BodyResolveMode.PARTIAL_FOR_COMPLETION
        )
    }


    private fun PsiElement.findStatementInBlock(): Pair<CjExpression, CjBlockExpression>? =
        parentsOfType<CjExpression>().firstNotNullOfOrNull { expression ->
            val parent = expression.parent
            if (parent is CjBlockExpression) {
                expression to parent
            } else {
                null
            }
        }

    private fun PsiElement.findSingleExpressionBody(): Pair<CjExpression, CjDeclaration>? =
        parentsOfType<CjExpression>().firstNotNullOfOrNull { expression ->
            val parent = expression.parent
            if (parent is CjDeclarationWithBody && parent.bodyExpression == expression) {
                expression to parent
            } else {
                null
            }
        }

    private fun CjExpression.isTooComplex(): Boolean {
        return anyDescendantOfType<CjBlockExpression> { it.statements.size > 1 }
    }
}
