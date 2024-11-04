package com.linqingying.cangjie.types.expressions

import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.impl.LocalVariableDescriptor
import com.linqingying.cangjie.psi.CjLoopExpression
import com.linqingying.cangjie.psi.CjTryExpression
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValue
import com.linqingying.cangjie.resolve.calls.smartcasts.IdentifierInfo
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
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

        @JvmStatic
        fun visitLoop(loopExpression: CjLoopExpression): PreliminaryLoopVisitor {
            val visitor = PreliminaryLoopVisitor()
            loopExpression.accept(visitor, null)
            return visitor
        }

        @JvmStatic
        fun visitTryBlock(tryExpression: CjTryExpression): PreliminaryLoopVisitor {
            val visitor = PreliminaryLoopVisitor()
            tryExpression.tryBlock.accept(visitor, null)
            return visitor
        }

        @JvmStatic
        fun visitCatchBlocks(tryExpression: CjTryExpression): PreliminaryLoopVisitor =
            visitCatchBlocks(tryExpression, tryExpression.catchClauses.map { true })

        @JvmStatic
        fun visitCatchBlocks(tryExpression: CjTryExpression, isBlockShouldBeVisited: List<Boolean>): PreliminaryLoopVisitor {
            val catchClauses = tryExpression.catchClauses
            assert(catchClauses.size == isBlockShouldBeVisited.size)
            val visitor = PreliminaryLoopVisitor()
            catchClauses.zip(isBlockShouldBeVisited)
                .filter { (_, shouldBeVisited) -> shouldBeVisited }
                .forEach { (clause, _) -> clause.catchBody?.accept(visitor, null) }
            return visitor
        }
    }
}
