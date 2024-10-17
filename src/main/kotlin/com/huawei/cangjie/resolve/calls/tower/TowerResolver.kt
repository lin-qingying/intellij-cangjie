package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus
import com.huawei.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.huawei.cangjie.resolve.calls.inference.model.LowerPriorityToPreserveCompatibility
import com.huawei.cangjie.resolve.calls.model.constraintSystemError
import com.huawei.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import com.huawei.cangjie.resolve.scopes.*
import com.huawei.cangjie.resolve.scopes.receivers.ImplicitClassReceiver
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import com.huawei.cangjie.resolve.scopes.util.parentsWithSelf
import com.huawei.cangjie.resolve.selectMostSpecificInEachOverridableGroup
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.error.ErrorScope
import com.huawei.cangjie.types.error.ThrowingScope
import com.huawei.cangjie.utils.OperatorNameConventions

interface Candidate {
    // this operation should be very fast
    val isSuccessful: Boolean

    val resultingApplicability: CandidateApplicability

    fun addCompatibilityWarning(other: Candidate)
}

interface CandidateFactory<out C : Candidate> {
    fun createCandidate(
        towerCandidate: CandidateWithBoundDispatchReceiver,
        explicitReceiverKind: ExplicitReceiverKind,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): C

    fun createErrorCandidate(): C

    fun createCandidate(
        towerCandidate: CandidateWithBoundDispatchReceiver,
        explicitReceiverKind: ExplicitReceiverKind,
        extensionReceiverCandidates: List<ReceiverValueWithSmartCastInfo>
    ): C
}

sealed class TowerData {
    object Empty : TowerData()
    class OnlyImplicitReceiver(val implicitReceiver: ReceiverValueWithSmartCastInfo) : TowerData()
    class TowerLevel(val level: ScopeTowerLevel) : TowerData()
    class BothTowerLevelAndImplicitReceiver(
        val level: ScopeTowerLevel,
        val implicitReceiver: ReceiverValueWithSmartCastInfo
    ) : TowerData()

    class BothTowerLevelAndContextReceiversGroup(
        val level: ScopeTowerLevel,
        val contextReceiversGroup: List<ReceiverValueWithSmartCastInfo>
    ) : TowerData()

    // Has the same meaning as BothTowerLevelAndImplicitReceiver, but it's only used for names lookup, so it doesn't need implicit receiver
    class ForLookupForNoExplicitReceiver(val level: ScopeTowerLevel) : TowerData()
}

interface ScopeTowerProcessor<out C> {
    // Candidates with matched receivers (dispatch receiver was already matched in ScopeTowerLevel)
    // Candidates in one groups have same priority, first group has highest priority.
    fun process(data: TowerData): List<Collection<C>>

    fun recordLookups(skippedData: Collection<TowerData>, name: Name)
}

interface CandidateFactoryProviderForInvoke<C : Candidate> {

    // variable here is resolved, invoke -- only chosen
    fun transformCandidate(variable: C, invoke: C): C

    fun factoryForVariable(stripExplicitReceiver: Boolean): CandidateFactory<C>

    // foo() -> ReceiverValue(foo), context for invoke
    // null means that there is no invoke on variable
    fun factoryForInvoke(
        variable: C,
        useExplicitReceiver: Boolean
    ): Pair<ReceiverValueWithSmartCastInfo, CandidateFactory<C>>?
}

internal class SyntheticScopeBasedTowerLevel(
    scopeTower: ImplicitScopeTower,
    private val syntheticScopes: SyntheticScopes
) : AbstractScopeTowerLevel(scopeTower) {
    override fun getVariables(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        if (extensionReceiver == null) return emptyList()

        return syntheticScopes.collectSyntheticExtensionProperties(extensionReceiver.allOriginalTypes, name, location)
            .map {
                createCandidateDescriptor(it, dispatchReceiver = null)
            }
    }


    override fun getClassType(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        return emptyList()
    }
    override fun getObjects(
        name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> =
        emptyList()

    override fun getFunctions(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> =
        emptyList()

    override fun recordLookup(name: Name) {

    }
}
internal class ContextReceiversGroupScopeTowerLevel(
    scopeTower: ImplicitScopeTower,
    val contextReceiversGroup: List<ReceiverValueWithSmartCastInfo>
) : AbstractScopeTowerLevel(scopeTower) {

    private val syntheticScopes = scopeTower.syntheticScopes

    private fun collectMembers(
        getMembers: ResolutionScope.(CangJieType?) -> Collection<CallableDescriptor>
    ): Collection<CandidateWithBoundDispatchReceiver> {
        val result = ArrayList<CandidateWithBoundDispatchReceiver>(0)

        for (contextReceiver in contextReceiversGroup) {
            val receiverValue = contextReceiver.receiverValue
            val memberScope = receiverValue.type.memberScope
            if (receiverValue.type is AbstractStubType && memberScope is ErrorScope && memberScope !is ThrowingScope) {
                return arrayListOf()
            }
            receiverValue.type.memberScope.getMembers(receiverValue.type).mapTo(result) {
                createCandidateDescriptor(it, contextReceiver)
            }
            if (receiverValue.type.isDynamic()) {
                scopeTower.dynamicScope.getMembers(null).mapTo(result) {
                    createCandidateDescriptor(it, contextReceiver, DynamicDescriptorDiagnostic)
                }
            }
        }

        return result
    }

    override fun getVariables(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        return contextReceiversGroup.map { contextReceiver ->
            collectMembers { getContributedVariablesAndIntercept(name, location, contextReceiver, extensionReceiver, scopeTower) }
        }.flatten()
    }

    override fun getClassType(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        return emptyList()

    }
    override fun getObjects(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        return emptyList()
    }

    override fun getFunctions(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        val collectMembers = { contextReceiver: ReceiverValueWithSmartCastInfo ->
            collectMembers {
                getContributedFunctionsAndIntercept(
                    name,
                    location,
                    contextReceiver,
                    extensionReceiver,
                    scopeTower
                ) + syntheticScopes.collectSyntheticMemberFunctions(listOfNotNull(it), name, location)
            }
        }
        return contextReceiversGroup.map(collectMembers).flatten()
    }

    override fun recordLookup(name: Name) {
        for (type in contextReceiversGroup.map { it.allOriginalTypes }.flatten()) {
            type.memberScope.recordLookup(name, location)
        }
    }
}

class TowerResolver {

    class AllCandidatesCollector<C : Candidate> : ResultCollector<C>() {
        private val allCandidates = ArrayList<C>()

        override fun getSuccessfulCandidates(): Collection<C>? = null

        override fun getFinalCandidates(): Collection<C> = allCandidates

        override fun pushCandidates(candidates: Collection<C>) {
            candidates.filterNotTo(allCandidates) {
                it.resultingApplicability == CandidateApplicability.HIDDEN
            }
        }
    }

    private fun <C : Candidate> ImplicitScopeTower.run(
        processor: ScopeTowerProcessor<C>,
        resultCollector: ResultCollector<C>,
        useOrder: Boolean,
        name: Name
    ): Collection<C> = Task(this, processor, resultCollector, useOrder, name).run()

    fun <C : Candidate> runWithEmptyTowerData(
        processor: ScopeTowerProcessor<C>,
        resultCollector: ResultCollector<C>,
        useOrder: Boolean
    ): Collection<C> =
        processTowerData(processor, resultCollector, useOrder, TowerData.Empty) ?: resultCollector.getFinalCandidates()

    fun <C : Candidate> runResolve(
        scopeTower: ImplicitScopeTower,
        processor: ScopeTowerProcessor<C>,
        useOrder: Boolean,
        name: Name
    ): Collection<C> = scopeTower.run(processor, SuccessfulResultCollector(), useOrder, name)

    abstract class ResultCollector<C : Candidate> {
        /**
         * 获取成功的候选人集合
         * 此方法用于从候选组中识别和返回成功的候选人集合成功的定义基于特定的业务逻辑，
         * 包括兼容性候选人的评估和是否需要停止解析的决策
         *
         * @return 成功的候选人集合，如果没有符合条件的候选人，则返回null
         */
        abstract fun getSuccessfulCandidates(): Collection<C>?

        abstract fun getFinalCandidates(): Collection<C>

        abstract fun pushCandidates(candidates: Collection<C>)
    }

    class SuccessfulResultCollector<C : Candidate> : ResultCollector<C>() {
        private var candidateGroups = arrayListOf<Collection<C>>()
        private var isSuccessful = false

        /**
         * 获取成功的候选人集合
         * 此方法用于从候选组中识别和返回成功的候选人集合成功的定义基于特定的业务逻辑，
         * 包括兼容性候选人的评估和是否需要停止解析的决策
         *
         * @return 成功的候选人集合，如果没有符合条件的候选人，则返回null
         */
        override fun getSuccessfulCandidates(): Collection<C>? {
            // 如果当前状态不是成功状态，则直接返回null
            if (!isSuccessful) return null

            // 初始化兼容性候选人和其所在的组
            var compatibilityCandidate: C? = null
            var compatibilityGroup: Collection<C>? = null
            // 初始化决定是否停止解析的组
            var shouldStopGroup: Collection<C>? = null

            // 遍历候选组，寻找兼容性候选人和决定是否停止解析的组
            outer@ for (group in candidateGroups) {
                for (candidate in group) {
                    // 如果当前候选人满足停止解析的条件，则记录当前组并跳出循环
                    if (shouldStopResolveOnCandidate(candidate)) {
                        shouldStopGroup = group
                        break@outer
                    }

                    // 如果尚未找到兼容性候选人，并且当前候选人满足兼容性条件，则记录当前组和候选人
                    if (compatibilityCandidate == null && isPreserveCompatibilityCandidate(candidate)) {
                        compatibilityGroup = group
                        compatibilityCandidate = candidate
                    }
                }
            }

            // 如果没有满足停止解析条件的组，则返回null
            if (shouldStopGroup == null) return null

            // 如果找到了兼容性候选人，并且它不在应该停止解析的组中，并且需要报告兼容性警告，则向所有应停止解析的候选人添加兼容性警告
            if (compatibilityCandidate != null
                && compatibilityGroup !== shouldStopGroup
                && needToReportCompatibilityWarning(compatibilityCandidate)
            ) {
                shouldStopGroup.forEach { it.addCompatibilityWarning(compatibilityCandidate) }
            }

            // 返回经过过滤的应停止解析的组，确保组中的候选人都满足停止解析的条件
            return shouldStopGroup.filter(::shouldStopResolveOnCandidate)
        }

        private fun needToReportCompatibilityWarning(candidate: C) = candidate is ResolutionCandidate &&
                candidate.diagnostics.any {
                    (it.constraintSystemError as? LowerPriorityToPreserveCompatibility)?.needToReportWarning == true
                }

        private fun shouldStopResolveOnCandidate(candidate: C): Boolean {
            return candidate.resultingApplicability.shouldStopResolve
        }

        private fun isPreserveCompatibilityCandidate(candidate: C): Boolean =
            candidate.resultingApplicability == CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY

        override fun pushCandidates(candidates: Collection<C>) {
            val thereIsSuccessful = candidates.any { it.isSuccessful }
            if (!isSuccessful && !thereIsSuccessful) {
                candidateGroups.add(candidates)
                return
            }

            if (!isSuccessful) {
                candidateGroups.clear()
                isSuccessful = true
            }
            if (thereIsSuccessful) {
                candidateGroups.add(candidates.filter { it.isSuccessful })
            }
        }

        override fun getFinalCandidates(): Collection<C> {
            val moreSuitableGroup = candidateGroups.maxByOrNull { it.groupApplicability } ?: return emptyList()
            val groupApplicability = moreSuitableGroup.groupApplicability
            if (groupApplicability == CandidateApplicability.HIDDEN) return emptyList()

            return moreSuitableGroup.filter { it.resultingApplicability == groupApplicability }
        }

        private val Collection<C>.groupApplicability: CandidateApplicability
            get() = maxOfOrNull { it.resultingApplicability } ?: CandidateApplicability.HIDDEN
    }

    private fun <C : Candidate> processTowerData(
        processor: ScopeTowerProcessor<C>,
        resultCollector: ResultCollector<C>,
        useOrder: Boolean,
        towerData: TowerData
    ): Collection<C>? {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val candidatesGroups = if (useOrder) {
            processor.process(towerData)
        } else {
            listOf(processor.process(towerData).flatten())
        }

        for (candidatesGroup in candidatesGroups) {
            resultCollector.pushCandidates(candidatesGroup)
            resultCollector.getSuccessfulCandidates()?.let { return it }
        }

        return null
    }

    private inner class Task<out C : Candidate>(
        private val implicitScopeTower: ImplicitScopeTower,
        private val processor: ScopeTowerProcessor<C>,
        private val resultCollector: ResultCollector<C>,
        private val useOrder: Boolean,
        private val name: Name
    ) {
//                private val isNameForHidesMember =
//            name in HIDES_MEMBERS_NAME_LIST ||
//                    implicitScopeTower.getNameForGivenImportAlias(name) in HIDES_MEMBERS_NAME_LIST
        private val skippedDataForLookup = mutableListOf<TowerData>()

        private val localLevels: Collection<ScopeTowerLevel> by lazy(LazyThreadSafetyMode.NONE) {
            implicitScopeTower.lexicalScope.parentsWithSelf.filterIsInstance<LexicalScope>()
                .filter { it.kind.withLocalDescriptors && it.mayFitForName(name) }
                .map { ScopeBasedTowerLevel(implicitScopeTower, it) }
                .toList()
        }

        private val nonLocalLevels: Collection<ScopeTowerLevel> by lazy(LazyThreadSafetyMode.NONE) {
            implicitScopeTower.createNonLocalLevels()
        }

//        val hidesMembersLevel = HidesMembersTowerLevel(implicitScopeTower)
//        val syntheticLevel = SyntheticScopeBasedTowerLevel(implicitScopeTower, implicitScopeTower.syntheticScopes)

        private fun ImplicitScopeTower.createNonLocalLevels(): Collection<ScopeTowerLevel> {
            val mainResult = mutableListOf<ScopeTowerLevel>()

            fun addLevel(scopeTowerLevel: ScopeTowerLevel, mayFitForName: Boolean) {
                if (mayFitForName) {
                    mainResult.add(scopeTowerLevel)
                } else {
                    skippedDataForLookup.add(TowerData.ForLookupForNoExplicitReceiver(scopeTowerLevel))
                }
            }

            fun addLevelForLexicalScope(scope: LexicalScope) {
                if (!scope.kind.withLocalDescriptors) {
                    addLevel(
                        ScopeBasedTowerLevel(this@createNonLocalLevels, scope),
                        scope.mayFitForName(name)
                    )
                }

                getImplicitReceiver(scope)?.let {
                    addLevel(
                        MemberScopeTowerLevel(this@createNonLocalLevels, it),
                        it.mayFitForName(name)
                    )
                }
            }

            fun addLevelForContextReceiverGroup(contextReceiversGroup: List<ReceiverValueWithSmartCastInfo>) =
                addLevel(
                    ContextReceiversGroupScopeTowerLevel(this@createNonLocalLevels, contextReceiversGroup),
                    contextReceiversGroup.any { it.mayFitForName(name) }
                )

            fun addLevelForImportingScope(scope: HierarchicalScope) =
                addLevel(
                    ImportingScopeBasedTowerLevel(this@createNonLocalLevels, scope as ImportingScope),
                    scope.mayFitForName(name)
                )

            if (!areContextReceiversEnabled) {
                lexicalScope.parentsWithSelf.forEach { scope ->
                    if (scope is LexicalScope) addLevelForLexicalScope(scope) else addLevelForImportingScope(scope)
                }
                return mainResult
            }

            val parentScopes = lexicalScope.parentsWithSelf.toList()

            val contextReceiversGroups = mutableListOf<List<ReceiverValueWithSmartCastInfo>>()
            var firstImportingScopeIndex = 0
            for ((i, scope) in parentScopes.withIndex()) {
                if (scope !is LexicalScope) {
                    firstImportingScopeIndex = i
                    break
                }
                addLevelForLexicalScope(scope)
                val contextReceiversGroup = getContextReceivers(scope)
                if (contextReceiversGroup.isNotEmpty()) {
                    contextReceiversGroups.add(contextReceiversGroup)
                }
            }
            contextReceiversGroups.forEach(::addLevelForContextReceiverGroup)
            parentScopes.subList(firstImportingScopeIndex, parentScopes.size).forEach(::addLevelForImportingScope)

            return mainResult
        }

        private fun TowerData.process() = processTowerData(processor, resultCollector, useOrder, this)?.also {
            recordLookups()
        }

        private fun TowerData.process(mayFitForName: Boolean): Collection<C>? {
            if (!mayFitForName) {
                skippedDataForLookup.add(this)
                return null
            }
            return process()
        }

        val syntheticLevel = SyntheticScopeBasedTowerLevel(implicitScopeTower, implicitScopeTower.syntheticScopes)

        fun run(): Collection<C> {
//            if (isNameForHidesMember) {
//                // hides members extensions for explicit receiver
//                TowerData.TowerLevel(hidesMembersLevel).process()?.let { return it }
//            }

            // possibly there is explicit member
            TowerData.Empty.process()?.let { return it }
            // synthetic property for explicit receiver
            TowerData.TowerLevel(syntheticLevel).process()?.let { return it }

            // local non-extensions or extension for explicit receiver
            for (localLevel in localLevels) {
                TowerData.TowerLevel(localLevel).process()?.let { return it }
            }

            val contextReceiversGroups = mutableListOf<List<ReceiverValueWithSmartCastInfo>>()

            fun processLexicalScope(
                scope: LexicalScope,
                resolveExtensionsForImplicitReceiver: Boolean
            ): Collection<C>? {
//                if (implicitScopeTower.areContextReceiversEnabled) {
//                    val contextReceiversGroup = implicitScopeTower.getContextReceivers(scope)
//                    if (contextReceiversGroup.isNotEmpty()) {
//                        contextReceiversGroups.add(contextReceiversGroup)
//                    }
//                }

                if (!scope.kind.withLocalDescriptors) {
                    TowerData.TowerLevel(ScopeBasedTowerLevel(implicitScopeTower, scope))
                        .process(scope.mayFitForName(name))?.let { return it }
                }
                implicitScopeTower.getImplicitReceiver(scope)
                    ?.let { processImplicitReceiver(it, resolveExtensionsForImplicitReceiver) }
                    ?.let { return it }
                return null
            }

            fun processContextReceiverGroup(contextReceiversGroup: List<ReceiverValueWithSmartCastInfo>): Collection<C>? {
                TowerData.TowerLevel(ContextReceiversGroupScopeTowerLevel(implicitScopeTower, contextReceiversGroup))
                    .process()?.let { return it }
                TowerData.BothTowerLevelAndContextReceiversGroup(syntheticLevel, contextReceiversGroup).process()
                    ?.let { return it }
                for (nonLocalLevel in nonLocalLevels) {
                    TowerData.BothTowerLevelAndContextReceiversGroup(nonLocalLevel, contextReceiversGroup).process()
                        ?.let { return it }
                }
                return null
            }

            fun processImportingScope(scope: ImportingScope): Collection<C>? {
               TowerData.TowerLevel(ImportingScopeBasedTowerLevel(implicitScopeTower, scope))
                    .process(scope.mayFitForName(name))?.let { return it }
                return null
            }

            fun processScopes(
                scopes: Sequence<HierarchicalScope>,
                resolveExtensionsForImplicitReceiver: (HierarchicalScope) -> Boolean
            ): Collection<C>? {
                if (!implicitScopeTower.areContextReceiversEnabled) {
                    scopes.forEach { scope ->
                        if (scope is LexicalScope) {
                            processLexicalScope(scope, resolveExtensionsForImplicitReceiver(scope))?.let { return it }
                        } else {
                            processImportingScope(scope as ImportingScope)?.let { return it }
                        }
                    }
                    return null
                }
                var firstImportingScopePassed = false
                for (scope in scopes) {
                    if (scope is LexicalScope) {
                        processLexicalScope(scope, resolveExtensionsForImplicitReceiver(scope))?.let { return it }
                    } else {
                        if (!firstImportingScopePassed) {
                            firstImportingScopePassed = true
                            contextReceiversGroups.forEach { contextReceiversGroup ->
                                processContextReceiverGroup(contextReceiversGroup)?.let { return it }
                            }
                        }
                        processImportingScope(scope as ImportingScope)?.let { return it }
                    }
                }
                return null
            }

            if (implicitScopeTower.implicitsResolutionFilter === ImplicitsExtensionsResolutionFilter.Default) {
                processScopes(implicitScopeTower.lexicalScope.parentsWithSelf) { true }
            } else {
                val scopeInfos = implicitScopeTower.allScopesWithImplicitsResolutionInfo()
                val scopeToResolveExtensionsForImplicitReceiverMap =
                    scopeInfos.map { it.scope to it.resolveExtensionsForImplicitReceiver }.toMap()
                processScopes(scopeInfos.map { it.scope }) {
                    scopeToResolveExtensionsForImplicitReceiverMap[it] ?: false
                }
            }

            recordLookups()

            return resultCollector.getFinalCandidates()
        }

        //
        private fun processImplicitReceiver(
            implicitReceiver: ReceiverValueWithSmartCastInfo,
            resolveExtensions: Boolean
        ): Collection<C>? {
//            if (isNameForHidesMember) {
//                // hides members extensions
//                TowerData.BothTowerLevelAndImplicitReceiver(hidesMembersLevel, implicitReceiver).process()
//                    ?.let { return it }
//            }

//             members of implicit receiver or member extension for explicit receiver
            TowerData.TowerLevel(MemberScopeTowerLevel(implicitScopeTower, implicitReceiver))
                .process(implicitReceiver.mayFitForName(name))?.let { return it }

//             synthetic properties
            TowerData.BothTowerLevelAndImplicitReceiver(syntheticLevel, implicitReceiver).process()?.let { return it }

            if (resolveExtensions) {
                // invokeExtension on local variable
                TowerData.OnlyImplicitReceiver(implicitReceiver).process()?.let { return it }

                // local extensions for implicit receiver
                for (localLevel in localLevels) {
                    TowerData.BothTowerLevelAndImplicitReceiver(localLevel, implicitReceiver).process()
                        ?.let { return it }
                }

                // extension for implicit receiver
                for (nonLocalLevel in nonLocalLevels) {
                    TowerData.BothTowerLevelAndImplicitReceiver(nonLocalLevel, implicitReceiver).process()
                        ?.let { return it }
                }
            }

            return null
        }

        private fun recordLookups() {
            processor.recordLookups(skippedDataForLookup, name)
        }

        private fun ReceiverValueWithSmartCastInfo.mayFitForName(name: Name): Boolean {
            if (receiverValue.type.mayFitForName(name)) return true
            if (!hasTypesFromSmartCasts()) return false
            return typesFromSmartCasts.any { it.mayFitForName(name) }
        }

        private fun CangJieType.mayFitForName(name: Name) =
            isDynamic() ||
                    !memberScope.definitelyDoesNotContainName(name) ||
                    !memberScope.definitelyDoesNotContainName(OperatorNameConventions.INVOKE)

        private fun ResolutionScope.mayFitForName(name: Name) =
            !definitelyDoesNotContainName(name) || !definitelyDoesNotContainName(OperatorNameConventions.INVOKE)
    }


}


// todo add static methods & fields with error
internal class MemberScopeTowerLevel(
    scopeTower: ImplicitScopeTower,
    val dispatchReceiver: ReceiverValueWithSmartCastInfo
) : AbstractScopeTowerLevel(scopeTower) {

    private val syntheticScopes = scopeTower.syntheticScopes
    private val isNewInferenceEnabled = scopeTower.isNewInferenceEnabled
    private val typeApproximator = scopeTower.typeApproximator

    private fun collectMembers(
        getMembers: ResolutionScope.(CangJieType?) -> Collection<CallableDescriptor>
    ): Collection<CandidateWithBoundDispatchReceiver> {
        val receiverValue = dispatchReceiver.receiverValue
        val memberScope = receiverValue.type.memberScope

        if (receiverValue.type is AbstractStubType && memberScope is ErrorScope && memberScope !is ThrowingScope) {
            return arrayListOf()
        }

        val result = ArrayList<CandidateWithBoundDispatchReceiver>(0)

        receiverValue.type.memberScope.getMembers(receiverValue.type).mapTo(result) {
            createCandidateDescriptor(it, dispatchReceiver)
        }

        val unstableError = if (dispatchReceiver.isStable) null else UnstableSmartCastDiagnostic
        val unstableCandidates = if (unstableError != null) ArrayList<CandidateWithBoundDispatchReceiver>(0) else null

        for (possibleType in dispatchReceiver.typesFromSmartCasts) {
            possibleType.memberScope.getMembers(possibleType).mapTo(unstableCandidates ?: result) {
                createCandidateDescriptor(
                    it,
                    dispatchReceiver.smartCastReceiver(possibleType),
                    unstableError, dispatchReceiverSmartCastType = possibleType
                )
            }
        }

        if (dispatchReceiver.hasTypesFromSmartCasts()) {
            if (unstableCandidates == null) {
                result.retainAll(result.selectMostSpecificInEachOverridableGroup {
                    descriptor.approximateCapturedTypes(
                        typeApproximator
                    )
                }.toSet())
            } else {
                result.addAll(
                    unstableCandidates.selectMostSpecificInEachOverridableGroup {
                        descriptor.approximateCapturedTypes(
                            typeApproximator
                        )
                    }
                )
            }
        }

        if (receiverValue.type.isDynamic()) {
            scopeTower.dynamicScope.getMembers(null).mapTo(result) {
                createCandidateDescriptor(it, dispatchReceiver, DynamicDescriptorDiagnostic)
            }
        }

        return result
    }

    /**
     * this is bad hack for test like BlackBoxCodegenTestGenerated.Reflection.Properties#testGetPropertiesMutableVsReadonly (see last get call)
     * Main reason for this hack: when we have List<*> we do capturing and transform receiver type to List<Capture(*)>.
     * So method get has signature get(Int): Capture(*). If we also have smartcast to MutableList<String>, then there is also method get(Int): String.
     * And we should chose get(Int): String.
     */
    private fun CallableDescriptor.approximateCapturedTypes(approximator: TypeApproximator): CallableDescriptor {
        if (!isNewInferenceEnabled) return this

        val wrappedSubstitution = object : TypeSubstitution() {
            override fun get(key: CangJieType): TypeProjection? = null
            override fun prepareTopLevelType(topLevelType: CangJieType, position: Variance) = when (position) {
                Variance.INVARIANT -> null

            } ?: topLevelType
        }
        return substitute(TypeSubstitutor.create(wrappedSubstitution))
    }

    private fun ReceiverValueWithSmartCastInfo.smartCastReceiver(targetType: CangJieType): ReceiverValueWithSmartCastInfo {
        if (receiverValue !is ImplicitClassReceiver) return this

        val newReceiverValue = CastImplicitClassReceiver(receiverValue.classDescriptor, targetType)
        return ReceiverValueWithSmartCastInfo(newReceiverValue, typesFromSmartCasts, isStable)
    }

    override fun getVariables(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        return collectMembers {
            getContributedVariablesAndIntercept(
                name,
                location,
                dispatchReceiver,
                extensionReceiver,
                scopeTower
            )
        }
    }
    override fun getObjects(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        return emptyList()
    }

    override fun getClassType(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        return emptyList()

    }

    override fun getFunctions(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        return collectMembers {
            getContributedFunctionsAndIntercept(
                name,
                location,
                dispatchReceiver,
                extensionReceiver,
                scopeTower
            ) + syntheticScopes.collectSyntheticMemberFunctions(listOfNotNull(it), name, location)
        }
    }

    override fun recordLookup(name: Name) {
        for (type in dispatchReceiver.allOriginalTypes) {
            type.memberScope.recordLookup(name, location)
        }
    }
}

private fun ResolutionScope.getContributedFunctionsAndIntercept(
    name: Name,
    location: LookupLocation,
    dispatchReceiver: ReceiverValueWithSmartCastInfo?,
    extensionReceiver: ReceiverValueWithSmartCastInfo?,
    scopeTower: ImplicitScopeTower
): Collection<FunctionDescriptor> {
    val result = getContributedFunctions(name, location)

    return scopeTower.interceptFunctionCandidates(this, name, result, location, dispatchReceiver, extensionReceiver)
}
