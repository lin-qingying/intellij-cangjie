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

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.collectFunctions
import org.cangnova.cangjie.search.isValidOperator
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.FuzzyType
import org.cangnova.cangjie.types.TypeOptionality
import org.cangnova.cangjie.types.DefaultTypeSubstitutor
import org.cangnova.cangjie.types.optionality
import org.cangnova.cangjie.types.toFuzzyType
import org.cangnova.cangjie.utils.addIfNotNull
import org.cangnova.cangjie.utils.isExtension

class TypesWithContainsDetector(
    scope: LexicalScope,
    indicesHelper: CangJieIndicesHelper?,
    private val argumentType: CangJieType
) : TypesWithOperatorDetector(OperatorNameConventions.CONTAINS, scope, indicesHelper) {

    override fun checkIsSuitableByType(
        operator: FunctionDescriptor,
        freeTypeParams: Collection<TypeParameterDescriptor>
    ): DefaultTypeSubstitutor? {
        val parameter = operator.valueParameters.single()
        val fuzzyParameterType = parameter.type.toFuzzyType(operator.typeParameters + freeTypeParams)
        return fuzzyParameterType.checkIsSuperTypeOf(argumentType)
    }
}

abstract class TypesWithOperatorDetector(
    private val name: Name,
    private val scope: LexicalScope,
    private val indicesHelper: CangJieIndicesHelper?
) {
    protected abstract fun checkIsSuitableByType(
        operator: FunctionDescriptor,
        freeTypeParams: Collection<TypeParameterDescriptor>
    ): DefaultTypeSubstitutor?

    private val cache = HashMap<FuzzyType, Pair<FunctionDescriptor, DefaultTypeSubstitutor>?>()

    val extensionOperators: Collection<FunctionDescriptor> by lazy {
        val result = ArrayList<FunctionDescriptor>()

        val extensionsFromScope = scope
            .collectFunctions(name, NoLookupLocation.FROM_IDE)
            .filter { it.isExtension }
        result.addSuitableOperators(extensionsFromScope)

        indicesHelper?.getTopLevelExtensionOperatorsByName(name.asString())?.let { result.addSuitableOperators(it) }

        result.distinctBy { it.original }
    }

    val classesWithMemberOperators: Collection<ClassDescriptor> by lazy {
        if (indicesHelper == null) return@lazy emptyList<ClassDescriptor>()
        val operators = ArrayList<FunctionDescriptor>().addSuitableOperators(indicesHelper.getMemberOperatorsByName(name.asString()))
        operators.map { it.containingDeclaration as ClassDescriptor }.distinct()
    }

    private fun MutableCollection<FunctionDescriptor>.addSuitableOperators(functions: Collection<FunctionDescriptor>): MutableCollection<FunctionDescriptor> {
        for (function in functions) {
            if (!function.isValidOperator()) continue

            var freeParameters = function.typeParameters
            val containingClass = function.containingDeclaration as? ClassDescriptor
            if (containingClass != null) {
                freeParameters += containingClass.typeConstructor.parameters
            }

            val substitutor = checkIsSuitableByType(function, freeParameters) ?: continue
            addIfNotNull(function.substitute(substitutor))
        }
        return this
    }

    fun findOperator(type: FuzzyType): Pair<FunctionDescriptor, DefaultTypeSubstitutor>? = if (cache.containsKey(type)) {
        cache[type]
    } else {
        val result = findOperatorNoCache(type)
        cache[type] = result
        result
    }

    private fun findOperatorNoCache(type: FuzzyType): Pair<FunctionDescriptor, DefaultTypeSubstitutor>? {
        if (type.optionality() != TypeOptionality.OPTIONAL) {
            for (memberFunction in type.type.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_IDE)) {
                if (memberFunction.isValidOperator()) {
                    val substitutor = checkIsSuitableByType(memberFunction, type.freeParameters) ?: continue
                    val substituted = memberFunction.substitute(substitutor) ?: continue
                    return substituted to substitutor
                }
            }
        }

        for (operator in extensionOperators) {
            // 在仓颉语言中，extend 成员使用 dispatchReceiver
            val receiverType = operator.dispatchReceiverParameter?.type?.toFuzzyType(operator.typeParameters) ?: continue
            val substitutor = type.checkIsSubtypeOf(receiverType) ?: continue
            val substituted = operator.substitute(substitutor) ?: continue
            return substituted to substitutor
        }

        return null
    }
}
