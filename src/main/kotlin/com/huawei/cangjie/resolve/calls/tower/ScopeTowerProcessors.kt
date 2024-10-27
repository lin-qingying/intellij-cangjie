package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.descriptors.ClassKind
import com.huawei.cangjie.descriptors.enumd.EnumEntryDescriptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjCallExpression
import com.huawei.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.huawei.cangjie.resolve.calls.model.CangJieCall
import com.huawei.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import com.huawei.cangjie.resolve.calls.util.FakeCallableDescriptorForObject

import com.huawei.cangjie.resolve.scopes.receivers.DetailedReceiver
import com.huawei.cangjie.resolve.scopes.receivers.QualifierReceiver
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo


internal abstract class AbstractSimpleScopeTowerProcessor<C : Candidate>(
    val candidateFactory: CandidateFactory<C>
) : SimpleScopeTowerProcessor<C> {
    fun createCandidates(
        collector: Collection<CandidateWithBoundDispatchReceiver>,
        kind: ExplicitReceiverKind,
        receiver: ReceiverValueWithSmartCastInfo?
    ): Collection<C> {
        val result = mutableListOf<C>()
        for (candidate in collector) {
            if (candidate.requiresExtensionReceiver == (receiver != null)) {
                result.add(
                    candidateFactory.createCandidate(
                        candidate,
                        kind,
                        extensionReceiver = receiver
                    )
                )
            }
        }
        return result
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
                ExplicitReceiverKind.DISPATCH_RECEIVER,
                null
            )

            is TowerData.TowerLevel -> createCandidates(
                data.level.collectCandidates(explicitReceiver),
                ExplicitReceiverKind.EXTENSION_RECEIVER,
                explicitReceiver
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
            ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            null
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
            ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            null
        )

        is TowerData.BothTowerLevelAndImplicitReceiver -> createCandidates(
            data.level.collectCandidates(data.implicitReceiver),
            ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            data.implicitReceiver
        )

        is TowerData.BothTowerLevelAndContextReceiversGroup -> {
            val groupsOfDuplicateCandidates = data.contextReceiversGroup.flatMap { receiver ->
                data.level.collectCandidates(receiver).map { it to receiver }
            }.filter { (candidate, _) ->
                candidate.requiresExtensionReceiver
            }.groupBy { it.first.descriptor }.values

            val candidateToReceivers = groupsOfDuplicateCandidates.map { l ->
                val candidate = l.first().first
                val receivers = l.map { it.second }
                candidate to receivers
            }
            candidateToReceivers.map {
                candidateFactory.createCandidate(
                    it.first,
                    ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                    it.second
                )
            }
        }

        else -> emptyList()
    }

    override fun recordLookups(skippedData: Collection<TowerData>, name: Name) {
        for (data in skippedData) {
            when (data) {
                is TowerData.TowerLevel -> data.level.recordLookup(name)
                is TowerData.BothTowerLevelAndImplicitReceiver -> data.level.recordLookup(name)
                is TowerData.BothTowerLevelAndContextReceiversGroup -> data.level.recordLookup(name)
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

fun <C : Candidate> createSimpleFunctionProcessor(
    scopeTower: ImplicitScopeTower, name: Name,
    context: CandidateFactory<C>, explicitReceiver: DetailedReceiver?, classValueReceiver: Boolean = true
) = createSimpleProcessor(scopeTower, context, explicitReceiver, classValueReceiver) { getFunctions(name, it) }

fun <C : Candidate> createProcessorWithReceiverValueOrEmpty(
    explicitReceiver: DetailedReceiver?,
    create: (ReceiverValueWithSmartCastInfo?) -> ScopeTowerProcessor<C>
): ScopeTowerProcessor<C> {
    return if (explicitReceiver is QualifierReceiver) {
        explicitReceiver.classValueReceiverWithSmartCastInfo?.let(create)
            ?: KnownResultProcessor<C>(listOf())
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

fun <C : Candidate> createEnumEntryProcessor(
    scopeTower: ImplicitScopeTower, name: Name,
    context: CandidateFactory<C>, explicitReceiver: DetailedReceiver?, classValueReceiver: Boolean = true
) = createSimpleProcessor(scopeTower, context, explicitReceiver, classValueReceiver) {
    getEnumTypeByKind(
        name,
        ClassKind.ENUM_ENTRY,
        it
    )
}

fun <C : Candidate> createEnumProcessor(
    scopeTower: ImplicitScopeTower, name: Name,
    context: CandidateFactory<C>, explicitReceiver: DetailedReceiver?, classValueReceiver: Boolean = true
) = createSimpleProcessor(scopeTower, context, explicitReceiver, classValueReceiver) {
//    getEnumTypeByKind(
//        name,
//        ClassKind.ENUM,
//        it
//    )
    emptyList()
}

fun <C : Candidate> createVariableProcessor(
    scopeTower: ImplicitScopeTower, name: Name,
    context: CandidateFactory<C>, explicitReceiver: DetailedReceiver?, classValueReceiver: Boolean = true
) = createSimpleProcessor(scopeTower, context, explicitReceiver, classValueReceiver) { getVariables(name, it) }

fun <C : Candidate> createEnumAndEntryProcessor(
    cangjieCall: CangJieCall,
    scopeTower: ImplicitScopeTower,
    context: CandidateFactory<C>, classValueReceiver: Boolean = true
) = EnumAndEntryTowerProcessor(
    cangjieCall,
    createEnumProcessor(scopeTower, cangjieCall.name, context, cangjieCall.explicitReceiver?.receiver),

    createEnumEntryProcessor(scopeTower, cangjieCall.name, context, cangjieCall.explicitReceiver?.receiver),


    )

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

    private fun Candidate.isEnumEntryCandidateByClassType(): Boolean {
        if (this !is ResolutionCandidate) return false
        val callableDescriptor = resolvedCall.candidateDescriptor as? ClassCallableDescriptor ?: return false
        return callableDescriptor.type is EnumEntryDescriptor
    }

    private fun Candidate.isEnumEntryCandidate(): Boolean {
        if (this !is ResolutionCandidate) return false
        val callableDescriptor = resolvedCall.candidateDescriptor as? FakeCallableDescriptorForObject ?: return false
        return callableDescriptor.classDescriptor.kind == ClassKind.ENUM_ENTRY
    }

    override fun recordLookups(skippedData: Collection<TowerData>, name: Name) {
        variableProcessor.recordLookups(skippedData, name)
        objectProcessor.recordLookups(skippedData, name)
    }
}

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
