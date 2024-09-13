package com.huawei.cangjie.ide.completion


import com.huawei.cangjie.lexer.CjModifierKeywordToken
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.allChildren
import com.huawei.cangjie.psi.psiUtil.startOffset
import com.huawei.cangjie.resolve.scopes.DescriptorKindExclude
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.types.FuzzyType
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.addingPolicy.PolicyController
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.Key
import com.intellij.platform.ml.impl.turboComplete.SuggestionGeneratorConsumer
import com.intellij.platform.ml.impl.turboComplete.SuggestionGeneratorWithArtifact

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

    private fun detectCompletionCategory(): CompletionCategory {
//        if (nameExpression == null) {
//            return if ((position.parent as? CjNamedDeclaration)?.nameIdentifier == position) DECLARATION_NAME else KEYWORDS_ONLY
//        }

//        if (OPERATOR_NAME.isApplicable()) {
//            return OPERATOR_NAME
//        }
//
//        if (NamedArgumentCompletion.isOnlyNamedArgumentExpected(nameExpression, resolutionFacade)) {
//            return NAMED_ARGUMENTS_ONLY
//        }
//
//        if (nameExpression.getStrictParentOfType<CjSuperExpression>() != null) {
//            return SUPER_QUALIFIER
//        }

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

    private val completionKind by lazy { detectCompletionCategory() }
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


}

var LookupElement.suppressItemSelectionByCharsOnTyping: Boolean by NotNullableUserDataProperty(
    Key("CANGJIE_SUPPRESS_ITEM_SELECTION_BY_CHARS_ON_TYPING"),
    defaultValue = false,
)
