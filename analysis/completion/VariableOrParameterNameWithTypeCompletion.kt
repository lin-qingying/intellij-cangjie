/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.completion

import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.indices.CangJieIndicesHelper
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.psiUtil.forEachDescendantOfType
import org.cangnova.cangjie.resolve.ResolutionFacade
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.collectDescriptorsFiltered
import org.cangnova.cangjie.resolve.scopes.getResolutionScope
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.isError
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementWeigher
import com.intellij.codeInsight.lookup.WeighingContext
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.NameUtil
import org.cangnova.cangjie.codeinsight.CangJieNameSuggester
import org.cangnova.cangjie.diagnostics.rendering.IdeDescriptorRenderers
import org.cangnova.cangjie.resolve.binding.BindingContext
import java.util.*

class VariableOrParameterNameWithTypeCompletion(
    private val collector: LookupElementsCollector,
    private val lookupElementFactory: BasicLookupElementFactory,
    private val prefixMatcher: PrefixMatcher,
    private val resolutionFacade: ResolutionFacade,
    private val withType: Boolean
) {
    private val userPrefixes: List<String>
    private val classNamePrefixMatchers: List<PrefixMatcher>

    init {
        val prefix = prefixMatcher.prefix
        val prefixWords = NameUtil.splitNameIntoWords(prefix)

        // prefixes to use to generate parameter names from class names
        val nameSuggestionPrefixes = if (prefix.isEmpty() || prefix[0].isUpperCase())
            emptyList()
        else
            prefixWords.indices.map { index -> if (index == 0) prefix else prefixWords.drop(index).joinToString("") }

        userPrefixes = nameSuggestionPrefixes.indices.map { prefixWords.take(it).joinToString("") }
        classNamePrefixMatchers = nameSuggestionPrefixes.map { CamelHumpMatcher(it.replaceFirstChar { it1 ->
            if (it1.isLowerCase()) it1.titlecase(
                Locale.US
            ) else it1.toString()
        }, false) }
    }

    private val suggestionsByTypesAdded = HashSet<Type>()
    private val lookupNamesAdded = HashSet<String>()

    fun addFromImportedClasses(
        position: PsiElement,
        bindingContext: BindingContext,
        visibilityFilter: (DeclarationDescriptor) -> Boolean
    ) {
        for ((classNameMatcher, userPrefix) in classNamePrefixMatchers.zip(userPrefixes)) {
            val resolutionScope = position.getResolutionScope(bindingContext, resolutionFacade)
            val classifiers =
                resolutionScope.collectDescriptorsFiltered(
                    DescriptorKindFilter.CLASSIFIERS,
                    classNameMatcher.asNameFilter()
                )

            for (classifier in classifiers) {
                if (visibilityFilter(classifier)) {
                    addSuggestionsForClassifier(classifier, userPrefix, notImported = false)
                }
            }

            collector.flushToResultSet()
        }
    }

    fun addFromAllClasses(parameters: CompletionParameters, indicesHelper: CangJieIndicesHelper) {
        for ((classNameMatcher, userPrefix) in classNamePrefixMatchers.zip(userPrefixes)) {
            AllClassesCompletion(
                parameters, indicesHelper, classNameMatcher, resolutionFacade, { true },
                includeTypeAliases = true
            ).collect { addSuggestionsForClassifier(it, userPrefix, notImported = true) }

            collector.flushToResultSet()
        }
    }

    fun addFromParametersInFile(
        position: PsiElement,
        resolutionFacade: ResolutionFacade,
        visibilityFilter: (DeclarationDescriptor) -> Boolean
    ) {
        val positionParameter = position.parent as? CjParameter
        val lookupElementToCount = LinkedHashMap<LookupElement, Pair<Int, String>>()
        position.containingFile.forEachDescendantOfType<CjParameter>(
            canGoInside = { it !is CjExpression || it is CjDeclaration } // we analyze parameters inside bodies to not resolve too much
        ) { parameter ->
            ProgressManager.checkCanceled()

            val name = parameter.name
            if (name != null && positionParameter !== parameter && prefixMatcher.isStartMatch(name)) {
                val descriptor = resolutionFacade.analyze(parameter)[BindingContext.VALUE_PARAMETER, parameter]
                if (descriptor != null) {
                    val parameterType = descriptor.type
                    if (parameterType.isVisible(visibilityFilter)) {
                        val lookupElement =
                            createLookupElement(name, ArbitraryType(parameterType), withType, lookupElementFactory)!!
                        val (count, s) = lookupElementToCount[lookupElement] ?: Pair(0, name)
                        lookupElementToCount[lookupElement] = Pair(count + 1, s)
                    }
                }
            }
        }

        for ((lookupElement, countAndName) in lookupElementToCount) {
            val (count, name) = countAndName
            lookupElement.putUserData(PRIORITY_KEY, -count)
            if (withType || !lookupNamesAdded.contains(name)) collector.addElement(lookupElement)
            lookupNamesAdded.add(name)
        }
    }

    private fun addSuggestionsForClassifier(
        classifier: DeclarationDescriptor,
        userPrefix: String,
        notImported: Boolean
    ) {
        addSuggestions(
            classifier.name.asString(),
            userPrefix,
            DescriptorType(classifier as ClassifierDescriptor),
            notImported
        )
    }


    private fun addSuggestions(className: String, userPrefix: String, type: Type, notImported: Boolean) {
        ProgressManager.checkCanceled()
        if (suggestionsByTypesAdded.contains(type)) return // don't add suggestions for the same with longer user prefix

        val nameSuggestions = CangJieNameSuggester.getCamelNames(className, { true }, userPrefix.isEmpty())
        for (name in nameSuggestions) {
            val parameterName = userPrefix + name
            if (prefixMatcher.isStartMatch(parameterName)) {
                val lookupElement = createLookupElement(parameterName, type, withType, lookupElementFactory)
                if (lookupElement != null) {
                    lookupElement.putUserData(
                        PRIORITY_KEY,
                        userPrefix.length
                    ) // suggestions with longer user prefix get lower priority
                    if (withType || !lookupNamesAdded.contains(parameterName)) collector.addElement(
                        lookupElement,
                        notImported
                    )
                    suggestionsByTypesAdded.add(type)
                    lookupNamesAdded.add(parameterName)
                }
            }
        }
    }

    private fun CangJieType.isVisible(visibilityFilter: (DeclarationDescriptor) -> Boolean): Boolean {
        if (isError) return false
        val classifier = constructor.declarationDescriptor ?: return false
        return visibilityFilter(classifier) && arguments.all { it.type.isVisible(visibilityFilter) }
    }

    private abstract class Type(val idString: String) {
        abstract fun createTypeLookupElement(lookupElementFactory: BasicLookupElementFactory): LookupElement?

        override fun equals(other: Any?) = other is Type && other.idString == idString
        override fun hashCode() = idString.hashCode()
    }

    private class DescriptorType(private val classifier: ClassifierDescriptor) :
        Type(IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(classifier)) {
        override fun createTypeLookupElement(lookupElementFactory: BasicLookupElementFactory) =
            lookupElementFactory.createLookupElement(classifier, qualifyNestedClasses = true)
    }


    private class ArbitraryType(private val type: CangJieType) :
        Type(IdeDescriptorRenderers.SOURCE_CODE.renderType(type)) {
        override fun createTypeLookupElement(lookupElementFactory: BasicLookupElementFactory) =
            lookupElementFactory.createLookupElementForType(type)
    }

    private fun createLookupElement(
        parameterName: String,
        type: Type,
        shouldInsertType: Boolean,
        factory: BasicLookupElementFactory
    ): LookupElement? {
        val typeLookupElement = type.createTypeLookupElement(factory) ?: return null
        val lookupElement =
            NameWithTypeLookupElementDecorator(parameterName, type.idString, typeLookupElement, shouldInsertType)
        if (!shouldInsertType) {
            lookupElement.suppressItemSelectionByCharsOnTyping = true
        }
        lookupElement.hideLookupOnColon = true
        return lookupElement.suppressAutoInsertion()
    }

    companion object {
        private val PRIORITY_KEY = Key<Int>("ParameterNameAndTypeCompletion.PRIORITY_KEY")
    }

    object Weigher : LookupElementWeigher("cangjie.parameterNameAndTypePriority") {
        override fun weigh(element: LookupElement, context: WeighingContext): Int =
            element.getUserData(PRIORITY_KEY) ?: 0
    }
}
