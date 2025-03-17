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

package cn.cangnova.cangjie.ide.search.operators

import cn.cangnova.cangjie.ide.stubindex.resolve.CangJieBaseAnalysisBundle
import cn.cangnova.cangjie.highlighter.namedUnwrappedElement
import cn.cangnova.cangjie.ide.restrictToCangJieSources
import cn.cangnova.cangjie.ide.search.CangJieSearchUsagesSupport.SearchUtils.forceResolveReferences
import cn.cangnova.cangjie.ide.search.CangJieSearchUsagesSupport.SearchUtils.getReceiverTypeSearcherInfo
import cn.cangnova.cangjie.ide.search.ExpressionsOfTypeProcessor
import cn.cangnova.cangjie.ide.search.ExpressionsOfTypeProcessor.Companion.logPresentation
import cn.cangnova.cangjie.ide.search.ExpressionsOfTypeProcessor.Companion.testLog
import cn.cangnova.cangjie.ide.search.ideExtensions.CangJieReferencesSearchOptions
import cn.cangnova.cangjie.ide.search.ideExtensions.CangJieRequestResultProcessor
import cn.cangnova.cangjie.lang.CangJieFileType
import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.collectDescendantsOfType
import cn.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import cn.cangnova.cangjie.utils.OperatorNameConventions
import cn.cangnova.cangjie.utils.OperatorNameConventions.asOperatorName
import cn.cangnova.cangjie.utils.exceptions.OperatorConventions
import cn.cangnova.cangjie.utils.ifTrue
import cn.cangnova.cangjie.utils.safeAs
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.util.ProgressWrapper
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.search.*
import com.intellij.util.Processor


abstract class OperatorReferenceSearcher<TReferenceElement : CjElement>(
    protected val targetDeclaration: PsiElement,
    private val searchScope: SearchScope,
    private val consumer: Processor<in PsiReference>,
    private val optimizer: SearchRequestCollector,
    private val options: CangJieReferencesSearchOptions,
    private val wordsToSearch: List<String>
) {
    private val project = targetDeclaration.project
    protected fun processReferenceElement(element: TReferenceElement): Boolean {
        val reference = extractReference(element) ?: return true
        testLog { "Resolved ${logPresentation(element)}" }
        return if (reference.isReferenceTo(targetDeclaration)) {
            consumer.process(reference)
        } else {
            true
        }
    }

    fun run() {

        val (psiClass, containsTypeOrDerivedInside) = runReadAction {
            targetDeclaration.getReceiverTypeSearcherInfo()
        } ?: return

        val inProgress = SearchesInProgress.get()
        if (psiClass != null) {
            if (!inProgress.add(psiClass)) {
                testLog {
                    "ExpressionOfTypeProcessor is already started for ${psiClass.name}. Exit for operator ${
                        logPresentation(
                            targetDeclaration
                        )
                    }."
                }
                return
            }
        } else {
            if (!inProgress.add(targetDeclaration)) {
                testLog { "ExpressionOfTypeProcessor is already started for operator ${logPresentation(targetDeclaration)}. Exit." }
                return //TODO: it's not quite correct
            }
        }

        try {
            val extend = runReadAction { targetDeclaration.getStrictParentOfType<CjExtend>() }
            if (extend != null && psiClass == null) {
                ExpressionsOfTypeProcessor(
                    containsTypeOrDerivedInside,
                    psiClass,
                    searchScope,
                    project,
                    possibleMatchHandler = { expression -> processPossibleReceiverExpression(expression) },
                    possibleMatchesInScopeHandler = { searchScope -> doPlainSearch(searchScope) }
                )

                    .runByExtendBasic(targetDeclaration)
            } else {
                ExpressionsOfTypeProcessor(
                    containsTypeOrDerivedInside,
                    psiClass,
                    searchScope,
                    project,
                    possibleMatchHandler = { expression -> processPossibleReceiverExpression(expression) },
                    possibleMatchesInScopeHandler = { searchScope -> doPlainSearch(searchScope) }
                )

                    .run()
            }

        } finally {
            inProgress.remove(psiClass ?: targetDeclaration)
        }
    }

    /**
     * Invoked for all expressions that may have type matching receiver type of our operator
     */
    protected abstract fun processPossibleReceiverExpression(expression: CjExpression)

    private fun SearchScope.logPresentation(): String {
        return when (this) {
            searchScope -> "whole search scope"

            is LocalSearchScope -> {
                scope
                    .map { element ->
                        "    " + runReadAction {
                            when (element) {
                                is CjFunctionLiteral -> element.text
                                is CjMatchEntry -> {
                                    if (element.isElse)
                                        "CjMatchEntry \"else\""
                                    else
                                        "CjMatchEntry \"" + element.conditions.joinToString(", ") { it.text } + "\""
                                }

                                is CjNamedDeclaration -> element.node.elementType.toString() + ":" + element.name
                                else -> element.toString()
                            }
                        }
                    }
                    .toList()
                    .sorted()
                    .joinToString("\n", "LocalSearchScope:\n")
            }

            else -> this.displayName
        }

    }

    private fun doPlainSearch(scope: SearchScope) {
        testLog { "Used plain search of ${logPresentation(targetDeclaration)} in ${scope.logPresentation()}" }

        val progress = ProgressWrapper.unwrap(ProgressIndicatorProvider.getGlobalProgressIndicator())

        if (scope is LocalSearchScope) {
            for (element in scope.scope) {
                if (element is CjElement) {
                    runReadAction {
                        if (element.isValid) {
                            progress?.checkCanceled()
                            val refs = ArrayList<PsiReference>()
                            val elements = element.collectDescendantsOfType<CjElement> {
                                val ref = extractReference(it) ?: return@collectDescendantsOfType false
                                refs.add(ref)
                                true
                            }.ifEmpty { return@runReadAction }

                            // resolve all references at once
                            element.containingFile.safeAs<CjFile>()?.forceResolveReferences(elements)

                            for (ref in refs) {
                                progress?.checkCanceled()
                                if (ref.isReferenceTo(targetDeclaration)) {
                                    consumer.process(ref)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            scope as GlobalSearchScope
            if (wordsToSearch.isNotEmpty()) {
                val unwrappedElement = targetDeclaration.namedUnwrappedElement ?: return
                val resultProcessor = CangJieRequestResultProcessor(
                    unwrappedElement,
                    filter = { ref -> isReferenceToCheck(ref) },
                    options = options
                )
                wordsToSearch.forEach {
                    optimizer.searchWord(
                        it,
                        scope.restrictToCangJieSources(),
                        UsageSearchContext.IN_CODE,
                        true,
                        unwrappedElement,
                        resultProcessor
                    )
                }
            } else {
                val psiManager = PsiManager.getInstance(project)
                // we must unwrap progress indicator because ProgressWrapper does not do anything on changing text and fraction
                progress?.pushState()
                progress?.text = CangJieBaseAnalysisBundle.message("searching.for.implicit.usages")
                progress?.isIndeterminate = false

                try {
                    val files = runReadAction { FileTypeIndex.getFiles(CangJieFileType.INSTANCE, scope) }
                    for ((index, file) in files.withIndex()) {
                        progress?.checkCanceled()
                        runReadAction {
                            if (file.isValid) {
                                progress?.fraction = index / files.size.toDouble()
                                progress?.text2 = file.path
                                psiManager.findFile(file).safeAs<CjFile>()?.let {
                                    doPlainSearch(LocalSearchScope(it))
                                }
                            }
                        }
                    }
                } finally {
                    progress?.popState()
                }
            }
        }
    }

    protected abstract fun isReferenceToCheck(ref: PsiReference): Boolean
    protected abstract fun extractReference(element: CjElement): PsiReference?

    companion object {
        fun create(
            declaration: PsiElement,
            searchScope: SearchScope,
            consumer: Processor<in PsiReference>,
            optimizer: SearchRequestCollector,
            options: CangJieReferencesSearchOptions
        ): OperatorReferenceSearcher<*>? = declaration.isValid.ifTrue {
            createInReadAction(declaration, searchScope, consumer, optimizer, options)
        }

        private fun createInReadAction(
            declaration: PsiElement,
            searchScope: SearchScope,
            consumer: Processor<in PsiReference>,
            optimizer: SearchRequestCollector,
            options: CangJieReferencesSearchOptions
        ): OperatorReferenceSearcher<*>? {
            val functionName = when (declaration) {
                is CjNamedFunction -> declaration.name

                else -> null
            } ?: return null

            if (!Name.isValidIdentifier(functionName)) return null
            val name = functionName.asOperatorName()


            return createInReadAction(declaration, name, consumer, optimizer, options, searchScope)
        }

        private fun createInReadAction(
            declaration: PsiElement,
            name: Name,
            consumer: Processor<in PsiReference>,
            optimizer: SearchRequestCollector,
            options: CangJieReferencesSearchOptions,
            searchScope: SearchScope
        ): OperatorReferenceSearcher<*>? {


            if (!options.searchForOperatorConventions) return null

            val operator = declaration !is CjElement
                    || (declaration is CjNamedFunction && CangJiePsiHeuristics.isPossibleOperator(declaration))

            val binaryOp = OperatorConventions.BINARY_OPERATION_NAMES.inverse()[name]
            val assignmentOp = OperatorConventions.ASSIGNMENT_OPERATIONS.inverse()[name]
            val unaryOp = OperatorConventions.UNARY_OPERATION_NAMES.inverse()[name]

//          TODO  操作符搜索
            return when {
                operator && binaryOp != null -> {
                    val counterpartAssignmentOp =
                        OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.inverse()[binaryOp]
                    val operationTokens = listOfNotNull(binaryOp, counterpartAssignmentOp)
                    BinaryOperatorReferenceSearcher(
                        declaration,
                        operationTokens,
                        searchScope,
                        consumer,
                        optimizer,
                        options
                    )
                }

                operator && assignmentOp != null ->
                    BinaryOperatorReferenceSearcher(
                        declaration,
                        listOf(assignmentOp),
                        searchScope,
                        consumer,
                        optimizer,
                        options
                    )

                operator && unaryOp != null ->
                    UnaryOperatorReferenceSearcher(declaration, unaryOp, searchScope, consumer, optimizer, options)

                operator && name == OperatorNameConventions.INVOKE ->
                    InvokeOperatorReferenceSearcher(declaration, searchScope, consumer, optimizer, options)

                operator && name == OperatorNameConventions.GET ->
                    IndexingOperatorReferenceSearcher(
                        declaration,
                        searchScope,
                        consumer,
                        optimizer,
                        options,
                        isSet = false
                    )

                operator && name == OperatorNameConventions.SET ->
                    IndexingOperatorReferenceSearcher(
                        declaration,
                        searchScope,
                        consumer,
                        optimizer,
                        options,
                        isSet = true
                    )

//                operator && name == OperatorNameConventions.CONTAINS ->
//                    ContainsOperatorReferenceSearcher(declaration, searchScope, consumer, optimizer, options)

                name == OperatorNameConventions.EQUALS || name == OperatorNameConventions.NOT_EQUALS ->
                    BinaryOperatorReferenceSearcher(
                        declaration,
                        listOf(CjTokens.EQEQ, CjTokens.EXCLEQ),
                        searchScope,
                        consumer,
                        optimizer,
                        options
                    )

                operator && (name == OperatorNameConventions.COMPARE_LT
                        || name == OperatorNameConventions.COMPARE_LTEQ
                        || name == OperatorNameConventions.COMPARE_GT
                        || name == OperatorNameConventions.COMPARE_GTEQ

                        ) ->
                    BinaryOperatorReferenceSearcher(
                        declaration,
                        listOf(CjTokens.LT, CjTokens.GT, CjTokens.LTEQ, CjTokens.GTEQ),
                        searchScope,
                        consumer,
                        optimizer,
                        options
                    )

//                operator && name == OperatorNameConventions.ITERATOR ->
//                    IteratorOperatorReferenceSearcher(declaration, searchScope, consumer, optimizer, options)

//                operator && (name == OperatorNameConventions.GET_VALUE || name == OperatorNameConventions.SET_VALUE || name == OperatorNameConventions.PROVIDE_DELEGATE) ->
//                    PropertyDelegationOperatorReferenceSearcher(declaration, searchScope, consumer, optimizer, options)

                else -> null
            }

        }

        private object SearchesInProgress : ThreadLocal<HashSet<PsiElement>>() {
            override fun initialValue() = HashSet<PsiElement>()
        }
    }
}
