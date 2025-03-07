/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.ide.completion

import cn.cangnova.cangjie.analyzer.analyzeInContext
import cn.cangnova.cangjie.descriptors.ModuleDescriptor
import cn.cangnova.cangjie.ide.cache.trackers.CangJieCodeBlockModificationListener
import cn.cangnova.cangjie.ide.cache.trackers.PureCangJieCodeBlockModificationListener
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.siblings
import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.resolve.CompositeBindingContext
import cn.cangnova.cangjie.resolve.ResolutionFacade
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import cn.cangnova.cangjie.resolve.getDataFlowInfoBefore
import cn.cangnova.cangjie.resolve.lazy.BodyResolveMode
import cn.cangnova.cangjie.resolve.scopes.LexicalScope
import cn.cangnova.cangjie.resolve.scopes.getResolutionScope
import cn.cangnova.cangjie.psi.psiUtil.anyDescendantOfType
import cn.cangnova.cangjie.utils.firstIsInstance
import cn.cangnova.cangjie.utils.firstIsInstanceOrNull
import cn.cangnova.cangjie.psi.psiUtil.parentsWithSelf
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

        val bindingContext =  resolutionFacade.analyze(
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
