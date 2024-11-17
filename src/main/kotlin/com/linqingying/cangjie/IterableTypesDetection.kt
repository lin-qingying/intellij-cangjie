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

package com.linqingying.cangjie

import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor
import com.linqingying.cangjie.ide.search.isValidOperator
import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjPsiFactory
import com.linqingying.cangjie.resolve.BindingTraceContext
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.linqingying.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import com.linqingying.cangjie.resolve.scopes.LexicalScope
import com.linqingying.cangjie.resolve.scopes.collectFunctions
import com.linqingying.cangjie.resolve.scopes.receivers.ExpressionReceiver
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.FuzzyType
import com.linqingying.cangjie.types.expressions.ExpressionTypingContext
import com.linqingying.cangjie.types.expressions.ForLoopConventionsChecker
import com.linqingying.cangjie.types.toFuzzyType
import com.linqingying.cangjie.types.util.TypeUtils
import com.linqingying.cangjie.utils.getOrPutNullable
import com.intellij.openapi.project.Project
import java.util.HashMap


class IterableTypesDetection(
    private val project: Project,
    private val forLoopConventionsChecker: ForLoopConventionsChecker,
    private val languageVersionSettings: LanguageVersionSettings,
    private val dataFlowValueFactory: DataFlowValueFactory
) {
    companion object {
        private val iteratorName = Name.identifier("iterator")
    }

    fun createDetector(scope: LexicalScope): IterableTypesDetector {
        return Detector(scope)
    }

    private inner class Detector(private val scope: LexicalScope) : IterableTypesDetector {
        private val cache = HashMap<FuzzyType, FuzzyType?>()

        private val typesWithExtensionIterator: Collection<CangJieType> = scope
            .collectFunctions(iteratorName, NoLookupLocation.FROM_IDE)
            .filter { it.isValidOperator() }
            .mapNotNull { it.extensionReceiverParameter?.type }

        override fun isIterable(type: FuzzyType, loopVarType: CangJieType?): Boolean {
            val elementType = elementType(type) ?: return false
            return loopVarType == null || elementType.checkIsSubtypeOf(loopVarType) != null
        }

        override fun isIterable(type: CangJieType, loopVarType: CangJieType?): Boolean =
            isIterable(type.toFuzzyType(emptyList()), loopVarType)

        private fun elementType(type: FuzzyType): FuzzyType? {
            return cache.getOrPutNullable(type) { elementTypeNoCache(type) }
        }

        override fun elementType(type: CangJieType): FuzzyType? = elementType(type.toFuzzyType(emptyList()))

        private fun elementTypeNoCache(type: FuzzyType): FuzzyType? {
            // optimization
            if (!canBeIterable(type)) return null

            val expression = CjPsiFactory(project).createExpression("fake")
            val context = ExpressionTypingContext.newContext(
                BindingTraceContext(), scope, DataFlowInfo.EMPTY, TypeUtils.NO_EXPECTED_TYPE, languageVersionSettings, dataFlowValueFactory
            )
            val expressionReceiver = ExpressionReceiver.create(expression, type.type, context.trace.bindingContext)
            val elementType = forLoopConventionsChecker.checkIterableConvention(expressionReceiver, context)
            return elementType?.toFuzzyType(type.freeParameters)
        }

        private fun canBeIterable(type: FuzzyType): Boolean {
            if (type.type.constructor is IntegerLiteralTypeConstructor) return false
            return type.type.memberScope.getContributedFunctions(iteratorName, NoLookupLocation.FROM_IDE).isNotEmpty() ||
                    typesWithExtensionIterator.any {
                        val freeParams = it.arguments.mapNotNull { it.type.constructor.declarationDescriptor as? TypeParameterDescriptor }
                        type.checkIsSubtypeOf(it.toFuzzyType(freeParams)) != null
                    }
        }
    }
}

interface IterableTypesDetector {
    fun isIterable(type: CangJieType, loopVarType: CangJieType? = null): Boolean

    fun isIterable(type: FuzzyType, loopVarType: CangJieType? = null): Boolean

    fun elementType(type: CangJieType): FuzzyType?
}
