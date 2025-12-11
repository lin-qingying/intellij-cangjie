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

package org.cangnova.cangjie.resolve.calls.tower

import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.calls.tasks.createSynthesizedInvokes
import org.cangnova.cangjie.resolve.scopes.receivers.DetailedReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.cangnova.cangjie.types.isBuiltinExtensionFunctionalType
import java.util.ArrayList


abstract class AbstractInvokeTowerProcessor<C : Candidate>(
    protected val factoryProviderForInvoke: CandidateFactoryProviderForInvoke<C>,
    protected val variableProcessor: ScopeTowerProcessor<C>
) : ScopeTowerProcessor<C> {
    // todo optimize it
    private val previousData = ArrayList<TowerData>()
    private val invokeProcessors: MutableList<Collection<VariableInvokeProcessor>> = ArrayList()

    protected fun hasInvokeProcessors() = invokeProcessors.isNotEmpty()

    private inner class VariableInvokeProcessor(
        var variableCandidate: C,
        val invokeProcessor: ScopeTowerProcessor<C>
    ) : ScopeTowerProcessor<C> {

        override fun process(data: TowerData) = invokeProcessor.process(data).map { candidateGroup ->
            candidateGroup.map {


                factoryProviderForInvoke.transformCandidate(variableCandidate, it)
            }
        }

        override fun recordLookups(skippedData: Collection<TowerData>, name: Name) {
            invokeProcessor.recordLookups(skippedData, name)
        }
    }

    private fun createVariableInvokeProcessor(variableCandidate: C): VariableInvokeProcessor? =
        createInvokeProcessor(variableCandidate)?.let { VariableInvokeProcessor(variableCandidate, it) }

    protected abstract fun createInvokeProcessor(variableCandidate: C): ScopeTowerProcessor<C>?

    protected abstract fun mayDataBeApplicable(data: TowerData): Boolean

    override fun process(data: TowerData): List<Collection<C>> {

        val candidateGroups = ArrayList<Collection<C>>(0)

        if (mayDataBeApplicable(data)) {
            previousData.add(data)
            for (processorsGroup in invokeProcessors) {
                candidateGroups.addAll(processorsGroup.processVariableGroup(data))
            }
        }

        for (variableCandidates in variableProcessor.process(data)) {
            val variableProcessors = variableCandidates.mapNotNull {
                if (it.isSuccessful) createVariableInvokeProcessor(it) else null
            }

            if (variableProcessors.isNotEmpty()) {
                invokeProcessors.add(variableProcessors)
                for (oldData in previousData) {
                    candidateGroups.addAll(variableProcessors.processVariableGroup(oldData))
                }
            }
        }

        return candidateGroups
    }

    private fun Collection<VariableInvokeProcessor>.processVariableGroup(data: TowerData): List<Collection<C>> {
        return when (size) {
            0 -> emptyList()
            1 -> single().process(data)

            else -> listOf(this.flatMap { it.process(data).flatten() })
        }
    }

}


private class InvokeExtensionScopeTowerProcessor<C : Candidate>(
    context: CandidateFactory<C>,
    private val invokeCandidateDescriptor: CandidateWithBoundDispatchReceiver,
    private val explicitReceiver: ReceiverValueWithSmartCastInfo?
) : AbstractSimpleScopeTowerProcessor<C>(context) {

    override fun simpleProcess(data: TowerData): Collection<C> {
        if (explicitReceiver != null && data == TowerData.Empty) {
            return listOf(
                candidateFactory.createCandidate(
                    invokeCandidateDescriptor,
                    ExplicitReceiverKind.BOTH_RECEIVERS,
                    explicitReceiver
                )
            )
        }

        if (explicitReceiver == null && data is TowerData.OnlyImplicitReceiver) {
            return listOf(
                candidateFactory.createCandidate(
                    invokeCandidateDescriptor,
                    ExplicitReceiverKind.DISPATCH_RECEIVER,
                    data.implicitReceiver
                )
            )
        }

        return emptyList()
    }

    // No lookups happen in `simpleProcess`
    override fun recordLookups(skippedData: Collection<TowerData>, name: Name) {}
}

// todo debug info
private fun ImplicitScopeTower.getExtensionInvokeCandidateDescriptor(
    extensionFunctionReceiver: ReceiverValueWithSmartCastInfo
): CandidateWithBoundDispatchReceiver? {
    val type = extensionFunctionReceiver.receiverValue.type
    if (!type.isBuiltinExtensionFunctionalType) return null // todo: missing smart cast?

    val invokeDescriptor = type.memberScope.getContributedFunctions(OperatorNameConventions.INVOKE, location).single()
    val synthesizedInvokes = createSynthesizedInvokes(listOf(invokeDescriptor))
    val synthesizedInvoke = synthesizedInvokes.singleOrNull()
        ?: error("No single synthesized invoke for $invokeDescriptor: $synthesizedInvokes")

    // here we don't add SynthesizedDescriptor diagnostic because it should has priority as member
    return CandidateWithBoundDispatchReceiver(extensionFunctionReceiver, synthesizedInvoke, listOf())
}

// case 1.(foo())() or (foo())()
fun <C : Candidate> createCallTowerProcessorForExplicitInvoke(
    scopeTower: ImplicitScopeTower,
    functionContext: CandidateFactory<C>,
    expressionForInvoke: ReceiverValueWithSmartCastInfo,
    explicitReceiver: ReceiverValueWithSmartCastInfo?
): ScopeTowerProcessor<C> {
    val invokeExtensionDescriptor = scopeTower.getExtensionInvokeCandidateDescriptor(expressionForInvoke)
    if (explicitReceiver != null) {
        return if (invokeExtensionDescriptor == null) {
            // case 1.(foo())(), where foo() isn't extension function
            KnownResultProcessor(emptyList())
        } else {
            InvokeExtensionScopeTowerProcessor(
                functionContext,
                invokeExtensionDescriptor,
                explicitReceiver = explicitReceiver
            )
        }
    } else {
        val usualInvoke = ExplicitReceiverScopeTowerProcessor(
            scopeTower,
            functionContext,
            expressionForInvoke
        ) { getFunctions(OperatorNameConventions.INVOKE, it) } // todo operator

        return if (invokeExtensionDescriptor == null) {
            usualInvoke
        } else {
            PrioritizedCompositeScopeTowerProcessor(
                usualInvoke,
                InvokeExtensionScopeTowerProcessor(functionContext, invokeExtensionDescriptor, explicitReceiver = null)
            )
        }
    }

}

class InvokeTowerProcessor<C : Candidate>(
    val scopeTower: ImplicitScopeTower,
    val name: Name,
    factoryProviderForInvoke: CandidateFactoryProviderForInvoke<C>,
    explicitReceiver: DetailedReceiver?
) : AbstractInvokeTowerProcessor<C>(
    factoryProviderForInvoke,
    createVariableAndObjectProcessor(
        scopeTower,
        name,
        factoryProviderForInvoke.factoryForVariable(stripExplicitReceiver = false),
        explicitReceiver
    )
) {

    // todo filter by operator
    override fun createInvokeProcessor(variableCandidate: C): ScopeTowerProcessor<C>? {
        val (variableReceiver, invokeContext) = factoryProviderForInvoke.factoryForInvoke(
            variableCandidate,
            useExplicitReceiver = false
        )
            ?: return null
        return ExplicitReceiverScopeTowerProcessor(
            scopeTower,
            invokeContext,
            variableReceiver
        ) { getFunctions(OperatorNameConventions.INVOKE, it) }
    }

    override fun mayDataBeApplicable(data: TowerData) =
        data == TowerData.Empty || data is TowerData.TowerLevel

    override fun recordLookups(skippedData: Collection<TowerData>, name: Name) {
        variableProcessor.recordLookups(skippedData, name)
        if (!hasInvokeProcessors()) return

        skippedData.forEach {
            if (it is TowerData.TowerLevel) {
                it.level.recordLookup(OperatorNameConventions.INVOKE)
            }
        }
    }
}
