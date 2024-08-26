package com.huawei.cangjie.ide

import com.huawei.cangjie.analyzer.analyzeAsReplacement
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.ide.codeinsight.CangJieCodeInsightBundle
import com.huawei.cangjie.ide.imports.ImportDescriptorResult
import com.huawei.cangjie.ide.imports.ImportInsertHelper
import com.huawei.cangjie.ide.imports.canBeReferencedViaImport
import com.huawei.cangjie.ide.imports.getImportableTargets
import com.huawei.cangjie.ide.references.mainReference
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.*
import com.huawei.cangjie.renderer.DescriptorRenderer
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.QualifiedExpressionResolver.Companion.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE
import com.huawei.cangjie.resolve.allowResolveInDispatchThread
import com.huawei.cangjie.resolve.caches.getResolutionFacade
import com.huawei.cangjie.resolve.calls.model.VariableAsFunctionResolvedCall
import com.huawei.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import com.huawei.cangjie.resolve.calls.util.getCall
import com.huawei.cangjie.resolve.calls.util.getCalleeExpressionIfAny
import com.huawei.cangjie.resolve.calls.util.getResolvedCall
import com.huawei.cangjie.resolve.calls.util.resolveCandidates
import com.huawei.cangjie.resolve.descriptorUtil.fqNameSafe
import com.huawei.cangjie.resolve.descriptorUtil.unwrapIfFakeOverride
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.huawei.cangjie.resolve.scopes.findFirstClassifierWithDeprecationStatus
import com.huawei.cangjie.resolve.scopes.findPackage
import com.huawei.cangjie.resolve.scopes.getResolutionScope
import com.huawei.cangjie.resolve.scopes.receivers.ImplicitReceiver
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue
import com.huawei.cangjie.utils.ShadowedDeclarationsFilter
import com.huawei.cangjie.utils.isDispatchThread
import com.huawei.cangjie.utils.runAction
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.impl.source.PostprocessReformattingAspect
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.createSmartPointer

// 元素缩短
class ShortenReferences(val options: (CjElement) -> Options = { Options.DEFAULT }) {
    data class Options(
        val removeThisLabels: Boolean = false,
        val removeThis: Boolean = false,
        // TODO: remove this option and all related stuff (RETAIN_COMPANION etc.) after KT-13934 fixed
        val removeExplicitCompanion: Boolean = true,
        val dropBracesInStringTemplates: Boolean = true
    ) {
        companion object {
            val DEFAULT = Options()
            val ALL_ENABLED = Options(removeThisLabels = true, removeThis = true)
        }
    }

    companion object {
        private val LOG = Logger.getInstance(ShortenReferences::class.java)

        @JvmField
        val DEFAULT = ShortenReferences()

        val RETAIN_COMPANION = ShortenReferences { Options(removeExplicitCompanion = false) }

        fun canBePossibleToDropReceiver(element: CjDotQualifiedExpression, bindingContext: BindingContext): Boolean {
            val nameRef = when (val receiver = element.receiverExpression) {
                is CjThisExpression -> return true
                is CjNameReferenceExpression -> receiver
                is CjDotQualifiedExpression -> receiver.selectorExpression as? CjNameReferenceExpression ?: return false
                else -> return false
            }
            if (nameRef.name == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE) return true

            when (val targetDescriptor = bindingContext[BindingContext.REFERENCE_TARGET, nameRef]) {
                is ClassDescriptor -> {

                    // for object receiver we should additionally check that it's dispatch receiver (that is the member is inside the object) or not a receiver at all
                    val resolvedCall = element.getResolvedCall(bindingContext)
                        ?: return element.getQualifiedElementSelector()?.mainReference?.resolveToDescriptors(
                            bindingContext
                        ) != null

                    val receiverKind = resolvedCall.explicitReceiverKind
                    return receiverKind == ExplicitReceiverKind.DISPATCH_RECEIVER || receiverKind == ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
                }

                is PackageViewDescriptor -> return true

                else -> return false
            }
        }

        private fun DeclarationDescriptor.asString() = DescriptorRenderer.FQ_NAMES_IN_TYPES.render(this)

        private fun CjReferenceExpression.targets(context: BindingContext) = getImportableTargets(context)

        private fun mayImport(descriptor: DeclarationDescriptor, file: CjFile): Boolean {
            return descriptor.canBeReferencedViaImport()
                    && ImportInsertHelper.getInstance(file.project).mayImportOnShortenReferences(descriptor, file)
        }
    }

    fun process(
        element: CjElement,
        elementFilter: (PsiElement) -> FilterResult = { FilterResult.PROCESS },
        runImmediately: Boolean = true
    ): CjElement {
        return if (isDispatchThread()) {
            val ref = Ref<CjElement>()
            ApplicationManagerEx.getApplicationEx().runWriteActionWithCancellableProgressInDispatchThread(
                CangJieCodeInsightBundle.message("progress.title.shortening.references"), element.project, null
            ) {
                ref.set(process(listOf(element), elementFilter, runImmediately).single())
            }
            ref.get()
        } else {
            process(listOf(element), elementFilter, runImmediately).single()
        }
    }

    @JvmOverloads
    fun process(
        element: CjElement,
        elementFilter: (PsiElement) -> FilterResult = { FilterResult.PROCESS },
    ): CjElement {
        return process(element, elementFilter, runImmediately = true)
    }

    @JvmOverloads
    fun process(
        file: CjFile,
        startOffset: Int,
        endOffset: Int,
        additionalFilter: (PsiElement) -> FilterResult = { FilterResult.PROCESS },
        runImmediately: Boolean = true
    ) {
        val rangeMarker = runReadAction {
            val documentManager = PsiDocumentManager.getInstance(file.project)
            val document = file.viewProvider.document!!
            check(documentManager.isCommitted(document)) { "Document should be committed to shorten references in range" }
            document.createRangeMarker(startOffset, endOffset).apply {
                isGreedyToLeft = true
                isGreedyToRight = true
            }
        }

        val rangeFilter = { element: PsiElement ->
            if (rangeMarker.isValid) {
                val range = TextRange(rangeMarker.startOffset, rangeMarker.endOffset)

                val elementRange = element.textRange!!
                when {
                    range.contains(elementRange) -> FilterResult.PROCESS

                    range.intersects(elementRange) -> {
                        // for qualified call expression allow to shorten only the part without parenthesis
                        val calleeExpression = ((element as? CjDotQualifiedExpression)
                            ?.selectorExpression as? CjCallExpression)
                            ?.calleeExpression
                        if (calleeExpression != null) {
                            val rangeWithoutParenthesis =
                                TextRange(elementRange.startOffset, calleeExpression.textRange!!.endOffset)
                            if (range.contains(rangeWithoutParenthesis)) FilterResult.PROCESS else FilterResult.GO_INSIDE
                        } else {
                            FilterResult.GO_INSIDE
                        }
                    }

                    else -> FilterResult.SKIP
                }
            } else {
                FilterResult.SKIP
            }
        }
        try {
            val filter = { element: PsiElement ->
                minOf(rangeFilter(element), additionalFilter(element))
            }
            process(listOf(file), filter, runImmediately)
        } finally {
            runReadAction { rangeMarker.dispose() }
        }
    }

    enum class FilterResult {
        SKIP,
        GO_INSIDE,
        PROCESS
    }

    @JvmOverloads
    fun process(
        elements: Iterable<CjElement>,
        elementFilter: (PsiElement) -> FilterResult = { FilterResult.PROCESS },
        runImmediately: Boolean = true
    ): Collection<CjElement> =
        runReadAction { elements.groupBy(CjElement::getContainingCjFile) }.flatMap { (file, elements) ->
            try {
                shortenReferencesInFile(file, elements, elementFilter, runImmediately)
            } catch (e: Throwable) {
                if (e is ControlFlowException) throw e

                LOG.warn(e)
                val processors: List<ShorteningProcessor<*>> = runReadAction {
                    listOf(
                        ShortenTypesProcessor(file, elementFilter, emptySet()),
                        ShortenQualifiedExpressionsProcessor(file, elementFilter, emptySet()),
                        ShortenPackageProcessor(file, elementFilter, emptySet()),
                    )
                }

                val resultElements = elements.toMutableSet()
                runReadAction {
                    for (processor in processors) {
                        for (element in resultElements) {
                            element.accept(processor.collectElementsVisitor)
                        }
                    }
                }

                for (processor in processors) {
                    processor.removeRootPrefixes(resultElements)
                }

                resultElements
            }
        }

    private fun shortenReferencesInFile(
        file: CjFile,
        elements: List<CjElement>,
        elementFilter: (PsiElement) -> FilterResult,
        runImmediately: Boolean = true
    ): Collection<CjElement> {
        //TODO: that's not correct since we have options!
        val elementsToUse = dropNestedElements(elements)

        val helper = ImportInsertHelper.getInstance(file.project)

        val failedToImportDescriptors = LinkedHashSet<DeclarationDescriptor>()

        val companionElementFilter = { element: PsiElement ->
            if (element is CjElement && !options(element).removeExplicitCompanion) {
                FilterResult.SKIP
            } else {
                elementFilter(element)
            }
        }

        while (true) {
            // Processors order is important here so that enclosing elements are not shortened before their children are, e.g.

            val processors: List<ShorteningProcessor<*>> = runReadAction {
                listOf(
                    ShortenTypesProcessor(file, elementFilter, failedToImportDescriptors),
                    ShortenThisExpressionsProcessor(file, elementFilter, failedToImportDescriptors),
                    ShortenQualifiedExpressionsProcessor(file, elementFilter, failedToImportDescriptors),
                    ShortenPackageProcessor(file, elementFilter, failedToImportDescriptors),
                    RemoveExplicitCompanionObjectReferenceProcessor(
                        file,
                        companionElementFilter,
                        failedToImportDescriptors
                    )
                )
            }

            // step 1: collect qualified elements to analyze (no resolve at this step)
            val visitors = processors.map { it.collectElementsVisitor }
            runReadAction {
                for (visitor in visitors) {
                    for (element in elementsToUse) {
                        visitor.options = options(element)
                        element.accept(visitor)
                    }
                }


                // step 2: analyze collected elements with resolve and decide which can be shortened now and which need descriptors to be imported before shortening
                val allElementsToAnalyze =
                    visitors.flatMap { visitor -> visitor.getElementsToAnalyze().map { it.element } }.toSet()
                val bindingContext = allowResolveInDispatchThread {
                    file.getResolutionFacade().analyze(allElementsToAnalyze, BodyResolveMode.PARTIAL_WITH_CFA)
                }

                processors.forEach { it.analyzeCollectedElements(bindingContext) }
            }

            // step 3: 缩短现在可以缩短的元素
            runAction(runImmediately) {
                processors.forEach { it.shortenElements(elementSetToUpdate = elementsToUse, options = options) }
            }
            var anyChange = false
            // step 4: try to import descriptors needed to shorten other elements
            val descriptorsToImport = runReadAction {
                processors.flatMap { it.getDescriptorsToImport() }.toSet()
            }
            runAction(runImmediately) {
                for (descriptor in descriptorsToImport) {
                    assert(descriptor !in failedToImportDescriptors)

//                    尝试导入并缩短元素
//                    导入
                    val result = helper.importDescriptor(file, descriptor)

                    if (result != ImportDescriptorResult.ALREADY_IMPORTED) {
                        anyChange = true
                    }
                    if (result == ImportDescriptorResult.FAIL) {
                        failedToImportDescriptors.add(descriptor)
                    }
                }

                if (!anyChange) {
                    processors.forEach { it.removeRootPrefixes(elementSetToUpdate = elementsToUse) }
                }
            }

            if (!anyChange) break
        }

        return elementsToUse
    }

    private fun dropNestedElements(elements: List<CjElement>): LinkedHashSet<CjElement> = runReadAction {
        val elementSet = elements.toSet()
        elementSet.filterTo(LinkedHashSet(elementSet.size)) { element ->
            element.parents.none { it in elementSet }
        }
    }

    private data class ElementToAnalyze<TElement>(val element: TElement, val level: Int)

    private abstract class CollectElementsVisitor<TElement : CjElement>(
        protected val elementFilter: (PsiElement) -> FilterResult
    ) : CjVisitorVoid() {

        var options: Options = Options.DEFAULT

        private val elementsToAnalyze = ArrayList<ElementToAnalyze<TElement>>()
        private val elementsWithRootPrefix = mutableListOf<SmartPsiElementPointer<TElement>>()

        private var level = 0

        protected fun nextLevel() {
            level++
        }

        protected fun prevLevel() {
            level--
            assert(level >= 0)
        }

        /**
         * Should be invoked by implementors when visiting the PSI tree for those elements that can potentially be shortened
         */
        protected fun addQualifiedElementToAnalyze(element: TElement) {
            if (element.isRootPrefix())
                elementsWithRootPrefix += element.createSmartPointer()
            else
                elementsToAnalyze += ElementToAnalyze(element, level)
        }

        override fun visitElement(element: PsiElement) {
            if (elementFilter(element) != FilterResult.SKIP) {
                element.acceptChildren(this)
            }
        }

        fun getElementsToAnalyze(): List<ElementToAnalyze<TElement>> = elementsToAnalyze
        fun getElementsWithRootPrefix(): Collection<SmartPsiElementPointer<TElement>> = elementsWithRootPrefix
    }

    private abstract class ShorteningProcessor<TElement : CjElement>(
        protected val file: CjFile,
        protected val failedToImportDescriptors: Set<DeclarationDescriptor>
    ) {
        protected val resolutionFacade = file.getResolutionFacade()
        private val elementsToShorten = ArrayList<SmartPsiElementPointer<TElement>>()
        private val descriptorsToImport = LinkedHashSet<DeclarationDescriptor>()

        abstract val collectElementsVisitor: CollectElementsVisitor<TElement>

        fun analyzeCollectedElements(bindingContext: BindingContext) {
            val elements = collectElementsVisitor.getElementsToAnalyze()

            var index = 0
            while (index < elements.size) {
                val (element, level) = elements[index++]

                val result = analyzeQualifiedElement(element, bindingContext)

                val toBeShortened: Boolean
                when (result) {
                    AnalyzeQualifiedElementResult.ShortenNow -> {
                        elementsToShorten.add(element.createSmartPointer())
                        toBeShortened = true
                    }

                    is AnalyzeQualifiedElementResult.ImportDescriptors -> {
                        val tryImport = result.descriptors.isNotEmpty()
                                && result.descriptors.none { it in failedToImportDescriptors }
                                && result.descriptors.all { mayImport(it, file) }

                        if (tryImport) {
                            descriptorsToImport.addAll(result.descriptors)
                        }
                        toBeShortened = tryImport
                    }

                    AnalyzeQualifiedElementResult.Skip -> {
                        toBeShortened = false
                    }
                }

                if (toBeShortened) {
                    // we are going to shorten qualified element - we must skip all elements inside its qualifier
                    while (index < elements.size && elements[index].level > level) {
                        index++
                    }
                }
            }
        }

        /**
         * This method is invoked for all qualified elements added by [CollectElementsVisitor.addQualifiedElementToAnalyze]
         */
        protected abstract fun analyzeQualifiedElement(
            element: TElement,
            bindingContext: BindingContext
        ): AnalyzeQualifiedElementResult

        protected sealed class AnalyzeQualifiedElementResult {
            object Skip : AnalyzeQualifiedElementResult()

            object ShortenNow : AnalyzeQualifiedElementResult()

            class ImportDescriptors(val descriptors: Collection<DeclarationDescriptor>) :
                AnalyzeQualifiedElementResult()
        }

        protected abstract fun shortenElement(element: TElement, options: Options): CjElement

        fun shortenElements(elementSetToUpdate: MutableSet<CjElement>, options: (CjElement) -> Options) {
            for (elementPointer in elementsToShorten) {
                val element = elementPointer.element ?: continue
                if (!element.isValid) continue
                shortenAndReplace(element, elementSetToUpdate, options(element))
            }
        }

        fun removeRootPrefixes(elementSetToUpdate: MutableSet<CjElement>) {
            for (pointer in collectElementsVisitor.getElementsWithRootPrefix()) {
                val element = pointer.element ?: continue
                shortenAndReplace(element, elementSetToUpdate, Options.DEFAULT)
            }
        }

        fun shortenAndReplace(element: TElement, elementSetToUpdate: MutableSet<CjElement>, options: Options) {

            val newElement =
                PostprocessReformattingAspect.getInstance(element.project).disablePostprocessFormattingInside(
                    Computable {
                        shortenElement(element, options)
                    })

            if (element in elementSetToUpdate && newElement != element) {
                elementSetToUpdate.remove(element)
                elementSetToUpdate.add(newElement)
            }
        }

        fun getDescriptorsToImport(): Set<DeclarationDescriptor> = descriptorsToImport
    }

    private class ShortenPackageProcessor(
        file: CjFile,
        elementFilter: (PsiElement) -> FilterResult,
        failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : ShorteningProcessor<CjUserType>(file, failedToImportDescriptors) {
        override val collectElementsVisitor: CollectElementsVisitor<CjUserType> =
            object : CollectElementsVisitor<CjUserType>(elementFilter) {
                override fun visitUserType(userType: CjUserType) {
                    val filterResult = elementFilter(userType)
                    if (filterResult == FilterResult.SKIP) return

                    userType.typeArgumentList?.accept(this)

                    if (filterResult == FilterResult.PROCESS) {
                        addQualifiedElementToAnalyze(userType)
                        nextLevel()
                    }

                    // elements in qualifier must be under
                    userType.qualifier?.accept(this)
                    if (filterResult == FilterResult.PROCESS) {
                        prevLevel()
                    }
                }
            }

        private val cacheFqNameMap = mutableMapOf<CjUserType, FqName>()
        override fun analyzeQualifiedElement(
            element: CjUserType,
            bindingContext: BindingContext
        ): AnalyzeQualifiedElementResult {
            if (element.qualifier == null) return AnalyzeQualifiedElementResult.Skip
            val referenceExpression = element.referenceExpression ?: return AnalyzeQualifiedElementResult.Skip

            val target = referenceExpression.targets(bindingContext).singleOrNull()
                ?: return AnalyzeQualifiedElementResult.Skip


            val import = file.importList?.imports?.filter {
                it.importedFqName == target.fqNameSafe.parent()
            }

//            val scope = element.getResolutionScope(bindingContext, resolutionFacade)
//            val name = target.name
//
//            val targetByName: DeclarationDescriptor?
//            val isDeprecated: Boolean
//
//            if (target is ClassifierDescriptor) {
//                val classifierWithDeprecation =
//                    scope.findFirstClassifierWithDeprecationStatus(name, NoLookupLocation.FROM_IDE)
//                targetByName = classifierWithDeprecation?.descriptor
//                isDeprecated = classifierWithDeprecation?.isDeprecated ?: false
//            } else {
//                targetByName = scope.findPackage(name)
//                isDeprecated = false
//            }

            val canShortenNow = !import.isNullOrEmpty()
            return if (canShortenNow) {
                AnalyzeQualifiedElementResult.ShortenNow
            } else {
                cacheFqNameMap[element] = target.fqNameSafe.parent()
                AnalyzeQualifiedElementResult.ImportDescriptors(
                    listOfNotNull(target)
                )
            }
        }

        override fun shortenElement(element: CjUserType, options: Options): CjElement {
//val fqName = element.fqName
//            val fqName = cacheFqNameMap[element]?.parent() ?: {
//                element.deleteQualifier()
//                element
//            }
//
//            cacheFqNameMap.remove(element)

            element.deleteQualifier(1)
            return element


        }
    }

    private class ShortenTypesProcessor(
        file: CjFile,
        elementFilter: (PsiElement) -> FilterResult,
        failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : ShorteningProcessor<CjUserType>(file, failedToImportDescriptors) {

        override val collectElementsVisitor: CollectElementsVisitor<CjUserType> =
            object : CollectElementsVisitor<CjUserType>(elementFilter) {
                override fun visitUserType(userType: CjUserType) {
                    val filterResult = elementFilter(userType)
                    if (filterResult == FilterResult.SKIP) return

                    userType.typeArgumentList?.accept(this)

                    if (filterResult == FilterResult.PROCESS) {
                        addQualifiedElementToAnalyze(userType)
                        nextLevel()
                    }

                    // elements in qualifier must be under
                    userType.qualifier?.accept(this)
                    if (filterResult == FilterResult.PROCESS) {
                        prevLevel()
                    }
                }
            }

        override fun analyzeQualifiedElement(
            element: CjUserType,
            bindingContext: BindingContext
        ): AnalyzeQualifiedElementResult {
            if (element.qualifier == null) return AnalyzeQualifiedElementResult.Skip
            val referenceExpression = element.referenceExpression ?: return AnalyzeQualifiedElementResult.Skip

            val target = referenceExpression.targets(bindingContext).singleOrNull()
                ?: return AnalyzeQualifiedElementResult.Skip

            val scope = element.getResolutionScope(bindingContext, resolutionFacade)
            val name = target.name

            val targetByName: DeclarationDescriptor?
            val isDeprecated: Boolean

            if (target is ClassifierDescriptor) {
                val classifierWithDeprecation =
                    scope.findFirstClassifierWithDeprecationStatus(name, NoLookupLocation.FROM_IDE)
                targetByName = classifierWithDeprecation?.descriptor
                isDeprecated = classifierWithDeprecation?.isDeprecated ?: false
            } else {
                targetByName = scope.findPackage(name)
                isDeprecated = false
            }

            val canShortenNow = targetByName?.asString() == target.asString() && !isDeprecated
            return if (canShortenNow) AnalyzeQualifiedElementResult.ShortenNow else AnalyzeQualifiedElementResult.ImportDescriptors(
                listOfNotNull(target)
            )
        }

        override fun shortenElement(element: CjUserType, options: Options): CjElement {
            element.deleteQualifier()
            return element
        }
    }

    private abstract class QualifiedExpressionShorteningProcessor(
        file: CjFile,
        elementFilter: (PsiElement) -> FilterResult,
        failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : ShorteningProcessor<CjDotQualifiedExpression>(file, failedToImportDescriptors) {

        protected open class MyVisitor(elementFilter: (PsiElement) -> FilterResult) :
            CollectElementsVisitor<CjDotQualifiedExpression>(elementFilter) {
            override fun visitDotQualifiedExpression(expression: CjDotQualifiedExpression) {
                val filterResult = elementFilter(expression)
                if (filterResult == FilterResult.SKIP) return

                expression.selectorExpression?.acceptChildren(this)

                if (filterResult == FilterResult.PROCESS) {
                    addQualifiedElementToAnalyze(expression)
                    nextLevel()
                }

                // elements in receiver must be under
                expression.receiverExpression.accept(this)
                if (filterResult == FilterResult.PROCESS) {
                    prevLevel()
                }
            }
        }

        override val collectElementsVisitor = MyVisitor(elementFilter)
    }

    private class ShortenQualifiedExpressionsProcessor(
        file: CjFile,
        elementFilter: (PsiElement) -> FilterResult,
        failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : QualifiedExpressionShorteningProcessor(file, elementFilter, failedToImportDescriptors) {

        override val collectElementsVisitor = object : MyVisitor(elementFilter) {
            override fun visitDotQualifiedExpression(expression: CjDotQualifiedExpression) {
                if (expression.receiverExpression is CjThisExpression && !options.removeThis) {
                    val filterResult = elementFilter(expression)
                    if (filterResult == FilterResult.SKIP) return
                    expression.selectorExpression?.acceptChildren(this)
                    return
                }
                super.visitDotQualifiedExpression(expression)
            }
        }

        override fun analyzeQualifiedElement(
            element: CjDotQualifiedExpression,
            bindingContext: BindingContext
        ): AnalyzeQualifiedElementResult {
            if (PsiTreeUtil.getParentOfType(
                    element,
                    CjImportDirective::class.java,
                    CjPackageDirective::class.java
                ) != null ||
                !canBePossibleToDropReceiver(element, bindingContext)
            ) return AnalyzeQualifiedElementResult.Skip

            val selector = element.selectorExpression ?: return AnalyzeQualifiedElementResult.Skip
            val callee = selector.getCalleeExpressionIfAny() as? CjReferenceExpression
                ?: return AnalyzeQualifiedElementResult.Skip


            val targets = callee.targets(bindingContext)
            val resolvedCall = callee.getResolvedCall(bindingContext)

            if (targets.isEmpty()) return AnalyzeQualifiedElementResult.Skip

            val (newContext, selectorAfterShortening) = copyShortenAndAnalyze(element, bindingContext)
            val newCallee = selectorAfterShortening.getCalleeExpressionIfAny() as CjReferenceExpression

            val targetsWhenShort = newCallee.targets(newContext)

            val resolvedCallWhenShort = newCallee.getResolvedCall(newContext)
            val targetsMatch = targetsMatch(targets, targetsWhenShort) &&
                    (resolvedCall !is VariableAsFunctionResolvedCall || (
                            resolvedCallWhenShort is VariableAsFunctionResolvedCall? &&
                                    resolvedCallsMatch(resolvedCall, resolvedCallWhenShort)))

            // Don't shorten references if it will result to call to deprecated classifier by short name
            val isShortenedReferenceResolvesToDeprecated =
                newContext[BindingContext.DEPRECATED_SHORT_NAME_ACCESS, newCallee] == true
            if (isShortenedReferenceResolvesToDeprecated) return AnalyzeQualifiedElementResult.Skip

            // If before and after shorten call can be resolved unambiguously, then preform comparing of such calls,
            // if it matches, then we can preform shortening
            // Otherwise, collecting candidates both before and after shorten,
            // then filtering out candidates hidden by more specific signature
            // (for ex local fun, and extension fun on same receiver with matching names and signature)
            // Then, checking that any of resolved calls when shorten matches with calls before shorten, it means that there can be
            // targetMatch == false, but shorten still can be preformed
            // TODO: Add possibility to check if descriptor from completion can't be resolved after shorten and not preform shorten than
            val resolvedCallsMatch = if (resolvedCall != null && resolvedCallWhenShort != null) {
                val originalCallDescriptor = resolvedCall.resultingDescriptor.original.unwrapIfFakeOverride()
                val shortenedCallDescriptor = resolvedCallWhenShort.resultingDescriptor.original.unwrapIfFakeOverride()

                originalCallDescriptor == shortenedCallDescriptor
            } else {
                val resolvedCalls =
                    selector.getCall(bindingContext)?.resolveCandidates(bindingContext, resolutionFacade) ?: emptyList()
                val callWhenShort = selectorAfterShortening.getCall(newContext)
                val resolvedCallsWhenShort =
                    selectorAfterShortening.getCall(newContext)?.resolveCandidates(newContext, resolutionFacade)
                        ?: emptyList()

                val descriptorsOfResolvedCallsWhenShort = resolvedCallsWhenShort.map { it.resultingDescriptor.original }
                val descriptorsOfResolvedCalls = resolvedCalls.mapTo(mutableSetOf()) { it.resultingDescriptor.original }

                val filter =
                    ShadowedDeclarationsFilter(
                        newContext,
                        resolutionFacade,
                        newCallee,
                        callWhenShort?.explicitReceiver as? ReceiverValue
                    )
                val availableDescriptorsWhenShort = filter.filter(descriptorsOfResolvedCallsWhenShort)

                availableDescriptorsWhenShort.any { it in descriptorsOfResolvedCalls }
            }


            val receiver = element.receiverExpression
            if (receiver is CjThisExpression) {
                if (!targetsMatch) return AnalyzeQualifiedElementResult.Skip
                val originalCall = selector.getResolvedCall(bindingContext) ?: return AnalyzeQualifiedElementResult.Skip
                val newCall =
                    selectorAfterShortening.getResolvedCall(newContext) ?: return AnalyzeQualifiedElementResult.Skip
                val receiverKind = originalCall.explicitReceiverKind
                val newReceiver = when (receiverKind) {
                    ExplicitReceiverKind.BOTH_RECEIVERS, ExplicitReceiverKind.EXTENSION_RECEIVER -> newCall.extensionReceiver
                    ExplicitReceiverKind.DISPATCH_RECEIVER -> newCall.dispatchReceiver
                    else -> return AnalyzeQualifiedElementResult.Skip
                } as? ImplicitReceiver ?: return AnalyzeQualifiedElementResult.Skip

                val thisTarget = receiver.instanceReference.targets(bindingContext).singleOrNull()
                if (newReceiver.declarationDescriptor.asString() != thisTarget?.asString()) return AnalyzeQualifiedElementResult.Skip
            }

            return when {
                targetsMatch || resolvedCallsMatch ->
                    AnalyzeQualifiedElementResult.ShortenNow

                // Function doesn't conflict with property
                targets.all { it is FunctionDescriptor } && targetsWhenShort.all { it is PropertyDescriptor } ->
                    AnalyzeQualifiedElementResult.ImportDescriptors(targets)

                // In other cases it makes no sense to insert import when there is a conflict with function, property etc
                targetsWhenShort.any { it !is ClassifierDescriptorWithTypeParameters && it !is PackageViewDescriptor } ->
                    AnalyzeQualifiedElementResult.Skip


                else ->
                    AnalyzeQualifiedElementResult.ImportDescriptors(targets)
            }
        }

        private fun copyShortenAndAnalyze(
            element: CjDotQualifiedExpression,
            bindingContext: BindingContext
        ): Pair<BindingContext, CjExpression> {
            val selector = element.selectorExpression!!

            //                selector V  V             selector V  V
            // When processing some.fq.Name::callable or some.fq.Name::class
            //                 ^  element ^              ^  element ^
            //
            // Result should be Name::callable or Name::class
            //                  ^  ^              ^  ^
            val doubleColonExpression = element.getParentOfType<CjDoubleColonExpression>(true)
            if (doubleColonExpression != null && doubleColonExpression.receiverExpression == element) {
                val doubleColonExpressionCopy = doubleColonExpression.copied()
                doubleColonExpressionCopy.receiverExpression!!.replace(selector)
                val newBindingContext =
                    doubleColonExpressionCopy.analyzeAsReplacement(
                        doubleColonExpression,
                        bindingContext,
                        resolutionFacade
                    )
                return newBindingContext to doubleColonExpressionCopy.receiverExpression!!
            }

            val qualifiedAbove = element.getQualifiedExpressionForReceiver()
            if (qualifiedAbove != null) {
                val qualifiedAboveCopy = qualifiedAbove.copied()
                qualifiedAboveCopy.receiverExpression.replace(selector)
                val newBindingContext =
                    qualifiedAboveCopy.analyzeAsReplacement(qualifiedAbove, bindingContext, resolutionFacade)
                return newBindingContext to qualifiedAboveCopy.receiverExpression
            }

            val copied = selector.copied()
            val newBindingContext = copied.analyzeAsReplacement(element, bindingContext, resolutionFacade)
            return newBindingContext to copied
        }

        private fun targetsMatch(
            targets1: Collection<DeclarationDescriptor>,
            targets2: Collection<DeclarationDescriptor>
        ): Boolean {
            if (targets1.size != targets2.size) return false
            return if (targets1.size == 1) {
                targets1.single().asString() == targets2.single().asString()
            } else {
                targets1.map { it.asString() }.toSet() == targets2.map { it.asString() }.toSet()
            }
        }

        private fun resolvedCallsMatch(
            rc1: VariableAsFunctionResolvedCall?,
            rc2: VariableAsFunctionResolvedCall?
        ): Boolean {
            return rc1?.variableCall?.candidateDescriptor?.asString() == rc2?.variableCall?.candidateDescriptor?.asString() &&
                    rc1?.functionCall?.candidateDescriptor?.asString() == rc2?.functionCall?.candidateDescriptor?.asString()
        }

        override fun shortenElement(element: CjDotQualifiedExpression, options: Options): CjElement {
            val parens = element.parent as? CjParenthesizedExpression
            val requiredParens = parens != null && !CjPsiUtil.areParenthesesUseless(parens)
            val commentSaver = CommentSaver(element)
            val shortenedElement = element.replace(element.selectorExpression!!) as CjElement
            commentSaver.restore(shortenedElement)
            val newParent = shortenedElement.parent
            if (requiredParens) if (newParent != null) {
                return newParent.replaced(shortenedElement)
            }
            if (options.dropBracesInStringTemplates && newParent is CjBlockStringTemplateEntry && newParent.canDropCurlyBrackets()) {
                newParent.dropCurlyBrackets()
            }
            return shortenedElement
        }
    }

    private class ShortenThisExpressionsProcessor(
        file: CjFile,
        elementFilter: (PsiElement) -> FilterResult,
        failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : ShorteningProcessor<CjThisExpression>(file, failedToImportDescriptors) {

        private val simpleThis = CjPsiFactory(file.project).createExpression("this") as CjThisExpression

        override val collectElementsVisitor: CollectElementsVisitor<CjThisExpression> =
            object : CollectElementsVisitor<CjThisExpression>(elementFilter) {
                override fun visitThisExpression(expression: CjThisExpression) {
                    if (options.removeThisLabels && elementFilter(expression) == FilterResult.PROCESS && expression.getTargetLabel() != null) {
                        addQualifiedElementToAnalyze(expression)
                    }
                }
            }

        override fun analyzeQualifiedElement(
            element: CjThisExpression,
            bindingContext: BindingContext
        ): AnalyzeQualifiedElementResult {
            val targetBefore = element.instanceReference.targets(bindingContext).singleOrNull()
                ?: return AnalyzeQualifiedElementResult.Skip
            val newContext = simpleThis.analyzeAsReplacement(element, bindingContext, resolutionFacade)
            val targetAfter = simpleThis.instanceReference.targets(newContext).singleOrNull()
            return if (targetBefore == targetAfter) AnalyzeQualifiedElementResult.ShortenNow else AnalyzeQualifiedElementResult.Skip
        }

        override fun shortenElement(element: CjThisExpression, options: Options): CjElement {
            return element.replace(simpleThis) as CjElement
        }
    }

    private class RemoveExplicitCompanionObjectReferenceProcessor(
        file: CjFile,
        elementFilter: (PsiElement) -> FilterResult,
        failedToImportDescriptors: Set<DeclarationDescriptor>
    ) : QualifiedExpressionShorteningProcessor(file, elementFilter, failedToImportDescriptors) {

        private fun CjExpression.singleTarget(context: BindingContext): DeclarationDescriptor? {
            return (getCalleeExpressionIfAny() as? CjReferenceExpression)?.targets(context)?.singleOrNull()
        }

        override fun analyzeQualifiedElement(
            element: CjDotQualifiedExpression,
            bindingContext: BindingContext
        ): AnalyzeQualifiedElementResult {
            val parent = element.parent
            if (parent is CjDoubleColonExpression && parent.receiverExpression == element) return AnalyzeQualifiedElementResult.Skip

            val receiver = element.receiverExpression

            if (PsiTreeUtil.getParentOfType(
                    element,
                    CjImportDirective::class.java,
                    CjPackageDirective::class.java
                ) != null
            ) {
                return AnalyzeQualifiedElementResult.Skip
            }

            val receiverTarget =
                receiver.singleTarget(bindingContext) as? ClassDescriptor ?: return AnalyzeQualifiedElementResult.Skip

            val selectorExpression = element.selectorExpression ?: return AnalyzeQualifiedElementResult.Skip
            val selectorTarget =
                selectorExpression.singleTarget(bindingContext) ?: return AnalyzeQualifiedElementResult.Skip


            val selectorsSelector = (parent as? CjDotQualifiedExpression)?.selectorExpression
                ?: return AnalyzeQualifiedElementResult.ShortenNow

            val selectorsSelectorTarget =
                selectorsSelector.singleTarget(bindingContext) ?: return AnalyzeQualifiedElementResult.Skip
            if (selectorsSelectorTarget is ClassDescriptor) return AnalyzeQualifiedElementResult.Skip
            // TODO: More generic solution may be possible
//            if (selectorsSelectorTarget is PropertyDescriptor) {
//                val source = selectorsSelectorTarget.source.getPsi() as? CjProperty
//                if (source != null && CangJiePsiHeuristics.isEnumCompanionPropertyWithEntryConflict(source, source.name ?: "")) {
//                    return AnalyzeQualifiedElementResult.Skip
//                }
//            }

            return AnalyzeQualifiedElementResult.ShortenNow
        }

        override fun shortenElement(element: CjDotQualifiedExpression, options: Options): CjElement {
            val receiver = element.receiverExpression
            val selector = element.selectorExpression ?: return element

            return when (receiver) {
                is CjSimpleNameExpression -> {
                    val identifier = receiver.getIdentifier() ?: return element
                    (selector.getCalleeExpressionIfAny() as? CjSimpleNameExpression)?.getIdentifier()
                        ?.replace(identifier)
                    element.replace(selector) as CjExpression
                }

                is CjQualifiedExpression -> {
                    val identifier =
                        (receiver.selectorExpression as? CjSimpleNameExpression)?.getIdentifier() ?: return element
                    (selector.getCalleeExpressionIfAny() as? CjSimpleNameExpression)?.getIdentifier()
                        ?.replace(identifier)
                    receiver.selectorExpression?.replace(selector)
                    element.replace(receiver) as CjExpression
                }

                else -> element
            }
        }
    }
}

private fun PsiElement.isRootPrefix(): Boolean = when (this) {
    is CjDotQualifiedExpression -> receiverExpression.text == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE
    is CjUserType -> qualifier?.text == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE
    else -> false
}


