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

package com.linqingying.cangjie.ide.completion

import com.linqingying.cangjie.ide.completion.handlers.InsertHandlerProvider
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.psi.CjPackageDirective
import com.linqingying.cangjie.psi.CjSimpleNameExpression
import com.linqingying.cangjie.resolve.caches.getResolutionFacade
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter
import com.linqingying.cangjie.utils.CallType
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PlainPrefixMatcher
import com.intellij.patterns.PlatformPatterns

/**
 * Performs completion in package directive. Should suggest only packages and avoid showing fake package produced by
 * DUMMY_IDENTIFIER.
 */
object PackageDirectiveCompletion {
    val DUMMY_IDENTIFIER = "___package___"
    val ACTIVATION_PATTERN = PlatformPatterns.psiElement().inside(CjPackageDirective::class.java)

    fun perform(parameters: CompletionParameters, result: CompletionResultSet): Boolean {
        val position = parameters.position
        if (!ACTIVATION_PATTERN.accepts(position)) return false

        val file = position.containingFile as CjFile

        val expression = file.findElementAt(parameters.offset)?.parent as? CjSimpleNameExpression ?: return false

        val prefixLength = parameters.offset - expression.textOffset
        val prefix = expression.text!!
        val prefixMatcher = PlainPrefixMatcher(prefix.substring(0, prefixLength))
        val resultSet = result.withPrefixMatcher(prefixMatcher)

        val resolutionFacade = expression.getResolutionFacade()

        val packageMemberScope = resolutionFacade.moduleDescriptor.getPackage(file.packageFqName.parent()).memberScope

        val variants = packageMemberScope.getContributedDescriptors(DescriptorKindFilter.PACKAGES, prefixMatcher.asNameFilter())
        val lookupElementFactory = BasicLookupElementFactory(
            resolutionFacade.project,
            InsertHandlerProvider(callType = CallType.PACKAGE_DIRECTIVE, editor = parameters.editor, expectedInfosCalculator = { emptyList() })
        )
        for (variant in variants) {
            val lookupElement = lookupElementFactory.createLookupElement(variant)
            if (!lookupElement.lookupString.contains(DUMMY_IDENTIFIER)) {
                resultSet.addElement(lookupElement)
            }
        }

        return true
    }
}
