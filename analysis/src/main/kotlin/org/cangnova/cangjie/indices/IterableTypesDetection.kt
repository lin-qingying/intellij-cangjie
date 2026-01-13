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

package org.cangnova.cangjie.indices

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.resolve.binding.BindingTraceContext
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.collectFunctions
import org.cangnova.cangjie.resolve.scopes.receivers.ExpressionReceiver
import org.cangnova.cangjie.search.isValidOperator
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.FuzzyType

import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.UnwrappedType
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.checker.DefaultCangJieTypeChecker
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext
import org.cangnova.cangjie.types.expressions.ForLoopConventionsChecker
import org.cangnova.cangjie.types.model.TypeSystemContext

import org.cangnova.cangjie.utils.getOrPutNullable
import org.cangnova.cangjie.utils.isExtension


class IterableTypesDetection(
    private val project: Project,
    private val forLoopConventionsChecker: ForLoopConventionsChecker,
    private val languageVersionSettings: LanguageVersionSettings,
    private val dataFlowValueFactory: DataFlowValueFactory,

) {
    companion object {
        private val iteratorName = Name.identifier("iterator")
    }

    fun createDetector(scope: LexicalScope): IterableTypesDetector {
        return Detector(scope)
    }

    private inner class Detector(private val scope: LexicalScope) : IterableTypesDetector {
        private val cache = HashMap<UnwrappedType, UnwrappedType?>()
        private val typeChecker = CangJieTypeChecker.DEFAULT

        private val typesWithExtensionIterator: Collection<UnwrappedType> = scope
            .collectFunctions(iteratorName, NoLookupLocation.FROM_IDE)
            .filter { it.isValidOperator() && it.isExtension }
            .mapNotNull { it.dispatchReceiverParameter?.type?.unwrap() }

        override fun isIterable(type: UnwrappedType, loopVarType: CangJieType?): Boolean {
            val elementType = elementType(type) ?: return false
            return loopVarType == null ||
                    typeChecker.isSubtypeOf(elementType, loopVarType.unwrap())
        }

        override fun isIterable(type: CangJieType, loopVarType: CangJieType?): Boolean =
            isIterable(type.unwrap(), loopVarType)

        private fun elementType(type: UnwrappedType): UnwrappedType? {
            return cache.getOrPutNullable(type) { elementTypeNoCache(type) }
        }

        override fun elementType(type: CangJieType): UnwrappedType? =
            elementType(type.unwrap())

        private fun elementTypeNoCache(type: UnwrappedType): UnwrappedType? {
            if (!canBeIterable(type)) return null

            val expression = CjPsiFactory(project).createExpression("fake")
            val context = ExpressionTypingContext.newContext(
                BindingTraceContext(),
                scope,
                DataFlowInfo.EMPTY,
                TypeUtils.NO_EXPECTED_TYPE,
                languageVersionSettings,
                dataFlowValueFactory
            )

            val expressionReceiver = ExpressionReceiver.create(
                expression,
                type,
                context.trace.bindingContext
            )

            return forLoopConventionsChecker
                .checkIterableConvention(expressionReceiver, context)
                ?.unwrap()
        }

        private fun canBeIterable(type: UnwrappedType): Boolean {
            if (type.constructor is IntegerLiteralTypeConstructor) return false

            val hasIteratorMember = type.memberScope
                .getContributedFunctions(iteratorName, NoLookupLocation.FROM_IDE)
                .isNotEmpty()

            return hasIteratorMember || typesWithExtensionIterator.any {
                typeChecker.isSubtypeOf(type, it)
            }
        }
    }
}

interface IterableTypesDetector {
    fun isIterable(type: CangJieType, loopVarType: CangJieType? = null): Boolean
    fun isIterable(type: UnwrappedType, loopVarType: CangJieType? = null): Boolean
    fun elementType(type: CangJieType): UnwrappedType?

    /**
     * 检查 FuzzyType 是否为可迭代类型
     *
     * @param type 模糊类型
     * @param loopVarType 循环变量的期望类型
     * @return Boolean true 表示是可迭代类型
     */
    fun isIterable(type: FuzzyType, loopVarType: CangJieType? = null): Boolean =
        isIterable(type.type, loopVarType)
}