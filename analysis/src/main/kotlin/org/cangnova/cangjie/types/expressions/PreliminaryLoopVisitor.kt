/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.types.expressions

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.impl.LocalVariableDescriptor
import org.cangnova.cangjie.psi.CjLoopExpression
import org.cangnova.cangjie.psi.CjTryExpression
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValue
import org.cangnova.cangjie.resolve.calls.smartcasts.IdentifierInfo
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.cangnova.cangjie.psi.CjVisitor
import java.util.LinkedHashSet

/**
 * The purpose of this class is to find all variable assignments
 * **before** loop analysis
 */
class PreliminaryLoopVisitor private constructor() : AssignedVariablesSearcher() {

    fun clearDataFlowInfoForAssignedLocalVariables(
        dataFlowInfo: DataFlowInfo,
        languageVersionSettings: LanguageVersionSettings
    ): DataFlowInfo {
        var resultFlowInfo = dataFlowInfo
        val nonTrivialValues = ObjectOpenHashSet<DataFlowValue>().apply {
            addAll(dataFlowInfo.completeNullabilityInfo.iterator().map { it._1 })
            addAll(dataFlowInfo.completeTypeInfo.iterator().map { it._1 })
        }
        val valueSetToClear = LinkedHashSet<DataFlowValue>()
        for (value in nonTrivialValues) {
            // Only stable variables are under interest here
            val identifierInfo = value.identifierInfo
            if (value.kind == DataFlowValue.Kind.STABLE_VARIABLE && identifierInfo is IdentifierInfo.Variable) {
                val variableDescriptor = identifierInfo.variable
                if (variableDescriptor is LocalVariableDescriptor && hasWriters(variableDescriptor)) {
                    valueSetToClear.add(value)
                }
            }
        }
        for (valueToClear in valueSetToClear) {
            resultFlowInfo = resultFlowInfo.clearValueInfo(valueToClear, languageVersionSettings)
        }
        return resultFlowInfo
    }

    companion object {

        
        fun visitLoop(loopExpression: CjLoopExpression): PreliminaryLoopVisitor {
            val visitor = PreliminaryLoopVisitor()
            @Suppress("UNCHECKED_CAST")
            loopExpression.accept(visitor as CjVisitor<Any?, Any?>, null as Any?)
            return visitor
        }

        
        fun visitTryBlock(tryExpression: CjTryExpression): PreliminaryLoopVisitor {
            val visitor = PreliminaryLoopVisitor()
            @Suppress("UNCHECKED_CAST")
            tryExpression.tryBlock.accept(visitor as CjVisitor<Any?, Any?>, null as Any?)
            return visitor
        }

        
        fun visitCatchBlocks(tryExpression: CjTryExpression): PreliminaryLoopVisitor =
            visitCatchBlocks(tryExpression, tryExpression.catchClauses.map { true })

        
        fun visitCatchBlocks(
            tryExpression: CjTryExpression,
            isBlockShouldBeVisited: List<Boolean>
        ): PreliminaryLoopVisitor {
            val catchClauses = tryExpression.catchClauses
            assert(catchClauses.size == isBlockShouldBeVisited.size)
            val visitor = PreliminaryLoopVisitor()
            catchClauses.zip(isBlockShouldBeVisited)
                .filter { (_, shouldBeVisited) -> shouldBeVisited }


                .forEach { (clause, _) ->@Suppress("UNCHECKED_CAST") clause.catchBody?.accept(visitor as CjVisitor<Any?, Any?>, null as Any?) }
            return visitor
        }
    }
}
