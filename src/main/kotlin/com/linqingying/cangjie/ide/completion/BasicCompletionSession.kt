package com.linqingying.cangjie.ide.completion

import com.linqingying.cangjie.NotPropertiesService
import com.linqingying.cangjie.analyzer.analyzeInContext
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.ide.CangJieIndicesHelper
import com.linqingying.cangjie.ide.ExpectedInfo
import com.linqingying.cangjie.ide.codeinsight.ReferenceVariantsHelper
import com.linqingying.cangjie.ide.completion.keywords.DefaultCompletionKeywordHandlerProvider
import com.linqingying.cangjie.ide.completion.keywords.KeywordCompletion
import com.linqingying.cangjie.ide.completion.keywords.KeywordValues
import com.linqingying.cangjie.ide.completion.keywords.createLookups
import com.linqingying.cangjie.ide.completion.smart.ExpectedInfoMatch
import com.linqingying.cangjie.ide.completion.smart.SMART_COMPLETION_ITEM_PRIORITY_KEY
import com.linqingying.cangjie.ide.completion.smart.SmartCompletion
import com.linqingying.cangjie.ide.completion.smart.SmartCompletionItemPriority
import com.linqingying.cangjie.ide.imports.importableFqName
import com.linqingying.cangjie.ide.indices.CangJiePackageIndexUtils
import com.linqingying.cangjie.ide.projectStructure.languageVersionSettings
import com.linqingying.cangjie.lexer.CjModifierKeywordToken
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.*
import com.linqingying.cangjie.references.CjSimpleNameReference
import com.linqingying.cangjie.references.mainReference
import com.linqingying.cangjie.renderer.render
import com.linqingying.cangjie.resolve.*
import com.linqingying.cangjie.resolve.descriptorUtil.getImportableDescriptor
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
import com.linqingying.cangjie.resolve.sam.SamConstructorDescriptor
import com.linqingying.cangjie.resolve.sam.SamConstructorDescriptorKindExclude
import com.linqingying.cangjie.resolve.scopes.*
import com.linqingying.cangjie.types.FuzzyType
import com.linqingying.cangjie.utils.CallType
import com.linqingying.cangjie.utils.CallTypeAndReceiver
import com.linqingying.cangjie.utils.safeAs
import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.addingPolicy.PolicyController
import com.intellij.codeInsight.completion.impl.BetterPrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Key
import com.intellij.platform.ml.impl.turboComplete.CompletionKind
import com.intellij.platform.ml.impl.turboComplete.SuggestionGeneratorConsumer
import com.intellij.platform.ml.impl.turboComplete.SuggestionGeneratorWithArtifact
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.GlobalSearchScope

class BasicCompletionSession(
    configuration: CompletionSessionConfiguration,
    completionParameters: CompletionParameters,
    private val policyController: PolicyController,
    private val suggestionGeneratorConsumer: SuggestionGeneratorConsumer,
) : CompletionSession(configuration, completionParameters, policyController.getObeyingResultSet()) {

    private interface CompletionCategory {
        val descriptorKindFilter: DescriptorKindFilter?
        fun generateCategories()
        fun shouldDisableAutoPopup(): Boolean = false
        fun addWeighers(sorter: CompletionSorter): CompletionSorter = sorter
    }

    val isNothingAddedToResult: Boolean
        get() = collector.isResultEmpty
    private val NAMED_ARGUMENTS_ONLY = object : OneKindCompletionCategory(CangJieCompletionKindName.NAMED_ARGUMENT) {
        override val descriptorKindFilter: DescriptorKindFilter? get() = null
        override fun fillResultSet(): Unit =
            NamedArgumentCompletion.complete(collector, expectedInfos, callTypeAndReceiver.callType)
    }

    private fun detectCompletionCategory(): CompletionCategory {
        if (nameExpression == null) {
            return if ((position.parent as? CjNamedDeclaration)?.nameIdentifier == position) DECLARATION_NAME else KEYWORDS_ONLY
        }

        if (OPERATOR_NAME.isApplicable()) {
            return OPERATOR_NAME
        }

        if (NamedArgumentCompletion.isOnlyNamedArgumentExpected(nameExpression, resolutionFacade)) {
            return NAMED_ARGUMENTS_ONLY
        }
//
        if (nameExpression.getStrictParentOfType<CjSuperExpression>() != null) {
            return SUPER_QUALIFIER
        }

        return ALL
    }

    private val ALL = object : CompletionCategory {
        override val descriptorKindFilter: DescriptorKindFilter by lazy {
            callTypeAndReceiver.callType.descriptorKindFilter.let { filter ->
                filter.takeIf { it.kindMask.and(DescriptorKindFilter.PACKAGES_MASK) != 0 }
                    ?.exclude(DescriptorKindExclude.TopLevelPackages)
                    ?: filter
            }
        }

        private fun isStartOfExtensionReceiverFor(): CjCallableDeclaration? {
            val userType = nameExpression!!.parent as? CjUserType ?: return null
            if (userType.qualifier != null) return null
            val typeRef = userType.parent as? CjTypeReference ?: return null
            if (userType != typeRef.typeElement) return null
            return when (val parent = typeRef.parent) {
                is CjNamedFunction -> parent.takeIf { typeRef == it.receiverTypeReference }
                is CjVariable -> parent.takeIf { typeRef == it.receiverTypeReference }
                else -> null
            }
        }

        override fun generateCategories() {


            fun addReferenceVariants(lookupElementFactory: LookupElementFactory, referenceVariants: ReferenceVariants) {
                collector.addDescriptorElements(
                    referenceVariantsHelper.excludeNonInitializedVariable(referenceVariants.imported, position),
                    lookupElementFactory, prohibitDuplicates = true
                )

                collector.addDescriptorElements(
                    referenceVariants.notImportedExtensions, lookupElementFactory,
                    notImported = true, prohibitDuplicates = true
                )
            }

            fun makeReferenceSuggestionGenerators(
                descriptors: List<DescriptorKindFilter>,
                lookupElementFactory: LookupElementFactory
            ): List<SuggestionGeneratorWithArtifact<Unit>> {
                val generators = descriptors.map { descriptorKindFilter ->
                    referenceVariantsCollector!!.makeReferenceVariantsCollectors(descriptorKindFilter)
                }

                val basicReferencesKind =
                    suggestionGeneratorForCompletionKind(CangJieCompletionKindName.REFERENCE_BASIC) {
                        generators.forEach {
                            addReferenceVariants(lookupElementFactory, it.basic.value)
                        }
                    }

                val extensionReferencesKind =
                    suggestionGeneratorForCompletionKind(CangJieCompletionKindName.REFERENCE_EXTENSION) {
                        generators.forEach {
                            addReferenceVariants(lookupElementFactory, it.extensions.value)
                        }
                    }

                return listOf(basicReferencesKind, extensionReferencesKind)
            }

            fun collectReferences(descriptors: List<DescriptorKindFilter>): Lazy<Set<FuzzyType>> {
                val provider = CollectRequiredTypesContextVariablesProvider()
                val lookupElementFactory = createLookupElementFactory(provider)
                val generators = makeReferenceSuggestionGenerators(descriptors, lookupElementFactory)
                /**
                 * Acknowledge the generators' existence, passing them to the consumer.
                 * So the consumer (which is a [com.intellij.turboComplete.SuggestionGeneratorExecutor])
                 * could use it at its discretion.
                 */
                generators.forEach { suggestionGeneratorConsumer.pass(it) }
                return lazy {
                    /**
                     * Make that all generators generated their artifacts by the moment, when
                     * the one who addressed this function's return value
                     */
                    generators.forEach { it.getArtifact() }
                    referenceVariantsCollector!!.collectingFinished()
                    provider.requiredTypes

                }
            }

            fun completeWithSmartCompletion(lookupElementFactory: LookupElementFactory) {
                if (smartCompletion != null) {
                    val (additionalItems, @Suppress("UNUSED_VARIABLE") inheritanceSearcher) = smartCompletion!!.additionalItems(
                        lookupElementFactory
                    )

                    // all additional items should have SMART_COMPLETION_ITEM_PRIORITY_KEY to be recognized by SmartCompletionInBasicWeigher
                    for (item in additionalItems) {
                        if (item.getUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY) == null) {
                            item.putUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY, SmartCompletionItemPriority.DEFAULT)
                        }
                    }

                    collector.addElements(additionalItems)
                }
            }

            val declaration = isStartOfExtensionReceiverFor()
            if (declaration != null) {
                completeDeclarationNameFromUnresolvedOrOverride(declaration)


                // no auto-popup on typing after "let", "var" and "fun" because it's likely the name of the declaration which is being typed by user
                if (parameters.invocationCount == 0 && (
                            // suppressOtherCompletion
                            declaration !is CjNamedFunction && declaration !is CjVariable ||
                                    prefixMatcher.prefix.let { it.isEmpty() || it[0].isLowerCase() /* function name usually starts with lower case letter */ }
                            )
                ) {
                    if (declaration is CjNamedFunction &&
                        declaration.modifierList?.allChildren.orEmpty()
                            .map { it.node.elementType }
                            .none { it is CjModifierKeywordToken && it !in CjTokens.VISIBILITY_MODIFIERS }
                    ) {
                        KEYWORDS_ONLY.generateCategories()
                    }
                    return
                }
            }


            KEYWORDS_ONLY.generateCategories()
            val contextVariableTypesForSmartCompletion = withCollectRequiredContextVariableTypes(
                CangJieCompletionKindName.SMART_ADDITIONAL_ITEM,
                ::completeWithSmartCompletion
            )

            val descriptors = when {
                prefix.isEmpty() ||
                        callTypeAndReceiver.receiver != null ||
                        CodeInsightSettings.getInstance().completionCaseSensitive == CodeInsightSettings.NONE
                    -> {
                    listOf(descriptorKindFilter)
                }

                prefix[0].isLowerCase() -> {
                    listOf(
                        USUALLY_START_LOWER_CASE.intersect(descriptorKindFilter),
                        USUALLY_START_UPPER_CASE.intersect(descriptorKindFilter)
                    )
                }

                else -> {
                    listOf(
                        USUALLY_START_UPPER_CASE.intersect(descriptorKindFilter),
                        USUALLY_START_LOWER_CASE.intersect(descriptorKindFilter)
                    )
                }
            }
            val references = collectReferences(descriptors)
            // getting root packages from scope is very slow so we do this in alternative way
            if (callTypeAndReceiver.receiver == null &&
                callTypeAndReceiver.callType.descriptorKindFilter.kindMask.and(DescriptorKindFilter.PACKAGES_MASK) != 0
            ) {
                addKind(CangJieCompletionKindName.PACKAGE_NAME) {
                    //TODO: move this code somewhere else?
                    val packageNames = CangJiePackageIndexUtils.getSubPackageFqNames(
                        FqName.ROOT,
                        GlobalSearchScope.allScope(project),
                        prefixMatcher.asNameFilter()
                    )
                        .toHashSet()


                    packageNames.forEach {
                        collector.addElement(
                            basicLookupElementFactory.createLookupElementForPackage(
                                it
                            )
                        )
                    }
                }
            }
            addKind(CangJieCompletionKindName.NAMED_ARGUMENT) {
                NamedArgumentCompletion.complete(collector, expectedInfos, callTypeAndReceiver.callType)
            }
            val contextVariablesProvider = RealContextVariablesProvider(referenceVariantsHelper, position)
            withContextVariablesProvider(contextVariablesProvider) { lookupElementFactory ->
                if (receiverTypes != null) {
                    addKind(CangJieCompletionKindName.EXTENSION_FUNCTION_TYPE_VALUE) {
                        ExtensionFunctionTypeValueCompletion(receiverTypes, callTypeAndReceiver.callType, lookupElementFactory)
                            .processVariables(contextVariablesProvider)
                            .forEach {
                                val lookupElements = it.factory.createStandardLookupElementsForDescriptor(
                                    it.invokeDescriptor,
                                    useReceiverTypes = true,
                                )
                                collector.addElements(lookupElements)
                            }
                    }
                }

                addKind(CangJieCompletionKindName.CONTEXT_VARIABLE_TYPE_SC) {
                    if (contextVariableTypesForSmartCompletion.getArtifact().any {
                            contextVariablesProvider.functionTypeVariables(it).isNotEmpty()
                        }) {
                        completeWithSmartCompletion(lookupElementFactory)
                    }
                }

                addKind(CangJieCompletionKindName.CONTEXT_VARIABLE_TYPE_REFERENCE) {
                    if (references.value.any { contextVariablesProvider.functionTypeVariables(it).isNotEmpty() }) {
                        val (imported, notImported) = referenceVariantsWithSingleFunctionTypeParameter()!!
                        collector.addDescriptorElements(imported, lookupElementFactory)
                        collector.addDescriptorElements(notImported, lookupElementFactory, notImported = true)
                    }
                }

                val staticMembersCompletion = lazy {
                    references.value
                    StaticMembersCompletion(
                        prefixMatcher,
                        resolutionFacade,
                        lookupElementFactory,
                        referenceVariantsCollector!!.allCollected.imported,

                        )
                }

                if (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT) {
                    addKind(CangJieCompletionKindName.STATIC_MEMBER_FROM_IMPORTS) {
                        staticMembersCompletion.value.completeFromImports(file, collector)
                    }
                }

                addKind(CangJieCompletionKindName.NON_IMPORTED) {
                    contextVariableTypesForSmartCompletion.getArtifact()
                    references.value
                    completeNonImported(lookupElementFactory)
                }

                if (isDebuggerContext) {
                    addKind(CangJieCompletionKindName.DEBUGGER_VARIANTS) {
                        val variantsAndFactory = getRuntimeReceiverTypeReferenceVariants(lookupElementFactory)
                        if (variantsAndFactory != null) {
                            val variants = variantsAndFactory.first
                            val resultLookupElementFactory = variantsAndFactory.second
                            collector.addDescriptorElements(
                                variants.imported,
                                resultLookupElementFactory,
                                withReceiverCast = true
                            )
                            collector.addDescriptorElements(
                                variants.notImportedExtensions,
                                resultLookupElementFactory,
                                withReceiverCast = true,
                                notImported = true
                            )
                        }
                    }
                }

                if (!receiverTypes.isNullOrEmpty()) {
                    // N.B.: callable references to member extensions are forbidden
                    val shouldCompleteExtensionsFromObjects = when (callTypeAndReceiver.callType) {
                        CallType.DEFAULT, CallType.DOT, CallType.SAFE -> true
                        else -> false
                    }

                    if (shouldCompleteExtensionsFromObjects) {
                        val receiverCangJieTypes by lazy { receiverTypes.map { it.type } }

                        addKind(CangJieCompletionKindName.STATIC_MEMBER_OBJECT_MEMBER) {
                            staticMembersCompletion.value.completeObjectMemberExtensionsFromIndices(
                                indicesHelper(mayIncludeInaccessible = false),
                                receiverCangJieTypes,
                                callTypeAndReceiver,
                                collector
                            )
                        }

                        addKind(CangJieCompletionKindName.STATIC_MEMBER_EXPLICIT_INHERITED) {
                            staticMembersCompletion.value.completeExplicitAndInheritedMemberExtensionsFromIndices(
                                indicesHelper(mayIncludeInaccessible = false),
                                receiverCangJieTypes,
                                callTypeAndReceiver,
                                collector
                            )
                        }
                    }
                }

                if (configuration.staticMembers && prefix.isNotEmpty()) {
                    if (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT) {
                        addKind(CangJieCompletionKindName.STATIC_MEMBER_INACCESSIBLE) {
                            staticMembersCompletion.value.completeFromIndices(indicesHelper(false), collector)
                        }
                    }
                }
            }

        }

        private fun completeNonImported(lookupElementFactory: LookupElementFactory) {
            if (shouldCompleteTopLevelCallablesFromIndex()) {
                processTopLevelCallables {
                    collector.addDescriptorElements(it, lookupElementFactory, notImported = true)
                    flushToResultSet()
                }
            }

            if (callTypeAndReceiver.receiver == null && prefix.isNotEmpty()) {
                val classKindFilter: ((ClassKind) -> Boolean)? = when (callTypeAndReceiver) {
                    is CallTypeAndReceiver.ANNOTATION -> {
                        { it == ClassKind.ANNOTATION_CLASS }
                    }

                    is CallTypeAndReceiver.DEFAULT, is CallTypeAndReceiver.TYPE -> {
                        { it != ClassKind.ENUM_ENTRY }
                    }

                    else -> null
                }

                if (classKindFilter != null) {
                    val prefixMatcher = if (configuration.useBetterPrefixMatcherForNonImportedClasses)
                        BetterPrefixMatcher(prefixMatcher, collector.bestMatchingDegree)
                    else
                        prefixMatcher

                    addClassesFromIndex(
                        kindFilter = classKindFilter,
                        prefixMatcher = prefixMatcher,
                        completionParameters = parameters,
                        indicesHelper = indicesHelper(true),
                        classifierDescriptorCollector = {
                            collector.addElement(basicLookupElementFactory.createLookupElement(it), notImported = true)
                        },

                        )
                }
            } else if (callTypeAndReceiver is CallTypeAndReceiver.DOT) {
                val qualifier = bindingContext[BindingContext.QUALIFIER, callTypeAndReceiver.receiver]
                if (qualifier != null) return
                val receiver = callTypeAndReceiver.receiver as? CjSimpleNameExpression ?: return
                val descriptors = mutableListOf<ClassifierDescriptorWithTypeParameters>()
                val fullTextPrefixMatcher = object : PrefixMatcher(receiver.getReferencedName()) {
                    override fun prefixMatches(name: String): Boolean = name == prefix
                    override fun cloneWithPrefix(prefix: String): PrefixMatcher =
                        throw UnsupportedOperationException("Not implemented")
                }

                addClassesFromIndex(
                    kindFilter = { true },
                    prefixMatcher = fullTextPrefixMatcher,
                    completionParameters = parameters.withPosition(receiver, receiver.startOffset),
                    indicesHelper = indicesHelper(false),
                    classifierDescriptorCollector = { descriptors += it },

                    )

                val foundDescriptors = HashSet<DeclarationDescriptor>()
                val classifiers = descriptors.asSequence().filter {

                    it.kind == ClassKind.ENUM ||
                            it.kind == ClassKind.ENUM_ENTRY

                }

                for (classifier in classifiers) {
                    val scope = nameExpression?.getResolutionScope(bindingContext) ?: return

                    val desc = classifier.getImportableDescriptor()
                    val newScope = scope.addImportingScope(ExplicitImportsScope(listOf(desc)))

                    val newContext = (nameExpression.parent as CjExpression).analyzeInContext(newScope)

                    val rvHelper = ReferenceVariantsHelper(
                        newContext,
                        resolutionFacade,
                        moduleDescriptor,
                        isVisibleFilter,
                        NotPropertiesService.getNotProperties(position)
                    )

                    val rvCollector = ReferenceVariantsCollector(
                        referenceVariantsHelper = rvHelper,
                        indicesHelper = indicesHelper(true),
                        prefixMatcher = prefixMatcher,
                        applicabilityFilter = applicabilityFilter,
                        nameExpression = nameExpression,
                        callTypeAndReceiver = callTypeAndReceiver,
                        resolutionFacade = resolutionFacade,
                        bindingContext = newContext,
                        importableFqNameClassifier = importableFqNameClassifier,
                        configuration = configuration,
                        allowExpectedDeclarations = allowExpectedDeclarations,
                    )

                    val receiverTypes = detectReceiverTypes(newContext, nameExpression, callTypeAndReceiver)
                    val factory = lookupElementFactory.copy(
                        receiverTypes = receiverTypes,
                        standardLookupElementsPostProcessor = { lookupElement ->
                            val lookupDescriptor = lookupElement.`object`
                                .safeAs<DescriptorBasedDeclarationLookupObject>()
                                ?.descriptor as? MemberDescriptor
                                ?: return@copy lookupElement

                            if (!desc.isAncestorOf(lookupDescriptor, false)) return@copy lookupElement

                            if (lookupDescriptor is CallableMemberDescriptor &&
                                lookupDescriptor.isExtension &&
                                lookupDescriptor.extensionReceiverParameter?.importableFqName != desc.fqNameSafe
                            ) {
                                return@copy lookupElement
                            }

                            val fqNameToImport =
                                lookupDescriptor.containingDeclaration.importableFqName ?: return@copy lookupElement

                            object : LookupElementDecorator<LookupElement>(lookupElement) {
                                val name = fqNameToImport.shortName()
                                val packageName = fqNameToImport.parent()

                                override fun handleInsert(context: InsertionContext) {
                                    super.handleInsert(context)
                                    context.commitDocument()
                                    val file = context.file as? CjFile
                                    if (file != null) {
                                        val receiverInFile = file.findElementAt(receiver.startOffset)
                                            ?.getParentOfType<CjSimpleNameExpression>(false)
                                            ?: return

                                        receiverInFile.mainReference.bindToFqName(
                                            fqNameToImport,
                                            CjSimpleNameReference.ShorteningMode.FORCED_SHORTENING
                                        )
                                    }
                                }

                                override fun renderElement(presentation: LookupElementPresentation) {
                                    super.renderElement(presentation)
                                    presentation.appendTailText(
                                        CangJieCompletionBundle.message(
                                            "presentation.tail.for.0.in.1",
                                            name,
                                            packageName,
                                        ),
                                        true,
                                    )
                                }
                            }
                        },
                    )

                    rvCollector.collectReferenceVariants(descriptorKindFilter) { (imported, notImportedExtensions) ->
                        val unique = imported.asSequence()
                            .filterNot { it.original in foundDescriptors }
                            .onEach { foundDescriptors += it.original }

                        val uniqueNotImportedExtensions = notImportedExtensions.asSequence()
                            .filterNot { it.original in foundDescriptors }
                            .onEach { foundDescriptors += it.original }

                        collector.addDescriptorElements(
                            unique.toList(), factory,
                            prohibitDuplicates = true
                        )

                        collector.addDescriptorElements(
                            uniqueNotImportedExtensions.toList(), factory,
                            notImported = true, prohibitDuplicates = true
                        )

                        flushToResultSet()
                    }
                }
            }
        }

    }

    private fun addClassesFromIndex(
        kindFilter: (ClassKind) -> Boolean,
        prefixMatcher: PrefixMatcher,
        completionParameters: CompletionParameters,
        indicesHelper: CangJieIndicesHelper,
        classifierDescriptorCollector: (ClassifierDescriptorWithTypeParameters) -> Unit,

        ) {
        AllClassesCompletion(
            parameters = completionParameters,
            cangjieIndicesHelper = indicesHelper,
            prefixMatcher = prefixMatcher,
            resolutionFacade = resolutionFacade,
            kindFilter = kindFilter,
            includeTypeAliases = true,

            ).collect { processWithShadowedFilter(it, classifierDescriptorCollector) }
    }


    fun shouldDisableAutoPopup(): Boolean = completionKind.shouldDisableAutoPopup()
    private val smartCompletion by lazy {
        expression?.let {
            SmartCompletion(
                expression = it,
                resolutionFacade = resolutionFacade,
                bindingContext = bindingContext,
                moduleDescriptor = moduleDescriptor,
                visibilityFilter = isVisibleFilter,
                applicabilityFilter = applicabilityFilter,
                indicesHelper = indicesHelper(false),
                prefixMatcher = prefixMatcher,
                inheritorSearchScope = GlobalSearchScope.EMPTY_SCOPE,
                toFromOriginalFileMapper = toFromOriginalFileMapper,
                callTypeAndReceiver = callTypeAndReceiver,

                forBasicCompletion = true,
            )
        }
    }
    private val completionKind by lazy { detectCompletionCategory() }
    override val descriptorKindFilter: DescriptorKindFilter? get() = completionKind.descriptorKindFilter

    override val expectedInfos: Collection<ExpectedInfo> get() = smartCompletion?.expectedInfos ?: emptyList()

    private abstract inner class OneKindCompletionCategory(private val name: CangJieCompletionKindName) :
        CompletionCategory {
        final override fun generateCategories() {
            suggestionGeneratorConsumer.pass(suggestionGeneratorForCompletionKind(name) {
                fillResultSet()
            })
        }

        abstract fun fillResultSet()
    }

    private fun completeDeclarationNameFromUnresolvedOrOverride(declaration: CjNamedDeclaration) {
        addKind(CangJieCompletionKindName.DECLARATION_NAME_FROM_UNRESOLVED_OVERRIDE) {
            if (declaration is CjCallableDeclaration && declaration.hasModifier(CjTokens.OVERRIDE_KEYWORD)) {
                OverridesCompletion(collector, basicLookupElementFactory).complete(position, declaration)
            } else {
                val referenceScope = referenceScope(declaration) ?: return@addKind
                val originalScope = toFromOriginalFileMapper.toOriginalFile(referenceScope) ?: return@addKind
                val afterOffset = if (referenceScope is CjBlockExpression) parameters.offset else null
                val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
                FromUnresolvedNamesCompletion(collector, prefixMatcher).addNameSuggestions(
                    originalScope,
                    afterOffset,
                    descriptor
                )
            }
        }
    }

    private fun addKind(name: CangJieCompletionKindName, generator: () -> Unit) {
        suggestionGeneratorConsumer.pass(suggestionGeneratorForCompletionKind(name) {
            generator()
        })
    }

    private val OPERATOR_NAME = object : OneKindCompletionCategory(CangJieCompletionKindName.OPERATOR_NAME) {
        override val descriptorKindFilter: DescriptorKindFilter? get() = null

        fun isApplicable(): Boolean {
            if (nameExpression == null || nameExpression != expression) return false
            val func = position.getParentOfType<CjNamedFunction>(strict = false) ?: return false
            val funcNameIdentifier = func.nameIdentifier ?: return false
            val identifierInNameExpression = nameExpression.nextLeaf {
                it is LeafPsiElement && it.elementType == CjTokens.IDENTIFIER
            } ?: return false

            if (!func.hasModifier(CjTokens.OPERATOR_KEYWORD) || identifierInNameExpression != funcNameIdentifier) return false
            val originalFunc = toFromOriginalFileMapper.toOriginalFile(func) ?: return false
            return !originalFunc.isTopLevel || (originalFunc.isExtensionDeclaration())
        }

        override fun fillResultSet() {
            OperatorNameCompletion.doComplete(collector, descriptorNameFilter)
        }
    }
    private val SUPER_QUALIFIER = object : OneKindCompletionCategory(CangJieCompletionKindName.SUPER_QUALIFIER) {
        override val descriptorKindFilter: DescriptorKindFilter
            get() = DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS

        override fun fillResultSet() {
            val classOrObject = position.parents.firstIsInstanceOrNull<CjTypeStatement>() ?: return
            val classDescriptor =
                resolutionFacade.resolveToDescriptor(classOrObject, BodyResolveMode.PARTIAL) as ClassDescriptor
            val superClasses = classDescriptor.defaultType.constructor.supertypesWithAny()
                .mapNotNull { it.constructor.declarationDescriptor as? ClassDescriptor }

//            if (callTypeAndReceiver.receiver != null) {
//                val referenceVariantsSet = referenceVariantsCollector!!.collectReferenceVariants(descriptorKindFilter).imported.toSet()
//                superClasses = superClasses.filter { it in referenceVariantsSet }
//            }

            superClasses
                .map {
                    basicLookupElementFactory.createLookupElement(
                        it,
                        qualifyNestedClasses = true,
                        includeClassTypeArguments = false
                    )
                }
                .forEach { collector.addElement(it) }
        }
    }

    private val DECLARATION_NAME = object : CompletionCategory {
        override val descriptorKindFilter: DescriptorKindFilter? get() = null

        override fun generateCategories() {
            val declaration = declaration()
            if (declaration is CjParameter && !NameWithTypeCompletion.shouldCompleteParameter(declaration)) {
                return // do not complete also keywords and from unresolved references in such case
            }

            addKind(CangJieCompletionKindName.DECLARATION_NAME) {
                collector.addLookupElementPostProcessor { lookupElement ->
                    lookupElement.apply { suppressItemSelectionByCharsOnTyping = true }
                }
            }

            KEYWORDS_ONLY.generateCategories()

            completeDeclarationNameFromUnresolvedOrOverride(declaration)

            when (declaration) {
                is CjParameter -> completeParameterOrVarNameAndType(withType = true)
                is CjTypeStatement -> {

                    addKind(CangJieCompletionKindName.TOP_LEVEL_CLASS_NAME) {
                        completeTopLevelClassName()
                    }

                }
            }
        }

        override fun shouldDisableAutoPopup(): Boolean = when {
            TemplateManager.getInstance(project).getActiveTemplate(parameters.editor) != null -> true
            declaration() is CjParameter && wasAutopopupRecentlyCancelled(parameters) -> true
            else -> false
        }

        override fun addWeighers(sorter: CompletionSorter): CompletionSorter {
            val declaration = declaration()
            return if (declaration is CjParameter && NameWithTypeCompletion.shouldCompleteParameter(declaration))
                sorter.weighBefore("prefix", VariableOrParameterNameWithTypeCompletion.Weigher)
            else
                sorter
        }

        private fun completeTopLevelClassName() {
            val name = parameters.originalFile.virtualFile.nameWithoutExtension
            if (!(Name.isValidIdentifier(name) && Name.identifier(name)
                    .render() == name && name[0].isUpperCase())
            ) return
            if ((parameters.originalFile as CjFile).declarations.any { it is CjTypeStatement && it.name == name }) return

            collector.addElement(LookupElementBuilder.create(name))
        }

        private fun declaration() = position.parent as CjNamedDeclaration
    }

    private fun completeParameterOrVarNameAndType(withType: Boolean) {
        collector.restartCompletionOnPrefixChange(NameWithTypeCompletion.prefixEndsWithUppercaseLetterPattern)
        addKind(CangJieCompletionKindName.PARAMETER_OR_VAR_NAME_AND_TYPE) {
            val nameWithTypeCompletion = VariableOrParameterNameWithTypeCompletion(
                collector,
                basicLookupElementFactory,
                prefixMatcher,
                resolutionFacade,
                withType,
            )
            nameWithTypeCompletion.addFromParametersInFile(position, resolutionFacade, isVisibleFilterCheckAlways)
            nameWithTypeCompletion.addFromImportedClasses(position, bindingContext, isVisibleFilterCheckAlways)
            nameWithTypeCompletion.addFromAllClasses(parameters, indicesHelper(false))
        }
    }

    private fun wasAutopopupRecentlyCancelled(parameters: CompletionParameters) =
        LookupCancelService.getInstance(project).wasAutoPopupRecentlyCancelled(parameters.editor, position.startOffset)

    private val KEYWORDS_ONLY = object : OneKindCompletionCategory(CangJieCompletionKindName.KEYWORD_ONLY) {
        override val descriptorKindFilter: DescriptorKindFilter? get() = null

        private val keywordCompletion = KeywordCompletion(object : KeywordCompletion.LanguageVersionSettingProvider {
            override fun getLanguageVersionSetting(element: PsiElement) = element.languageVersionSettings
            override fun getLanguageVersionSetting(module: Module) = module.languageVersionSettings
        })

        override fun fillResultSet() {
            val keywordsToSkip = HashSet<String>()
            val keywordValueConsumer = object : KeywordValues.Consumer {
                override fun consume(
                    lookupString: String,
                    expectedInfoMatcher: (ExpectedInfo) -> ExpectedInfoMatch,
                    suitableOnPsiLevel: PsiElement.() -> Boolean,
                    priority: SmartCompletionItemPriority,
                    factory: () -> LookupElement
                ) {
                    keywordsToSkip.add(lookupString)
                    val lookupElement = factory()
                    val matched = expectedInfos.any {
                        val match = expectedInfoMatcher(it)
                        assert(!match.makeNotNullable) { "Nullable keyword values not supported" }
                        match.isMatch()
                    }

                    // 'expectedInfos' is filled with the compiler's insight.
                    // In cases like missing import statement or undeclared variable desired data cannot be retrieved. Here is where we can
                    // analyse PSI and calling 'suitableOnPsiLevel()' does the trick.
                    if (matched || (expectedInfos.isEmpty() && position.suitableOnPsiLevel())) {
                        lookupElement.putUserData(SmartCompletionInBasicWeigher.KEYWORD_VALUE_MATCHED_KEY, Unit)
                        lookupElement.putUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY, priority)
                    }
                    collector.addElement(lookupElement)
                }
            }

            KeywordValues.process(
                keywordValueConsumer,
                position,
                callTypeAndReceiver,
                bindingContext,
                resolutionFacade,
                moduleDescriptor,

                )

            keywordCompletion.complete(expression ?: position, collector.resultSet.prefixMatcher) { lookupElement ->
                val keyword = lookupElement.lookupString
                if (keyword in keywordsToSkip) return@complete

                val completionKeywordHandler = DefaultCompletionKeywordHandlerProvider.getHandlerForKeyword(keyword)
                if (completionKeywordHandler != null) {
                    val lookups = completionKeywordHandler.createLookups(parameters, expression, lookupElement, project)
                    collector.addElements(lookups)
                    return@complete
                }

                when (keyword) {
                    // if "this" is parsed correctly in the current context - insert it and all this@xxx items
                    "this" -> {
                        if (expression != null) {
                            collector.addElements(
                                thisExpressionItems(
                                    bindingContext,
                                    expression,
                                    prefix,
                                    resolutionFacade
                                ).map { it.createLookupElement() })
                        } else {
                            // for completion in secondary constructor delegation call
                            collector.addElement(lookupElement)
                        }
                    }

                    // if "return" is parsed correctly in the current context - insert it and all return
                    "return" -> {
                        if (expression != null) {
                            collector.addElements(returnExpressionItems(bindingContext, expression))
                        }
                    }

                    "override" -> {
                        collector.addElement(lookupElement)

                        OverridesCompletion(collector, basicLookupElementFactory).complete(position, declaration = null)
                    }

                    "class" -> {
                        if (callTypeAndReceiver !is CallTypeAndReceiver.CALLABLE_REFERENCE) { // otherwise it should be handled by KeywordValues
                            collector.addElement(lookupElement)
                        }
                    }


//                    "break", "continue" -> {
//                        if (expression != null) {
//                            analyze(expression) {
//                                val cjKeywordToken = when (keyword) {
//                                    "break" -> CjTokens.BREAK_KEYWORD
//                                    "continue" -> CjTokens.CONTINUE_KEYWORD
//                                    else -> error("'$keyword' can only be 'break' or 'continue'")
//                                }
//                                collector.addElements(
//                                    BreakContinueKeywordHandler(cjKeywordToken).createLookups(
//                                        expression
//                                    )
//                                )
//                            }
//                        }
//                    }

                    else -> collector.addElement(lookupElement)
                }
            }
        }
    }

    override fun doComplete() {
        assert(parameters.completionType == CompletionType.BASIC)

        if (parameters.isAutoPopup) {
            collector.addLookupElementPostProcessor { lookupElement ->
                lookupElement.putUserData(LookupCancelService.AUTO_POPUP_AT, position.startOffset)
                lookupElement
            }

            if (isAtFunctionLiteralStart(position)) {
                collector.addLookupElementPostProcessor { lookupElement ->
                    lookupElement.apply { suppressItemSelectionByCharsOnTyping = true }
                }
            }
        }

        collector.addLookupElementPostProcessor { lookupElement ->
            position.argList?.let { lookupElement.argList = it }
            lookupElement
        }

        completionKind.generateCategories()
    }

    private fun <T> suggestionGeneratorForCompletionKind(
        name: CangJieCompletionKindName,
        fillResultSet: () -> T
    ) = object : SuggestionGeneratorWithArtifact<T>(
        CompletionKind(name, CangJieKindVariety), collector.resultSet, policyController, parameters
    ) {
        override fun generateVariantsAndArtifact(): T {
            val artifact = fillResultSet()
            flushToResultSet()
            return artifact
        }
    }

    private fun withCollectRequiredContextVariableTypes(
        kindName: CangJieCompletionKindName,
        action: (LookupElementFactory) -> Unit
    ): SuggestionGeneratorWithArtifact<Set<FuzzyType>> {
        val provider = CollectRequiredTypesContextVariablesProvider()
        val lookupElementFactory = createLookupElementFactory(provider)

        val actionAsKind = suggestionGeneratorForCompletionKind(kindName) {
            action(lookupElementFactory)
            flushToResultSet()
            provider.requiredTypes
        }

        suggestionGeneratorConsumer.pass(actionAsKind)
        return actionAsKind
    }
}

var LookupElement.suppressItemSelectionByCharsOnTyping: Boolean by NotNullableUserDataProperty(
    Key("CANGJIE_SUPPRESS_ITEM_SELECTION_BY_CHARS_ON_TYPING"),
    defaultValue = false,
)
private val USUALLY_START_LOWER_CASE = DescriptorKindFilter(
    DescriptorKindFilter.CALLABLES_MASK or DescriptorKindFilter.PACKAGES_MASK,
    listOf(SamConstructorDescriptorKindExclude)
)
private val USUALLY_START_UPPER_CASE = DescriptorKindFilter(
    DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.FUNCTIONS_MASK,
    listOf(
        NonSamConstructorFunctionExclude,
        DescriptorKindExclude.Extensions /* needed for faster getReferenceVariants */
    )
)

private object NonSamConstructorFunctionExclude : DescriptorKindExclude() {
    override fun excludes(descriptor: DeclarationDescriptor) =
        descriptor is FunctionDescriptor && descriptor !is SamConstructorDescriptor

    override val fullyExcludedDescriptorKinds: Int get() = 0
}
