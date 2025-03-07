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

import cn.cangnova.cangjie.NotPropertiesService
import cn.cangnova.cangjie.analyzer.ModuleOrigin
import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.doc.psi.impl.CDocLink
import cn.cangnova.cangjie.doc.psi.impl.CDocName
import cn.cangnova.cangjie.ide.CangJieIndicesHelper
import cn.cangnova.cangjie.ide.ExpectedInfo
import cn.cangnova.cangjie.ide.codeinsight.ReferenceVariantsHelper
import cn.cangnova.cangjie.ide.completion.handlers.InsertHandlerProvider
import cn.cangnova.cangjie.ide.fuzzyType
import cn.cangnova.cangjie.ide.imports.ImportInsertHelper
import cn.cangnova.cangjie.ide.imports.importableFqName
import cn.cangnova.cangjie.ide.isExcludedFromAutoImport
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import cn.cangnova.cangjie.psi.psiUtil.isInsideAnnotationEntryArgumentList
import cn.cangnova.cangjie.psi.psiUtil.parents
import cn.cangnova.cangjie.references.mainReference
import cn.cangnova.cangjie.references.resolveCDocLink
import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.resolve.ImportPath
import cn.cangnova.cangjie.resolve.caches.getResolutionFacade
import cn.cangnova.cangjie.resolve.caches.resolveToDescriptorIfAny
import cn.cangnova.cangjie.resolve.calls.util.receiverTypesWithIndex
import cn.cangnova.cangjie.resolve.compareDescriptors
import cn.cangnova.cangjie.resolve.descriptorUtil.denotedClassDescriptor
import cn.cangnova.cangjie.resolve.descriptorUtil.module
import cn.cangnova.cangjie.resolve.isVisible
import cn.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import cn.cangnova.cangjie.resolve.scopes.getResolutionScope
import cn.cangnova.cangjie.resolve.scopes.getResolveScope
import cn.cangnova.cangjie.resolve.scopes.receivers.ExpressionReceiver
import cn.cangnova.cangjie.types.FuzzyType
import cn.cangnova.cangjie.types.checker.CangJieTypeChecker
import cn.cangnova.cangjie.types.toFuzzyType
import cn.cangnova.cangjie.types.util.TypeUtils
import cn.cangnova.cangjie.types.util.makeNotNullable
import cn.cangnova.cangjie.utils.*
import cn.cangnova.cangjie.utils.fqname.ImportableFqNameClassifier
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher
import com.intellij.codeInsight.completion.impl.RealPrefixMatchingWeigher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ProcessingContext


class CompletionSessionConfiguration(
    val useBetterPrefixMatcherForNonImportedClasses: Boolean,
    val nonAccessibleDeclarations: Boolean,

    val staticMembers: Boolean,
//    val dataClassComponentFunctions: Boolean,
//    val excludeEnumEntries: Boolean,
)

fun CompletionSessionConfiguration(parameters: CompletionParameters) = CompletionSessionConfiguration(
    useBetterPrefixMatcherForNonImportedClasses = parameters.invocationCount < 2,
    nonAccessibleDeclarations = parameters.invocationCount >= 2,
//    javaGettersAndSetters = parameters.invocationCount >= 2,
//    javaClassesNotToBeUsed = parameters.invocationCount >= 2,
    staticMembers = parameters.invocationCount >= 2,
//    dataClassComponentFunctions = parameters.invocationCount >= 2,
//    excludeEnumEntries = !parameters.position.languageVersionSettings.supportsFeature(LanguageFeature.EnumEntries),
)

abstract class CompletionSession(
    protected val configuration: CompletionSessionConfiguration,
    originalParameters: CompletionParameters,
    resultSet: CompletionResultSet
) {
    init {
        CompletionBenchmarkSink.instance.onCompletionStarted(this)
    }

    protected val parameters = run {
        val fixedPosition = addParamTypesIfNeeded(originalParameters.position)
        originalParameters.withPosition(fixedPosition, fixedPosition.textOffset)
    }

    //
    protected val toFromOriginalFileMapper = ToFromOriginalFileMapper.create(this.parameters)
    protected val position = this.parameters.position

    protected val file = position.containingFile as CjFile
    protected val resolutionFacade = file.getResolutionFacade()
    protected val moduleDescriptor = resolutionFacade.moduleDescriptor
    protected val project = position.project


    protected val isDebuggerContext = file is CjCodeFragment

    //
    protected val nameExpression: CjSimpleNameExpression?
    protected val expression: CjExpression?

    protected val applicabilityFilter: (DeclarationDescriptor) -> Boolean

    //
    init {
        val reference = (position.parent as? CjSimpleNameExpression)?.mainReference
        if (reference != null) {
            if (reference.expression is CjLabelReferenceExpression) {
                this.nameExpression = null
                this.expression =
                    reference.expression.parents.match(CjContainerNode::class, last = CjExpressionWithLabel::class)
            } else {
                this.nameExpression = reference.expression
                this.expression = nameExpression
            }
        } else {
            this.nameExpression = null
            this.expression = null
        }

        if (position.isInsideAnnotationEntryArgumentList()) {
            applicabilityFilter = { suggestDescriptorInsideAnnotationEntryArgumentList(it, expectedInfos) }
        } else {
            applicabilityFilter = { true }
        }
    }

    protected val importableFqNameClassifier = ImportableFqNameClassifier(file) {
        ImportInsertHelper.getInstance(file.project).isImportedWithDefault(ImportPath(it, false), file)
    }

    protected val isVisibleFilter: (DeclarationDescriptor) -> Boolean =
        { isVisibleDescriptor(it, completeNonAccessible = configuration.nonAccessibleDeclarations) }
    protected val prefix = CompletionUtil.findIdentifierPrefix(
        originalParameters.position.containingFile,
        originalParameters.offset,
        cangjieIdentifierPartPattern(),
        cangjieIdentifierStartPattern()
    )

    protected val bindingContext =
        CompletionBindingContextProvider.getInstance(project).getBindingContext(position, resolutionFacade)

    protected val prefixMatcher = CamelHumpMatcher(prefix)

    protected val callTypeAndReceiver =
        if (nameExpression == null) CallTypeAndReceiver.UNKNOWN else CallTypeAndReceiver.detect(nameExpression)
    protected val allowExpectedDeclarations = false

    //    // LookupElementsCollector instantiation is deferred because virtual call to createSorter uses data from derived classes
    protected val collector: LookupElementsCollector by lazy(LazyThreadSafetyMode.NONE) {
        LookupElementsCollector(
            { CompletionBenchmarkSink.instance.onFlush(this) },
            prefixMatcher, originalParameters, resultSet,
            createSorter(), (file as? CjCodeFragment)?.extraCompletionFilter,
            allowExpectedDeclarations,

            )
    }
    protected val basicLookupElementFactory =
        BasicLookupElementFactory(
            project,
            InsertHandlerProvider(callTypeAndReceiver.callType, parameters.editor) { expectedInfos })
    private val inDescriptor = position.getResolutionScope(bindingContext, resolutionFacade).ownerDescriptor

    protected val receiverTypes: List<ReceiverType>? =
        nameExpression?.let { detectReceiverTypes(bindingContext, nameExpression, callTypeAndReceiver) }
            ?: (position.parent as? CDocName)?.let { detectReceiverTypesForCDocName(bindingContext, it) }

    protected val descriptorNameFilter: (String) -> Boolean = prefixMatcher.asStringNameFilter()
    protected abstract val descriptorKindFilter: DescriptorKindFilter?

    //    protected val referenceVariantsHelper = ReferenceVariantsHelper(
//        bindingContext,
//        resolutionFacade,
//        moduleDescriptor,
//        isVisibleFilter,
//        NotPropertiesService.getNotProperties(position)
//    )
    protected abstract val expectedInfos: Collection<ExpectedInfo>
    protected val searchScope: GlobalSearchScope =
        getResolveScope(originalParameters.originalFile as CjFile)
    protected val isVisibleFilterCheckAlways: (DeclarationDescriptor) -> Boolean =
        { isVisibleDescriptor(it, completeNonAccessible = false) }
    protected val referenceVariantsHelper = ReferenceVariantsHelper(
        bindingContext,
        resolutionFacade,
        moduleDescriptor,
        isVisibleFilter,
        NotPropertiesService.getNotProperties(position)
    )

    protected val shadowedFilter: ((Collection<DeclarationDescriptor>) -> Collection<DeclarationDescriptor>)? by lazy {
        ShadowedDeclarationsFilter.create(
            bindingContext = bindingContext,
            resolutionFacade = resolutionFacade,
            context = nameExpression!!,
            callTypeAndReceiver = callTypeAndReceiver,
        )?.createNonImportedDeclarationsFilter(
            importedDeclarations = referenceVariantsCollector!!.allCollected.imported,
            allowExpectedDeclarations = allowExpectedDeclarations,
        )
    }
    protected val referenceVariantsCollector = if (nameExpression != null) {
        ReferenceVariantsCollector(
            referenceVariantsHelper = referenceVariantsHelper,
            indicesHelper = indicesHelper(true),
            prefixMatcher = prefixMatcher,
            applicabilityFilter = applicabilityFilter,
            nameExpression = nameExpression,
            callTypeAndReceiver = callTypeAndReceiver,
            resolutionFacade = resolutionFacade,
            bindingContext = bindingContext,
            importableFqNameClassifier = importableFqNameClassifier,
            configuration = configuration,
            allowExpectedDeclarations = allowExpectedDeclarations,
        )
    } else {
        null
    }

    private fun isVisibleDescriptor(descriptor: DeclarationDescriptor, completeNonAccessible: Boolean): Boolean {

        if (descriptor is TypeParameterDescriptor && !isTypeParameterVisible(descriptor)) return false

        if (descriptor is DeclarationDescriptorWithVisibility) {
            val visible = descriptor.isVisible(
                position,
                callTypeAndReceiver.receiver as? CjExpression,
                bindingContext,
                resolutionFacade
            )
            if (visible) return true
            return completeNonAccessible && (!descriptor.isFromLibrary() || isDebuggerContext)
        }

        val fqName = descriptor.importableFqName
        return fqName == null || !fqName.isExcludedFromAutoImport(project, file)
    }

    private fun isTypeParameterVisible(typeParameter: TypeParameterDescriptor): Boolean {
        val owner = typeParameter.containingDeclaration
        var parent: DeclarationDescriptor? = inDescriptor
        while (parent != null) {
            if (parent == owner) return true
            if (parent is ClassDescriptor) return false
            parent = parent.containingDeclaration
        }
        return true
    }

    private fun DeclarationDescriptor.isFromLibrary(): Boolean {
        if (module.getCapability(OriginCapability) == ModuleOrigin.LIBRARY) return true

        if (this is CallableMemberDescriptor && kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            return overriddenDescriptors.all { it.isFromLibrary() }
        }

        return false
    }


    protected fun getRuntimeReceiverTypeReferenceVariants(lookupElementFactory: LookupElementFactory): Pair<ReferenceVariants, LookupElementFactory>? {
        val evaluator = file.getCopyableUserData(CodeFragmentUtils.RUNTIME_TYPE_EVALUATOR) ?: return null
        val referenceVariants = referenceVariantsCollector?.allCollected ?: return null

        val explicitReceiver = callTypeAndReceiver.receiver as? CjExpression ?: return null
        val type = bindingContext.getType(explicitReceiver) ?: return null
        if (!TypeUtils.canHaveSubtypes(CangJieTypeChecker.DEFAULT, type)) return null

        val runtimeType = evaluator(explicitReceiver)
        if (runtimeType == null || runtimeType == type) return null

        val expressionReceiver = ExpressionReceiver.create(explicitReceiver, runtimeType, bindingContext)
        val (variants, notImportedExtensions) = ReferenceVariantsCollector(
            referenceVariantsHelper = referenceVariantsHelper,
            indicesHelper = indicesHelper(true),
            prefixMatcher = prefixMatcher,
            applicabilityFilter = applicabilityFilter,
            nameExpression = nameExpression!!,
            callTypeAndReceiver = callTypeAndReceiver,
            resolutionFacade = resolutionFacade,
            bindingContext = bindingContext,
            importableFqNameClassifier = importableFqNameClassifier,
            configuration = configuration,
            allowExpectedDeclarations = allowExpectedDeclarations,
            runtimeReceiver = expressionReceiver,
        ).collectReferenceVariants(descriptorKindFilter!!)

        val filteredVariants = filterVariantsForRuntimeReceiverType(variants, referenceVariants.imported)
        val filteredNotImportedExtensions =
            filterVariantsForRuntimeReceiverType(notImportedExtensions, referenceVariants.notImportedExtensions)

        val runtimeVariants = ReferenceVariants(filteredVariants, filteredNotImportedExtensions)
        return Pair(runtimeVariants, lookupElementFactory.copy(receiverTypes = listOf(ReceiverType(runtimeType, 0))))
    }

    protected fun processTopLevelCallables(processor: (DeclarationDescriptor) -> Unit) {
        indicesHelper(true).processTopLevelCallables({ prefixMatcher.prefixMatches(it) }) {
            processWithShadowedFilter(it, processor)
        }
    }
    protected open fun shouldCompleteTopLevelCallablesFromIndex(): Boolean {
        if (nameExpression == null) return false
        if ((descriptorKindFilter?.kindMask ?: 0).and(DescriptorKindFilter.CALLABLES_MASK) == 0) return false
        if (callTypeAndReceiver is CallTypeAndReceiver.IMPORT_DIRECTIVE) return false
        return callTypeAndReceiver.receiver == null
    }

    protected inline fun <reified T : DeclarationDescriptor> processWithShadowedFilter(
        descriptor: T,
        processor: (T) -> Unit
    ) {
        val shadowedFilter = shadowedFilter
        val element = if (shadowedFilter != null) {
            shadowedFilter(listOf(descriptor)).singleOrNull()?.let { it as T }
        } else {
            descriptor
        }

        element?.let(processor)
    }

    protected fun withContextVariablesProvider(
        contextVariablesProvider: ContextVariablesProvider,
        action: (LookupElementFactory) -> Unit
    ) {
        val lookupElementFactory = createLookupElementFactory(contextVariablesProvider)
        action(lookupElementFactory)
    }

    private fun <TDescriptor : DeclarationDescriptor> filterVariantsForRuntimeReceiverType(
        runtimeVariants: Collection<TDescriptor>,
        baseVariants: Collection<TDescriptor>
    ): Collection<TDescriptor> {
        val baseVariantsByName = baseVariants.groupBy { it.name }
        val result = ArrayList<TDescriptor>()
        for (variant in runtimeVariants) {
            val candidates = baseVariantsByName[variant.name]
            if (candidates == null || candidates.none { compareDescriptors(project, variant, it) }) {
                result.add(variant)
            }
        }
        return result
    }

    protected fun referenceVariantsWithSingleFunctionTypeParameter(): ReferenceVariants? {
        val variants = referenceVariantsCollector?.allCollected ?: return null
        val filter = { descriptor: DeclarationDescriptor ->
            descriptor is FunctionDescriptor && LookupElementFactory.hasSingleFunctionTypeParameter(descriptor)
        }
        return ReferenceVariants(variants.imported.filter(filter), variants.notImportedExtensions.filter(filter))
    }

    protected open fun createLookupElementFactory(contextVariablesProvider: ContextVariablesProvider): LookupElementFactory {
        return LookupElementFactory(
            basicLookupElementFactory, parameters.editor, receiverTypes,
            callTypeAndReceiver.callType, inDescriptor, contextVariablesProvider
        )
    }

    protected fun flushToResultSet() {
        collector.flushToResultSet()
    }

    protected fun detectReceiverTypes(
        bindingContext: BindingContext,
        nameExpression: CjSimpleNameExpression,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>
    ): List<ReceiverType>? {
        var receiverTypes = callTypeAndReceiver.receiverTypesWithIndex(
            bindingContext, nameExpression, moduleDescriptor, resolutionFacade,
            stableSmartCastsOnly = true, /* we don't include smart cast receiver types for "unstable" receiver value to mark members grayed */
            withImplicitReceiversWhenExplicitPresent = true
        )

        if (callTypeAndReceiver is CallTypeAndReceiver.SAFE || isDebuggerContext) {
            receiverTypes = receiverTypes?.map { ReceiverType(it.type.makeNotNullable(), it.receiverIndex) }
        }

        return receiverTypes
    }

    private fun detectReceiverTypesForCDocName(
        context: BindingContext,
        cDocName: CDocName,
    ): List<ReceiverType>? {
        val cDocLink = cDocName.getStrictParentOfType<CDocLink>() ?: return null
        val cDocOwner = cDocName.getContainingDoc().getOwner()
        val cDocOwnerDescriptor = cDocOwner?.resolveToDescriptorIfAny() ?: return null

        return resolveCDocLink(
            context,
            resolutionFacade,
            cDocOwnerDescriptor,
            cDocLink,
            cDocLink.getTagIfSubject(),
            cDocLink.qualifier
        )
            .filterIsInstance<ClassifierDescriptorWithTypeParameters>()
            .mapNotNull { it.denotedClassDescriptor }
            .flatMap { listOfNotNull(it) }
            .map { ReceiverType(it.defaultType, receiverIndex = 0) }
    }

    fun complete(): Boolean {
        return try {
            _complete().also {
                CompletionBenchmarkSink.instance.onCompletionEnded(this, false)
            }
        } catch (pce: ProcessCanceledException) {
            CompletionBenchmarkSink.instance.onCompletionEnded(this, true)
            throw pce
        }
    }

    private fun calcContextForStatisticsInfo(): String? {
        if (expectedInfos.isEmpty()) return null

        var context = expectedInfos
            .mapNotNull { it.fuzzyType?.type?.constructor?.declarationDescriptor?.importableFqName }
            .distinct()
            .singleOrNull()
            ?.let { "expectedType=$it" }

        if (context == null) {
            context = expectedInfos
                .mapNotNull { it.expectedName }
                .distinct()
                .singleOrNull()
                ?.let { "expectedName=$it" }
        }

        return context
    }

    private fun _complete(): Boolean {
        val prefixPattern = StandardPatterns.string().with(
            object : PatternCondition<String>("get or set prefix") {
                override fun accepts(prefix: String, context: ProcessingContext?) = prefix == "get" || prefix == "set"
            }
        )
        collector.restartCompletionOnPrefixChange(prefixPattern)

        val statisticsContext = calcContextForStatisticsInfo()
        if (statisticsContext != null) {
            collector.addLookupElementPostProcessor { lookupElement ->
                // we should put data into the original element because of DecoratorCompletionStatistician
                lookupElement.putUserDataDeep(STATISTICS_INFO_CONTEXT_KEY, statisticsContext)
                lookupElement
            }
        }

        doComplete()
        flushToResultSet()
        return !collector.isResultEmpty
    }

    fun addLookupElementPostProcessor(processor: (LookupElement) -> LookupElement) {
        collector.addLookupElementPostProcessor(processor)
    }


    protected abstract fun doComplete()


    //    protected val importableFqNameClassifier = ImportableFqNameClassifier(file) {
//        ImportInsertHelper.getInstance(file.project).isImportedWithDefault(ImportPath(it, false), file)
//    }
//
    protected open fun createSorter(): CompletionSorter {
        var sorter = CompletionSorter.defaultSorter(parameters, prefixMatcher)!!

//        sorter = sorter.weighBefore(
//            "stats", DeprecatedWeigher, PriorityWeigher, PreferGetSetMethodsToPropertyWeigher,
//            NotImportedWeigher(importableFqNameClassifier),
//            NotImportedStaticMemberWeigher(importableFqNameClassifier),
//            KindWeigher, CallableWeigher
//        )

//        sorter = sorter.weighAfter("stats", VariableOrFunctionWeigher, ImportedWeigher(importableFqNameClassifier))

//        val preferContextElementsWeigher = PreferContextElementsWeigher(inDescriptor)
//        sorter =
//            if (callTypeAndReceiver is CallTypeAndReceiver.SUPER_MEMBERS) { // for completion after "super." strictly prefer the current member
//                sorter.weighBefore("cangjie.deprecated", preferContextElementsWeigher)
//            } else {
//                sorter.weighBefore("cangjie.proximity", preferContextElementsWeigher)
//            }

//        sorter = sorter.weighBefore("middleMatching", PreferMatchingItemWeigher)

        // we insert one more RealPrefixMatchingWeigher because one inserted in default sorter is placed in a bad position (after "stats")
        sorter = sorter.weighAfter("lift.shorter", RealPrefixMatchingWeigher())

//        sorter = sorter.weighAfter("cangjie.proximity", ByNameAlphabeticalWeigher, PreferLessParametersWeigher)

//        sorter = sorter.weighBefore("prefix", K1SoftDeprecationWeigher)

//        sorter = if (expectedInfos.all { it.fuzzyType?.type?.isUnit() == true }) {
//            sorter.weighBefore("prefix", PreferDslMembers)
//        } else {
//            sorter.weighAfter("cangjie.preferContextElements", PreferDslMembers)
//        }

        return sorter
    }

    protected fun withCollectRequiredContextVariableTypes(action: (LookupElementFactory) -> Unit): Collection<FuzzyType> {
        val provider = CollectRequiredTypesContextVariablesProvider()
        val lookupElementFactory = createLookupElementFactory(provider)
        action(lookupElementFactory)
        return provider.requiredTypes
    }

    //    protected val referenceVariantsCollector = if (nameExpression != null) {
//        ReferenceVariantsCollector(
//            referenceVariantsHelper = referenceVariantsHelper,
//            indicesHelper = indicesHelper(true),
//            prefixMatcher = prefixMatcher,
//            applicabilityFilter = applicabilityFilter,
//            nameExpression = nameExpression,
//            callTypeAndReceiver = callTypeAndReceiver,
//            resolutionFacade = resolutionFacade,
//            bindingContext = bindingContext,
//            importableFqNameClassifier = importableFqNameClassifier,
//            configuration = configuration,
//            allowExpectedDeclarations = allowExpectedDeclarations,
//        )
//    } else {
//        null
//    }
    protected fun indicesHelper(mayIncludeInaccessible: Boolean): CangJieIndicesHelper {
        val visibilityFilter = if (mayIncludeInaccessible) isVisibleFilter else isVisibleFilterCheckAlways
        return CangJieIndicesHelper(
            resolutionFacade,
            searchScope,
            visibilityFilter,
            applicabilityFilter = applicabilityFilter,
            filterOutPrivate = !mayIncludeInaccessible,
            declarationTranslator = { toFromOriginalFileMapper.toSyntheticFile(it) },
            file = file
        )
    }

    companion object {
        private fun suggestDescriptorInsideAnnotationEntryArgumentList(
            descriptor: DeclarationDescriptor,
            expectedInfos: Collection<ExpectedInfo>,
        ): Boolean {
//            if (descriptor.annotations.any { it.classId == StandardClassIds.Annotations.IntrinsicConstEvaluation }) return true

            if (descriptor !is CallableDescriptor) return true

            return when (descriptor) {
//                is VariableCallableDescriptor -> descriptor.isConst
                is FunctionDescriptor -> {
//                    if (descriptor.fqNameOrNull() !in ArrayFqNames.ARRAY_CALL_FQ_NAMES) return false

                    val fuzzyType = descriptor.returnType?.toFuzzyType(descriptor.typeParameters) ?: return false
                    expectedInfos.isEmpty() || expectedInfos.any { it.fuzzyType?.checkIsSubtypeOf(fuzzyType) != null }
                }

                else -> false
            }
        }
    }
}

val CDocLink.qualifier: List<String> get() = getLinkText().split('.').dropLast(1)
