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

import org.cangnova.cangjie.descriptors.EnumConstructorDescriptor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import org.cangnova.cangjie.resolve.calls.model.CangJieCall
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.calls.util.EnumConstructorAccessDescriptor
import org.cangnova.cangjie.resolve.scopes.receivers.DetailedReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.QualifierReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo


internal abstract class AbstractSimpleScopeTowerProcessor<C : Candidate>(
    val candidateFactory: CandidateFactory<C>
) : SimpleScopeTowerProcessor<C> {
    fun createCandidates(
        collector: Collection<CandidateWithBoundDispatchReceiver>,
        kind: ExplicitReceiverKind
    ): Collection<C> {
        return collector.map { candidate ->
            candidateFactory.createCandidate(candidate, kind)
        }
    }
}

private typealias CandidatesCollector =
        ScopeTowerLevel.(extensionReceiver: ReceiverValueWithSmartCastInfo?) -> Collection<CandidateWithBoundDispatchReceiver>

internal class ExplicitReceiverScopeTowerProcessor<C : Candidate>(
    val scopeTower: ImplicitScopeTower,
    context: CandidateFactory<C>,
    val explicitReceiver: ReceiverValueWithSmartCastInfo,
    val collectCandidates: CandidatesCollector
) : AbstractSimpleScopeTowerProcessor<C>(context) {
    override fun simpleProcess(data: TowerData): Collection<C> {
        return when (data) {
            TowerData.Empty -> createCandidates(
                MemberScopeTowerLevel(scopeTower, explicitReceiver).collectCandidates(null),
                ExplicitReceiverKind.DISPATCH_RECEIVER
            )

            is TowerData.TowerLevel -> createCandidates(
                data.level.collectCandidates(explicitReceiver),
                ExplicitReceiverKind.DISPATCH_RECEIVER
            )

            else -> emptyList()
        }
    }

    override fun recordLookups(skippedData: Collection<TowerData>, name: Name) {
        for (data in skippedData) {
            if (data is TowerData.TowerLevel) {
                data.level.recordLookup(name)
            }
        }
    }
}

private class QualifierScopeTowerProcessor<C : Candidate>(
    val scopeTower: ImplicitScopeTower,
    context: CandidateFactory<C>,
    val qualifier: QualifierReceiver,
    val collectCandidates: CandidatesCollector
) : AbstractSimpleScopeTowerProcessor<C>(context) {
    override fun simpleProcess(data: TowerData): Collection<C> {
        if (data != TowerData.Empty) return emptyList()

        return createCandidates(
            QualifierScopeTowerLevel(scopeTower, qualifier).collectCandidates(null),
            ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
        )
    }

    // QualifierScopeTowerProcessor works only with TowerData.Empty that should not be ignored
    override fun recordLookups(skippedData: Collection<TowerData>, name: Name) {}
}

private class NoExplicitReceiverScopeTowerProcessor<C : Candidate>(
    context: CandidateFactory<C>,
    val collectCandidates: CandidatesCollector
) : AbstractSimpleScopeTowerProcessor<C>(context) {
    override fun simpleProcess(data: TowerData): Collection<C> = when (data) {
        is TowerData.TowerLevel -> createCandidates(
            data.level.collectCandidates(null),
            ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
        )

        is TowerData.BothTowerLevelAndImplicitReceiver -> createCandidates(
            data.level.collectCandidates(data.implicitReceiver),
            ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
        )

        else -> emptyList()
    }

    override fun recordLookups(skippedData: Collection<TowerData>, name: Name) {
        for (data in skippedData) {
            when (data) {
                is TowerData.TowerLevel -> data.level.recordLookup(name)
                is TowerData.BothTowerLevelAndImplicitReceiver -> data.level.recordLookup(name)
                is TowerData.ForLookupForNoExplicitReceiver -> data.level.recordLookup(name)
                else -> {}
            }
        }
    }
}

private fun <C : Candidate> createSimpleProcessorWithoutClassValueReceiver(
    scopeTower: ImplicitScopeTower,
    context: CandidateFactory<C>,
    explicitReceiver: DetailedReceiver?,
    collectCandidates: CandidatesCollector
): SimpleScopeTowerProcessor<C> =
    when (explicitReceiver) {
        is ReceiverValueWithSmartCastInfo -> ExplicitReceiverScopeTowerProcessor(
            scopeTower,
            context,
            explicitReceiver,
            collectCandidates
        )

        is QualifierReceiver -> QualifierScopeTowerProcessor(scopeTower, context, explicitReceiver, collectCandidates)
        else -> {
            assert(explicitReceiver == null) {
                "Illegal explicit receiver: $explicitReceiver(${explicitReceiver!!::class.java.simpleName})"
            }
            NoExplicitReceiverScopeTowerProcessor(context, collectCandidates)
        }
    }

private fun <C : Candidate> createSimpleProcessor(
    scopeTower: ImplicitScopeTower,
    context: CandidateFactory<C>,
    explicitReceiver: DetailedReceiver?,
    classValueReceiver: Boolean,
    collectCandidates: CandidatesCollector
): ScopeTowerProcessor<C> {
    val withoutClassValueProcessor =
        createSimpleProcessorWithoutClassValueReceiver(scopeTower, context, explicitReceiver, collectCandidates)

    if (classValueReceiver && explicitReceiver is QualifierReceiver) {
        val classValue = explicitReceiver.classValueReceiverWithSmartCastInfo ?: return withoutClassValueProcessor
        return PrioritizedCompositeScopeTowerProcessor(
            withoutClassValueProcessor,
            ExplicitReceiverScopeTowerProcessor(scopeTower, context, classValue, collectCandidates)
        )
    }
    return withoutClassValueProcessor
}

fun <C : Candidate> createCallableReferenceProcessor(
    scopeTower: ImplicitScopeTower,
    name: Name, context: CandidateFactory<C>,
    explicitReceiver: DetailedReceiver?
): SimpleScopeTowerProcessor<C> {
//    return createSimpleFunctionProcessor(scopeTower, name, context, explicitReceiver)
//    val variable = createSimpleProcessorWithoutClassValueReceiver(scopeTower, context, explicitReceiver) { getVariables(name, it) }
    val function =
        createSimpleProcessorWithoutClassValueReceiver(scopeTower, context, explicitReceiver) { getFunctions(name, it) }
    return SamePriorityCompositeScopeTowerProcessor(/*variable,*/ function)
}

fun <C : Candidate> createSimpleFunctionProcessor(
    scopeTower: ImplicitScopeTower, name: Name,
    context: CandidateFactory<C>,
    explicitReceiver: DetailedReceiver?,
    classValueReceiver: Boolean = true
) = createSimpleProcessor(scopeTower, context, explicitReceiver, classValueReceiver) { getFunctions(name, it) }

fun <C : Candidate> createProcessorWithReceiverValueOrEmpty(
    explicitReceiver: DetailedReceiver?,
    create: (ReceiverValueWithSmartCastInfo?) -> ScopeTowerProcessor<C>
): ScopeTowerProcessor<C> {
    return if (explicitReceiver is QualifierReceiver) {
        explicitReceiver.classValueReceiverWithSmartCastInfo?.let(create)
            ?: KnownResultProcessor(listOf())
    } else {
        create(explicitReceiver as ReceiverValueWithSmartCastInfo?)
    }
}

class KnownResultProcessor<out C>(
    val result: Collection<C>
) : ScopeTowerProcessor<C> {
    override fun process(data: TowerData) =
        if (data == TowerData.Empty) listOfNotNull(result.takeIf { it.isNotEmpty() }) else emptyList()

    override fun recordLookups(skippedData: Collection<TowerData>, name: Name) {}
}

fun <C : Candidate> createFunctionProcessor(
    scopeTower: ImplicitScopeTower,
    name: Name,
    simpleContext: CandidateFactory<C>,
    factoryProviderForInvoke: CandidateFactoryProviderForInvoke<C>,
    explicitReceiver: DetailedReceiver?
): PrioritizedCompositeScopeTowerProcessor<C> {

    // a.foo() -- simple function call
    val simpleFunction = createSimpleFunctionProcessor(scopeTower, name, simpleContext, explicitReceiver)

    // a.foo() -- property a.foo + foo.invoke()
    val invokeProcessor = InvokeTowerProcessor(scopeTower, name, factoryProviderForInvoke, explicitReceiver)

    // a.foo() -- property foo is extension function with receiver a -- a.invoke()
//    val invokeExtensionProcessor = createProcessorWithReceiverValueOrEmpty(explicitReceiver) {
//        InvokeExtensionTowerProcessor(scopeTower, name, factoryProviderForInvoke, it)
//    }

    return PrioritizedCompositeScopeTowerProcessor(simpleFunction, invokeProcessor/*invokeExtensionProcessor*/)
}

fun <C : Candidate> createPropertyProcessor(
    scopeTower: ImplicitScopeTower, name: Name,
    context: CandidateFactory<C>, explicitReceiver: DetailedReceiver?, classValueReceiver: Boolean = true
) = createSimpleProcessor(scopeTower, context, explicitReceiver, classValueReceiver) { getVariables(name, it) }




fun <C : Candidate> createVariableProcessor(
    scopeTower: ImplicitScopeTower, name: Name,
    context: CandidateFactory<C>, explicitReceiver: DetailedReceiver?, classValueReceiver: Boolean = true
) = createSimpleProcessor(scopeTower, context, explicitReceiver, classValueReceiver) { getVariables(name, it) }






fun <C : Candidate> createVariableAndObjectProcessor(
    scopeTower: ImplicitScopeTower, name: Name,
    context: CandidateFactory<C>, explicitReceiver: DetailedReceiver?, classValueReceiver: Boolean = true
) = VariableAndObjectScopeTowerProcessor(
    createVariableProcessor(scopeTower, name, context, explicitReceiver),
//    createPropertyProcessor(scopeTower, name, context, explicitReceiver),

    createSimpleProcessor(scopeTower, context, explicitReceiver, classValueReceiver) {
        getObjects(name, it)
    },
//    createSimpleProcessor(scopeTower, context, explicitReceiver, classValueReceiver) { getClassType(name, it) }
)


class VariableAndObjectScopeTowerProcessor<out C : Candidate>(
    private val variableProcessor: ScopeTowerProcessor<C>,
    private val objectProcessor: ScopeTowerProcessor<C>,
//    private val classTypeProcessor: ScopeTowerProcessor<C>,
) : ScopeTowerProcessor<C> {
    override fun process(data: TowerData): List<Collection<C>> {
        val variablesResult = variableProcessor.process(data)
        val objectResult = objectProcessor.process(data)
//        val classTypeResult = classTypeProcessor.process(data)
        if (objectResult.isEmpty()) return variablesResult
        if (objectResult.none { level ->
                level.any {
                    it.isEnumEntryCandidate()
                }
            }
        )
            return variablesResult + objectResult
        val result = mutableListOf<List<C>>()
        result.addAll(variablesResult.map { it.toMutableList() })
//        result.addAll(classTypeResult.map { it.toMutableList() })

        for ((index, objectLevel) in objectResult.withIndex()) {
            val enumEntryLevel = objectLevel.filter { it.isEnumEntryCandidate() }.toMutableList()


            if (enumEntryLevel.isEmpty()) continue
            if (index < variablesResult.size) {
                // It's guaranteed this element is a mutable list
                (result[index] as MutableList).addAll(enumEntryLevel)
            } else {

//                val classResult =
//                    classTypeResult.firstOrNull()?.filter { !it.isEnumEntryCandidateByClassType() } ?: emptyList()
//                enumEntryLevel.addAll(classResult)
                result.add(enumEntryLevel)


            }


        }
        for (objectLevel in objectResult) {
            val nonEnumEntryLevel = objectLevel.filter { !it.isEnumEntryCandidate() }
            if (nonEnumEntryLevel.isEmpty()) continue
            result.add(nonEnumEntryLevel)
        }
        return result
    }


    private fun Candidate.isEnumEntryCandidate(): Boolean {
        if (this !is ResolutionCandidate) return false
        val callableDescriptor = resolvedCall.candidateDescriptor as? EnumConstructorAccessDescriptor ?: return false
        return callableDescriptor.classDescriptor is EnumConstructorDescriptor
    }

    override fun recordLookups(skippedData: Collection<TowerData>, name: Name) {
        variableProcessor.recordLookups(skippedData, name)
        objectProcessor.recordLookups(skippedData, name)
    }
}

class EnumEntryTowerProcessor<out C : Candidate>(
    val cangjieCall: CangJieCall,

    private val entryProcessor: ScopeTowerProcessor<C>,

    ) : ScopeTowerProcessor<C> {
    override fun recordLookups(skippedData: Collection<TowerData>, name: Name) {

        entryProcessor.recordLookups(skippedData, name)

    }

    override fun process(data: TowerData): List<Collection<C>> {

        val entryResult = entryProcessor.process(data)
        val result = mutableListOf<List<C>>()

        result.addAll(entryResult.map { it.toMutableList() })



        return result
    }
}

@Deprecated("use EnumEntryTowerProcessor")
class EnumAndEntryTowerProcessor<out C : Candidate>(
    val cangjieCall: CangJieCall,

    private val enumProcessor: ScopeTowerProcessor<C>,
    private val entryProcessor: ScopeTowerProcessor<C>,

    ) : ScopeTowerProcessor<C> {
    override fun process(data: TowerData): List<Collection<C>> {
        val enumResult = enumProcessor.process(data)
        val entryResult = entryProcessor.process(data)
        val result = mutableListOf<List<C>>()

        result.addAll(enumResult.map { it.toMutableList() })
        result.addAll(entryResult.map { it.toMutableList() })
//        for ((index, enumLevel) in enumResult.withIndex()) {
//
//
//            result.add (
//
//                enumLevel.filter {
//                    val isCall = cangjieCall.psiCangJieCall.psiCall.callElement is CjCallExpression &&  (cangjieCall.psiCangJieCall.psiCall.callElement as CjCallExpression) .valueArgumentList != null
//                    !isCall
//                }
//            )
//
//
//
//        }


        return result
    }

    override fun recordLookups(skippedData: Collection<TowerData>, name: Name) {
        enumProcessor.recordLookups(skippedData, name)
        entryProcessor.recordLookups(skippedData, name)

    }
}

// use this if processors priority is important
class PrioritizedCompositeScopeTowerProcessor<out C>(
    vararg val processors: ScopeTowerProcessor<C>
) : ScopeTowerProcessor<C> {
    override fun process(data: TowerData): List<Collection<C>> = processors.flatMap { it.process(data) }

    override fun recordLookups(skippedData: Collection<TowerData>, name: Name) {
        processors.forEach { it.recordLookups(skippedData, name) }
    }

}


interface SimpleScopeTowerProcessor<out C> : ScopeTowerProcessor<C> {
    fun simpleProcess(data: TowerData): Collection<C>

    override fun process(data: TowerData): List<Collection<C>> =
        listOfNotNull(simpleProcess(data).takeIf { it.isNotEmpty() })
}


// use this if all processors has same priority
class SamePriorityCompositeScopeTowerProcessor<out C>(
    private vararg val processors: SimpleScopeTowerProcessor<C>
) : SimpleScopeTowerProcessor<C> {
    override fun simpleProcess(data: TowerData): Collection<C> = processors.flatMap { it.simpleProcess(data) }
    override fun recordLookups(skippedData: Collection<TowerData>, name: Name) {
        processors.forEach { it.recordLookups(skippedData, name) }
    }

}
