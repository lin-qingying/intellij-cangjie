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

package org.cangnova.cangjie.resolve.calls.tower

import com.intellij.openapi.diagnostic.Logger
import org.cangnova.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import org.cangnova.cangjie.resolve.calls.inference.model.LowerPriorityToPreserveCompatibility
import org.cangnova.cangjie.resolve.calls.model.constraintSystemError
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.constants.FloatLiteralTypeConstructor
import org.cangnova.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import org.cangnova.cangjie.resolve.extend.ExtendManager
import org.cangnova.cangjie.resolve.extend.ExtendVisibilityChecker

import org.cangnova.cangjie.resolve.scopes.*
import org.cangnova.cangjie.resolve.scopes.receivers.ImplicitClassReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.cangnova.cangjie.resolve.scopes.util.parentsWithSelf
import org.cangnova.cangjie.resolve.selectMostSpecificInEachOverridableGroup
import org.cangnova.cangjie.types.*

private val LOG = Logger.getInstance("org.cangnova.cangjie.resolve.calls.tower.TowerResolver")

/**
 * 候选项接口
 *
 * 表示符号解析过程中的一个候选项（如函数、属性等）
 */
interface Candidate {
    /**
     * 判断候选项是否成功
     * 此操作应该非常快速，用于快速过滤候选项
     */
    val isSuccessful: Boolean

    /**
     * 候选项的适用性结果
     * 表示候选项是否适用于当前解析上下文
     */
    val resultingApplicability: CandidateApplicability

    /**
     * 添加兼容性警告
     *
     * @param other 另一个候选项，用于生成兼容性警告
     */
    fun addCompatibilityWarning(other: Candidate)
}

/**
 * 候选项工厂接口
 *
 * 用于创建具体的候选项实例
 *
 * @param C 候选项类型
 */
interface CandidateFactory<out C : Candidate> {
    /**
     * 创建候选项
     *
     * @param towerCandidate 带有绑定分发接收者的候选项
     * @param explicitReceiverKind 显式接收者类型
     * @return 创建的候选项
     */
    fun createCandidate(
        towerCandidate: CandidateWithBoundDispatchReceiver,
        explicitReceiverKind: ExplicitReceiverKind
    ): C

    /**
     * 创建错误候选项
     *
     * @return 错误候选项
     */
    fun createErrorCandidate(): C
}

/**
 * 塔式数据密封类
 *
 * 表示符号解析塔中不同层级的数据，用于组织和管理解析过程中的作用域层级
 */
sealed class TowerData {
    /**
     * 空数据
     * 表示没有隐式接收者的情况
     */
    data object Empty : TowerData()

    /**
     * 仅包含隐式接收者
     *
     * @property implicitReceiver 隐式接收者及其智能转换信息
     */
    class OnlyImplicitReceiver(val implicitReceiver: ReceiverValueWithSmartCastInfo) : TowerData()

    /**
     * 塔式层级
     *
     * @property level 作用域塔层级
     */
    class TowerLevel(val level: ScopeTowerLevel) : TowerData()

    /**
     * 同时包含塔式层级和隐式接收者
     *
     * @property level 作用域塔层级
     * @property implicitReceiver 隐式接收者及其智能转换信息
     */
    class BothTowerLevelAndImplicitReceiver(
        val level: ScopeTowerLevel,
        val implicitReceiver: ReceiverValueWithSmartCastInfo
    ) : TowerData()

    /**
     * 同时包含塔式层级和上下文接收者组
     *
     * @property level 作用域塔层级
     * @property contextReceiversGroup 上下文接收者组列表
     */
    class BothTowerLevelAndContextReceiversGroup(
        val level: ScopeTowerLevel,
        val contextReceiversGroup: List<ReceiverValueWithSmartCastInfo>
    ) : TowerData()

    /**
     * 用于无显式接收者的名称查找
     *
     * 与 BothTowerLevelAndImplicitReceiver 含义相同，但仅用于名称查找，不需要隐式接收者
     *
     * @property level 作用域塔层级
     */
    class ForLookupForNoExplicitReceiver(val level: ScopeTowerLevel) : TowerData()
}

/**
 * 作用域塔处理器接口
 *
 * 用于处理塔式作用域中的符号解析
 *
 * @param C 候选项类型
 */
interface ScopeTowerProcessor<out C> {
    /**
     * 处理塔式数据
     *
     * 处理匹配接收者的候选项（分发接收者已在 ScopeTowerLevel 中匹配）
     * 一个组中的候选项具有相同的优先级，第一组具有最高优先级
     *
     * @param data 塔式数据
     * @return 候选项集合的列表，每个集合代表一个优先级组
     */
    fun process(data: TowerData): List<Collection<C>>

    /**
     * 记录查找
     *
     * @param skippedData 跳过的塔式数据集合
     * @param name 查找的名称
     */
    fun recordLookups(skippedData: Collection<TowerData>, name: Name)
}

/**
 * invoke 调用的候选项工厂提供者接口
 *
 * 用于处理 invoke 约定调用（例如 foo() 实际调用 foo.invoke()）
 *
 * @param C 候选项类型
 */
interface CandidateFactoryProviderForInvoke<C : Candidate> {

    /**
     * 转换候选项
     *
     * 变量已解析，invoke 仅被选中
     *
     * @param variable 变量候选项
     * @param invoke invoke 候选项
     * @return 转换后的候选项
     */
    fun transformCandidate(variable: C, invoke: C): C

    /**
     * 创建变量的工厂
     *
     * @param stripExplicitReceiver 是否移除显式接收者
     * @return 候选项工厂
     */
    fun factoryForVariable(stripExplicitReceiver: Boolean): CandidateFactory<C>

    /**
     * 创建 invoke 的工厂
     *
     * foo() -> ReceiverValue(foo)，作为 invoke 的上下文
     *
     * @param variable 变量候选项
     * @param useExplicitReceiver 是否使用显式接收者
     * @return 接收者值和候选项工厂的配对，如果变量上没有 invoke 则返回 null
     */
    fun factoryForInvoke(
        variable: C,
        useExplicitReceiver: Boolean
    ): Pair<ReceiverValueWithSmartCastInfo, CandidateFactory<C>>?
}

/**
 * 基于合成作用域的塔式层级
 *
 * 用于处理合成扩展属性（如数据类的 component1, component2 等）
 *
 * @property syntheticScopes 合成作用域
 */
internal class SyntheticScopeBasedTowerLevel(
    scopeTower: ImplicitScopeTower,
    private val syntheticScopes: SyntheticScopes
) : AbstractScopeTowerLevel(scopeTower) {
    /**
     * 获取变量（属性）
     *
     * 仅处理扩展接收者的合成扩展属性
     *
     * @param name 变量名称
     * @param extensionReceiver 扩展接收者
     * @return 带有绑定分发接收者的候选项集合
     */
    override fun getVariables(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?,
        isEnumConstructor: Boolean
    ): Collection<CandidateWithBoundDispatchReceiver> {
        // 如果没有扩展接收者，返回空列表
        if (extensionReceiver == null) return emptyList()

        // 收集合成扩展属性
        return syntheticScopes.collectSyntheticExtensionProperties(extensionReceiver.allOriginalTypes, name, location)
            .map {
                createCandidateDescriptor(it, dispatchReceiver = null)
            }
    }

    /**
     * 获取函数
     *
     * 合成作用域不提供函数，返回空列表
     *
     * @param name 函数名称
     * @param extensionReceiver 扩展接收者
     * @return 空集合
     */
    override fun getFunctions(
        name: Name,
        isEnumConstructor: Boolean
    ): Collection<CandidateWithBoundDispatchReceiver> =
        emptyList()

    /**
     * 记录查找
     *
     * 合成作用域不需要记录查找
     *
     * @param name 名称
     */
    override fun recordLookup(name: Name) {
        // 不需要记录
    }
}

/**
 * 上下文接收者组作用域塔式层级
 *
 * 用于处理上下文接收者（context receivers）的符号解析
 *
 * @property contextReceiversGroup 上下文接收者组列表
 */
internal class ContextReceiversGroupScopeTowerLevel(
    scopeTower: ImplicitScopeTower,
    val contextReceiversGroup: List<ReceiverValueWithSmartCastInfo>
) : AbstractScopeTowerLevel(scopeTower) {

    private val syntheticScopes = scopeTower.syntheticScopes

    /**
     * 收集成员
     *
     * 从所有上下文接收者中收集指定的成员
     *
     * @param getMembers 获取成员的函数
     * @return 带有绑定分发接收者的候选项集合
     */
    private fun collectMembers(
        getMembers: ResolutionScope.(CangJieType?) -> Collection<CallableDescriptor>
    ): Collection<CandidateWithBoundDispatchReceiver> {
        val result = ArrayList<CandidateWithBoundDispatchReceiver>(0)

        // 遍历所有上下文接收者
        for (contextReceiver in contextReceiversGroup) {
            val receiverValue = contextReceiver.receiverValue
            val memberScope = receiverValue.type.memberScope
            // 如果是抽象存根类型且成员作用域是错误作用域（非抛出作用域），返回空列表
            if (receiverValue.type is AbstractStubType && memberScope is ErrorScope && memberScope !is ThrowingScope) {
                return arrayListOf()
            }
            // 从成员作用域获取成员并创建候选项描述符
            receiverValue.type.memberScope.getMembers(receiverValue.type).mapTo(result) {
                createCandidateDescriptor(it, contextReceiver)
            }
            // 如果接收者类型是动态类型，从动态作用域获取成员
            if (receiverValue.type.isDynamic()) {
                scopeTower.dynamicScope.getMembers(null).mapTo(result) {
                    createCandidateDescriptor(it, contextReceiver, DynamicDescriptorDiagnostic)
                }
            }
        }

        return result
    }

    /**
     * 获取变量（属性）
     *
     * @param name 变量名称
     * @param extensionReceiver 扩展接收者
     * @return 带有绑定分发接收者的候选项集合
     */
    override fun getVariables(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?,
        isEnumConstructor: Boolean
    ): Collection<CandidateWithBoundDispatchReceiver> {
        return contextReceiversGroup.map { contextReceiver ->
            collectMembers {
                getContributedVariablesAndIntercept(
                    name,
                    location,
                    contextReceiver,
                    scopeTower
                )
            }
        }.flatten()
    }

    /**
     * 获取函数
     *
     * @param name 函数名称
     * @param extensionReceiver 扩展接收者
     * @return 带有绑定分发接收者的候选项集合
     */
    override fun getFunctions(
        name: Name,
        isEnumConstructor: Boolean
    ): Collection<CandidateWithBoundDispatchReceiver> {

        val collectMembers = { contextReceiver: ReceiverValueWithSmartCastInfo ->
            collectMembers {
                getContributedFunctionsAndIntercept(
                    name,
                    location,
                    contextReceiver,

                    scopeTower
                ) + syntheticScopes.collectSyntheticMemberFunctions(listOfNotNull(it), name, location)
            }
        }
        return contextReceiversGroup.map(collectMembers).flatten()
    }

    /**
     * 记录查找
     *
     * 在所有上下文接收者类型的成员作用域中记录名称查找
     *
     * @param name 名称
     */
    override fun recordLookup(name: Name) {
        for (type in contextReceiversGroup.map { it.allOriginalTypes }.flatten()) {
            type.memberScope.recordLookup(name, location)
        }
    }
}

/**
 * 塔式解析器
 *
 * 负责使用塔式作用域策略进行符号解析。解析过程按照优先级从高到低逐层搜索作用域，
 * 包括局部作用域、隐式接收者、扩展函数等，直到找到合适的候选项。
 */
class TowerResolver {

    /**
     * 所有候选项收集器
     *
     * 收集所有候选项，不进行过滤（除了隐藏的候选项）
     *
     * @param C 候选项类型
     */
    class AllCandidatesCollector<C : Candidate> : ResultCollector<C>() {
        private val allCandidates = ArrayList<C>()

        /**
         * 获取成功的候选项
         *
         * @return 始终返回 null，因为此收集器收集所有候选项
         */
        override fun getSuccessfulCandidates(): Collection<C>? = null

        /**
         * 获取最终候选项
         *
         * @return 所有收集的候选项
         */
        override fun getFinalCandidates(): Collection<C> = allCandidates

        /**
         * 推送候选项
         *
         * 将候选项添加到集合中，排除隐藏的候选项
         *
         * @param candidates 候选项集合
         */
        override fun pushCandidates(candidates: Collection<C>) {
            candidates.filterNotTo(allCandidates) {
                it.resultingApplicability == CandidateApplicability.HIDDEN
            }
        }
    }

    /**
     * 运行塔式解析
     *
     * @param processor 作用域塔处理器
     * @param resultCollector 结果收集器
     * @param useOrder 是否使用顺序（保持候选项组的优先级顺序）
     * @param name 要解析的名称
     * @return 候选项集合
     */
    private fun <C : Candidate> ImplicitScopeTower.run(
        processor: ScopeTowerProcessor<C>,
        resultCollector: ResultCollector<C>,
        useOrder: Boolean,
        name: Name
    ): Collection<C> = Task(this, processor, resultCollector, useOrder, name).run()

    /**
     * 使用空塔式数据运行解析
     *
     * 仅处理显式接收者的情况
     *
     * @param processor 作用域塔处理器
     * @param resultCollector 结果收集器
     * @param useOrder 是否使用顺序
     * @return 候选项集合
     */
    fun <C : Candidate> runWithEmptyTowerData(
        processor: ScopeTowerProcessor<C>,
        resultCollector: ResultCollector<C>,
        useOrder: Boolean
    ): Collection<C> =
        processTowerData(processor, resultCollector, useOrder, TowerData.Empty) ?: resultCollector.getFinalCandidates()

    /**
     * 运行解析
     *
     * 主要的解析入口点
     *
     * @param scopeTower 隐式作用域塔
     * @param processor 作用域塔处理器
     * @param useOrder 是否使用顺序
     * @param name 要解析的名称
     * @return 候选项集合
     */
    fun <C : Candidate> runResolve(
        scopeTower: ImplicitScopeTower,
        processor: ScopeTowerProcessor<C>,
        useOrder: Boolean,
        name: Name
    ): Collection<C> = scopeTower.run(processor, SuccessfulResultCollector(), useOrder, name)

    /**
     * 结果收集器抽象类
     *
     * 用于收集和管理解析过程中的候选项
     *
     * @param C 候选项类型
     */
    abstract class ResultCollector<C : Candidate> {
        /**
         * 获取成功的候选项集合
         *
         * 此方法用于从候选组中识别和返回成功的候选项集合。成功的定义基于特定的业务逻辑，
         * 包括兼容性候选项的评估和是否需要停止解析的决策。
         *
         * @return 成功的候选项集合，如果没有符合条件的候选项，则返回 null
         */
        abstract fun getSuccessfulCandidates(): Collection<C>?

        /**
         * 获取最终候选项集合
         *
         * @return 最终候选项集合
         */
        abstract fun getFinalCandidates(): Collection<C>

        /**
         * 推送候选项
         *
         * 将新的候选项添加到收集器中
         *
         * @param candidates 候选项集合
         */
        abstract fun pushCandidates(candidates: Collection<C>)
    }

    /**
     * 成功结果收集器
     *
     * 收集成功的候选项，一旦找到成功的候选项就停止搜索
     *
     * @param C 候选项类型
     */
    class SuccessfulResultCollector<C : Candidate> : ResultCollector<C>() {
        // 候选项组列表
        private val candidateGroups = arrayListOf<Collection<C>>()

        // 是否已找到成功的候选项
        private var isSuccessful = false

        /**
         * 获取成功的候选项集合
         *
         * 此方法用于从候选组中识别和返回成功的候选项集合。成功的定义基于特定的业务逻辑，
         * 包括兼容性候选项的评估和是否需要停止解析的决策。
         *
         * 实现逻辑：
         * 1. 如果当前状态不是成功状态，直接返回 null
         * 2. 遍历候选组，寻找兼容性候选项和决定是否停止解析的组
         * 3. 如果找到兼容性候选项且需要报告警告，则向所有应停止解析的候选项添加兼容性警告
         * 4. 返回经过过滤的应停止解析的组
         *
         * @return 成功的候选项集合，如果没有符合条件的候选项，则返回 null
         */
        override fun getSuccessfulCandidates(): Collection<C>? {
            // 如果当前状态不是成功状态，则直接返回 null
            if (!isSuccessful) return null

            // 初始化兼容性候选项和其所在的组
            var compatibilityCandidate: C? = null
            var compatibilityGroup: Collection<C>? = null
            // 初始化决定是否停止解析的组
            var shouldStopGroup: Collection<C>? = null

            // 遍历候选组，寻找兼容性候选项和决定是否停止解析的组
            outer@ for (group in candidateGroups) {
                for (candidate in group) {
                    // 如果当前候选项满足停止解析的条件，则记录当前组并跳出循环
                    if (shouldStopResolveOnCandidate(candidate)) {
                        shouldStopGroup = group
                        break@outer
                    }

                    // 如果尚未找到兼容性候选项，并且当前候选项满足兼容性条件，则记录当前组和候选项
                    if (compatibilityCandidate == null && isPreserveCompatibilityCandidate(candidate)) {
                        compatibilityGroup = group
                        compatibilityCandidate = candidate
                    }
                }
            }

            // 如果没有满足停止解析条件的组，则返回 null
            if (shouldStopGroup == null) return null

            // 如果找到了兼容性候选项，并且它不在应该停止解析的组中，并且需要报告兼容性警告，
            // 则向所有应停止解析的候选项添加兼容性警告
            if (compatibilityCandidate != null
                && compatibilityGroup !== shouldStopGroup
                && needToReportCompatibilityWarning(compatibilityCandidate)
            ) {
                shouldStopGroup.forEach { it.addCompatibilityWarning(compatibilityCandidate) }
            }

            // 返回经过过滤的应停止解析的组，确保组中的候选项都满足停止解析的条件
            return shouldStopGroup.filter(::shouldStopResolveOnCandidate)
        }

        /**
         * 判断是否需要报告兼容性警告
         *
         * @param candidate 候选项
         * @return 如果需要报告兼容性警告返回 true，否则返回 false
         */
        private fun needToReportCompatibilityWarning(candidate: C) = candidate is ResolutionCandidate &&
                candidate.diagnostics.any {
                    (it.constraintSystemError as? LowerPriorityToPreserveCompatibility)?.needToReportWarning == true
                }

        /**
         * 判断是否应该停止解析
         *
         * @param candidate 候选项
         * @return 如果应该停止解析返回 true，否则返回 false
         */
        private fun shouldStopResolveOnCandidate(candidate: C): Boolean {
            return candidate.resultingApplicability.shouldStopResolve
        }

        /**
         * 判断是否是保持兼容性的候选项
         *
         * @param candidate 候选项
         * @return 如果是保持兼容性的候选项返回 true，否则返回 false
         */
        private fun isPreserveCompatibilityCandidate(candidate: C): Boolean =
            candidate.resultingApplicability == CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY

        /**
         * 推送候选项
         *
         * 将新的候选项添加到收集器中。如果发现成功的候选项，则清除之前的非成功候选项。
         *
         * @param candidates 候选项集合
         */
        override fun pushCandidates(candidates: Collection<C>) {
            val thereIsSuccessful = candidates.any { it.isSuccessful }
            // 如果当前没有成功的候选项，且新候选项中也没有成功的，则直接添加
            if (!isSuccessful && !thereIsSuccessful) {
                candidateGroups.add(candidates)
                return
            }

            // 如果之前没有成功的候选项，但现在有了，清除之前的所有候选项
            if (!isSuccessful) {
                candidateGroups.clear()
                isSuccessful = true
            }
            // 仅添加成功的候选项
            if (thereIsSuccessful) {
                candidateGroups.add(candidates.filter { it.isSuccessful })
            }
        }

        /**
         * 获取最终候选项集合
         *
         * 从所有候选组中选择适用性最高的组，并返回该组中具有相同适用性的候选项
         *
         * @return 最终候选项集合
         */
        override fun getFinalCandidates(): Collection<C> {
            // 选择适用性最高的组
            val moreSuitableGroup = candidateGroups.maxByOrNull { it.groupApplicability } ?: return emptyList()
            val groupApplicability = moreSuitableGroup.groupApplicability
            // 如果最高适用性是隐藏，返回空列表
            if (groupApplicability == CandidateApplicability.HIDDEN) return emptyList()

            // 返回具有相同适用性的候选项
            return moreSuitableGroup.filter { it.resultingApplicability == groupApplicability }
        }

        /**
         * 候选项集合的组适用性
         *
         * 获取候选项集合中的最高适用性
         */
        private val Collection<C>.groupApplicability: CandidateApplicability
            get() = maxOfOrNull { it.resultingApplicability } ?: CandidateApplicability.HIDDEN
    }

    /**
     * 处理塔式数据
     *
     * 使用处理器处理给定的塔式数据，并收集候选项
     *
     * @param processor 作用域塔处理器
     * @param resultCollector 结果收集器
     * @param useOrder 是否使用顺序
     * @param towerData 塔式数据
     * @return 如果找到成功的候选项则返回候选项集合，否则返回 null
     */
    private fun <C : Candidate> processTowerData(
        processor: ScopeTowerProcessor<C>,
        resultCollector: ResultCollector<C>,
        useOrder: Boolean,
        towerData: TowerData
    ): Collection<C>? {
        // 检查是否取消
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        // 处理塔式数据，根据 useOrder 决定是否保持候选项组的顺序
        val candidatesGroups = if (useOrder) {
            processor.process(towerData)
        } else {
            listOf(processor.process(towerData).flatten())
        }

        // 将候选项组推送到结果收集器，如果找到成功的候选项则立即返回
        for (candidatesGroup in candidatesGroups) {
            resultCollector.pushCandidates(candidatesGroup)
            resultCollector.getSuccessfulCandidates()?.let { return it }
        }

        return null
    }

    /**
     * 塔式解析任务
     *
     * 执行实际的塔式作用域解析逻辑
     *
     * @property implicitScopeTower 隐式作用域塔
     * @property processor 作用域塔处理器
     * @property resultCollector 结果收集器
     * @property useOrder 是否使用顺序
     * @property name 要解析的名称
     */
    private inner class Task<out C : Candidate>(
        private val implicitScopeTower: ImplicitScopeTower,
        private val processor: ScopeTowerProcessor<C>,
        private val resultCollector: ResultCollector<C>,
        private val useOrder: Boolean,
        private val name: Name
    ) {
        // 跳过的塔式数据列表，用于查找记录
        private val skippedDataForLookup = mutableListOf<TowerData>()

        /**
         * 局部层级（延迟初始化）
         *
         * 包含局部描述符的词法作用域层级
         */
        private val localLevels: Collection<ScopeTowerLevel> by lazy(LazyThreadSafetyMode.NONE) {
            implicitScopeTower.lexicalScope.parentsWithSelf.filterIsInstance<LexicalScope>()
                .filter { it.kind.withLocalDescriptors && it.mayFitForName(name) }
                .map { ScopeBasedTowerLevel(implicitScopeTower, it) }
                .toList()
        }

        /**
         * 非局部层级（延迟初始化）
         *
         * 不包含局部描述符的作用域层级
         */
        private val nonLocalLevels: Collection<ScopeTowerLevel> by lazy(LazyThreadSafetyMode.NONE) {
            implicitScopeTower.createNonLocalLevels()
        }

        /**
         * 创建非局部层级
         *
         * 创建包含词法作用域、隐式接收者和上下文接收者的非局部层级
         *
         * @return 非局部层级集合
         */
        private fun ImplicitScopeTower.createNonLocalLevels(): Collection<ScopeTowerLevel> {
            val mainResult = mutableListOf<ScopeTowerLevel>()

            /**
             * 添加层级
             *
             * 根据名称是否匹配，将层级添加到结果列表或跳过列表
             *
             * @param scopeTowerLevel 作用域塔层级
             * @param mayFitForName 名称是否可能匹配
             */
            fun addLevel(scopeTowerLevel: ScopeTowerLevel, mayFitForName: Boolean) {
                if (mayFitForName) {
                    mainResult.add(scopeTowerLevel)
                } else {
                    skippedDataForLookup.add(TowerData.ForLookupForNoExplicitReceiver(scopeTowerLevel))
                }
            }

            /**
             * 为词法作用域添加层级
             *
             * 添加词法作用域本身以及其隐式接收者的层级
             *
             * @param scope 词法作用域
             */
            fun addLevelForLexicalScope(scope: LexicalScope) {
                // 如果不包含局部描述符，添加作用域层级
                if (!scope.kind.withLocalDescriptors) {
                    addLevel(
                        ScopeBasedTowerLevel(this@createNonLocalLevels, scope),
                        scope.mayFitForName(name)
                    )
                }

                // 如果有隐式接收者，添加隐式接收者层级
                getImplicitReceiver(scope)?.let {
                    addLevel(
                        MemberScopeTowerLevel(this@createNonLocalLevels, it),
                        it.mayFitForName(name)
                    )
                }
            }

            /**
             * 为上下文接收者组添加层级
             *
             * @param contextReceiversGroup 上下文接收者组
             */
            fun addLevelForContextReceiverGroup(contextReceiversGroup: List<ReceiverValueWithSmartCastInfo>) =
                addLevel(
                    ContextReceiversGroupScopeTowerLevel(this@createNonLocalLevels, contextReceiversGroup),
                    contextReceiversGroup.any { it.mayFitForName(name) }
                )

            /**
             * 为导入作用域添加层级
             *
             * @param scope 层级作用域
             */
            fun addLevelForImportingScope(scope: HierarchicalScope) =
                addLevel(
                    ImportingScopeBasedTowerLevel(this@createNonLocalLevels, scope as ImportingScope),
                    scope.mayFitForName(name)
                )

            val parentScopes = lexicalScope.parentsWithSelf.toList()

            // 先处理所有词法作用域
            var firstImportingScopeIndex = 0
            for ((i, scope) in parentScopes.withIndex()) {
                if (scope !is LexicalScope) {
                    firstImportingScopeIndex = i
                    break
                }
                addLevelForLexicalScope(scope)
            }
            // 再处理所有导入作用域
            parentScopes.subList(firstImportingScopeIndex, parentScopes.size).forEach(::addLevelForImportingScope)

            return mainResult
        }

        /**
         * 处理塔式数据的扩展函数
         *
         * @return 如果找到成功的候选项则返回候选项集合，否则返回 null
         */
        private fun TowerData.process() = processTowerData(processor, resultCollector, useOrder, this)?.also {
            recordLookups()
        }

        /**
         * 有条件地处理塔式数据
         *
         * @param mayFitForName 名称是否可能匹配
         * @return 如果找到成功的候选项则返回候选项集合，否则返回 null
         */
        private fun TowerData.process(mayFitForName: Boolean): Collection<C>? {
            if (!mayFitForName) {
                skippedDataForLookup.add(this)
                return null
            }
            return process()
        }

        // 合成作用域层级
        val syntheticLevel = SyntheticScopeBasedTowerLevel(implicitScopeTower, implicitScopeTower.syntheticScopes)

        /**
         * 处理导入作用域
         *
         * @param scope 导入作用域
         * @return 如果找到成功的候选项则返回候选项集合，否则返回 null
         */
        fun processImportingScope(scope: ImportingScope): Collection<C>? {
            TowerData.TowerLevel(ImportingScopeBasedTowerLevel(implicitScopeTower, scope))
                .process(scope.mayFitForName(name))?.let { return it }
            return null
        }

        /**
         * 处理词法作用域
         *
         * @param scope 词法作用域
         * @param resolveExtensionsForImplicitReceiver 是否为隐式接收者解析扩展
         * @return 如果找到成功的候选项则返回候选项集合，否则返回 null
         */
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

        /**
         * 处理上下文接收者组
         *
         * @param contextReceiversGroup 上下文接收者组
         * @return 如果找到成功的候选项则返回候选项集合，否则返回 null
         */
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

        /**
         * 运行塔式解析
         *
         * 按照优先级从高到低逐层搜索作用域，直到找到合适的候选项。
         *
         * 解析顺序：
         * 1. 空数据（显式接收者的成员）
         * 2. 合成属性（扩展接收者）
         * 3. 局部层级
         * 4. 各级词法作用域和隐式接收者
         * 5. 导入作用域
         *
         * @return 候选项集合
         */
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

            fun processScopes(
                scopes: Sequence<HierarchicalScope>,
                resolveExtensionsForImplicitReceiver: (HierarchicalScope) -> Boolean
            ): Collection<C>? {
//                if (!implicitScopeTower.areContextReceiversEnabled) {
//                    scopes.forEach { scope ->
//                        if (scope is LexicalScope) {
//                            processLexicalScope(scope, resolveExtensionsForImplicitReceiver(scope))?.let { return it }
//                        } else {
//                            processImportingScope(scope as ImportingScope)?.let { return it }
//                        }
//                    }
//                    return null
//                }
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

        /**
         * 处理隐式接收者
         *
         * @param implicitReceiver 隐式接收者
         * @param resolveExtensions 是否解析扩展
         * @return 如果找到成功的候选项则返回候选项集合，否则返回 null
         */
        private fun processImplicitReceiver(
            implicitReceiver: ReceiverValueWithSmartCastInfo,
            resolveExtensions: Boolean
        ): Collection<C>? {
            // 隐式接收者的成员或显式接收者的成员扩展
            TowerData.TowerLevel(MemberScopeTowerLevel(implicitScopeTower, implicitReceiver))
                .process(implicitReceiver.mayFitForName(name))?.let { return it }

            // 合成属性
            TowerData.BothTowerLevelAndImplicitReceiver(syntheticLevel, implicitReceiver).process()?.let { return it }

            if (resolveExtensions) {
                // 局部变量上的 invoke 扩展
                TowerData.OnlyImplicitReceiver(implicitReceiver).process()?.let { return it }

                // 隐式接收者的局部扩展
                for (localLevel in localLevels) {
                    TowerData.BothTowerLevelAndImplicitReceiver(localLevel, implicitReceiver).process()
                        ?.let { return it }
                }

                // 隐式接收者的扩展
                for (nonLocalLevel in nonLocalLevels) {
                    TowerData.BothTowerLevelAndImplicitReceiver(nonLocalLevel, implicitReceiver).process()
                        ?.let { return it }
                }
            }

            return null
        }

        /**
         * 记录查找
         *
         * 在处理器中记录跳过的数据，用于补全等功能
         */
        private fun recordLookups() {
            processor.recordLookups(skippedDataForLookup, name)
        }

        /**
         * 判断接收者值是否可能适合名称
         *
         * 检查接收者类型（包括智能转换后的类型）是否可能包含指定名称
         *
         * @param name 名称
         * @return 如果可能适合返回 true，否则返回 false
         */
        private fun ReceiverValueWithSmartCastInfo.mayFitForName(name: Name): Boolean {
            if (receiverValue.type.mayFitForName(name)) return true
            if (!hasTypesFromSmartCasts()) return false
            return typesFromSmartCasts.any { it.mayFitForName(name) }
        }

        /**
         * 判断类型是否可能适合名称
         *
         * 检查类型是否是动态类型，或其成员作用域是否可能包含指定名称或 invoke 操作符
         *
         * @param name 名称
         * @return 如果可能适合返回 true，否则返回 false
         */
        private fun CangJieType.mayFitForName(name: Name) =
            isDynamic() ||
                    !memberScope.definitelyDoesNotContainName(name) ||
                    !memberScope.definitelyDoesNotContainName(OperatorNameConventions.INVOKE)

        /**
         * 判断解析作用域是否可能适合名称
         *
         * 检查作用域是否可能包含指定名称或 invoke 操作符
         *
         * @param name 名称
         * @return 如果可能适合返回 true，否则返回 false
         */
        private fun ResolutionScope.mayFitForName(name: Name) =
            !definitelyDoesNotContainName(name) || !definitelyDoesNotContainName(OperatorNameConventions.INVOKE)
    }


}


/**
 * 成员作用域塔式层级
 *
 * 用于处理类型成员（包括智能转换后的类型成员）的符号解析
 *
 * @property dispatchReceiver 分发接收者
 */
internal class MemberScopeTowerLevel(
    scopeTower: ImplicitScopeTower,
    val dispatchReceiver: ReceiverValueWithSmartCastInfo
) : AbstractScopeTowerLevel(scopeTower) {

    private val syntheticScopes = scopeTower.syntheticScopes

    private val typeApproximator = scopeTower.typeApproximator

    /**
     * 收集成员
     *
     * 从分发接收者类型（包括智能转换类型）中收集成员
     *
     * @param getMembers 获取成员的函数
     * @return 带有绑定分发接收者的候选项集合
     */
    private fun collectMembers(
        getMembers: ResolutionScope.(CangJieType?) -> Collection<CallableDescriptor>
    ): Collection<CandidateWithBoundDispatchReceiver> {
        val receiverValue = dispatchReceiver.receiverValue
        val memberScope = receiverValue.type.memberScope

        // 如果是抽象存根类型且成员作用域是错误作用域（非抛出作用域），返回空列表
        if (receiverValue.type is AbstractStubType && memberScope is ErrorScope && memberScope !is ThrowingScope) {
            return arrayListOf()
        }

        val result = ArrayList<CandidateWithBoundDispatchReceiver>(0)

        // 从接收者类型的成员作用域获取成员
        receiverValue.type.memberScope.getMembers(receiverValue.type).mapTo(result) {
            createCandidateDescriptor(it, dispatchReceiver)
        }

        // 从 extend 声明中收集成员
        collectExtendMembers(receiverValue.type, getMembers, result)

        // 处理不稳定的智能转换
        val unstableError = if (dispatchReceiver.isStable) null else UnstableSmartCastDiagnostic
        val unstableCandidates = if (unstableError != null) ArrayList<CandidateWithBoundDispatchReceiver>(0) else null

        // 从智能转换类型中收集成员
        for (possibleType in dispatchReceiver.typesFromSmartCasts) {
            possibleType.memberScope.getMembers(possibleType).mapTo(unstableCandidates ?: result) {
                createCandidateDescriptor(
                    it,
                    dispatchReceiver.smartCastReceiver(possibleType),
                    unstableError, dispatchReceiverSmartCastType = possibleType
                )
            }
            // 也从智能转换类型的 extend 声明中收集成员
            collectExtendMembers(possibleType, getMembers, unstableCandidates ?: result)
        }

        // 如果存在智能转换类型
        if (dispatchReceiver.hasTypesFromSmartCasts()) {
            if (unstableCandidates == null) {
                // 稳定的智能转换：保留每个可覆盖组中最具体的候选项
                result.retainAll(result.selectMostSpecificInEachOverridableGroup {
                    descriptor.approximateCapturedTypes(
                        typeApproximator
                    )
                }.toSet())
            } else {
                // 不稳定的智能转换：将最具体的候选项添加到结果中
                result.addAll(
                    unstableCandidates.selectMostSpecificInEachOverridableGroup {
                        descriptor.approximateCapturedTypes(
                            typeApproximator
                        )
                    }
                )
            }
        }

        // 如果接收者类型是动态类型，从动态作用域获取成员
        if (receiverValue.type.isDynamic()) {
            scopeTower.dynamicScope.getMembers(null).mapTo(result) {
                createCandidateDescriptor(it, dispatchReceiver, DynamicDescriptorDiagnostic)
            }
        }

        return result
    }

    /**
     * 从 extend 声明中收集成员
     *
     * 查找针对指定类型的 extend 声明，并从这些声明的成员作用域中收集成员。
     * 这使得通过 extend 声明添加到类型的方法和属性可以在成员解析中被发现。
     *
     * @param type 目标类型
     * @param getMembers 获取成员的函数
     * @param result 结果集合
     */
    private fun collectExtendMembers(
        type: CangJieType,
        getMembers: ResolutionScope.(CangJieType?) -> Collection<CallableDescriptor>,
        result: MutableList<CandidateWithBoundDispatchReceiver>
    ) {
        // 获取类型构造器
        // 对于字面量类型，需要使用其近似类型的构造器来查找扩展
        val originalConstructor = type.constructor
        val typeConstructor = when (originalConstructor) {
            is IntegerLiteralTypeConstructor -> originalConstructor.getApproximatedType().constructor
            is FloatLiteralTypeConstructor -> originalConstructor.getApproximatedType().constructor
            else -> originalConstructor
        }

        if (LOG.isDebugEnabled) {
            LOG.debug(
                "collectExtendMembers: type=$type, " +
                    "originalConstructor=${originalConstructor::class.simpleName}@${System.identityHashCode(originalConstructor)}, " +
                    "typeConstructor=${typeConstructor::class.simpleName}@${System.identityHashCode(typeConstructor)}, " +
                    "declarationDescriptor=${typeConstructor.declarationDescriptor?.name}, " +
                    "hashCode=${typeConstructor.hashCode()}"
            )
        }

        // 从 scopeTower 的 lexicalScope 获取 ownerDescriptor，然后获取模块
        val ownerDescriptor = scopeTower.lexicalScope.ownerDescriptor
        val module = DescriptorUtils.getContainingModuleOrNull(ownerDescriptor) ?: return

        // 从模块获取 ExtendManager
        val extendManager = module.projectDescriptor.extendManager

        // 获取针对该类型的所有 extend 定义
        val extensionDefs = extendManager.getExtensionsForType(typeConstructor)

        if (LOG.isDebugEnabled) {
            LOG.debug("collectExtendMembers: found ${extensionDefs.size} extend(s) for type ${typeConstructor.declarationDescriptor?.name}")
        }

        if (extensionDefs.isEmpty()) return

        // 获取当前词法作用域，用于检查扩展可见性
        val lexicalScope = scopeTower.lexicalScope

        // 从每个 extend 声明的成员作用域收集成员
        for (extensionDef in extensionDefs) {
            // 检查扩展的可见性：如果扩展实现了接口且与被扩展类型不在同一个包，
            // 则需要检查调用处是否导入了至少一个接口
            if (!ExtendVisibilityChecker.isExtendAccessible(extensionDef, lexicalScope)) {
                if (LOG.isDebugEnabled) {
                    LOG.debug("collectExtendMembers: skipping extend '${extensionDef.id}' - interface not imported")
                }
                continue
            }

            // 获取 extend 声明的成员作用域
            val memberScope = extensionDef.memberScope ?: continue

            // 从成员作用域获取成员并创建候选项描述符
            memberScope.getMembers(type).mapTo(result) {
                createCandidateDescriptor(it, dispatchReceiver)
            }
        }
    }

    /**
     * 近似捕获类型
     *
     * 这是一个针对特定测试场景的修正方法（BlackBoxCodegenTestGenerated.Reflection.Properties#testGetPropertiesMutableVsReadonly）。
     *
     * 主要原因：当我们有 List<*> 时，会进行捕获并将接收者类型转换为 List<Capture(*)>。
     * 因此方法 get 的签名是 get(Int): Capture(*)。如果我们还有智能转换到 MutableList<String>，
     * 那么还有方法 get(Int): String。我们应该选择 get(Int): String。
     *
     * @param approximator 类型近似器
     * @return 近似后的可调用描述符
     */
    private fun CallableDescriptor.approximateCapturedTypes(approximator: TypeApproximator): CallableDescriptor {
        // 创建一个空替换器,仅用于触发类型近似
        // 使用 EMPTY 替换器不会改变类型,但会通过 substitute 方法触发类型处理
        return substitute(ComposableTypeSubstitutor.EMPTY)!!
    }

    /**
     * 创建智能转换接收者
     *
     * 将接收者值智能转换为目标类型
     *
     * @param targetType 目标类型
     * @return 智能转换后的接收者值
     */
    private fun ReceiverValueWithSmartCastInfo.smartCastReceiver(targetType: CangJieType): ReceiverValueWithSmartCastInfo {
        if (receiverValue !is ImplicitClassReceiver) return this

        val newReceiverValue = CastImplicitClassReceiver(receiverValue.classDescriptor, targetType)
        return ReceiverValueWithSmartCastInfo(newReceiverValue, typesFromSmartCasts, isStable)
    }

    /**
     * 获取变量（属性）
     *
     * @param name 变量名称
     * @param extensionReceiver 扩展接收者
     * @return 带有绑定分发接收者的候选项集合
     */
    override fun getVariables(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?,
        isEnumConstructor: Boolean
    ): Collection<CandidateWithBoundDispatchReceiver> {
        return collectMembers {
            getContributedVariablesAndIntercept(
                name,
                location,
                dispatchReceiver,
                scopeTower
            )
        }
    }

    /**
     * 获取函数
     *
     * @param name 函数名称
     * @param extensionReceiver 扩展接收者
     * @return 带有绑定分发接收者的候选项集合
     */
    override fun getFunctions(
        name: Name,
        isEnumConstructor: Boolean
    ): Collection<CandidateWithBoundDispatchReceiver> {

        return collectMembers {
            getContributedFunctionsAndIntercept(
                name,
                location,
                dispatchReceiver,
                scopeTower
            ) + syntheticScopes.collectSyntheticMemberFunctions(listOfNotNull(it), name, location)
        }
    }

    /**
     * 记录查找
     *
     * 在分发接收者的所有原始类型的成员作用域中记录名称查找
     *
     * @param name 名称
     */
    override fun recordLookup(name: Name) {
        for (type in dispatchReceiver.allOriginalTypes) {
            type.memberScope.recordLookup(name, location)
        }
    }
}

/**
 * 获取并拦截贡献的函数
 *
 * 从解析作用域获取贡献的函数，并通过作用域塔的拦截器进行拦截处理
 *
 * @param name 函数名称
 * @param location 查找位置
 * @param dispatchReceiver 分发接收者
 * @param extensionReceiver 扩展接收者
 * @param scopeTower 隐式作用域塔
 * @return 函数描述符集合
 */
private fun ResolutionScope.getContributedFunctionsAndIntercept(
    name: Name,
    location: LookupLocation,
    dispatchReceiver: ReceiverValueWithSmartCastInfo?,

    scopeTower: ImplicitScopeTower
): Collection<FunctionDescriptor> {
    val result = getContributedFunctions(name, location)

    return scopeTower.interceptFunctionCandidates(this, name, result, location, dispatchReceiver)
}
