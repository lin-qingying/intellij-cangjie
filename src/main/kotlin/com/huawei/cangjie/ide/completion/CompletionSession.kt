package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.analyzer.ModuleOrigin
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.doc.psi.impl.CDocLink
import com.huawei.cangjie.doc.psi.impl.CDocName
import com.huawei.cangjie.ide.CangJieIndicesHelper
import com.huawei.cangjie.ide.ExpectedInfo
import com.huawei.cangjie.ide.codeinsight.ReferenceVariantsHelper
import com.huawei.cangjie.ide.fuzzyType
import com.huawei.cangjie.ide.imports.importableFqName
import com.huawei.cangjie.ide.isExcludedFromAutoImport
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.getStrictParentOfType
import com.huawei.cangjie.psi.psiUtil.isInsideAnnotationEntryArgumentList
import com.huawei.cangjie.psi.psiUtil.parents
import com.huawei.cangjie.references.mainReference
import com.huawei.cangjie.references.resolveCDocLink
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.caches.getResolutionFacade
import com.huawei.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.huawei.cangjie.resolve.calls.util.receiverTypesWithIndex
import com.huawei.cangjie.resolve.descriptorUtil.denotedClassDescriptor
import com.huawei.cangjie.resolve.descriptorUtil.module
import com.huawei.cangjie.resolve.isVisible
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.resolve.scopes.getResolutionScope
import com.huawei.cangjie.resolve.scopes.getResolveScope
import com.huawei.cangjie.types.FuzzyType
import com.huawei.cangjie.types.toFuzzyType
import com.huawei.cangjie.types.util.makeNotNullable
import com.huawei.cangjie.utils.CallTypeAndReceiver
import com.huawei.cangjie.utils.ReceiverType
import com.huawei.cangjie.utils.match
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
//    val useBetterPrefixMatcherForNonImportedClasses: Boolean,
    val nonAccessibleDeclarations: Boolean,

    val staticMembers: Boolean,
//    val dataClassComponentFunctions: Boolean,
//    val excludeEnumEntries: Boolean,
)

fun CompletionSessionConfiguration(parameters: CompletionParameters) = CompletionSessionConfiguration(
//    useBetterPrefixMatcherForNonImportedClasses = parameters.invocationCount < 2,
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
