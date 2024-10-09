package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.ide.completion.keywords.createLookups
import com.huawei.cangjie.ide.ExpectedInfo
import com.huawei.cangjie.ide.completion.keywords.DefaultCompletionKeywordHandlerProvider
import com.huawei.cangjie.ide.completion.keywords.KeywordCompletion
import com.huawei.cangjie.ide.completion.keywords.KeywordValues
import com.huawei.cangjie.ide.completion.smart.ExpectedInfoMatch
import com.huawei.cangjie.ide.completion.smart.SMART_COMPLETION_ITEM_PRIORITY_KEY
import com.huawei.cangjie.ide.completion.smart.SmartCompletion
import com.huawei.cangjie.ide.completion.smart.SmartCompletionItemPriority
import com.huawei.cangjie.ide.projectStructure.languageVersionSettings
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.*
import com.huawei.cangjie.renderer.render
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.huawei.cangjie.resolve.scopes.DescriptorKindExclude
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.resolve.supertypesWithAny
import com.huawei.cangjie.types.FuzzyType
import com.huawei.cangjie.utils.CallTypeAndReceiver
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.addingPolicy.PolicyController
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
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
        override fun fillResultSet(): Unit = NamedArgumentCompletion.complete(collector, expectedInfos, callTypeAndReceiver.callType)
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
//            val declaration = isStartOfExtensionReceiverFor()
//            if (declaration != null) {
////                completeDeclarationNameFromUnresolvedOrOverride(declaration)
////
////                if (declaration is CjProperty) {
////                    // we want to insert type only if the property is lateinit,
////                    // because lateinit var cannot have its type deduced from initializer
////                    completeParameterOrVarNameAndType(withType = declaration.hasModifier(CjTokens.LATEINIT_KEYWORD))
////                }
//
//                // no auto-popup on typing after "val", "var" and "fun" because it's likely the name of the declaration which is being typed by user
//                if (parameters.invocationCount == 0 && (
//                            // suppressOtherCompletion
//                            declaration !is CjNamedFunction && declaration !is CjVariable ||
//                                    prefixMatcher.prefix.let { it.isEmpty() || it[0].isLowerCase() /* function name usually starts with lower case letter */ }
//                            )
//                ) {
//                    if (declaration is CjNamedFunction &&
//                        declaration.modifierList?.allChildren.orEmpty()
//                            .map { it.node.elementType }
//                            .none { it is CjModifierKeywordToken && it !in CjTokens.VISIBILITY_MODIFIERS }
//                    ) {
//                        KEYWORDS_ONLY.generateCategories()
//                    }
//                    return
//                }
//
//                fun makeReferenceSuggestionGenerators(
//                    descriptors: List<DescriptorKindFilter>,
//                    lookupElementFactory: LookupElementFactory
//                ): List<SuggestionGeneratorWithArtifact<Unit>> {
//                    val generators = descriptors.map { descriptorKindFilter ->
//                        referenceVariantsCollector!!.makeReferenceVariantsCollectors(descriptorKindFilter)
//                    }
//
//                    val basicReferencesKind =
//                        suggestionGeneratorForCompletionKind(CangJieCompletionKindName.REFERENCE_BASIC) {
//                            generators.forEach {
//                                addReferenceVariants(lookupElementFactory, it.basic.value)
//                            }
//                        }
//
//                    val extensionReferencesKind =
//                        suggestionGeneratorForCompletionKind(CangJieCompletionKindName.REFERENCE_EXTENSION) {
//                            generators.forEach {
//                                addReferenceVariants(lookupElementFactory, it.extensions.value)
//                            }
//                        }
//
//                    return listOf(basicReferencesKind, extensionReferencesKind)
//                }
//
//                fun collectReferences(descriptors: List<DescriptorKindFilter>): Lazy<Set<FuzzyType>> {
//                    val provider = CollectRequiredTypesContextVariablesProvider()
//                    val lookupElementFactory = createLookupElementFactory(provider)
//                    val generators = makeReferenceSuggestionGenerators(descriptors, lookupElementFactory)
//                    /**
//                     * Acknowledge the generators' existence, passing them to the consumer.
//                     * So the consumer (which is a [com.intellij.turboComplete.SuggestionGeneratorExecutor])
//                     * could use it at its discretion.
//                     */
//                    generators.forEach { suggestionGeneratorConsumer.pass(it) }
//                    return lazy {
//                        /**
//                         * Make that all generators generated their artifacts by the moment, when
//                         * the one who addressed this function's return value
//                         */
//                        generators.forEach { it.getArtifact() }
//                        referenceVariantsCollector!!.collectingFinished()
//                        provider.requiredTypes
//                    }
//                }
//
//            }
        }

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
            val classDescriptor = resolutionFacade.resolveToDescriptor(classOrObject, BodyResolveMode.PARTIAL) as ClassDescriptor
            var superClasses = classDescriptor.defaultType.constructor.supertypesWithAny()
                .mapNotNull { it.constructor.declarationDescriptor as? ClassDescriptor }

//            if (callTypeAndReceiver.receiver != null) {
//                val referenceVariantsSet = referenceVariantsCollector!!.collectReferenceVariants(descriptorKindFilter).imported.toSet()
//                superClasses = superClasses.filter { it in referenceVariantsSet }
//            }

            superClasses
                .map { basicLookupElementFactory.createLookupElement(it, qualifyNestedClasses = true, includeClassTypeArguments = false) }
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

                    // if "return" is parsed correctly in the current context - insert it and all return@xxx items
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
