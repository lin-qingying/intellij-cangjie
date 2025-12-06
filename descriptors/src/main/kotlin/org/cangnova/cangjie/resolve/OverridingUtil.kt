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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.extend.ExtendDescriptor
import org.cangnova.cangjie.descriptors.impl.FunctionDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.PropertyAccessorDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.PropertyDescriptorImpl
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.checker.CangJieTypePreparator
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.utils.DFS
import org.cangnova.cangjie.utils.SmartSet
import java.util.*

// ========================================
// 扩展属性：提供便捷访问
// ========================================

/**
 * 获取可继承描述符的类型种类
 * - ClassDescriptor: 返回类的 kind
 * - EnumDescriptor: 返回枚举的 kind
 * - ExtendDescriptor: 返回 EXTEND
 */
val InheritableDescriptor.kind
    get() = when (this) {
        is ClassDescriptor -> this.kind
        is EnumDescriptor -> this.kind
        is ExtendDescriptor -> ClassKind.EXTEND
        else -> error("$this is not a class")
    }

/**
 * 获取可继承描述符的模态性（modality）
 * - ClassDescriptor/EnumDescriptor: 返回其模态性
 * - 其他类型: 返回 FINAL
 */
val InheritableDescriptor.modality
    get() = when (this) {
        is ClassDescriptor -> this.modality
        is EnumDescriptor -> this.modality
        else -> Modality.FINAL
    }

// ========================================
// 核心工具类：OverridingUtil
// ========================================

/**
 * 覆盖工具类
 *
 * 负责处理方法覆盖、可见性检查和假覆盖（fake override）的创建
 *
 * @property equalityAxioms 类型构造器相等性判断逻辑
 * @property cangjieTypeRefiner 类型精炼器
 * @property cangjieTypePreparator 类型准备器
 * @property customSubtype 自定义子类型判断函数
 */
class OverridingUtil private constructor(
    private val equalityAxioms: CangJieTypeChecker.TypeConstructorEquality,
    private val cangjieTypeRefiner: CangJieTypeRefiner,
    private val cangjieTypePreparator: CangJieTypePreparator,
    private val customSubtype: ((CangJieType, CangJieType) -> Boolean)?
) {

    companion object {
        /**
         * 默认实例：使用标准的类型检查规则
         */
        val DEFAULT: OverridingUtil by lazy {
            OverridingUtil(
                { obj1, obj2 -> obj1 == obj2 },
                CangJieTypeRefiner.Default,
                CangJieTypePreparator.Default,
                null
            )
        }

        /**
         * 外部覆盖条件：通过 SPI 加载的扩展点
         */
        private val EXTERNAL_CONDITIONS: List<ExternalOverridabilityCondition> by lazy {
            ServiceLoader.load(
                ExternalOverridabilityCondition::class.java,
                ExternalOverridabilityCondition::class.java.classLoader
            ).toList()
        }

        // ========================================
        // 可见性检查
        // ========================================

        /**
         * 检查成员是否对覆盖可见
         *
         * @param overriding 覆盖方的成员
         * @param fromSuper 父类的成员
         * @param useSpecialRulesForPrivateSealedConstructors 是否使用 private sealed 构造函数的特殊规则
         * @return 如果 fromSuper 对 overriding 可见则返回 true
         */
        fun isVisibleForOverride(
            overriding: MemberDescriptor,
            fromSuper: MemberDescriptor,
            useSpecialRulesForPrivateSealedConstructors: Boolean = false
        ): Boolean = !DescriptorVisibilities.isPrivate(fromSuper.visibility) &&
                DescriptorVisibilities.isVisibleIgnoringReceiver(
                    fromSuper,
                    overriding,
                    useSpecialRulesForPrivateSealedConstructors
                )

        /**
         * 过滤出对当前类可见的假覆盖
         */
        fun filterVisibleFakeOverrides(
            current: InheritableDescriptor,
            toFilter: Collection<CallableMemberDescriptor>
        ): Collection<CallableMemberDescriptor> = toFilter.filter { descriptor ->
            !DescriptorVisibilities.isPrivate(descriptor.visibility) &&
                    DescriptorVisibilities.isVisibleIgnoringReceiver(descriptor, current, false)
        }

        // ========================================
        // 工厂方法
        // ========================================

        /**
         * 创建覆盖工具实例
         */
        fun create(
            cangjieTypeRefiner: CangJieTypeRefiner,
            equalityAxioms: CangJieTypeChecker.TypeConstructorEquality
        ): OverridingUtil = OverridingUtil(
            equalityAxioms,
            cangjieTypeRefiner,
            CangJieTypePreparator.Default,
            null
        )

        /**
         * 使用类型精炼器创建实例
         */
        fun createWithTypeRefiner(cangjieTypeRefiner: CangJieTypeRefiner): OverridingUtil =
            OverridingUtil(
                { obj1, obj2 -> obj1 == obj2 },
                cangjieTypeRefiner,
                CangJieTypePreparator.Default,
                null
            )

        // ========================================
        // 覆盖关系查询
        // ========================================

        /**
         * 获取一个描述符所有被覆盖的声明
         *
         * 递归遍历覆盖链，返回所有真实的（非假覆盖的）声明
         */
        fun getOverriddenDeclarations(descriptor: CallableMemberDescriptor): Set<CallableMemberDescriptor> =
            mutableSetOf<CallableMemberDescriptor>().apply {
                collectOverriddenDeclarations(descriptor, this)
            }

        /**
         * 递归收集覆盖的声明
         *
         * - 如果是真实声明（非假覆盖），直接添加
         * - 如果是假覆盖，递归处理其覆盖的描述符
         */
        private fun collectOverriddenDeclarations(
            descriptor: CallableMemberDescriptor,
            result: MutableSet<CallableMemberDescriptor>
        ) {
            when {
                descriptor.kind.isReal -> result.add(descriptor)
                descriptor.overriddenDescriptors.isEmpty() ->
                    error("No overridden descriptors found for (fake override) $descriptor")

                else -> descriptor.overriddenDescriptors.forEach {
                    collectOverriddenDeclarations(it, result)
                }
            }
        }

        /**
         * 检查函数 f 是否覆盖函数 g
         *
         * @param allowDeclarationCopies 是否允许声明副本
         * @param distinguishExpectsAndNonExpects 是否区分 expect 和非 expect 声明
         */
        fun <D : CallableDescriptor> overrides(
            f: D,
            g: D,
            allowDeclarationCopies: Boolean = false,
            distinguishExpectsAndNonExpects: Boolean = true
        ): Boolean {
            // 结构等价性检查
            if (f != g && DescriptorEquivalenceForOverrides.areEquivalent(
                    f.original,
                    g.original,
                    allowDeclarationCopies,
                    distinguishExpectsAndNonExpects
                )
            ) {
                return true
            }

            // 检查 f 是否覆盖了 g 的任何覆盖版本
            val originalG = g.original
            return DescriptorUtils.getAllOverriddenDescriptors(f).any { overriddenFunction ->
                DescriptorEquivalenceForOverrides.areEquivalent(
                    originalG,
                    overriddenFunction,
                    allowDeclarationCopies,
                    distinguishExpectsAndNonExpects
                )
            }
        }

        // ========================================
        // 覆盖关系过滤
        // ========================================

        /**
         * 过滤覆盖关系，移除被其他成员覆盖的成员
         *
         * 例如：如果 A 覆盖 B，B 覆盖 C，则结果中只保留 A
         *
         * @param candidateSet 候选集合
         * @param allowDescriptorCopies 是否允许描述符副本
         * @param cancellationCallback 取消回调（用于长时间操作的中断）
         * @param transformFirst 转换函数，将候选类型转换为 CallableDescriptor
         */
        fun <D> filterOverrides(
            candidateSet: Set<D>,
            allowDescriptorCopies: Boolean = false,
            cancellationCallback: (() -> Unit)? = null,
            transformFirst: (D, D) -> Pair<CallableDescriptor, CallableDescriptor>
        ): Set<D> {
            if (candidateSet.size <= 1) return candidateSet

            val result = linkedSetOf<D>()

            candidateSet@ for (meD in candidateSet) {
                cancellationCallback?.invoke()

                val iterator = result.iterator()
                while (iterator.hasNext()) {
                    val otherD = iterator.next()
                    val (me, other) = transformFirst(meD, otherD)

                    when {
                        // 如果 me 覆盖 other，移除 other
                        overrides(me, other, allowDescriptorCopies, true) -> iterator.remove()
                        // 如果 other 覆盖 me，跳过 me
                        overrides(other, me, allowDescriptorCopies, true) -> continue@candidateSet
                    }
                }
                result.add(meD)
            }

            check(result.isNotEmpty()) { "All candidates filtered out from $candidateSet" }
            return result
        }

        /**
         * 过滤掉被覆盖的描述符（占位符实现）
         */
        fun <D : CallableDescriptor> filterOutOverridden(candidateSet: Set<D>): Set<D> = candidateSet

        // ========================================
        // 可见性处理
        // ========================================

        /**
         * 查找最大可见性
         *
         * 返回所有描述符中最大的可见性，如果存在不兼容的可见性则返回 null
         */
        fun findMaxVisibility(descriptors: Collection<CallableMemberDescriptor>): DescriptorVisibility? {
            if (descriptors.isEmpty()) return DescriptorVisibilities.DEFAULT_VISIBILITY

            var maxVisibility: DescriptorVisibility? = null

            // 第一遍：找到最大可见性
            for (descriptor in descriptors) {
                val visibility = descriptor.visibility
                require(visibility != DescriptorVisibilities.INHERITED) {
                    "Visibility should have been computed for $descriptor"
                }

                when {
                    maxVisibility == null -> maxVisibility = visibility
                    else -> {
                        val compareResult = DescriptorVisibilities.compare(visibility, maxVisibility)
                        when {
                            compareResult == null -> maxVisibility = null
                            compareResult > 0 -> maxVisibility = visibility
                        }
                    }
                }
            }

            // 第二遍：验证所有描述符都不比最大可见性更可见
            maxVisibility?.let { max ->
                for (descriptor in descriptors) {
                    val compareResult = DescriptorVisibilities.compare(max, descriptor.visibility)
                    if (compareResult == null || compareResult < 0) {
                        return null
                    }
                }
            }

            return maxVisibility
        }

        /**
         * 解析未知可见性（INHERITED）
         *
         * 递归地为成员及其访问器推断合适的可见性
         *
         * @param memberDescriptor 需要解析可见性的成员
         * @param cannotInferVisibility 无法推断时的回调
         */
        fun resolveUnknownVisibilityForMember(
            memberDescriptor: CallableMemberDescriptor,
            cannotInferVisibility: ((CallableMemberDescriptor) -> Unit)? = null
        ) {
            // 递归解析覆盖描述符的可见性
            memberDescriptor.overriddenDescriptors.forEach { descriptor ->
                if (descriptor.visibility == DescriptorVisibilities.INHERITED) {
                    resolveUnknownVisibilityForMember(descriptor, cannotInferVisibility)
                }
            }

            if (memberDescriptor.visibility != DescriptorVisibilities.INHERITED) return

            // 计算要继承的可见性
            val maxVisibility = computeVisibilityToInherit(memberDescriptor)
            val visibilityToInherit = maxVisibility ?: run {
                cannotInferVisibility?.invoke(memberDescriptor)
                DescriptorVisibilities.PUBLIC
            }

            // 根据成员类型设置可见性
            when (memberDescriptor) {
                is PropertyDescriptorImpl -> {
                    memberDescriptor.visibility = visibilityToInherit
                    // 递归解析属性访问器的可见性
                    (memberDescriptor as PropertyDescriptor).accessors.forEach { accessor ->
                        resolveUnknownVisibilityForMember(
                            accessor,
                            if (maxVisibility == null) null else cannotInferVisibility
                        )
                    }
                }

                is FunctionDescriptorImpl -> memberDescriptor.visibility = visibilityToInherit
                is PropertyAccessorDescriptorImpl -> memberDescriptor.visibility = visibilityToInherit
            }
        }

        /**
         * 计算要继承的可见性
         *
         * 对于假覆盖：要求所有非抽象的被覆盖成员具有相同的可见性
         * 对于真实覆盖：直接使用最大可见性
         */
        private fun computeVisibilityToInherit(memberDescriptor: CallableMemberDescriptor): DescriptorVisibility? {
            val overriddenDescriptors = memberDescriptor.overriddenDescriptors
            val maxVisibility = findMaxVisibility(overriddenDescriptors) ?: return null

            if (memberDescriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                // 假覆盖：检查所有非抽象成员是否具有一致的可见性
                for (overridden in overriddenDescriptors) {
                    if (overridden.modality != Modality.ABSTRACT && overridden.visibility != maxVisibility) {
                        return null
                    }
                }
                return maxVisibility
            }
            return maxVisibility.normalize()
        }

        // ========================================
        // 特异性（Specificity）检查
        // ========================================

        /**
         * 检查描述符 a 是否比 b 更具体
         *
         * 更具体的定义：
         * 1. 可见性更严格或相等
         * 2. 返回类型是子类型（对于函数）
         * 3. 对于 var 属性，类型必须完全相等；对于 val，返回类型是子类型
         */
        fun isMoreSpecific(a: CallableDescriptor, b: CallableDescriptor): Boolean {
            val aReturnType = requireNotNull(a.returnType) { "Return type of $a is null" }
            val bReturnType = requireNotNull(b.returnType) { "Return type of $b is null" }

            if (!isVisibilityMoreSpecific(a, b)) return false

            val checkerState = DEFAULT.createTypeCheckerState(a.typeParameters, b.typeParameters)

            return when {
                a is FunctionDescriptor -> {
                    require(b is FunctionDescriptor) { "b is ${b::class}" }
                    isReturnTypeMoreSpecific(a, aReturnType, b, bReturnType, checkerState)
                }

                a is VariableDescriptor -> {
                    require(b is VariableDescriptor) { "b is ${b::class}" }
                    when {
                        // 两个都是 var：类型必须相等
                        a.isVar && b.isVar ->
                            AbstractTypeChecker.equalTypes(checkerState, aReturnType.unwrap(), bReturnType.unwrap())
                        // a 是 val，b 是 var：不允许
                        // 其他情况：返回类型检查
                        else -> !(!a.isVar && b.isVar) && isReturnTypeMoreSpecific(
                            a, aReturnType, b, bReturnType, checkerState
                        )
                    }
                }

                else -> error("Unexpected callable: ${a::class}")
            }
        }

        /**
         * 检查可见性是否更具体（更严格或相等）
         */
        private fun isVisibilityMoreSpecific(
            a: DeclarationDescriptorWithVisibility,
            b: DeclarationDescriptorWithVisibility
        ): Boolean {
            val result = DescriptorVisibilities.compare(a.visibility, b.visibility)
            return result == null || result >= 0
        }

        /**
         * 检查返回类型是否更具体（是否为子类型）
         */
        private fun isReturnTypeMoreSpecific(
            a: CallableDescriptor,
            aReturnType: CangJieType,
            b: CallableDescriptor,
            bReturnType: CangJieType,
            typeCheckerState: TypeCheckerState
        ): Boolean = AbstractTypeChecker.isSubtypeOf(
            typeCheckerState,
            aReturnType.unwrap(),
            bReturnType.unwrap()
        )

        /**
         * 检查候选者是否比集合中所有描述符都更具体
         */
        private fun isMoreSpecificThenAllOf(
            candidate: CallableDescriptor,
            descriptors: Collection<CallableDescriptor>
        ): Boolean = descriptors.all { isMoreSpecific(candidate, it) }

        /**
         * 从可覆盖的集合中选择最具体的成员
         *
         * 算法：
         * 1. 找到比所有其他成员都更具体的成员（真正的最具体）
         * 2. 如果没有，找到传递性最具体的成员
         * 3. 如果有多个候选，优先选择非 flexible 类型的
         */
        fun <H> selectMostSpecificMember(
            overridables: Collection<H>,
            descriptorByHandle: (H) -> CallableDescriptor
        ): H {
            require(overridables.isNotEmpty()) { "Should have at least one overridable descriptor" }
            if (overridables.size == 1) return overridables.first()

            val candidates = mutableListOf<H>()
            val callableMemberDescriptors = overridables.map(descriptorByHandle)

            var transitivelyMostSpecific = overridables.first()
            var transitivelyMostSpecificDescriptor = descriptorByHandle(transitivelyMostSpecific)

            // 查找真正的最具体成员和传递性最具体成员
            for (overridable in overridables) {
                val descriptor = descriptorByHandle(overridable)

                // 如果比所有成员都更具体，加入候选
                if (isMoreSpecificThenAllOf(descriptor, callableMemberDescriptors)) {
                    candidates.add(overridable)
                }

                // 更新传递性最具体成员
                if (isMoreSpecific(descriptor, transitivelyMostSpecificDescriptor) &&
                    !isMoreSpecific(transitivelyMostSpecificDescriptor, descriptor)
                ) {
                    transitivelyMostSpecific = overridable
                    transitivelyMostSpecificDescriptor = descriptor
                }
            }

            return when {
                candidates.isEmpty() -> transitivelyMostSpecific
                candidates.size == 1 -> candidates.first()
                else -> {
                    // 优先选择非 flexible 类型的候选
                    candidates.find { candidate ->
                        descriptorByHandle(candidate).returnType?.isFlexible() == false
                    } ?: candidates.first()
                }
            }
        }

        // ========================================
        // 双向覆盖能力检查
        // ========================================

        /**
         * 获取双向覆盖能力
         *
         * 检查两个描述符是否可以互相覆盖
         */
        fun getBothWaysOverridability(
            overriderDescriptor: CallableDescriptor,
            candidateDescriptor: CallableDescriptor
        ): OverrideCompatibilityInfo.Result {
            val result1 = DEFAULT.isOverridableBy(candidateDescriptor, overriderDescriptor, null).result
            val result2 = DEFAULT.isOverridableBy(overriderDescriptor, candidateDescriptor, null).result

            return when {
                result1 == OverrideCompatibilityInfo.Result.OVERRIDABLE &&
                        result2 == OverrideCompatibilityInfo.Result.OVERRIDABLE ->
                    OverrideCompatibilityInfo.Result.OVERRIDABLE

                result1 == OverrideCompatibilityInfo.Result.CONFLICT ||
                        result2 == OverrideCompatibilityInfo.Result.CONFLICT ->
                    OverrideCompatibilityInfo.Result.CONFLICT

                else -> OverrideCompatibilityInfo.Result.INCOMPATIBLE
            }
        }

        // ========================================
        // 基本覆盖检查
        // ========================================

        /**
         * 获取基本覆盖问题
         *
         * 执行基础检查（不涉及类型兼容性）：
         * - 成员种类是否匹配（函数 vs 属性）
         * - 名称是否匹配
         * - 参数数量是否匹配
         */
        fun getBasicOverridabilityProblem(
            superDescriptor: CallableDescriptor,
            subDescriptor: CallableDescriptor
        ): OverrideCompatibilityInfo? {
            // 检查成员种类匹配
            if ((superDescriptor is FunctionDescriptor && subDescriptor !is FunctionDescriptor) ||
                (superDescriptor is VariableDescriptor && subDescriptor !is VariableDescriptor)
            ) {
                return OverrideCompatibilityInfo.incompatible("Member kind mismatch")
            }

            // 确保是可检查的类型
            if (superDescriptor !is FunctionDescriptor && superDescriptor !is VariableDescriptor) {
                error("This type of CallableDescriptor cannot be checked for overridability: $superDescriptor")
            }

            // 检查名称匹配
            if (superDescriptor.name != subDescriptor.name) {
                return OverrideCompatibilityInfo.incompatible("Name mismatch")
            }

            return checkReceiverAndParameterCount(superDescriptor, subDescriptor)
        }

        /**
         * 检查接收者和参数数量
         */
        private fun checkReceiverAndParameterCount(
            superDescriptor: CallableDescriptor,
            subDescriptor: CallableDescriptor
        ): OverrideCompatibilityInfo? {
            // 检查值参数数量匹配
            if (superDescriptor.valueParameters.size != subDescriptor.valueParameters.size) {
                return OverrideCompatibilityInfo.incompatible("Value parameter number mismatch")
            }
            return null
        }

        // ========================================
        // 双向可覆盖成员提取
        // ========================================

        /**
         * 提取双向可覆盖的成员
         *
         * 从集合中提取所有与 overrider 双向可覆盖的成员
         *
         * @param overrider 覆盖者
         * @param extractFrom 待提取的集合（会被修改）
         * @param descriptorByHandle 句柄到描述符的转换函数
         * @param onConflict 冲突处理回调
         * @return 所有双向可覆盖的成员（包括 overrider）
         */
        fun <H> extractMembersOverridableInBothWays(
            overrider: H,
            extractFrom: MutableCollection<H>,
            descriptorByHandle: (H) -> CallableDescriptor,
            onConflict: (H) -> Unit
        ): Collection<H> {
            val overridable = mutableListOf<H>()
            overridable.add(overrider)
            val overriderDescriptor = descriptorByHandle(overrider)

            val iterator = extractFrom.iterator()
            while (iterator.hasNext()) {
                val candidate = iterator.next()
                if (overrider == candidate) {
                    iterator.remove()
                    continue
                }

                val candidateDescriptor = descriptorByHandle(candidate)
                val finalResult = getBothWaysOverridability(overriderDescriptor, candidateDescriptor)

                when (finalResult) {
                    OverrideCompatibilityInfo.Result.OVERRIDABLE -> {
                        overridable.add(candidate)
                        iterator.remove()
                    }

                    OverrideCompatibilityInfo.Result.CONFLICT -> {
                        onConflict(candidate)
                        iterator.remove()
                    }

                    else -> {
                        // INCOMPATIBLE - 不做处理
                    }
                }
            }
            return overridable
        }

        /**
         * 提取双向可覆盖的成员（CallableMemberDescriptor 版本）
         */
        private fun extractMembersOverridableInBothWays(
            overrider: CallableMemberDescriptor,
            extractFrom: Queue<CallableMemberDescriptor>,
            onConflict: (CallableMemberDescriptor) -> Unit
        ): Collection<CallableMemberDescriptor> = extractMembersOverridableInBothWays(
            overrider,
            extractFrom,
            { it },
            onConflict
        )

        // ========================================
        // 类型相关辅助方法
        // ========================================

        /**
         * 编译值参数类型列表
         */
        private fun compiledValueParameters(callableDescriptor: CallableDescriptor): List<CangJieType> =
            callableDescriptor.valueParameters.map { it.type }

        /**
         * 检查两个类型是否等价
         */
        private fun areTypesEquivalent(
            typeInSuper: CangJieType,
            typeInSub: CangJieType,
            typeCheckerState: TypeCheckerState
        ): Boolean {
            val bothErrors = typeInSuper.isError && typeInSub.isError
            return bothErrors || AbstractTypeChecker.equalTypes(
                typeCheckerState,
                typeInSuper.unwrap(),
                typeInSub.unwrap()
            )
        }

        /**
         * 检查类型参数是否等价
         *
         * 要求：
         * 1. 上界数量相同
         * 2. 每个上界类型等价
         */
        private fun areTypeParametersEquivalent(
            superTypeParameter: TypeParameterDescriptor,
            subTypeParameter: TypeParameterDescriptor,
            typeCheckerState: TypeCheckerState
        ): Boolean {
            val superBounds = superTypeParameter.upperBounds
            val subBounds = subTypeParameter.upperBounds.toMutableList()

            if (superBounds.size != subBounds.size) return false

            for (superBound in superBounds) {
                val iterator = subBounds.listIterator()
                var found = false
                while (iterator.hasNext()) {
                    val subBound = iterator.next()
                    if (areTypesEquivalent(superBound, subBound, typeCheckerState)) {
                        iterator.remove()
                        found = true
                        break
                    }
                }
                if (!found) return false
            }
            return true
        }

        // ========================================
        // 假覆盖创建
        // ========================================

        /**
         * 检查所有描述符是否有相同的包含声明
         */
        private fun allHasSameContainingDeclaration(notOverridden: Collection<CallableMemberDescriptor>): Boolean {
            if (notOverridden.size < 2) return true
            val containingDeclaration = notOverridden.first().containingDeclaration
            return notOverridden.all { it.containingDeclaration == containingDeclaration }
        }

        /**
         * 创建并绑定假覆盖
         *
         * 将未被覆盖的父类成员创建为假覆盖（fake override）
         *
         * @param current 当前类
         * @param notOverridden 未被覆盖的成员集合
         * @param strategy 覆盖策略
         */
        private fun createAndBindFakeOverrides(
            current: InheritableDescriptor,
            notOverridden: Collection<CallableMemberDescriptor>,
            strategy: OverridingStrategy
        ) {
            // 优化：如果所有成员来自同一个父类，直接创建假覆盖
            if (allHasSameContainingDeclaration(notOverridden)) {
                notOverridden.forEach { descriptor ->
                    createAndBindFakeOverride(setOf(descriptor), current, strategy)
                }
                return
            }

            // 否则，需要处理多个父类的情况
            val fromSuperQueue = LinkedList(notOverridden)
            while (fromSuperQueue.isNotEmpty()) {
                // 选择最大可见性的成员
                val notOverriddenFromSuper = findMemberWithMaxVisibility(fromSuperQueue)
                // 提取所有双向可覆盖的成员
                val overridables = extractMembersOverridableInBothWays(
                    notOverriddenFromSuper,
                    fromSuperQueue
                ) { descriptor ->
                    strategy.inheritanceConflict(notOverriddenFromSuper, descriptor)
                }
                // 为这组成员创建假覆盖
                createAndBindFakeOverride(overridables, current, strategy)
            }
        }

        /**
         * 创建并绑定单个假覆盖
         *
         * @param overridables 可覆盖的成员集合（可能来自多个父类）
         * @param current 当前类
         * @param strategy 覆盖策略
         */
        private fun createAndBindFakeOverride(
            overridables: Collection<CallableMemberDescriptor>,
            current: InheritableDescriptor,
            strategy: OverridingStrategy
        ) {
            val visibleOverridables = filterVisibleFakeOverrides(current, overridables)
            val allInvisible = visibleOverridables.isEmpty()
            val effectiveOverridden = if (allInvisible) overridables else visibleOverridables

            // 确定假覆盖的模态性和可见性
            val modality = determineModalityForFakeOverride(effectiveOverridden, current)
            val visibility = if (allInvisible) {
                DescriptorVisibilities.INVISIBLE_FAKE
            } else {
                DescriptorVisibilities.INHERITED
            }

            // 选择最具体的成员作为基础
            val mostSpecific = selectMostSpecificMember(effectiveOverridden) { it }

            // 复制并创建假覆盖
            val fakeOverride = mostSpecific.copy(
                current,
                modality,
                visibility,
                CallableMemberDescriptor.Kind.FAKE_OVERRIDE,
                false
            )

            // 设置覆盖关系
            strategy.setOverriddenDescriptors(fakeOverride, effectiveOverridden)

            require(fakeOverride.overriddenDescriptors.isNotEmpty()) {
                "Overridden descriptors should be set for ${CallableMemberDescriptor.Kind.FAKE_OVERRIDE}"
            }

            strategy.addFakeOverride(fakeOverride)
        }

        /**
         * 确定假覆盖的模态性
         *
         * 规则：
         * - 如果任何父类成员是 FINAL，返回 FINAL
         * - 如果全是 OPEN，返回 OPEN
         * - 如果全是 ABSTRACT，根据当前类的模态性决定
         * - 混合情况：取最小模态性
         */
        private fun determineModalityForFakeOverride(
            descriptors: Collection<CallableMemberDescriptor>,
            current: InheritableDescriptor
        ): Modality {
            var hasOpen = false
            var hasAbstract = false

            for (descriptor in descriptors) {
                when (descriptor.modality) {
                    Modality.FINAL -> return Modality.FINAL
                    Modality.SEALED -> error("Member cannot have SEALED modality: $descriptor")
                    Modality.OPEN -> hasOpen = true
                    Modality.ABSTRACT -> hasAbstract = true
                }
            }

            val transformAbstractToClassModality =
                current.modality != Modality.ABSTRACT && current.modality != Modality.SEALED

            return when {
                hasOpen && !hasAbstract -> Modality.OPEN
                !hasOpen && hasAbstract -> {
                    if (transformAbstractToClassModality) current.modality else Modality.ABSTRACT
                }

                else -> {
                    val allOverriddenDeclarations = descriptors
                        .flatMap { getOverriddenDeclarations(it) }
                        .toSet()
                    getMinimalModality(
                        filterOutOverridden(allOverriddenDeclarations),
                        transformAbstractToClassModality,
                        current.modality
                    )
                }
            }
        }

        /**
         * 获取最小模态性
         */
        private fun getMinimalModality(
            descriptors: Collection<CallableMemberDescriptor>,
            transformAbstractToClassModality: Boolean,
            classModality: Modality
        ): Modality {
            var result = Modality.ABSTRACT
            for (descriptor in descriptors) {
                val effectiveModality = if (transformAbstractToClassModality &&
                    descriptor.modality == Modality.ABSTRACT
                ) {
                    classModality
                } else {
                    descriptor.modality
                }
                if (effectiveModality < result) {
                    result = effectiveModality
                }
            }
            return result
        }
    }

    // ========================================
    // 实例方法
    // ========================================

    /**
     * 为函数组生成覆盖关系
     *
     * 处理当前类中的成员和父类成员之间的覆盖关系，并创建必要的假覆盖
     *
     * @param name 函数名（确保所有描述符有相同名称）
     * @param membersFromSupertypes 来自父类的成员
     * @param membersFromCurrent 当前类的成员
     * @param current 当前类
     * @param strategy 覆盖策略
     */
    fun <T : CallableMemberDescriptor> generateOverridesInFunctionGroup(
        name: Name,
        membersFromSupertypes: Collection<T>,
        membersFromCurrent: Collection<T>,
        current: InheritableDescriptor,
        strategy: OverridingStrategy
    ) {
        val notOverridden = LinkedHashSet(membersFromSupertypes)

        // 为当前类的每个成员提取并绑定覆盖关系
        for (fromCurrent in membersFromCurrent) {
            val bound = extractAndBindOverridesForMember(
                fromCurrent,
                membersFromSupertypes,
                current,
                strategy
            )
            notOverridden.removeAll(bound.toSet())
        }

        // 为未被覆盖的父类成员创建假覆盖
        createAndBindFakeOverrides(current, notOverridden, strategy)
    }

    /**
     * 创建类型检查器状态
     *
     * 用于在类型参数之间建立对应关系
     */
    private fun createTypeCheckerState(
        firstParameters: List<TypeParameterDescriptor>,
        secondParameters: List<TypeParameterDescriptor>
    ): TypeCheckerState {
        require(firstParameters.size == secondParameters.size) {
            "Should be the same number of type parameters: $firstParameters vs $secondParameters"
        }

        if (firstParameters.isEmpty()) {
            return OverridingUtilTypeSystemContext(
                null, equalityAxioms, cangjieTypeRefiner, cangjieTypePreparator, customSubtype
            ).newTypeCheckerState(true, true)
        }

        val matchingTypeConstructors = firstParameters.indices.associate { i ->
            firstParameters[i].typeConstructor to secondParameters[i].typeConstructor
        }

        return OverridingUtilTypeSystemContext(
            matchingTypeConstructors, equalityAxioms, cangjieTypeRefiner, cangjieTypePreparator, customSubtype
        ).newTypeCheckerState(true, true)
    }

    /**
     * 检查是否可以在不考虑外部条件的情况下覆盖
     *
     * 执行内部一致性检查：
     * - 基本兼容性（名称、参数数量等）
     * - 类型参数兼容性
     * - 值参数类型兼容性
     * - 返回类型兼容性（可选）
     */
    fun isOverridableByWithoutExternalConditions(
        superDescriptor: CallableDescriptor,
        subDescriptor: CallableDescriptor,
        checkReturnType: Boolean = false
    ): OverrideCompatibilityInfo {
        // 基本覆盖检查
        getBasicOverridabilityProblem(superDescriptor, subDescriptor)?.let { return it }

        val superValueParameters = compiledValueParameters(superDescriptor)
        val subValueParameters = compiledValueParameters(subDescriptor)
        val superTypeParameters = superDescriptor.typeParameters
        val subTypeParameters = subDescriptor.typeParametersNotExtend

        // 类型参数数量检查
        if (superTypeParameters.size != subTypeParameters.size) {
            for (i in superValueParameters.indices) {
                if (!CangJieTypeChecker.DEFAULT.equalTypes(superValueParameters[i], subValueParameters[i])) {
                    return OverrideCompatibilityInfo.incompatible("Type parameter number mismatch")
                }
            }
            return OverrideCompatibilityInfo.conflict("Type parameter number mismatch")
        }

        val typeCheckerState = createTypeCheckerState(superTypeParameters, subTypeParameters)

        // 检查类型参数等价性
        for (i in superTypeParameters.indices) {
            if (!areTypeParametersEquivalent(
                    superTypeParameters[i],
                    subTypeParameters[i],
                    typeCheckerState
                )
            ) {
                return OverrideCompatibilityInfo.incompatible("Type parameter bounds mismatch")
            }
        }

        // 检查值参数类型等价性
        for (i in superValueParameters.indices) {
            if (!areTypesEquivalent(
                    superValueParameters[i],
                    subValueParameters[i],
                    typeCheckerState
                )
            ) {
                return OverrideCompatibilityInfo.incompatible("Value parameter type mismatch")
            }
        }

        // 检查返回类型（如果需要）
        if (checkReturnType) {
            val superReturnType = superDescriptor.returnType
            val subReturnType = subDescriptor.returnType

            if (superReturnType != null && subReturnType != null) {
                val bothErrors = subReturnType.isError && superReturnType.isError
                if (!bothErrors && !AbstractTypeChecker.isSubtypeOf(
                        typeCheckerState,
                        subReturnType.unwrap(),
                        superReturnType.unwrap()
                    )
                ) {
                    return OverrideCompatibilityInfo.conflict("Return type mismatch")
                }
            }
        }

        return OverrideCompatibilityInfo.success()
    }

    /**
     * 检查是否可以覆盖（包含外部条件）
     *
     * 先执行内部检查，然后运行所有外部覆盖条件
     *
     * @param superDescriptor 父类描述符
     * @param subDescriptor 子类描述符
     * @param subClassDescriptor 子类描述符（可选）
     * @param checkReturnType 是否检查返回类型
     */
    fun isOverridableBy(
        superDescriptor: CallableDescriptor,
        subDescriptor: CallableDescriptor,
        subClassDescriptor: InheritableDescriptor?,
        checkReturnType: Boolean = false
    ): OverrideCompatibilityInfo {
        val basicResult = isOverridableByWithoutExternalConditions(
            superDescriptor,
            subDescriptor,
            checkReturnType
        )
        var wasSuccess = basicResult.result == OverrideCompatibilityInfo.Result.OVERRIDABLE

        // 第一轮：运行非 CONFLICTS_ONLY 条件
        for (externalCondition in EXTERNAL_CONDITIONS) {
            if (externalCondition.contract == ExternalOverridabilityCondition.Contract.CONFLICTS_ONLY) {
                continue
            }
            if (wasSuccess &&
                externalCondition.contract == ExternalOverridabilityCondition.Contract.SUCCESS_ONLY
            ) {
                continue
            }

            when (externalCondition.isOverridable(superDescriptor, subDescriptor, subClassDescriptor)) {
                ExternalOverridabilityCondition.Result.OVERRIDABLE -> wasSuccess = true
                ExternalOverridabilityCondition.Result.INCOMPATIBLE ->
                    return OverrideCompatibilityInfo.incompatible("External condition")

                ExternalOverridabilityCondition.Result.UNKNOWN -> {
                    // 继续下一个条件
                }
            }
        }

        if (!wasSuccess) return basicResult

        // 第二轮：运行 CONFLICTS_ONLY 条件
        for (externalCondition in EXTERNAL_CONDITIONS) {
            if (externalCondition.contract != ExternalOverridabilityCondition.Contract.CONFLICTS_ONLY) {
                continue
            }

            when (externalCondition.isOverridable(superDescriptor, subDescriptor, subClassDescriptor)) {
                ExternalOverridabilityCondition.Result.INCOMPATIBLE ->
                    return OverrideCompatibilityInfo.incompatible("External condition")

                ExternalOverridabilityCondition.Result.OVERRIDABLE ->
                    error("Contract violation in ${externalCondition::class.java.name}")

                ExternalOverridabilityCondition.Result.UNKNOWN -> {
                    // 继续下一个条件
                }
            }
        }

        return OverrideCompatibilityInfo.success()
    }

    /**
     * 检查是否可以覆盖（默认不检查返回类型）
     */
    fun isOverridableBy(
        superDescriptor: CallableDescriptor,
        subDescriptor: CallableDescriptor,
        subClassDescriptor: InheritableDescriptor?
    ): OverrideCompatibilityInfo = isOverridableBy(
        superDescriptor,
        subDescriptor,
        subClassDescriptor,
        false
    )

    /**
     * 提取并绑定成员的覆盖信息
     *
     * 处理当前类成员与父类成员的覆盖关系
     *
     * @return 被绑定的父类成员集合
     */
    private fun extractAndBindOverridesForMember(
        fromCurrent: CallableMemberDescriptor,
        descriptorsFromSuper: Collection<CallableMemberDescriptor>,
        current: InheritableDescriptor,
        strategy: OverridingStrategy
    ): Collection<CallableMemberDescriptor> {
        val bound = mutableListOf<CallableMemberDescriptor>()
        val overridden = SmartSet.create<CallableMemberDescriptor>()

        for (fromSupertype in descriptorsFromSuper) {
            val resultInfo = isOverridableBy(fromSupertype, fromCurrent, current)
            val result = resultInfo.result
            val isVisibleForOverride = isVisibleForOverride(fromCurrent, fromSupertype, false)

            when (result) {
                OverrideCompatibilityInfo.Result.OVERRIDABLE -> {
                    if (isVisibleForOverride) {
                        overridden.add(fromSupertype)
                    }
                    bound.add(fromSupertype)
                }

                OverrideCompatibilityInfo.Result.STATIC_CONFLICT -> {
                    if (isVisibleForOverride) {
                        strategy.staticConflict(fromSupertype, fromCurrent, resultInfo.debugMessage)
                    }
                    bound.add(fromSupertype)
                }

                OverrideCompatibilityInfo.Result.CONFLICT -> {
                    if (isVisibleForOverride) {
                        strategy.overrideConflict(fromSupertype, fromCurrent)
                    }
                    bound.add(fromSupertype)
                }

                OverrideCompatibilityInfo.Result.INCOMPATIBLE -> {
                    // 不兼容，不进行任何操作
                }
            }
        }

        strategy.setOverriddenDescriptors(fromCurrent, overridden)
        return bound
    }

    // ========================================
    // 覆盖兼容性信息
    // ========================================

    /**
     * 覆盖兼容性信息
     *
     * 表示两个描述符之间的覆盖关系检查结果
     */
    data class OverrideCompatibilityInfo(
        val result: Result,
        val debugMessage: String
    ) {
        companion object {
            private val SUCCESS = OverrideCompatibilityInfo(Result.OVERRIDABLE, "SUCCESS")

            fun success(): OverrideCompatibilityInfo = SUCCESS

            fun incompatible(debugMessage: String): OverrideCompatibilityInfo =
                OverrideCompatibilityInfo(Result.INCOMPATIBLE, debugMessage)

            fun staticConflict(debugMessage: String): OverrideCompatibilityInfo =
                OverrideCompatibilityInfo(Result.STATIC_CONFLICT, debugMessage)

            fun conflict(debugMessage: String): OverrideCompatibilityInfo =
                OverrideCompatibilityInfo(Result.CONFLICT, debugMessage)
        }

        override fun toString(): String = "$result: $debugMessage"

        /**
         * 覆盖结果枚举
         */
        enum class Result {
            /** 可以被覆盖 */
            OVERRIDABLE,

            /** 不兼容，无法覆盖 */
            INCOMPATIBLE,

            /** 冲突，需要显式解决 */
            CONFLICT,

            /** 静态冲突 */
            STATIC_CONFLICT
        }
    }
}

// ========================================
// 扩展函数
// ========================================

/**
 * 从集合中选择每个可覆盖组中最具体的成员
 *
 * @param H 句柄类型，包装 CallableDescriptor
 * @param descriptorByHandle 句柄到描述符的转换函数
 */
fun <H : Any> Collection<H>.selectMostSpecificInEachOverridableGroup(
    descriptorByHandle: H.() -> CallableDescriptor
): Collection<H> {
    if (size <= 1) return this

    val queue = LinkedList<H>(this)
    val result = SmartSet.create<H>()

    while (queue.isNotEmpty()) {
        val nextHandle: H = queue.first()
        val conflictedHandles = SmartSet.create<H>()

        // 提取双向可覆盖的组
        val overridableGroup = OverridingUtil.extractMembersOverridableInBothWays(
            nextHandle,
            queue,
            descriptorByHandle
        ) { conflictedHandles.add(it) }

        // 如果只有一个成员且没有冲突，直接添加
        if (overridableGroup.size == 1 && conflictedHandles.isEmpty()) {
            result.add(overridableGroup.single())
            continue
        }

        // 选择最具体的成员
        val mostSpecific = OverridingUtil.selectMostSpecificMember(
            overridableGroup,
            descriptorByHandle
        )
        val mostSpecificDescriptor = mostSpecific.descriptorByHandle()

        // 过滤出不够具体的成员
        overridableGroup.filterNotTo(conflictedHandles) {
            OverridingUtil.isMoreSpecific(mostSpecificDescriptor, it.descriptorByHandle())
        }

        // 添加冲突和最具体的成员
        if (conflictedHandles.isNotEmpty()) {
            result.addAll(conflictedHandles)
        }
        result.add(mostSpecific)
    }

    return result
}

/**
 * 查找最顶层的被覆盖描述符
 *
 * 使用 DFS 遍历覆盖链，找到所有没有被进一步覆盖的描述符
 */
fun <D : CallableDescriptor> D.findTopMostOverriddenDescriptors(): List<D> {
    return DFS.dfs(
        listOf(this),
        { current -> current.overriddenDescriptors },
        object : DFS.CollectingNodeHandler<CallableDescriptor, CallableDescriptor, ArrayList<D>>(
            ArrayList<D>()
        ) {
            override fun afterChildren(current: CallableDescriptor) {
                if (current.overriddenDescriptors.isEmpty()) {
                    @Suppress("UNCHECKED_CAST")
                    result.add(current as D)
                }
            }
        }
    )
}

/**
 * 查找原始的最顶层被覆盖描述符
 *
 * 返回所有最顶层描述符的原始版本（去重）
 */
fun <D : CallableDescriptor> D.findOriginalTopMostOverriddenDescriptors(): Set<D> {
    return findTopMostOverriddenDescriptors().mapTo(LinkedHashSet<D>()) {
        @Suppress("UNCHECKED_CAST")
        (it.original as D)
    }
}