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

package cn.cangnova.cangjie.ide.search

import cn.cangnova.cangjie.CjNodeTypes
import cn.cangnova.cangjie.descriptors.PsiDiagnosticUtils
import cn.cangnova.cangjie.doc.psi.impl.CDocName
import cn.cangnova.cangjie.ide.base.projectStructure.RootKindFilter
import cn.cangnova.cangjie.ide.base.projectStructure.matches
import cn.cangnova.cangjie.ide.everythingScopeExcludeFileTypes
import cn.cangnova.cangjie.ide.restrictToCangJieSources
import cn.cangnova.cangjie.ide.search.ideExtensions.CangJieReferencesSearchOptions
import cn.cangnova.cangjie.ide.search.ideExtensions.CangJieReferencesSearchParameters
import cn.cangnova.cangjie.ide.stubindex.resolve.isUnitTestMode
import cn.cangnova.cangjie.lang.CangJieFileType
import cn.cangnova.cangjie.lang.CangJieLanguage
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.*
import cn.cangnova.cangjie.utils.CangJieExceptionWithAttachments
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.isAncestor
import com.intellij.util.Processor
import org.jetbrains.annotations.TestOnly
import java.util.*


class ExpressionsOfTypeProcessor(
    private val containsTypeOrDerivedInside: (CjDeclaration) -> Boolean,
    private val classToSearch: CjTypeStatement?,
    private val searchScope: SearchScope,
    private val project: Project,
    private val possibleMatchHandler: (CjExpression) -> Unit,
    private val possibleMatchesInScopeHandler: (SearchScope) -> Unit
) {
    enum class Mode {
        ALWAYS_SMART,
        ALWAYS_PLAIN,
        PLAIN_WHEN_NEEDED // use plain search for LocalSearchScope and when unknown type of reference encountered
    }

    companion object {
        val LOG = Logger.getInstance(ExpressionsOfTypeProcessor::class.java)

        @get:TestOnly
        var mode = Mode.ALWAYS_SMART

        @get:TestOnly
        var testLog: MutableCollection<String>? = null

        inline fun testLog(s: () -> String) {
            testLog?.add(s())
        }

        fun logPresentation(element: PsiElement): String? {
            return runReadAction {
                if (element !is CjDeclaration) return@runReadAction element.text
                val fqName = element.cangjieFqName?.asString()
                    ?: (element as? CjNamedDeclaration)?.name
                when (element) {

                    is CjFunction -> fqName + element.valueParameterList!!.text
                    is CjParameter -> {
                        val owner = element.ownerFunction?.let { logPresentation(it) } ?: element.parent.toString()
                        "parameter ${element.name} of $owner"
                    }

                    is CjDestructuringDeclaration -> element.entries.joinToString(
                        ", ",
                        prefix = "(",
                        postfix = ")"
                    ) { it.text }

                    else -> fqName
                }
            }
        }
    }

    private interface Task {
        fun perform()
    }

    private val tasks = ArrayDeque<Task>()
    private val taskSet = HashSet<Task>()
    private val scopesToUsePlainSearch = LinkedHashMap<CjFile, ArrayList<PsiElement>>()

    private fun processTasks() {
        while (tasks.isNotEmpty()) {
            tasks.pop().perform()
        }
    }

    private fun addTask(task: Task) {
        if (taskSet.add(task)) {
            tasks.push(task)
        }

    }

    fun runByExtendBasic(psiElement: PsiElement) {
        searchReferences(psiElement, searchScope) { reference ->

            return@searchReferences true
        }
    }

    fun run() {
        val usePlainSearch = when (mode) {
            Mode.ALWAYS_SMART -> false
            Mode.ALWAYS_PLAIN -> true
            Mode.PLAIN_WHEN_NEEDED -> searchScope is LocalSearchScope // for local scope it's faster to use plain search
        }

        /*
         *           如果是扩展的操作符重载函数，并且被扩展的类型是没有源psi的类型，例如内置类型，基本类型，将无法查找用法
         *         由于获取的psiClass为null ，所以无法调用 [addClassToProcess]
         *
         */
        if (usePlainSearch || classToSearch == null) {
            possibleMatchesInScopeHandler(searchScope)
            return
        }

        // optimization
        if (runReadAction {
                noCangJieFilesInScope(searchScope)
            }) return

        // for class from library always use plain search because we cannot search usages in compiled code (we could though)
        if (!runReadAction {
                classToSearch.isValid && isInProjectScope(classToSearch)
            }) {
            possibleMatchesInScopeHandler(searchScope)
            return
        }

        addClassToProcess(classToSearch)

        processTasks()

        runReadAction {
            val scopeElements = scopesToUsePlainSearch.values
                .flatten()
                .filter { it.isValid }
                .toTypedArray()
            if (scopeElements.isNotEmpty()) {
                possibleMatchesInScopeHandler(LocalSearchScope(scopeElements))
            }
        }
    }

    private fun noCangJieFilesInScope(searchScope: SearchScope): Boolean {
        if (searchScope is GlobalSearchScope && !FileTypeIndex.containsFileOfType(
                CangJieFileType.INSTANCE,
                searchScope
            )
        ) {
            return true
        }
        return searchScope is LocalSearchScope && searchScope.virtualFiles.none { it.fileType == CangJieFileType.INSTANCE }
    }

    private fun searchReferences(element: PsiElement, scope: SearchScope, processor: (PsiReference) -> Boolean) {
        val parameters = ReferencesSearch.SearchParameters(element, scope, false)
        searchReferences(parameters, processor)
    }

    private fun searchReferences(parameters: ReferencesSearch.SearchParameters, processor: (PsiReference) -> Boolean) {
        ReferencesSearch.search(parameters).forEach(Processor { ref ->
            ProgressManager.checkCanceled()
            runReadAction {
                if (ref.element.isValid) {
                    processor(ref)
                } else {
                    true
                }
            }
        })
    }

    private fun getFallbackDiagnosticsMessage(reference: PsiReference, debugInfo: StringBuilder? = null): String {
        val element = reference.element
        val document = PsiDocumentManager.getInstance(project).getDocument(element.containingFile)
        val lineAndCol = PsiDiagnosticUtils.offsetToLineAndColumn(document, element.startOffset)
        return "Unsupported reference: '${element.text}' in ${element.containingFile.virtualFile} [${element.language}] " +
                "line ${lineAndCol.line} column ${lineAndCol.column}${debugInfo?.let { " .$it" } ?: ""}"
    }

    private fun isImplicitlyTyped(declaration: CjDeclaration): Boolean {
        return when (declaration) {
            is CjFunction -> !declaration.hasDeclaredReturnType()
            is CjVariableDeclaration -> declaration.typeReference == null
            is CjParameter -> declaration.typeReference == null
            else -> false
        }
    }

    /**
     * Process declaration which may have implicit type of our class (or our class used anywhere inside that type)
     */
    private fun processSuspiciousDeclaration(declaration: CjDeclaration) {
        if (declaration is CjDestructuringDeclaration) {
            declaration.entries.forEach { processSuspiciousDeclaration(it) }
        } else {
            if (!isImplicitlyTyped(declaration)) return

            testLog { "Checked type of ${logPresentation(declaration)}" }

            if (containsTypeOrDerivedInside(declaration)) {
                addCallableDeclarationOfOurType(declaration)
            }
        }
    }

    private fun processReferenceToCallableOfOurType(reference: PsiReference) = when (reference.element.language) {
        CangJieLanguage -> {
//            if (reference is CjDestructuringDeclarationReference) {
//                // declaration usage in form of destructuring declaration entry
//                addCallableDeclarationOfOurType(reference.element)
//            } else {
            (reference.element as? CjReferenceExpression)?.let { processSuspiciousExpression(it) }
//            }
            true
        }

        else -> false // reference in unknown language - we don't know how to handle it
    }

    private enum class ReferenceProcessor(val handler: (ExpressionsOfTypeProcessor, PsiReference) -> Boolean) {
        CallableOfOurType(ExpressionsOfTypeProcessor::processReferenceToCallableOfOurType),

        ProcessLambdasInCalls({ processor, reference ->
            (reference.element as? CjReferenceExpression)?.let { processor.processLambdasForCallableReference(it) }
            true
        })
    }

    private fun processLambdasForCallableReference(expression: CjReferenceExpression) {
        //TODO: receiver?
        usePlainSearchInLambdas(expression.parent!!)
    }

    private fun addCallableDeclarationOfOurType(declaration: PsiElement) {
        addCallableDeclarationToProcess(
            declaration,
            searchScope.restrictToCangJieSources(),
            ReferenceProcessor.CallableOfOurType
        )
    }

    private fun addCallableDeclarationToProcess(
        declaration: PsiElement,
        scope: SearchScope,
        processor: ReferenceProcessor
    ) {

        data class ProcessCallableUsagesTask(
            val declaration: PsiElement,
            val processor: ReferenceProcessor,
            val scope: SearchScope
        ) : Task {
            override fun perform() {
                if (scope is LocalSearchScope) {
                    testLog { runReadAction { "Searched imported static member $declaration in ${scope.scope.toList()}" } }
                } else {
                    testLog { runReadAction { "Searched references to ${logPresentation(declaration)} in non-Java files" } }
                }

                val searchParameters = CangJieReferencesSearchParameters(
                    declaration, scope, cangjieOptions = CangJieReferencesSearchOptions(searchNamedArguments = false)
                )
                searchReferences(searchParameters) { reference ->
                    val processed = processor.handler(this@ExpressionsOfTypeProcessor, reference)
                    if (!processed) { // we don't know how to handle this reference and down-shift to plain search
                        downShiftToPlainSearch(reference)
                    }
                    processed
                }
            }
        }
        addTask(ProcessCallableUsagesTask(declaration, processor, scope))
    }

    private fun downShiftToPlainSearch(reference: PsiReference) {
        val message = getFallbackDiagnosticsMessage(reference)
        LOG.info("ExpressionsOfTypeProcessor: $message")
        testLog { "Downgrade to plain text search: $message" }

        tasks.clear()
        scopesToUsePlainSearch.clear()
        possibleMatchesInScopeHandler(searchScope)
    }

    /**
     * Process expression which may have type of our class (or our class used anywhere inside that type)
     */
    private fun processSuspiciousExpression(expression: CjExpression) {
        var inScope = expression in searchScope
        var affectedScope: PsiElement = expression
        ParentsLoop@
        for (element in expression.parentsWithSelf) {
            affectedScope = element
            if (element !is CjExpression) continue

            if (searchScope is LocalSearchScope) { // optimization to not check every expression
                inScope = inScope && element in searchScope
            }
            if (inScope) {
                possibleMatchHandler(element)
            }

            when (val parent = element.parent) {
                is CjDestructuringDeclaration -> { // "val (x, y) = <expr>"
                    processSuspiciousDeclaration(parent)
                    break@ParentsLoop
                }

                is CjDeclarationWithInitializer -> { // "val x = <expr>" or "fun f() = <expr>"
                    if (element == parent.initializer) {
                        processSuspiciousDeclaration(parent)
                    }
                    break@ParentsLoop
                }

                is CjContainerNode -> {
                    if (parent.node.elementType == CjNodeTypes.LOOP_RANGE) { // "for (x in <expr>) ..."
                        val forExpression = parent.parent as CjForExpression
                        (forExpression.destructuringDeclaration ?: forExpression.loopParameter as CjDeclaration?)?.let {
                            processSuspiciousDeclaration(it)
                        }
                        break@ParentsLoop
                    }
                }
            }

            if (!element.mayTypeAffectAncestors()) break
        }

        // use plain search in all lambdas and anonymous functions inside because they parameters or receiver can be implicitly typed with our class
        usePlainSearchInLambdas(affectedScope)
    }

    private fun usePlainSearchInLambdas(scope: PsiElement) {
        scope.forEachDescendantOfType<CjFunction> {
            if (it.nameIdentifier == null) {
                usePlainSearch(it)
            }
        }
    }

    //TODO: code is quite similar to PartialBodyResolveFilter.isValueNeeded
    private fun CjExpression.mayTypeAffectAncestors(): Boolean {
        when (val parent = this.parent) {
            is CjBlockExpression -> {
                return this == parent.statements.last() && parent.mayTypeAffectAncestors()
            }

            is CjDeclarationWithBody -> {
                if (this == parent.bodyExpression) {
                    return !parent.hasBlockBody() && !parent.hasDeclaredReturnType()
                }
            }

            is CjContainerNode -> {
                val grandParent = parent.parent
                return when (parent.node.elementType) {
                    CjNodeTypes.CONDITION, CjNodeTypes.BODY -> false
                    CjNodeTypes.THEN, CjNodeTypes.ELSE -> (grandParent as CjExpression).mayTypeAffectAncestors()
                    CjNodeTypes.LOOP_RANGE, CjNodeTypes.INDICES -> true
                    else -> true // something else unknown
                }
            }
        }
        return true // we don't know
    }

    private fun processClassUsageInUserType(userType: CjUserType): Boolean {
        val typeRef = userType.parents.lastOrNull { it is CjTypeReference }
        when (val typeRefParent = typeRef?.parent) {
            is CjExtend -> {
                return true
            }
            // TODO: type alias
            //is CjTypeAlias -> {}
            is CjCallableDeclaration -> {
                when (typeRef) {
                    typeRefParent.typeReference -> { // usage in type of callable declaration
                        addCallableDeclarationOfOurType(typeRefParent)

                        if (typeRefParent is CjParameter) { //TODO: what if functional type is declared with "FunctionN<...>"?
                            val usedInsideFunctionalType =
                                userType.parents.takeWhile { it != typeRef }.any { it is CjFunctionType }
                            if (usedInsideFunctionalType) {
                                val function = (typeRefParent.parent as? CjParameterList)?.parent as? CjFunction
                                if (function != null) {
                                    addCallableDeclarationOfOurType(function)
                                }
                            }
                        }

                        return true
                    }

                    typeRefParent.receiverTypeReference -> { // usage in receiver type of callable declaration
                        // we must use plain search inside extensions because implicit 'this' can happen anywhere
                        usePlainSearch(typeRefParent)
                        return true
                    }
                }
            }

            is CjTypeProjection -> { // usage in type arguments of a call
                val callExpression = (typeRefParent.parent as? CjTypeArgumentList)?.parent as? CjCallExpression
                if (callExpression != null) {
                    processSuspiciousExpression(callExpression)
                    return true
                }
            }

            is CjConstructorCalleeExpression -> { // super-class name in the list of bases
                val parent = typeRefParent.parent
                if (parent is CjSuperTypeCallEntry) {
                    val classOrObject = (parent.parent as CjSuperTypeList).parent as CjTypeStatement

                    addClassToProcess(classOrObject)
                    return true
                }
            }

            is CjSuperTypeListEntry -> { // super-interface name in the list of bases
                if (typeRef == typeRefParent.typeReference) {
                    val classOrObject = (typeRefParent.parent as CjSuperTypeList).parent as CjTypeStatement

                    addClassToProcess(classOrObject)
                    return true
                }
            }

            is CjIsExpression -> { // <expr> is <class name>
                val scopeOfPossibleSmartCast = typeRefParent.getParentOfType<CjDeclarationWithBody>(true)
                scopeOfPossibleSmartCast?.let { usePlainSearch(it) }
                return true
            }

//            is CjMatchConditionIsPattern -> { // "is <class name>" or "!is <class name>" in when
//                val whenEntry = typeRefParent.parent as CjMatchEntry
//                if (typeRefParent.isNegated) {
//                    val whenExpression = whenEntry.parent as CjMatchExpression
//                    val entriesAfter = whenExpression.entries.dropWhile { it != whenEntry }.drop(1)
//                    entriesAfter.forEach { usePlainSearch(it) }
//                } else {
//                    usePlainSearch(whenEntry)
//                }
//                return true
//            }

            is CjBinaryExpressionWithTypeRHS -> { // <expr> as <class name>
                processSuspiciousExpression(typeRefParent)
                return true
            }

            is CjTypeParameter -> { // <expr> as `<reified T : ClassName>`
                typeRefParent.extendsBound?.let {
                    addCallableDeclarationOfOurType(it)
                    return true
                }
            }
        }

        return false // unsupported case
    }

    private fun processClassUsageInCangJie(element: PsiElement, debugInfo: StringBuilder?): Boolean {
        //TODO: type aliases

        when (element) {
            is CjReferenceExpression -> {
                val elementParent = element.parent
                debugInfo?.apply { append(", elementParent: $elementParent") }
                when (elementParent) {
                    is CjUserType -> { // usage in type
                        val userTypeParent = elementParent.parent
                        if (userTypeParent is CjUserType && userTypeParent.qualifier == elementParent) {
                            return true // type qualifier
                        }

                        return processClassUsageInUserType(elementParent)
                    }

                    is CjCallExpression -> {
                        debugInfo?.apply { append(", CjCallExpression condition: ${element == elementParent.calleeExpression}") }
                        if (element == elementParent.calleeExpression) {  // constructor or invoke operator invocation
                            processSuspiciousExpression(element)
                            return true
                        }
                    }

                    is CjContainerNode -> {
                        if (elementParent.node.elementType == CjNodeTypes.LABEL_QUALIFIER) {
                            return true // this@ClassName - it will be handled anyway because members and extensions are processed with plain search
                        }
                    }

                    is CjQualifiedExpression -> {
                        // <class name>.memberName or some.<class name>.memberName or some.<class name>::class
                        if (element == elementParent.receiverExpression ||
                            elementParent.parent is CjQualifiedExpression
//                            ||
//                            elementParent.parent is CjClassLiteralExpression
                        ) {
                            return true // companion object member or static member access - ignore it
                        }
                    }

                    is CjCallableReferenceExpression -> {
                        when (element) {
                            elementParent.receiverExpression -> { // usage in receiver of callable reference (before "::") - ignore it
                                return true
                            }

                            elementParent.callableReference -> { // usage after "::" in callable reference - should be reference to constructor of our class
                                processSuspiciousExpression(element)
                                return true
                            }
                        }
                    }


                }

                if (element.getStrictParentOfType<CjImportDirectiveItem>() != null) return true // ignore usage in import

                processSuspiciousExpression(element)
                return true
            }

            is CDocName -> return true // ignore usage in doc-comment
        }

        return false // unsupported type of reference
    }

    private fun addClassToProcess(classToSearch: CjTypeStatement) {
        data class ProcessClassUsagesTask(val classToSearch: CjTypeStatement) : Task {
            override fun perform() {
                val debugInfo: StringBuilder? = if (isUnitTestMode()) StringBuilder() else null
                testLog { "Searched references to ${logPresentation(classToSearch)}" }
                debugInfo?.apply { append("Searched references to ").append(logPresentation(classToSearch)) }
                val scope =
                    project.everythingScopeExcludeFileTypes(XmlFileType.INSTANCE) // ignore usages in XML - they don't affect us
                searchReferences(classToSearch, scope) { reference ->
                    val element = reference.element
                    val language = element.language
                    debugInfo?.apply { append(", found reference element [$language]: $element") }
                    val wasProcessed = when (language) {
                        CangJieLanguage -> processClassUsageInCangJie(element, debugInfo)

                        else -> {
                            when (language.displayName) {

                                else -> {
                                    // If there's no PsiClass - consider processed
                                    element.getParentOfType<CjTypeStatement>(true) == null
                                }
                            }
                        }
                    }

                    if (wasProcessed) return@searchReferences true

                    if (mode != Mode.ALWAYS_SMART) {
                        downShiftToPlainSearch(reference)
                        return@searchReferences false
                    }

                    throw CangJieExceptionWithAttachments("Unsupported reference")
                        .withPsiAttachment("reference.txt", element)
                        .withAttachment("diagnostic_message.txt", getFallbackDiagnosticsMessage(reference, debugInfo))
                }

                // we must use plain search inside our class (and inheritors) because implicit 'this' can happen anywhere
                usePlainSearch(classToSearch)
            }
        }
        addTask(ProcessClassUsagesTask(classToSearch))
    }

    private fun usePlainSearch(scope: CjElement) {
        runReadAction {
            if (!scope.isValid) return@runReadAction

            val file = scope.getContainingCjFile()
            val restricted = LocalSearchScope(scope).intersectWith(searchScope)
            if (restricted is LocalSearchScope) {
                ScopeLoop@
                for (element in restricted.scope) {
                    val prevElements = scopesToUsePlainSearch.getOrPut(file) { ArrayList() }
                    for ((index, prevElement) in prevElements.withIndex()) {
                        if (!prevElement.isValid) continue@ScopeLoop
                        if (prevElement.isAncestor(element, strict = false)) continue@ScopeLoop
                        if (element.isAncestor(prevElement)) {
                            prevElements[index] = element
                            continue@ScopeLoop
                        }
                    }
                    prevElements.add(element)
                }
            } else {
                assert(SearchScope.isEmptyScope(restricted))
            }

        }
    }

    private fun isInProjectScope(classToSearch: CjTypeStatement): Boolean {
        return RootKindFilter.projectSources.copy().matches(classToSearch)
    }

}
